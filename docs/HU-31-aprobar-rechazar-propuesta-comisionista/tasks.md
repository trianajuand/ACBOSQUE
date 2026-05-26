# Tareas de implementación — HU-31: Aprobación o rechazo de propuesta del comisionista

## Estado general: COMPLETADA

---

## Tareas de modelo y DTOs

- [x] **T1 — Crear `DecisionPropuestaDTO` en `ordenes/dto/` (o `autenticacion/dto/`)**
  - Campos: `comentario` (String, opcional, nullable).
  - Usado como body en los endpoints de aprobar y rechazar.

---

## Tareas backend

- [x] **T2 — Implementar `IOrden.obtenerPropuestasPendientesInversionista()` en `OrdenService`**
  - Método: `obtenerPropuestasPendientesInversionista(Long inversionistaId) → List<OrdenDTO>`
  - Consulta: `PropuestaOrdenRepository.findByInversionistaIdAndEstado(inversionistaId, "PENDIENTE_APROBACION")`.
  - Retorna la lista mapeada a `OrdenDTO` con campos de propuesta (comentarioComisionista, comisionistaId).

- [x] **T3 — Implementar `IOrden.aprobarPropuesta()` en `OrdenService`**
  - Método: `aprobarPropuesta(Long propuestaId, Long inversionistaId, String comentario) → OrdenDTO`
  - Validaciones:
    1. Buscar `PropuestaOrden` por `propuestaId` — lanza `404` si no existe.
    2. Verificar `propuesta.inversionistaId == inversionistaId` — lanza `403` si no.
    3. Verificar `propuesta.estado == PENDIENTE_APROBACION` — lanza `400` si no.
  - Actualización: `estado = APROBADA`, `aprobadaEn = LocalDateTime.now()`, `comentarioInversionista = comentario`.
  - Auditoría: `IAuditLog.registrar(PROPUESTA_ORDEN_APROBADA, correo, "Propuesta {id} aprobada")`.

- [x] **T4 — Implementar `IOrden.rechazarPropuesta()` en `OrdenService`**
  - Método: `rechazarPropuesta(Long propuestaId, Long inversionistaId, String comentario) → OrdenDTO`
  - Mismas validaciones que T3.
  - Actualización: `estado = RECHAZADA`, `rechazadaEn = LocalDateTime.now()`, `comentarioInversionista = comentario`.
  - Auditoría: `IAuditLog.registrar(PROPUESTA_ORDEN_RECHAZADA, correo, "Propuesta {id} rechazada")`.

- [x] **T5 — Crear `PropuestaController` en `ordenes/controller/`**
  - Mapping base: `@RequestMapping("/api/propuestas")`
  - Endpoints:
    - `GET /api/propuestas` → `IOrden.obtenerPropuestasPendientesInversionista(inversionistaId)`.
    - `POST /api/propuestas/{propuestaId}/aprobar` → `IOrden.aprobarPropuesta(propuestaId, inversionistaId, comentario)`.
    - `POST /api/propuestas/{propuestaId}/rechazar` → `IOrden.rechazarPropuesta(propuestaId, inversionistaId, comentario)`.
  - Método privado `resolverInversionista(String correo)`: busca usuario y valida rol `INVERSIONISTA`.

- [x] **T6 — Declarar los tres métodos en la interfaz `IOrden`**
  - `obtenerPropuestasPendientesInversionista(Long inversionistaId)`
  - `aprobarPropuesta(Long propuestaId, Long inversionistaId, String comentario)`
  - `rechazarPropuesta(Long propuestaId, Long inversionistaId, String comentario)`

- [x] **T7 — Registrar endpoints en `SecurityConfig`**
  - Ruta `/api/propuestas/**` requiere rol `INVERSIONISTA`.

---

## Tareas frontend (Angular)

- [x] **T8 — Sección de propuestas pendientes en el dashboard del inversionista**
  - Lista con datos de la propuesta: símbolo, tipo de orden, lado, cantidad, precio límite/stop estimado, comisión estimada, comentario del comisionista.
  - Botones por fila: "Aprobar" y "Rechazar".

- [x] **T9 — Modal de confirmación con comentario opcional**
  - Al hacer clic en "Aprobar" o "Rechazar": modal con textarea opcional para comentario.
  - Al confirmar: llama al endpoint correspondiente con el JWT del inversionista.
  - Al completar: la propuesta desaparece de la lista de pendientes.

- [x] **T10 — Mostrar mensaje de resultado**
  - Éxito: "Propuesta aprobada / rechazada correctamente."
  - Error: mensaje en español según el código HTTP devuelto.

---

## Tareas de verificación

- [x] **T11 — Probar `GET /api/propuestas` con JWT de inversionista con propuestas pendientes**
  - Resultado esperado: lista con la propuesta en estado PENDIENTE_APROBACION.

- [x] **T12 — Probar aprobación exitosa (con y sin comentario)**
  - Resultado esperado: 200 OK, `estado = "APROBADA"`, `aprobadaEn` no nulo.

- [x] **T13 — Probar rechazo exitoso (con y sin comentario)**
  - Resultado esperado: 200 OK, `estado = "RECHAZADA"`, `rechazadaEn` no nulo.

- [x] **T14 — Probar aprobación de propuesta ya aprobada**
  - Resultado esperado: 400 Bad Request.

- [x] **T15 — Probar aprobación de propuesta de otro inversionista**
  - Resultado esperado: 403 Forbidden.

- [x] **T16 — Probar aprobación de propuesta inexistente**
  - Resultado esperado: 404 Not Found.

- [x] **T17 — Verificar eventos en auditoría**
  - Log debe contener `PROPUESTA_ORDEN_APROBADA` o `PROPUESTA_ORDEN_RECHAZADA` con correo del inversionista.

- [x] **T18 — Actualizar `docs/PROGRESO.md`**
  - Marcar HU-31 con ✅ en la tabla del Sprint 4.
