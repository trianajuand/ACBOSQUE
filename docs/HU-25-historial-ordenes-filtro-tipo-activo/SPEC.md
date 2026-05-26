# SPEC — Historial de órdenes filtrado por tipo de orden y activo

---

## Ficha de la historia

| Campo | Valor |
|---|---|
| ID | HU-25 |
| Sprint | 3 |
| Prioridad MoSCoW | Must Have |
| Estado | Completada |
| Épica | Órdenes / Historial |
| CU asociado | CU-25 |
| Autor | Juan Diego Triana Mejia |
| Creada | 2026-05-20 |
| Última revisión | 2026-05-24 |
| Versión de esta spec | 1.0 |

**Trazabilidad:**

| Tipo | ID | Descripción |
|---|---|---|
| Requerimiento funcional | RF-24 | Historial de órdenes con filtro por tipo de orden y símbolo |
| Historia relacionada | HU-24 | Filtro por período (mismo endpoint) |
| Historia relacionada | HU-26 | Filtro por estado (mismo endpoint) |

---

## Historia de usuario

**Como** inversionista autenticado,
**quiero** filtrar el historial de órdenes por tipo de orden (Market, Limit, etc.) o por símbolo del activo,
**para** encontrar rápidamente las operaciones relevantes para mi análisis.

---

## Actores y precondiciones

Ver HU-24 (idénticos).

---

## Flujo principal

1. Frontend llama `GET /api/ordenes/historial?tipoOrden=MARKET` o `?simbolo=AAPL` o ambos.
2. `OrdenService` aplica filtros por `tipo_orden` y/o por símbolo del activo (case insensitive).
3. Los filtros se pueden combinar con `desde`, `hasta` (HU-24) y `estado` (HU-26).

---

## Flujos de error

Ver HU-24 (idénticos).

---

## Contrato de API

El contrato completo está en HU-24 SPEC. Los parámetros específicos de esta historia son:

| Parámetro | Tipo | Valores válidos |
|---|---|---|
| `tipoOrden` | `string` (opcional) | `MARKET`, `LIMIT`, `STOP_LOSS`, `TAKE_PROFIT` |
| `simbolo` | `string` (opcional) | Ticker del activo (ej. `AAPL`, `TSLA`) — parámetro de query real en `OrdenController` |

---

## Criterios de verificación

### Escenarios de aceptación (Gherkin)

```gherkin
Funcionalidad: Historial por tipo y activo

  Antecedentes:
    Dado que "ana@test.com" tiene órdenes MARKET y LIMIT, y órdenes de AAPL y TSLA

  Escenario: Filtro por tipo de orden MARKET
    Cuando se envía GET /api/ordenes/historial?tipoOrden=MARKET
    Entonces todas las órdenes retornadas tienen tipo_orden=MARKET

  Escenario: Filtro por simbolo AAPL
    Cuando se envía GET /api/ordenes/historial?simbolo=AAPL
    Entonces todas las órdenes retornadas corresponden al activo AAPL

  Escenario: Filtros combinados (tipo + simbolo + período)
    Cuando se envía GET /api/ordenes/historial?tipoOrden=LIMIT&simbolo=AAPL&desde=2026-05-01
    Entonces el sistema retorna solo órdenes LIMIT de AAPL desde mayo 2026
```

---

## Definición de terminado

- [x] `GET /api/ordenes/historial?tipoOrden=` filtra por tipo de orden.
- [x] `GET /api/ordenes/historial?simbolo=` filtra por símbolo del activo (parámetro real en `OrdenController`).
- [x] Los filtros se combinan con los de HU-24 y HU-26.
- [x] `docs/PROGRESO.md` marcado con ✅ para HU-25.

---

## Historial de cambios

| Versión | Fecha | Descripción | Razón |
|---|---|---|---|
| 1.0 | 2026-05-24 | Refactorización a estructura SDD del proyecto. | Unificación de todos los SPEC.md bajo plantilla canónica SDD del proyecto |
| 1.1 | 2026-05-26 | Auditoría SDD: parámetro `ticker` → `simbolo` en todas las referencias del endpoint historial. El parámetro real en `OrdenController` es `simbolo`. | Código real usa `simbolo`, no `ticker`. |
