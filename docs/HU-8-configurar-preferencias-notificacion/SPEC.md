# SPEC — Configuración de preferencias de notificación

---

## Ficha de la historia

| Campo | Valor |
|---|---|
| ID | HU-8 |
| Sprint | 2 |
| Prioridad MoSCoW | Must Have |
| Estado | Completada |
| Épica | Autenticación / Gestión de perfil |
| CU asociado | CU-08 |
| Autor | Juan Diego Triana Mejia |
| Creada | 2026-05-20 |
| Última revisión | 2026-05-24 |
| Versión de esta spec | 1.0 |

**Trazabilidad:**

| Tipo | ID | Descripción |
|---|---|---|
| Requerimiento funcional | RF-07 | Configuración de canales y tipos de notificación |
| Escenario de calidad | EC-12 | Trazabilidad de PREFERENCIAS_NOTIFICACION_ACTUALIZADAS |
| Historia relacionada | HU-41 | El despachador de notificaciones usa estas preferencias para filtrar envíos |

---

## Historia de usuario

**Como** inversionista autenticado,
**quiero** configurar mis preferencias de notificación (canales activos y tipos de eventos),
**para** recibir solo las notificaciones que me interesan, por los canales que prefiero.

---

## Motivación y contexto

### Por qué existe esta historia

El sistema envía notificaciones por múltiples canales (Email, SMS, WhatsApp) y para múltiples tipos de eventos (ORDENES, MERCADO, SEGURIDAD). Esta historia permite al inversionista controlar:
1. Si las notificaciones están activas globalmente (`notificacionesActivas` — master switch).
2. Qué canales individuales están activos (`notificacionEmail`, `notificacionSms`, `notificacionWhatsapp`).
3. Qué tipos de eventos recibe (`tiposNotificacion` como lista).

---

## Actores y precondiciones

### Actores

| Actor | Rol en el sistema | Participación en este flujo |
|---|---|---|
| Inversionista autenticado | `INVERSIONISTA` | Iniciador — actualiza sus preferencias de notificación |
| `PerfilService` | Módulo `autenticacion` | Persiste las preferencias en `usuario` |
| `AuditLogService` | Módulo `trazabilidad` (vía `IAuditLog`) | Registra evento PREFERENCIAS_NOTIFICACION_ACTUALIZADAS |

### Precondiciones

- JWT válido en cabecera `Authorization: Bearer`.
- Existe `inversionista` vinculado al usuario del JWT.

### Postcondiciones

- Campos `notificaciones_activas`, `notificacion_email`, `notificacion_sms`, `notificacion_whatsapp`, `tipos_notificacion` actualizados en `usuario` (normalización 3NF — estos campos ya no están en `inversionista`).
- Evento `PREFERENCIAS_NOTIFICACION_ACTUALIZADAS` registrado en auditoría.

---

## Flujo principal

1. Usuario modifica sus preferencias en la sección de notificaciones de `/perfil`.
2. Frontend envía `PUT /api/perfil/preferencias/notificaciones` con `PreferenciasNotificacionDTO` y JWT.

**Backend — `PerfilService.actualizarPreferenciasNotificacion(correo, dto)`:**

3. Spring Security extrae `correo` del JWT.
4. `usuarioRepository.findByCorreo` → carga `Usuario`.
5. Actualiza `usuario.notificacionesActivas = dto.notificacionesActivas` (default: `true`).
6. Actualiza `usuario.notificacionEmail = dto.notificacionEmail`.
7. Actualiza `usuario.notificacionSms = dto.notificacionSms`.
8. Actualiza `usuario.notificacionWhatsapp = dto.notificacionWhatsapp`.
9. Si `dto.tiposNotificacion != null`: convierte la lista a CSV y actualiza `usuario.tiposNotificacion`.
10. `usuarioRepository.save(usuario)`.
11. `IAuditLog.registrar(PREFERENCIAS_NOTIFICACION_ACTUALIZADAS, correo, "Preferencias de notificación actualizadas")`.
12. Responde `200 OK` con `RespuestaDTO{mensaje: "Preferencias de notificación actualizadas"}`.

---

## Flujos de error

### Error 1 — No autenticado

| Campo | Valor |
|---|---|
| Condición | JWT ausente, inválido o expirado |
| HTTP | 401 Unauthorized |
| Evento de auditoría | Ninguno |

### Error 2 — Error técnico genérico

| Campo | Valor |
|---|---|
| Condición | Falla BD u otro error inesperado |
| HTTP | 500 Internal Server Error |
| Cuerpo | `RespuestaDTO{error: "Error interno del servidor"}` |
| Evento de auditoría | Ninguno |

---

## Contrato de API

### Endpoint — `PUT /api/perfil/preferencias/notificaciones`

