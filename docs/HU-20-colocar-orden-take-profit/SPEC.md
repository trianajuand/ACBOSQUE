# SPEC — Colocación de orden Take Profit

---

## Ficha de la historia

| Campo | Valor |
|---|---|
| ID | HU-20 |
| Sprint | 3 |
| Prioridad MoSCoW | Must Have |
| Estado | Completada |
| Épica | Órdenes / Ejecución |
| CU asociado | CU-20 |
| Autor | Juan Diego Triana Mejia |
| Creada | 2026-05-20 |
| Última revisión | 2026-05-24 |
| Versión de esta spec | 1.0 |

**Trazabilidad:**

| Tipo | ID | Descripción |
|---|---|---|
| Requerimiento funcional | RF-19 | Colocación de órdenes Take Profit para asegurar ganancias |
| Escenario de calidad | EC-13 | Previsualización de comisión antes de confirmar (compartido con HU-17) |
| Historia relacionada | HU-17 | Comparte endpoint y lógica base; ver HU-17 para DDL y contrato completo |

---

## Historia de usuario

**Como** inversionista autenticado,
**quiero** colocar una orden Take Profit con un precio objetivo de ganancias,
**para** asegurar automáticamente mis ganancias cuando el activo alcanza el precio que definí.

---

## Motivación y contexto

### Por qué existe esta historia

Una orden Take Profit es una orden de venta Limit que se activa cuando el precio sube hasta el nivel de ganancia deseado. El inversionista define un `precioLimite` objetivo y la orden se ejecuta automáticamente cuando el precio lo alcanza, asegurando la ganancia sin necesidad de monitoreo continuo.

### Diferencias con HU-18 (Limit) 

| Aspecto | Limit (HU-18) | Take Profit (HU-20) |
|---|---|---|
| `tipoOrden` | `LIMIT` | `TAKE_PROFIT` |
| Semántica | Compra a máximo / venta a mínimo | Venta al precio objetivo de ganancia |
| Tipo en Alpaca | `"limit"` | `"limit"` (mismo tipo) |
| Uso típico | Ambos lados | VENTA (tomar ganancias de posición larga) |

> **Nota de implementación:** Alpaca mapea `TAKE_PROFIT` a tipo `"limit"` (igual que `LIMIT`). La diferencia es semántica para el usuario y de trazabilidad en auditoría.

---

## Actores y precondiciones

Idénticos a HU-17.

### Precondiciones

- JWT válido, cuenta activa.
- `precioLimite > 0`.
- Para venta: `holding.cantidad >= cantidad`.

---

## Flujo principal

Idéntico a HU-18 (orden Limit), con `tipoOrden = "TAKE_PROFIT"`:

1. `OrdenService` valida `precioLimite != null && > 0`.
2. Al enviar a Alpaca: `type = "limit"`, `limit_price = precioLimite`.
3. Auditado como `ORDEN_ENVIADA_ALPACA` con `tipoOrden = TAKE_PROFIT`.

---

## Flujos de error

Idénticos a HU-18 (ver sección de errores en HU-18 SPEC). El error de `precioLimite` ausente aplica igual.

---

## Contrato de API

### Ejemplo de request para Take Profit

```yaml
# POST /api/ordenes/previsualizar o POST /api/ordenes
example:
  simbolo: "AAPL"
  tipoOrden: "TAKE_PROFIT"
  lado: "VENTA"
  cantidad: 10
  precioLimite: 210.00
```

---

## Criterios de verificación

### Escenarios de aceptación (Gherkin)

```gherkin
Funcionalidad: Colocación de orden Take Profit

  Escenario: Orden Take Profit de venta exitosa
    Dado que "ana@test.com" tiene holding de AAPL=10 unidades
    Cuando se envía POST /api/ordenes con { simbolo: "AAPL", tipoOrden: "TAKE_PROFIT", lado: "VENTA", cantidad: 10, precioLimite: 210.00 }
    Entonces el sistema responde 200 OK con OrdenDTO
    Y la orden tiene precio_limite=210.00
    Y Alpaca recibe type="limit" con limit_price=210.00

  Escenario: precioLimite ausente retorna 400
    Cuando se envía POST /api/ordenes con tipoOrden="TAKE_PROFIT" sin precioLimite
    Entonces el sistema responde 400 Bad Request
```

---

## Definición de terminado

- [x] `POST /api/ordenes` con TAKE_PROFIT crea orden con `precio_limite` almacenado.
- [x] `precioLimite` ausente o inválido retorna 400.
- [x] Alpaca recibe `type: "limit"` con `limit_price`.
- [x] `docs/PROGRESO.md` marcado con ✅ para HU-20.

---

## Historial de cambios

| Versión | Fecha | Descripción | Razón |
|---|---|---|---|
| 1.0 | 2026-05-24 | Refactorización a estructura SDD del proyecto. | Unificación de todos los SPEC.md bajo plantilla canónica SDD del proyecto |
| 1.1 | 2026-05-26 | Auditoría SDD: `activoId` → `simbolo` en ejemplos de request. HTTP 201→200. | Código real usa `simbolo` en `CrearOrdenRequestDTO`. |
