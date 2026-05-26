# Tareas de implementación — HU-16: Consulta de saldo y comisiones

> Estado: Completada. Todas las tareas fueron ejecutadas durante Sprint 2.

---

## Backend

- [x] **T1 — Entity `CuentaFondos` con PK compartida**
  - PK: `inversionista_id` (no auto-increment).
  - Campos: `saldoDisponible` (DECIMAL 15,4), `fondosReservados` (DECIMAL 15,4).
  - FK a `Inversionista`.

- [x] **T2 — `CuentaFondosRepository`**
  - Hereda de `CrudRepository<CuentaFondos, Long>` (PK = `inversionista_id`).
  - Método `findById(inversionistaId)`.

- [x] **T3 — `SaldoService.obtenerOCrear(inversionistaId)`**
  - Si existe en BD: retorna la entidad.
  - Si no existe: crea `CuentaFondos` con `saldo = 0`, `fondosReservados = 0` y persiste.
  - **Requiere `@Transactional` (NO readOnly) en el método que lo llame.**

- [x] **T4 — `SaldoService.obtenerSaldoDTO(inversionistaId)`**
  - Llama a `obtenerOCrear`.
  - Suma `monto_comision` de todas las órdenes EJECUTADAS del inversionista.
  - Retorna `SaldoDTO`.

- [x] **T5 — `SaldoDTO`**
  - Campos: `saldoDisponible`, `fondosReservados`, `totalComisionesPagadas`.

- [x] **T6 — `OrdenRepository.sumMontoComisionByInversionistaAndEstado(id, estado)`**
  - Query JPQL: `SELECT COALESCE(SUM(o.montoComision), 0) FROM Orden o WHERE o.inversionistaId = :id AND o.estado = :estado`.
  - Retorna `BigDecimal`.

- [x] **T7 — `PortafolioService.obtenerSaldo(correo)`**
  - `@Transactional` (no readOnly) para permitir el INSERT de `obtenerOCrear`.
  - Carga inversionista, delega a `SaldoService.obtenerSaldoDTO`.

- [x] **T8 — `PortafolioController.saldo()`**
  - `GET /api/portafolio/saldo`.
  - `@PreAuthorize("hasRole('INVERSIONISTA')")`.

- [x] **T9 — `PortafolioService.depositar(correo, monto)` (sandbox)**
  - Obtiene `CuentaFondos`; `saldoDisponible += monto`; persiste.

- [x] **T10 — `PortafolioController.depositar(monto)`**
  - `POST /api/portafolio/depositar?monto=X`.
  - Solo sandbox; no requiere protección de perfil prod por ahora.

- [x] **T11 — `PortafolioService.sincronizar(correo)`**
  - Llama a `AlpacaAdapter.obtenerSaldoCuenta(alpacaAccountId)`.
  - Actualiza `saldoDisponible` con el valor retornado por Alpaca.

- [x] **T12 — `PortafolioController.sincronizar()`**
  - `POST /api/portafolio/sincronizar`.

---

## Frontend Angular

- [x] **T13 — `PortafolioService.getSaldo()` en Angular**
  - `GET /api/portafolio/saldo` con JWT.
  - Retorna `Observable<SaldoDTO>`.

- [x] **T14 — Sección de saldo en el dashboard**
  - Tarjetas con: "Saldo disponible", "Fondos reservados", "Comisiones pagadas".
  - Formato moneda USD con 2 decimales.

- [x] **T15 — Botón "Depositar fondos" (sandbox)**
  - Input de monto + botón que llama `POST /api/portafolio/depositar?monto=X`.
  - Actualiza las tarjetas de saldo tras el depósito.

- [x] **T16 — Manejo de errores en UI**
  - 401 → redirigir a login.

---

## Pruebas

- [x] **T17 — Prueba manual: saldo inicial**
  - Usuario nuevo → `saldoDisponible = 0`, `fondosReservados = 0`, `totalComisionesPagadas = 0`.

- [x] **T18 — Prueba manual: depósito sandbox**
  - `POST /api/portafolio/depositar?monto=10000` → verificar que `saldoDisponible` aumenta.

- [x] **T19 — Prueba manual: fondos reservados tras orden**
  - Colocar orden de compra → verificar que `fondosReservados` aumenta en el `totalADebitar`.

- [x] **T20 — Prueba de bug: `@Transactional` readOnly**
  - Verificar que `GET /api/portafolio/saldo` no lanza `PSQLException: no se puede ejecutar INSERT en transacción de sólo lectura` (bug corregido el 2026-05-06).

---

## Documentación

- [x] **T21 — Actualizar `docs/PROGRESO.md`**
  - Marcar HU-16 con ✅ en la tabla del sprint.
