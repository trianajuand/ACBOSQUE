# Plan de implementación — HU-19: Colocación de orden Stop Loss

## Objetivo

Extender la lógica de HU-17 para soportar órdenes Stop Loss, donde el inversionista define un precio de activación (`precioStop`) a partir del cual la posición se liquida automáticamente, limitando así las pérdidas máximas.

---

## Módulos involucrados

Los mismos que HU-17. No se añaden módulos nuevos.

---

## Estrategia general

Una orden Stop Loss es una variante de la orden de venta Market: cuando el precio cae hasta el `precioStop`, Alpaca convierte la orden en una Market sell y la ejecuta. El sistema valida el campo `precioStop`, lo almacena en `orden.precio_stop` y lo envía a Alpaca con `type = "stop"`.

---

## Diferencias respecto a HU-17 (Market) y HU-18 (Limit)

| Aspecto | Stop Loss (HU-19) |
|---|---|
| Campo requerido | `precioStop` (en lugar de `precioLimite`) |
| Tipo de orden en Alpaca | `"stop"` con `stop_price: precioStop` |
| Lado típico | VENTA (proteger posición larga) |
| Precio estimado (previsualización) | `precioStop` |
| Campo en BD | `precio_stop` (no `precio_limite`) |

---

## Flujo de validación adicional

```
OrdenService.crearOrden(dto, correo)
  ...
  si dto.tipoOrden == STOP_LOSS:
    si dto.precioStop == null || dto.precioStop <= 0:
      throw new OrdenInvalidaException("El precio stop es obligatorio para órdenes Stop Loss")
      → 400 Bad Request
  precioEstimado = dto.precioStop
  ...
  // El resto del flujo es idéntico a HU-17
```

---

## Envío a Alpaca

```java
// En AlpacaAdapter.crearOrden(...)
if (tipoOrden == STOP_LOSS) {
    body.put("type", "stop");
    body.put("stop_price", precioStop.toString());
    body.put("time_in_force", "gtc");
}
```

---

## Gestión de fondos y holdings para VENTA Stop Loss

| Evento | Efecto |
|---|---|
| Stop Loss colocado (VENTA, estado ENVIADA) | Holding queda íntegro; no se reservan fondos (es una venta, no una compra). |
| Stop Loss ejecutado por Alpaca | Alpaca notifica webhook; `OrdenService` actualiza estado a `EJECUTADA`, acredita `totalARecibir` en `saldo_disponible`, descuenta holding. |

> Nota: la ejecución real del Stop Loss la dispara Alpaca cuando el precio cae al nivel de stop. El webhook de Alpaca (o el poleo de estado) actualiza el estado de la orden en BD.

---

## Decisiones técnicas

- **`precioEstimado = precioStop`:** para el cálculo de comisión en la previsualización se usa `precioStop` como precio estimado, dado que el usuario no sabe el precio exacto de ejecución (puede diferir ligeramente en una orden stop).
- **Ejecución interna (símbolo global):** para símbolos que no van a Alpaca, el sistema ejecuta la venta al `precioStop` inmediatamente si el mercado está abierto (simplificación académica).
- **Holdings no bloqueados:** a diferencia de una compra (donde se bloquean fondos), en una venta Stop Loss el holding no se bloquea en la BD local; Alpaca gestiona la orden en su sistema hasta que se dispara.

---

## Escenarios de calidad cubiertos

Los mismos que HU-17 (EC-13, EC-12). No añade nuevos escenarios de calidad.

---

## Dependencias previas

- HU-17 completamente implementado.
- `OrdenService.crearOrden` diferencia tipos de orden al construir el request de Alpaca.
- `OrdenInvalidaException` con mensaje parametrizable disponible.

---

## Criterios de aceptación resumidos

- POST /api/ordenes con STOP_LOSS/VENTA y precioStop=175.00 → 201; orden con `precio_stop=175.00`.
- Alpaca recibe `type: "stop"` con `stop_price=175.00`.
- `precioStop` ausente o <= 0 → 400.
- Previsualización usa `precioStop` como precio estimado.
- Holding insuficiente → 422; sin JWT → 401; cuenta suspendida → 403.
