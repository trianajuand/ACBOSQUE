# Plan de implementación — HU-9: Configurar preferencias de operación

## Contexto

El inversionista puede preconfigurar su experiencia de trading: el tipo de orden que aparece seleccionado por defecto en el formulario de nueva orden (HU-17..20) y el modo de visualización de su portafolio (lista o gráfico, consumido en HU-15). Estos datos se guardan en `inversionista` porque son datos operativos del perfil financiero, no datos de contacto transversales.

---

## Estado

**Completada** — implementación en `PerfilController` + `PerfilService`. Campos en tabla `inversionista`.

---

## Decisiones de diseño

| Decisión | Justificación |
|---|---|
| Preferencias en tabla `inversionista`, no en `usuario` | Son datos operativos del rol de inversionista (relacionados con el comportamiento de trading), no datos de contacto ni seguridad |
| Ambos campos opcionales (nullable) | Permite actualización parcial: si el usuario solo quiere cambiar `tipoOrdenDefault`, no tiene que enviar `vistaPortafolio` |
| Valores como String en BD (VARCHAR), no como enum de BD | Flexibilidad en evolución del modelo sin migraciones de columna. La validación de valores válidos la hace Jackson al deserializar el enum Java |
| Respuesta `RespuestaDTO{mensaje}` sin devolver objeto completo | El frontend ya tiene el estado previo; solo necesita confirmación |
| Endpoint separado de datos personales y preferencias de notificación | Separación clara de responsabilidades; facilita pruebas y permisos específicos por endpoint |

---

## Módulos involucrados

| Módulo | Componente | Rol |
|---|---|---|
| `autenticacion` | `PerfilController` | Recibe `PUT /api/perfil/preferencias/operacion` |
| `autenticacion` | `PerfilService` | Carga inversionista, aplica cambios, persiste |
| `autenticacion` | `PreferenciasOperacionDTO` | DTO de entrada con 2 campos opcionales enum |
| `autenticacion` | `Inversionista` (entidad) | Contiene `tipoOrdenDefault` y `vistaPortafolio` |
| `trazabilidad` | `AuditLogService` (vía `IAuditLog`) | Registra `PREFERENCIAS_OPERACION_ACTUALIZADAS` |

---

## Flujo de implementación

```
PUT /api/perfil/preferencias/operacion
  → JwtFilter valida token, SecurityContext contiene correo
  → PerfilController.actualizarPreferenciasOperacion(PreferenciasOperacionDTO dto)
    → extrae correo de Authentication
    → delega a PerfilService.actualizarPreferenciasOperacion(correo, dto)
      → usuarioRepository.findByCorreo(correo)
      → inversionistaRepository.findById(usuario.id)
      → if dto.tipoOrdenDefault != null → inversionista.tipoOrdenDefault = dto.tipoOrdenDefault
      → if dto.vistaPortafolio != null → inversionista.vistaPortafolio = dto.vistaPortafolio
      → inversionistaRepository.save(inversionista)
      → IAuditLog.registrar(PREFERENCIAS_OPERACION_ACTUALIZADAS, correo, "Preferencias de operación actualizadas")
      → return RespuestaDTO{mensaje: "Preferencias de operación actualizadas"}
    → 200 OK con RespuestaDTO
```

---

## Modelo de datos (tabla `inversionista`)

| Columna | Tipo SQL | Default | Valores válidos |
|---|---|---|---|
| `tipo_orden_default` | `VARCHAR(50)` | `'MARKET'` | MARKET, LIMIT, STOP_LOSS, TAKE_PROFIT |
| `vista_portafolio` | `VARCHAR(50)` | `'LISTA'` | LISTA, GRAFICO |

---

## Contrato resumido

| Verbo | URL | Auth | Cuerpo | Respuesta exitosa |
|---|---|---|---|---|
| PUT | `/api/perfil/preferencias/operacion` | Bearer JWT (INVERSIONISTA) | `PreferenciasOperacionDTO` | 200 `RespuestaDTO{mensaje}` |

**Códigos de error:**
- `400` — valor de enum inválido (deserialización Jackson falla)
- `401` — JWT ausente, inválido o expirado
- `500` — error técnico genérico

---

## Escenarios de calidad cubiertos

| EC | Táctica | Materialización |
|---|---|---|
| EC-12 | Audit Trail | `PREFERENCIAS_OPERACION_ACTUALIZADAS` registrado con correo y detalle |

---

## Relación con otras historias

| HU | Relación |
|---|---|
| HU-15 (Portafolio) | Lee `vistaPortafolio` del perfil para decidir si mostrar LISTA o GRAFICO |
| HU-17..20 (Órdenes) | El formulario de nueva orden precarga `tipoOrdenDefault` como tipo seleccionado |

---

## Notas para el desarrollador

- `tipoOrdenDefault` y `vistaPortafolio` son campos Java con tipo `String` en la entidad (no enum de JPA) para evitar migraciones DDL al agregar nuevos tipos en el futuro.
- El DTO puede usar enum Java (`TipoOrden`, `VistaPortafolio`) que Jackson deserializa automáticamente. Si el valor no corresponde a ningún valor del enum, Jackson lanza `HttpMessageNotReadableException` → 400.
- Ambos campos null en el DTO es válido: el request no modifica nada y retorna 200 OK (idempotente).
