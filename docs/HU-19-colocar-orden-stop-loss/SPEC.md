# SPEC — Colocación de orden Stop Loss

---

## Ficha de la historia

| Campo | Valor |
|---|---|
| ID | HU-19 |
| Sprint | 3 |
| Prioridad MoSCoW | Must Have |
| Estado | Completada |
| Épica | Órdenes / Ejecución |
| CU asociado | CU-19 |
| Autor | Juan Diego Triana Mejia |
| Creada | 2026-05-20 |
| Última revisión | 2026-05-24 |
| Versión de esta spec | 1.0 |

**Trazabilidad:**

| Tipo | ID | Descripción |
|---|---|---|
| Requerimiento funcional | RF-18 | Colocación de órdenes Stop Loss para limitar pérdidas |
| Escenario de calidad | EC-13 | Previsualización de comisión antes de confirmar (compartido con HU-17) |
| Historia relacionada | HU-17 | Comparte endpoint y lógica base; ver HU-17 para DDL y contrato completo |

---

## Historia de usuario

**Como** inversionista autenticado,
**quiero** colocar una orden Stop Loss especificando un precio de activación,
**para** limitar automáticamente mis pérdidas si el precio cae hasta el nivel que defino.

---

## Motivación y contexto

### Por qué existe esta historia

Una orden Stop Loss protege al inversionista de pérdidas extremas: cuando el precio de un activo cae hasta el `precioStop`, la orden se convierte en una orden de venta Market. Es una herramienta de gestión de riesgo fundamental en el day trading.

### Diferencias con HU-17 (Market) y HU-18 (Limit)

| Aspecto | Stop Loss (HU-19) |
|---|---|
| Campo requerido | `precioStop` |
| Tipo en Alpaca | `"stop"` con `stop_price: precioStop` |
| Lado típico | VENTA (para proteger posición larga) |
| Activación | Cuando el precio cae hasta/por debajo de `precioStop` |
| Precio estimado (previsualización) | `precioStop` |

---

## Actores y precondiciones

### Actores

Idénticos a HU-17.

### Precondiciones

- JWT válido, cuenta activa.
- `precioStop > 0`.
- Para venta: `holding.cantidad >= cantidad`.
- Para compra con stop: `saldo_disponible >= precioStop × cantidad + comision`.

---

## Flujo principal

Idéntico a HU-17 con estas diferencias:

1. `CrearOrdenRequestDTO.tipoOrden = "STOP_LOSS"`.
2. `CrearOrdenRequestDTO.precioStop` debe ser provisto y mayor a 0.
3. `OrdenService` valida que `precioStop != null && > 0`; si no, retorna 400.
4. El precio estimado en la previsualización es `precioStop`.
5. Al enviar a Alpaca: `type = "stop"`, `stop_price = precioStop`.
6. La ejecución interna (símbolo global): ejecuta al `precioStop`.

---

## Flujos de error

### Error 1 — `precioStop` ausente o inválido

| Campo | Valor |
|---|---|
| Condición | `precioStop` es null, 0 o negativo en orden STOP_LOSS |
| HTTP | 400 Bad Request |
| Cuerpo | `RespuestaDTO{error: "El precio stop es obligatorio para órdenes Stop Loss"}` |

### Errores 2-7

Idénticos a HU-17.

---

## Contrato de API

### Ejemplo de request para Stop Loss

```yaml
# POST /api/ordenes/previsualizar o POST /api/ordenes
example:
  simbolo: "AAPL"
  tipoOrden: "STOP_LOSS"
  lado: "VENTA"
  cantidad: 10
  precioStop: 175.00
```

**Validaciones adicionales para STOP_LOSS:**

| Campo | Restricción | HTTP |
|---|---|---|
| `precioStop` | Requerido, > 0 | 400 si ausente o inválido |

---

## Modelo de datos

Ver DDL completo en HU-17 SPEC. El campo `precio_stop` almacena el precio de activación.

---

## Criterios de verificación

### Escenarios de aceptación (Gherkin)

```gherkin
Funcionalidad: Colocación de orden Stop Loss

  Escenario: Orden Stop Loss de venta exitosa
    Dado que "ana@test.com" tiene holding de AAPL=10 unidades
    Cuando se envía POST /api/ordenes con { simbolo: "AAPL", tipoOrden: "STOP_LOSS", lado: "VENTA", cantidad: 10, precioStop: 175.00 }
    Entonces el sistema responde 200 OK con OrdenDTO
    Y la orden tiene precio_stop=175.00
    Y Alpaca recibe type="stop" con stop_price=175.00

  Escenario: precioStop ausente retorna 400
    Cuando se envía POST /api/ordenes con tipoOrden="STOP_LOSS" sin precioStop
    Entonces el sistema responde 400 Bad Request
```

---

## Definición de terminado

- [x] `POST /api/ordenes` con STOP_LOSS crea orden con `precio_stop` almacenado.
- [x] `precioStop` ausente o inválido retorna 400.
- [x] Alpaca recibe `type: "stop"` con `stop_price`.
- [x] `docs/PROGRESO.md` marcado con ✅ para HU-19.

---

## Historial de cambios

| Versión | Fecha | Descripción | Razón |
|---|---|---|---|
| 1.0 | 2026-05-24 | Refactorización a estructura SDD del proyecto. | Unificación de todos los SPEC.md bajo plantilla canónica SDD del proyecto |
| 1.1 | 2026-05-26 | Auditoría SDD: `activoId` → `simbolo` en ejemplos de request. HTTP 201→200. | Código real usa `simbolo` en `CrearOrdenRequestDTO`. |
