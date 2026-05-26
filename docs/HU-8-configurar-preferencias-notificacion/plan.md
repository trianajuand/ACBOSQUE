# Plan de implementación — HU-8: Configurar preferencias de notificación

## Contexto

El sistema envía notificaciones por tres canales (Email, SMS, WhatsApp) y por distintos tipos de evento (ORDENES, MERCADO, SEGURIDAD). Esta historia permite al inversionista controlar qué canales están activos, si las notificaciones están habilitadas globalmente y qué tipos de eventos quiere recibir. Los cambios afectan directamente al `DespachadorNotificaciones` (HU-41) que consulta estas preferencias antes de enviar.

---

## Estado

**Completada** — implementación en `PerfilController` + `PerfilService`. Campos migrados a tabla `usuario` en normalización 3NF.

---

## Decisiones de diseño

| Decisión | Justificación |
|---|---|
| Preferencias de notificación en tabla `usuario`, no en `inversionista` | Normalización 3NF: los canales de contacto son accesibles por todos los módulos (incluido `DespachadorNotificaciones` en módulo `integracion`) sin cruzar la frontera del módulo `autenticacion` |
| `notificacionesActivas` como master switch | Un solo campo `false` deshabilita todos los canales sin modificar los flags individuales. Al reactivar, los flags individuales retoman su valor |
| `tiposNotificacion` como CSV en BD | Evita tabla de unión para un dato de baja cardinalidad (máx 3 tipos). Consistente con `interesesMercado` en `inversionista` |
| Endpoint separado de `PUT /api/perfil` | Las preferencias de notificación tienen semántica diferente a los datos personales. Separar endpoints facilita permisos y evolución futura |
| Respuesta `RespuestaDTO{mensaje}` sin devolver el objeto completo | El frontend ya tiene el estado previo y solo necesita confirmar que el save fue exitoso |

---

## Módulos involucrados

| Módulo | Componente | Rol |
|---|---|---|
| `autenticacion` | `PerfilController` | Recibe `PUT /api/perfil/preferencias/notificaciones` |
| `autenticacion` | `PerfilService` | Persiste los 5 campos de preferencia en `usuario` |
| `autenticacion` | `PreferenciasNotificacionDTO` | DTO de entrada con los 5 campos de preferencia |
| `autenticacion` | `Usuario` (entidad) | Contiene los 5 campos: `notificacionesActivas`, `notificacionEmail`, `notificacionSms`, `notificacionWhatsapp`, `tiposNotificacion` |
| `trazabilidad` | `AuditLogService` (vía `IAuditLog`) | Registra `PREFERENCIAS_NOTIFICACION_ACTUALIZADAS` |
| `integracion` | `DespachadorNotificaciones` | Consume estas preferencias en HU-41 para filtrar envíos |

---

## Flujo de implementación

```
PUT /api/perfil/preferencias/notificaciones
  → JwtFilter valida token, SecurityContext contiene correo
  → PerfilController.actualizarPreferenciasNotificacion(PreferenciasNotificacionDTO dto)
    → extrae correo de Authentication
    → delega a PerfilService.actualizarPreferenciasNotificacion(correo, dto)
      → usuarioRepository.findByCorreo(correo)   // 401 ya garantiza que existe
      → usuario.notificacionesActivas = dto.notificacionesActivas (default true)
      → usuario.notificacionEmail = dto.notificacionEmail
      → usuario.notificacionSms = dto.notificacionSms
      → usuario.notificacionWhatsapp = dto.notificacionWhatsapp
      → if dto.tiposNotificacion != null
          → usuario.tiposNotificacion = String.join(",", dto.tiposNotificacion)
      → usuarioRepository.save(usuario)
      → IAuditLog.registrar(PREFERENCIAS_NOTIFICACION_ACTUALIZADAS, correo, "Preferencias de notificación actualizadas")
      → return RespuestaDTO{mensaje: "Preferencias de notificación actualizadas"}
    → 200 OK con RespuestaDTO
```

---

## Modelo de datos (tabla `usuario`)

| Columna | Tipo SQL | Default | Descripción |
|---|---|---|---|
| `notificaciones_activas` | `BOOLEAN NOT NULL` | `TRUE` | Master switch: si false ningún canal envía |
| `notificacion_email` | `BOOLEAN NOT NULL` | `TRUE` | Canal Email habilitado |
| `notificacion_sms` | `BOOLEAN NOT NULL` | `FALSE` | Canal SMS habilitado |
| `notificacion_whatsapp` | `BOOLEAN NOT NULL` | `FALSE` | Canal WhatsApp habilitado |
| `tipos_notificacion` | `VARCHAR(500)` | `NULL` | CSV de tipos: `"ORDENES,MERCADO,SEGURIDAD"` |

---

## Contrato resumido

| Verbo | URL | Auth | Cuerpo | Respuesta exitosa |
|---|---|---|---|---|
| PUT | `/api/perfil/preferencias/notificaciones` | Bearer JWT (INVERSIONISTA) | `PreferenciasNotificacionDTO` | 200 `RespuestaDTO{mensaje}` |

**Códigos de error:**
- `401` — JWT ausente, inválido o expirado
- `500` — error técnico genérico

---

## Escenarios de calidad cubiertos

| EC | Táctica | Materialización |
|---|---|---|
| EC-12 | Audit Trail | `PREFERENCIAS_NOTIFICACION_ACTUALIZADAS` registrado con correo y detalle |

---

## Relación con HU-41

El `DespachadorNotificaciones` (HU-41) consulta las preferencias antes de enviar cada notificación:
1. Comprueba `notificacionesActivas`. Si `false`, no envía por ningún canal.
2. Por cada canal activo (`notificacionEmail`, `notificacionSms`, `notificacionWhatsapp`), verifica si el tipo de evento está en `tiposNotificacion`.
3. Solo entonces invoca `EmailSender`, `SmsSender` o `WhatsAppSender`.

Esta historia provee la configuración; HU-41 la consume.

---

## Notas para el desarrollador

- Los valores válidos de `tiposNotificacion` son: `ORDENES`, `MERCADO`, `SEGURIDAD`. No hay validación enum en MVP — el frontend garantiza los valores.
- Si `dto.tiposNotificacion` viene como `null`, mantener el valor previo en BD (no sobreescribir con null).
- Si viene como lista vacía `[]`, almacenar como string vacío (significa "ningún tipo de evento").
- El campo `notificacionesActivas` tiene default `true` en el DTO; si el cliente no lo envía, Jackson lo deserializa como `true`.
