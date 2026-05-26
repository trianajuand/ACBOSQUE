# Plan de implementación — HU-18: Colocación de orden Limit

## Objetivo

Extender la lógica de HU-17 para soportar órdenes Limit, donde el inversionista especifica un precio máximo de compra o mínimo de venta, garantizando que la operación no se ejecute a un precio peor que el indicado.

---

## Módulos involucrados

Los mismos que HU-17. Ver plan.md de HU-17 para la lista completa. No se añaden módulos nuevos.

---

## Estrategia general

La orden Limit reutiliza el mismo endpoint `POST /api/ordenes` y el mismo flujo de `OrdenService.crearOrden`. Las diferencias son:

1. `tipoOrden = "LIMIT"` en el DTO.
2. `precioLimite` es obligatorio y debe ser > 0.
3. El precio estimado en la previsualización y el precio base para el cálculo de comisión es `precioLimite` (no el precio de mercado).
4. Al enviar a Alpaca: `type = "limit"`, `limit_price = precioLimite`.
5. Para símbolos globales (ejecución interna): se ejecuta al `precioLimite`.

---

## Diferencias respecto a HU-17 (Market)

| Aspecto | HU-17 Market | HU-18 Limit |
|---|---|---|
| Campo adicional | — | `precioLimite` (requerido, > 0) |
| Precio para cálculo de comisión | Precio actual de mercado | `precioLimite` |
| Tipo de orden en Alpaca | `"market"` | `"limit"` con `limit_price` |
| Validación extra | — | 400 si `precioLimite` null o <= 0 |
| Precio estimado en previsualización | Precio actual de mercado | `precioLimite` |

---

## Flujo de validación adicional

```
OrdenService.crearOrden(dto, correo)
  ...
  si dto.tipoOrden == LIMIT:
    si dto.precioLimite == null || dto.precioLimite <= 0:
      throw new OrdenInvalidaException("El precio límite es obligatorio para órdenes Limit")
      → 400 Bad Request
  precioEstimado = dto.precioLimite
  ...
  // El resto del flujo es idéntico a HU-17
```

---

## Envío a Alpaca

```java
// En AlpacaAdapter.crearOrden(...)
if (tipoOrden == LIMIT) {
    body.put("type", "limit");
    body.put("limit_price", precioLimite.toString());
    body.put("time_in_force", "gtc");  // Good Till Cancelled
}
```

---

## Decisiones técnicas

- **Precio base para comisión:** se usa `precioLimite × cantidad` porque ese es el valor máximo que el usuario acepta pagar (o recibir mínimo). El sistema reserva fondos por ese monto para evitar que el usuario coloque una Limit order por debajo del saldo disponible pero que no alcance a cubrir si se ejecuta al precio límite.
- **`time_in_force = "gtc"`:** las órdenes Limit se envían como GTC (Good Till Cancelled) a Alpaca sandbox. Para producción, esto debería ser configurable.
- **Ejecución interna (símbolo global):** se ejecuta inmediatamente al `precioLimite` si el mercado está abierto, independientemente del precio actual de caché. Esto es una simplificación académica.

---

## Escenarios de calidad cubiertos

Los mismos que HU-17 (EC-13, EC-12). No añade nuevos escenarios.

---

## Dependencias previas

- HU-17 completamente implementado.
- `OrdenService.crearOrden` ya verifica por tipo de orden al construir el request de Alpaca.
- `OrdenInvalidaException` registrada en `GlobalExceptionHandler`.

---

## Criterios de aceptación resumidos

- POST /api/ordenes con LIMIT y precioLimite=185.00 → 201; orden creada con `precio_limite=185.00`.
- Comisión calculada sobre `precioLimite × cantidad`.
- Alpaca recibe `type: "limit"` con `limit_price`.
- `precioLimite` ausente o <= 0 → 400.
- Previsualización con LIMIT usa `precioLimite` como precio estimado.
- Sin JWT → 401; fondos insuficientes → 402; holding insuficiente (venta) → 422.