```yaml
PUT /api/perfil/preferencias/notificaciones:
  summary: Actualiza las preferencias de notificación del inversionista
  security:
    - bearerAuth: []
  requestBody:
    required: true
    content:
      application/json:
        schema:
          $ref: '#/components/schemas/PreferenciasNotificacionDTO'
        example:
          notificacionesActivas: true
          notificacionEmail: true
          notificacionSms: false
          notificacionWhatsapp: false
          tiposNotificacion: ["ORDENES", "SEGURIDAD"]
  responses:
    '200':
      description: Preferencias actualizadas exitosamente
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'
          example:
            mensaje: "Preferencias de notificación actualizadas"
    '401':
      description: No autenticado
    '500':
      description: Error interno del servidor
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'

components:
  schemas:
    PreferenciasNotificacionDTO:
      type: object
      properties:
        notificacionesActivas:
          type: boolean
          default: true
          description: "Master switch — si false, ningún canal envía notificaciones"
        notificacionEmail:
          type: boolean
          default: false
        notificacionSms:
          type: boolean
          default: false
        notificacionWhatsapp:
          type: boolean
          default: false
        tiposNotificacion:
          type: array
          items:
            type: string
          description: "Opcional. Valores: ORDENES, MERCADO, SEGURIDAD. Se almacena como CSV"
          nullable: true
```

---

## Modelo de datos

### Campos en tabla `usuario` (actualizados en HU-8 — normalización 3NF)

| Campo | Tipo | Descripción |
|---|---|---|
| `notificaciones_activas` | `BOOLEAN NOT NULL DEFAULT TRUE` | Master switch de notificaciones |
| `notificacion_email` | `BOOLEAN NOT NULL DEFAULT TRUE` | Canal email habilitado |
| `notificacion_sms` | `BOOLEAN NOT NULL DEFAULT FALSE` | Canal SMS habilitado |
| `notificacion_whatsapp` | `BOOLEAN NOT NULL DEFAULT FALSE` | Canal WhatsApp habilitado |
| `tipos_notificacion` | `VARCHAR(500)` | CSV de tipos de evento: `"ORDENES,MERCADO,SEGURIDAD"` |

**Nota de migración:** Estos campos fueron movidos de `inversionista` a `usuario` en la normalización 3NF. Al estar en `usuario`, son accesibles por todos los módulos del sistema (incluido `DespachadorNotificaciones`) sin cruzar fronteras hacia el perfil de negocio de `inversionista`.

---

## Módulos y arquitectura

### Módulos involucrados

| Módulo | Rol | Componentes específicos |
|---|---|---|
| `autenticacion` | Coordinador del flujo | `PerfilController`, `PerfilService` |
| `trazabilidad` | Registro de eventos | `AuditLogService` (impl. de `IAuditLog`) |

### Escenarios de calidad y tácticas materializadas

| EC | Táctica | Cómo se materializa en HU-8 |
|---|---|---|
| EC-12 | Audit Trail | `PREFERENCIAS_NOTIFICACION_ACTUALIZADAS` registrado |

---

## Eventos y efectos transversales

### Eventos de auditoría emitidos

| Evento (`TipoEvento`) | Cuándo se emite | Datos en `detalle` |
|---|---|---|
| `PREFERENCIAS_NOTIFICACION_ACTUALIZADAS` | Actualización exitosa | `"Preferencias de notificación actualizadas"` |

---

## Criterios de verificación

### Escenarios de aceptación (Gherkin)

```gherkin
Funcionalidad: Configuración de preferencias de notificación

  Antecedentes:
    Dado que "ana@test.com" tiene JWT válido y perfil existente

  Escenario: Actualización de preferencias de notificación
    Cuando se envía PUT /api/perfil/preferencias/notificaciones con notificacionEmail=true, notificacionSms=false, tiposNotificacion=["ORDENES"]
    Entonces el sistema responde 200 OK
    Y usuario.notificacion_email es true
    Y usuario.notificacion_sms es false
    Y usuario.tipos_notificacion es "ORDENES"
    Y se emite evento PREFERENCIAS_NOTIFICACION_ACTUALIZADAS en auditoría

  Escenario: Master switch desactiva todas las notificaciones
    Cuando se envía PUT con notificacionesActivas=false
    Entonces usuario.notificaciones_activas es false

  Escenario: Sin JWT — 401
    Cuando se envía PUT sin Authorization
    Entonces el sistema responde 401 Unauthorized
```

---

## Definición de terminado

- [x] `PUT /api/perfil/preferencias/notificaciones` actualiza los 5 campos correctamente.
- [x] `tiposNotificacion` almacenado como CSV en BD.
- [x] Evento `PREFERENCIAS_NOTIFICACION_ACTUALIZADAS` en auditoría.
- [x] Sin JWT responde 401.
- [x] `docs/PROGRESO.md` marcado con ✅ para HU-8.

---

## Historial de cambios

| Versión | Fecha | Descripción | Razón |
|---|---|---|---|
| 1.0 | 2026-05-24 | Refactorización a estructura SDD del proyecto. | Unificación de todos los SPEC.md bajo plantilla canónica SDD del proyecto |
