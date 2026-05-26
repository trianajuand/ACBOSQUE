# Tareas de implementación — HU-26: Historial de órdenes filtrado por estado

## Estado general: COMPLETADA

---

## Tareas backend

- [x] **T1 — Agregar parámetro `estado` al endpoint `GET /api/ordenes/historial`**
  - Clase: `OrdenController.historial()`
  - Cambio: `@RequestParam(required = false) String estado`
  - Criterio: el parámetro es opcional; si no se envía, no filtra por estado.

- [x] **T2 — Implementar filtro por estado en `OrdenService`**
  - Método: `obtenerHistorialOrdenes(Long usuarioId, LocalDate desde, LocalDate hasta, String tipoOrden, String simbolo, String estado)`
  - Cambio: añadir condición `AND o.estado = :estado` si el parámetro no es nulo.
  - Criterio: el filtro se combina en AND con los filtros existentes de período, tipo y símbolo.

- [x] **T3 — Verificar que `OrdenDTO` incluye el campo `estado` en la respuesta**
  - Sin cambios requeridos si ya estaba presente en HU-22.

- [x] **T4 — Verificar manejo de enum inválido**
  - Criterio: enviar `estado=INVALIDO` debe retornar 400 (deserialización automática por Spring).

---

## Tareas frontend (Angular)

- [x] **T5 — Agregar selector de estado en el panel de historial del dashboard**
  - Componente: panel historial en `DashboardComponent` o componente dedicado de órdenes.
  - Opciones: PENDIENTE, ENVIADA, EN_COLA, EJECUTADA, CANCELADA, (Todos).
  - Criterio: al cambiar el selector, se recarga el historial con el nuevo filtro.

- [x] **T6 — Pasar el filtro `estado` al `ApiService.getHistorialOrdenes()`**
  - Cambio: incluir `estado` como query param en la URL construida por el servicio.

- [x] **T7 — Mostrar el estado en la tabla de historial**
  - Criterio: cada fila de la tabla muestra el estado de la orden con diferenciación visual (badge de color si se tiene).

---

## Tareas de verificación

- [x] **T8 — Probar filtro `estado=EJECUTADA` en Postman**
  - Resultado esperado: 200 con lista donde cada orden tiene `estado=EJECUTADA`.

- [x] **T9 — Probar filtro combinado `estado=CANCELADA&desde=2026-05-01`**
  - Resultado esperado: solo órdenes canceladas dentro del período.

- [x] **T10 — Probar `estado=INVALIDO`**
  - Resultado esperado: 400 Bad Request.

- [x] **T11 — Actualizar `docs/PROGRESO.md`**
  - Marcar HU-26 con ✅ en la tabla del Sprint 4.
