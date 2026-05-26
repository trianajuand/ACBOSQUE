# Tareas de implementación — HU-29: Consulta de órdenes de cliente asignado

## Estado general: COMPLETADA

---

## Tareas backend

- [x] **T1 — Endpoint `GET /api/comisionista/clientes/{clienteId}/ordenes/activas`**
  - Clase: `ComisionistaController.ordenesActivasCliente(correo, clienteId)`
  - Lógica:
    1. `resolverComisionista(correo)` — valida rol.
    2. `IAsignacionComisionista.validarClienteAsignado(comisionistaId, clienteId)` — 403 si no asignado.
    3. `IOrden.obtenerOrdenesActivas(clienteId)` — retorna órdenes activas.
  - Criterio: solo retorna órdenes del cliente especificado con estados activos.

- [x] **T2 — Endpoint `GET /api/comisionista/clientes/{clienteId}/ordenes/historial`**
  - Clase: `ComisionistaController.historialCliente(correo, clienteId)`
  - Lógica:
    1. `resolverComisionista(correo)` — valida rol.
    2. `IAsignacionComisionista.validarClienteAsignado(comisionistaId, clienteId)` — 403 si no asignado.
    3. `IOrden.obtenerHistorialOrdenes(clienteId)` — retorna historial completo.
  - Criterio: retorna el historial de todos los estados del cliente.

- [x] **T3 — Verificar que `IOrden.obtenerOrdenesActivas(Long clienteId)` soporta clienteId externo**
  - El método ya existe para el caso del inversionista propio; asegurar que acepta cualquier `clienteId` como parámetro.

---

## Tareas frontend (Angular)

- [x] **T4 — Agregar sección de órdenes activas del cliente en la vista del comisionista**
  - Muestra las órdenes activas del cliente seleccionado (mismo formato que la vista de órdenes del inversionista).

- [x] **T5 — Agregar sección de historial de órdenes del cliente**
  - Muestra el historial con las mismas columnas que el historial del inversionista.
  - Opcionalmente: filtros básicos de período y estado.

---

## Tareas de verificación

- [x] **T6 — Probar `GET /api/comisionista/clientes/{id}/ordenes/activas` con cliente asignado**
  - Resultado esperado: 200 OK con lista de órdenes activas del cliente.

- [x] **T7 — Probar `GET /api/comisionista/clientes/{id}/ordenes/historial` con cliente asignado**
  - Resultado esperado: 200 OK con historial del cliente.

- [x] **T8 — Probar ambos endpoints con cliente NO asignado**
  - Resultado esperado: 403 Forbidden.

- [x] **T9 — Verificar que el historial del cliente incluye propuestas enviadas por el comisionista**
  - Resultado esperado: las órdenes creadas por propuesta del comisionista (HU-32) aparecen en el historial del cliente.

- [x] **T10 — Actualizar `docs/PROGRESO.md`**
  - Marcar HU-29 con ✅ en la tabla del Sprint 4.
