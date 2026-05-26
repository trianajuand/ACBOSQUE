# SPEC — Historial de órdenes filtrado por estado

---

## Ficha de la historia

| Campo | Valor |
|---|---|
| ID | HU-26 |
| Sprint | 3 |
| Prioridad MoSCoW | Must Have |
| Estado | Completada |
| Épica | Órdenes / Historial |
| CU asociado | CU-26 |
| Autor | Juan Diego Triana Mejia |
| Creada | 2026-05-20 |
| Última revisión | 2026-05-24 |
| Versión de esta spec | 1.0 |

**Trazabilidad:**

| Tipo | ID | Descripción |
|---|---|---|
| Requerimiento funcional | RF-25 | Historial de órdenes con filtro por estado |
| Historia relacionada | HU-24 | Filtro por período (mismo endpoint) |
| Historia relacionada | HU-25 | Filtro por tipo y símbolo (mismo endpoint) |

---

## Historia de usuario

**Como** inversionista autenticado,
**quiero** filtrar el historial de órdenes por estado (ejecutadas, canceladas, en cola, etc.),
**para** diferenciar fácilmente el resultado de cada operación.

---

## Actores y precondiciones

Ver HU-24 (idénticos).

---

## Flujo principal

1. Frontend llama `GET /api/ordenes/historial?estado=EJECUTADA` (o cualquier estado válido).
2. `OrdenService` aplica filtro `WHERE estado = ?`.
3. Los filtros se combinan con los de HU-24 y HU-25.

---

## Contrato de API

El contrato completo está en HU-24 SPEC. El parámetro específico de esta historia es:

| Parámetro | Tipo | Valores válidos |
|---|---|---|
| `estado` | `string` (opcional) | `PENDIENTE`, `ENVIADA`, `EN_COLA`, `EJECUTADA`, `CANCELADA` |

---

## Criterios de verificación

### Escenarios de aceptación (Gherkin)

```gherkin
Funcionalidad: Historial por estado

  Antecedentes:
    Dado que "ana@test.com" tiene órdenes en varios estados

  Escenario: Solo órdenes ejecutadas
    Cuando se envía GET /api/ordenes/historial?estado=EJECUTADA
    Entonces todas las órdenes retornadas tienen estado=EJECUTADA

  Escenario: Solo órdenes canceladas
    Cuando se envía GET /api/ordenes/historial?estado=CANCELADA
    Entonces todas las órdenes retornadas tienen estado=CANCELADA

  Escenario: Estado inválido
    Cuando se envía GET /api/ordenes/historial?estado=INVALIDO
    Entonces el sistema responde 400 Bad Request (deserialización de enum)
```

---

## Definición de terminado

- [x] `GET /api/ordenes/historial?estado=` filtra por estado de la orden.
- [x] Estado inválido retorna 400.
- [x] Los filtros se combinan con los de HU-24 y HU-25.
- [x] `docs/PROGRESO.md` marcado con ✅ para HU-26.

---

## Historial de cambios

| Versión | Fecha | Descripción | Razón |
|---|---|---|---|
| 1.0 | 2026-05-24 | Refactorización a estructura SDD del proyecto. | Unificación de todos los SPEC.md bajo plantilla canónica SDD del proyecto |
