# Tareas de implementación — HU-30: Propuesta de orden para cliente

## Estado general: COMPLETADA

---

## Tareas de modelo y base de datos

- [x] **T1 — Crear entidad `PropuestaOrden` en `ordenes/model/`**
  - Campos: `id`, `inversionistaId`, `comisionistaId`, `activoId`, `tipoOrden`, `lado`, `cantidad`, `precioLimite`, `precioStop`, `montoBaseEstimado`, `montoComisionEstimado`, `estado`, `comentarioComisionista`, `comentarioInversionista`, `aprobadaEn`, `rechazadaEn`, `firmadaEn`, `ordenId`, `ipOrigen`, `fechaCreacion`, `fechaActualizacion`.
  - Estado inicial: `PENDIENTE_APROBACION`.

- [x] **T2 — Crear `PropuestaOrdenRepository` en `ordenes/repository/`**
  - Métodos: `findByInversionistaIdAndEstado(Long inversionistaId, String estado)`, `findByComisionistaIdAndEstado(Long comisionistaId, String estado)`.

- [x] **T3 — Crear `CrearPropuestaOrdenDTO` en `ordenes/dto/`**
  - Campos: `activoId` (Long, requerido), `tipoOrden` (String, requerido), `lado` (String, requerido), `cantidad` (Double, requerido, min 0.000001), `precioLimite` (BigDecimal, opcional), `precioStop` (BigDecimal, opcional), `comentarioComisionista` (String, opcional).
  - Validaciones con Bean Validation (`@NotNull`, `@Positive`).

---

## Tareas backend

- [x] **T4 — Implementar `IOrden.crearPropuestaOrden()` en `OrdenService`**
  - Método: `crearPropuestaOrden(Long comisionistaId, Long clienteId, CrearPropuestaOrdenDTO dto, String ipOrigen) → OrdenDTO`
  - Lógica:
    1. `IAsignacionComisionista.validarClienteAsignado(comisionistaId, clienteId)` — 403 si no asignado.
    2. Verificar que el estado de cuenta del cliente es `ACTIVA`.
    3. Buscar el `Activo` por `dto.activoId` — 400 si no existe.
    4. Calcular `montoBase = cantidad × precioCache` y `comision = montoBase × %`.
    5. Persistir `PropuestaOrden` con `estado = PENDIENTE_APROBACION`.
    6. `IAuditLog.registrar(PROPUESTA_ORDEN_CREADA, correoCliente, "creada por {correoComisionista}")`.
    7. Retornar `OrdenDTO` con los datos de la propuesta.

- [x] **T5 — Endpoint `POST /api/comisionista/clientes/{clienteId}/propuestas` en `ComisionistaController`**
  - Llamada: `IOrden.crearPropuestaOrden(comisionistaId, clienteId, dto, ipOrigen)`.
  - Retorna `200 OK` (o `201 Created`) con `RespuestaDTO.exito(OrdenDTO)`.

- [x] **T6 — Declarar `crearPropuestaOrden()` en la interfaz `IOrden`**

---

## Tareas frontend (Angular)

- [x] **T7 — Formulario de nueva propuesta en la vista del comisionista**
  - Campos: selector de activo (o campo de búsqueda), tipo de orden, lado (COMPRA/VENTA), cantidad, precio límite/stop (condicional según tipo), campo de comentario/recomendación.
  - Acción: botón "Proponer orden" que llama a `POST /api/comisionista/clientes/{id}/propuestas`.

- [x] **T8 — Mostrar confirmación tras crear la propuesta**
  - Mensaje de éxito con el estado `PENDIENTE_APROBACION`.

---

## Tareas de verificación

- [x] **T9 — Probar creación de propuesta con datos válidos**
  - Resultado esperado: 200/201 con `OrdenDTO.estado = "PENDIENTE_APROBACION"`.

- [x] **T10 — Verificar que no se reservaron fondos del cliente**
  - `GET /api/portafolio/saldo` del cliente no debe mostrar cambios.

- [x] **T11 — Probar con cliente no asignado**
  - Resultado esperado: 403 Forbidden.

- [x] **T12 — Verificar evento en auditoría**
  - Log debe contener `PROPUESTA_ORDEN_CREADA` con correo del cliente.

- [x] **T13 — Actualizar `docs/PROGRESO.md`**
  - Marcar HU-30 con ✅ en la tabla del Sprint 4.
