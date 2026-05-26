# Plan de implementación — HU-16: Consulta de saldo y comisiones

## Objetivo

Dar al inversionista visibilidad completa de su situación financiera: saldo disponible para operar, fondos bloqueados por órdenes pendientes y total de comisiones pagadas históricamente.

---

## Módulos involucrados

| Módulo | Componentes |
|---|---|
| `ordenes` | `PortafolioController`, `PortafolioService` (`obtenerSaldo`), `CuentaFondosRepository`, `OrdenRepository` |
| `integracion` | `AlpacaAdapter` (solo en `sincronizar`, no en consulta normal) |

---

## Estrategia general

1. `GET /api/portafolio/saldo` extrae correo del JWT.
2. `PortafolioService.obtenerSaldo(correo)` carga `cuenta_fondos` del inversionista.
3. Calcula `totalComisionesPagadas` sumando `orden.monto_comision` de todas las órdenes con estado `EJECUTADA`.
4. Retorna `SaldoDTO` con los tres campos.
5. `POST /api/portafolio/depositar` (sandbox) incrementa `saldo_disponible` sin afectar `fondos_reservados`.
6. `POST /api/portafolio/sincronizar` trae el saldo real desde Alpaca y actualiza `cuenta_fondos`.

---

## Flujo de datos — consulta de saldo

```
Frontend GET /api/portafolio/saldo
  → PortafolioController.saldo()
    → PortafolioService.obtenerSaldo(correo)  // @Transactional (no readOnly)
      → InversionistaRepository.findByUsuarioCorreo(correo)
      → SaldoService.obtenerSaldoDTO(inversionistaId)
          → CuentaFondosRepository.findById(inversionistaId)
          → si no existe: crear CuentaFondos con saldo=0 (INSERT, necesita transacción writable)
          → OrdenRepository.sumMontoComisionByInversionistaAndEstado(id, EJECUTADA)
  ← 200 OK con SaldoDTO
```

---

## Flujo de datos — depósito sandbox

```
Frontend POST /api/portafolio/depositar?monto=X
  → PortafolioService.depositar(correo, monto)
    → obtener o crear CuentaFondos
    → cuentaFondos.saldoDisponible += monto
    → CuentaFondosRepository.save()
  ← 200 OK con mensaje de confirmación
```

---

## Flujo de datos — sincronizar con Alpaca

```
Frontend POST /api/portafolio/sincronizar
  → PortafolioService.sincronizar(correo)
    → InversionistaRepository para alpacaAccountId
    → AlpacaAdapter.obtenerSaldoCuenta(alpacaAccountId)
    → actualizar cuentaFondos.saldoDisponible con valor Alpaca
  ← 200 OK con mensaje
```

---

## Modelo de datos clave

```sql
CREATE TABLE cuenta_fondos (
    inversionista_id  BIGINT PRIMARY KEY REFERENCES inversionista(id),
    saldo_disponible  DECIMAL(15,4) NOT NULL DEFAULT 0,
    fondos_reservados DECIMAL(15,4) NOT NULL DEFAULT 0
);
```

- PK compartida con `inversionista` (no tiene `id` propio).
- Se crea automáticamente al primer acceso si no existe (patrón `obtenerOCrear`).

---

## Decisiones técnicas

- **`@Transactional` (no readOnly) en `obtenerSaldo`:** el patrón `obtenerOCrear` puede hacer INSERT; si la transacción exterior es readOnly, PostgreSQL rechaza el INSERT. Bug conocido, ya corregido (ver bitácora 2026-05-06).
- **Depósito sandbox:** endpoint habilitado en todos los perfiles por ahora; en producción se protegeía con `@Profile("!prod")` o un flag de feature.
- **Comisiones pagadas:** se calcula como `SUM(monto_comision)` de órdenes EJECUTADAS del inversionista. No se suma `monto_plataforma` ni `monto_comisionista` por separado; el desglose está disponible en el historial de órdenes.

---

## Escenarios de calidad cubiertos

No aplica un EC específico. El cálculo correcto de saldo es un requisito de exactitud financiera (RF-15).

---

## Dependencias previas

- `CuentaFondos` entity con `inversionista_id` como PK compartida.
- `OrdenRepository.sumMontoComision(inversionistaId, estado)` implementado (query JPQL o native).
- `AlpacaAdapter.obtenerSaldoCuenta` disponible.

---

## Criterios de aceptación resumidos

- GET /api/portafolio/saldo → 200 con `saldoDisponible`, `fondosReservados`, `totalComisionesPagadas`.
- POST /api/portafolio/depositar?monto=X → incrementa `saldo_disponible` en BD.
- POST /api/portafolio/sincronizar → actualiza saldo con el valor real de Alpaca.
- Sin JWT → 401.
- `cuenta_fondos` se crea automáticamente si no existe.
