# Tareas de implementación — HU-17: Colocación de orden Market

> Estado: Completada. Todas las tareas fueron ejecutadas durante Sprint 3.

---

## Backend

### Entidades y repositorios

- [x] **T1 — Entity `Activo`**
  - Tabla `activo`: `id` (BIGSERIAL PK), `ticker` VARCHAR UNIQUE, `nombre`, `mercado`, `tipo`.
  - Método en `ActivoRepository`: `findByTickerIgnoreCase(ticker)`.

- [x] **T2 — Entity `Orden`**
  - Campos: `id`, `inversionistaId`, `activoId`, `parametroComisionId`, `tipoOrden`, `lado`, `cantidad`, `precioLimite`, `precioStop`, `precioEjecucion`, `montoBase`, `porcentajeComision`, `montoComision`, `montoPlatforma`, `montoComisionista`, `estado`, `alpacaOrderId`, `ipOrigen`, `fechaCreacion`, `fechaEjecucion`.

- [x] **T3 — `OrdenRepository`**
  - `findByInversionistaIdAndEstadoIn(id, List<EstadoOrden>)` para órdenes activas.
  - `findByInversionistaIdAndFechaCreacionBetween(id, desde, hasta)` para historial.
  - `sumMontoComisionByInversionistaIdAndEstado(id, estado)` para totales.

### DTOs

- [x] **T4 — `CrearOrdenRequestDTO`**
  - Campos: `activoId` (Long, requerido), `tipoOrden` (String enum), `lado` (String enum), `cantidad` (Double > 0), `precioLimite` (Double nullable), `precioStop` (Double nullable).
  - Validaciones `@NotNull`, `@Positive` con Bean Validation.

- [x] **T5 — `ResumenComisionDTO`**
  - Campos: `ticker`, `tipoOrden`, `lado`, `cantidad`, `precioEstimado`, `montoBase`, `porcentajeComision`, `montoComision`, `montoPlataforma`, `montoComisionista`, `totalADebitar`, `totalARecibir`, `mercadoAbierto`, `advertencia`.

- [x] **T6 — `OrdenDTO`**
  - Campos: `id`, `ticker`, `tipoOrden`, `lado`, `cantidad`, `precioEjecucion`, `montoComision`, `estado`, `alpacaOrderId`, `fechaCreacion`, `fechaEjecucion`.

### Excepciones

- [x] **T7 — `FondosInsuficientesException` → 402**
  - Registrada en `GlobalExceptionHandler`.

- [x] **T8 — `HoldingInsuficienteException` → 422**
  - Registrada en `GlobalExceptionHandler`.

- [x] **T9 — `SimboloInvalidoException` → 400**
  - Registrada en `GlobalExceptionHandler`.

- [x] **T10 — `OrdenInvalidaException` → 400**
  - Registrada en `GlobalExceptionHandler` (usada también en HU-18 y HU-19).

### Lógica de negocio

- [x] **T11 — `OrdenService.previsualizarOrden(dto, correo)`**
  - Obtiene precio actual; calcula comisión usando `IGestorParametros`; retorna `ResumenComisionDTO`.
  - No persiste ninguna entidad.

- [x] **T12 — `OrdenService.crearOrden(dto, correo)`**
  - Paso 1: validar usuario puede operar (`IConsultaInversionista`).
  - Paso 2: obtener precio actual de mercado.
  - Paso 3: calcular comisión.
  - Paso 4: verificar fondos (compra) o holding (venta).
  - Paso 5: persistir `Orden` con estado `PENDIENTE`.
  - Paso 6: reservar fondos si compra.
  - Paso 7: audit `ORDEN_PENDIENTE`.
  - Paso 8: si mercado abierto → enviar a Alpaca o ejecutar internamente.
  - Paso 9: si mercado cerrado → `EN_COLA`.
  - Paso 10: audit del nuevo estado; notificar.

- [x] **T13 — `SaldoService.reservarFondos(inversionistaId, monto)`**
  - `saldoDisponible -= monto`; `fondosReservados += monto`.
  - Lanza `FondosInsuficientesException` si `saldoDisponible < monto`.

- [x] **T14 — `SaldoService.liberarFondos(inversionistaId, monto)`**
  - `fondosReservados -= monto`; `saldoDisponible += monto`.
  - Usado al cancelar orden (HU-21).

- [x] **T15 — `HoldingService.actualizarHoldingCompra(inversionistaId, activoId, cantidad, precioEjecucion)`**
  - Si existe holding: recalcular `precioPromedioCompra` (media ponderada); incrementar `cantidad`.
  - Si no existe: crear nuevo `Holding`.

- [x] **T16 — `HoldingService.actualizarHoldingVenta(inversionistaId, activoId, cantidad)`**
  - Decrementar `cantidad`; no borrar el registro aunque quede en 0.

- [x] **T17 — `AlpacaAdapter.crearOrden(accountId, simbolo, tipo, lado, cantidad, precioLimite, precioStop)`**
  - Para Market: `{"type": "market", "side": "buy/sell", "qty": ...}`.
  - Retorna el `alpacaOrderId` o null si fallo.

### Controlador

- [x] **T18 — `OrdenController`**
  - `POST /api/ordenes/previsualizar` → `OrdenService.previsualizarOrden`.
  - `POST /api/ordenes` → `OrdenService.crearOrden`.
  - Ambos `@PreAuthorize("hasRole('INVERSIONISTA')")`.

---

## Frontend Angular

- [x] **T19 — `OrdenService.previsualizarOrden(dto)` en Angular**
  - `POST /api/ordenes/previsualizar` con JWT.

- [x] **T20 — `OrdenService.crearOrden(dto)` en Angular**
  - `POST /api/ordenes` con JWT.

- [x] **T21 — Modal de nueva orden**
  - Selección de símbolo (o activo), tipo MARKET, lado, cantidad.
  - Botón "Previsualizar" → llama previsualizar y muestra desglose de comisión.
  - Botón "Confirmar" → llama crearOrden.
  - Muestra mensaje de éxito o error tras la operación.

- [x] **T22 — Manejo de errores en UI**
  - 402: "Fondos insuficientes para ejecutar la orden".
  - 422: "No tienes suficientes acciones para vender".
  - 403: "Tu cuenta no puede realizar operaciones en este momento".

---

## Pruebas

- [x] **T23 — Prueba: previsualización sin crear orden**
  - `POST /api/ordenes/previsualizar` → verificar que no hay nueva fila en tabla `orden`.

- [x] **T24 — Prueba: orden MARKET/COMPRA exitosa**
  - Verificar estado `ENVIADA` y `fondos_reservados` actualizado.

- [x] **T25 — Prueba: fondos insuficientes → 402**

- [x] **T26 — Prueba: venta sin holding → 422**

- [x] **T27 — Prueba: mercado cerrado → EN_COLA**
  - Configurar `app.mercado.sandbox-siempre-abierto=false` y verificar.

- [x] **T28 — Prueba: sin JWT → 401**

---

## Documentación

- [x] **T29 — Actualizar `docs/PROGRESO.md`**
  - Marcar HU-17 con ✅.
