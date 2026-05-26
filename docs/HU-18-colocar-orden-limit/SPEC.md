# SPEC — Colocación de orden Limit

---

## Ficha de la historia

| Campo | Valor |
|---|---|
| ID | HU-18 |
| Sprint | 3 |
| Prioridad MoSCoW | Must Have |
| Estado | Completada |
| Épica | Órdenes / Ejecución |
| CU asociado | CU-18 |
| Autor | Juan Diego Triana Mejia |
| Creada | 2026-05-20 |
| Última revisión | 2026-05-24 |
| Versión de esta spec | 1.0 |

**Trazabilidad:**

| Tipo | ID | Descripción |
|---|---|---|
| Requerimiento funcional | RF-17 | Colocación de órdenes Limit con precio máximo de compra/mínimo de venta |
| Escenario de calidad | EC-13 | Previsualización de comisión antes de confirmar (compartido con HU-17) |
| Historia relacionada | HU-17 | Comparte endpoint y lógica base; ver HU-17 para DDL y contrato completo |
| Historia relacionada | HU-23 | Las órdenes Limit fuera de horario se encolan |

---

## Historia de usuario

**Como** inversionista autenticado,
**quiero** colocar una orden Limit especificando un precio máximo de compra o mínimo de venta,
**para** ejecutar la operación solo si el mercado alcanza el precio que considero favorable.

---

## Motivación y contexto

### Por qué existe esta historia

A diferencia de una orden Market (HU-17), la orden Limit garantiza que la operación no se ejecutará a un precio peor que el especificado. Para compras: el precio de ejecución no puede superar `precioLimite`. Para ventas: el precio no puede ser menor que `precioLimite`.

### Diferencias con HU-17 (Market)

| Aspecto | Market (HU-17) | Limit (HU-18) |
|---|---|---|
| `precioLimite` | No requerido | **Requerido** |
| Precio de estimación | Precio actual de mercado | `precioLimite` especificado |
| Ejecución en Alpaca | `type: "market"` | `type: "limit"`, `limit_price: precioLimite` |
| Ejecución interna (global) | Al precio de caché | Al `precioLimite` |

---

## Actores y precondiciones

### Actores

Idénticos a HU-17. Ver sección correspondiente.

### Precondiciones

- JWT válido, cuenta activa para operar.
- Para compra: `saldo_disponible >= precioLimite × cantidad + comision`.
- Para venta: `holding.cantidad >= cantidad`.
- `precioLimite > 0`.

### Postcondiciones

Idénticas a HU-17, con `tipoOrden = LIMIT` y `precio_limite` almacenado en la orden.

---

## Flujo principal

Idéntico a HU-17 con estas diferencias:

1. `CrearOrdenRequestDTO.tipoOrden = "LIMIT"`.
2. `CrearOrdenRequestDTO.precioLimite` debe ser provisto y mayor a 0.
3. `OrdenService` valida que `precioLimite != null && > 0`; si no, retorna 400.
4. El precio estimado en la previsualización es `precioLimite`.
5. La comisión se calcula sobre `precioLimite × cantidad`.
6. Al enviar a Alpaca: `type = "limit"`, `limit_price = precioLimite`.
7. El resto del flujo es idéntico a HU-17.

---

## Flujos de error

### Error 1 — `precioLimite` ausente o inválido

| Campo | Valor |
|---|---|
| Condición | `precioLimite` es null, 0 o negativo en orden LIMIT |
| HTTP | 400 Bad Request |
| Cuerpo | `RespuestaDTO{error: "El precio límite es obligatorio para órdenes Limit"}` |
| Evento de auditoría | Ninguno |

### Errores 2-7

Idénticos a HU-17 (no autenticado, fondos insuficientes, holding insuficiente, símbolo inválido, cuenta no puede operar, fallo Alpaca).

---

## Contrato de API

### Endpoint 1 — `POST /api/ordenes/previsualizar` (compartido con HU-17)

```yaml
# Ver HU-17 para schema completo de CrearOrdenRequestDTO
# Para Limit, el ejemplo es:
example:
  simbolo: "AAPL"
  tipoOrden: "LIMIT"
  lado: "COMPRA"
  cantidad: 10
  precioLimite: 185.00
```

### Endpoint 2 — `POST /api/ordenes` (compartido con HU-17)

```yaml
# Para Limit:
example:
  simbolo: "AAPL"
  tipoOrden: "LIMIT"
  lado: "COMPRA"
  cantidad: 10
  precioLimite: 185.00
```

**Validaciones adicionales para LIMIT:**

| Campo | Restricción | HTTP |
|---|---|---|
| `precioLimite` | Requerido, > 0 | 400 si ausente o inválido |

---

## Modelo de datos

Ver DDL completo en HU-17 SPEC (tabla `orden`). El campo `precio_limite` almacena el precio límite de la orden.

---

## Módulos y arquitectura

Idénticos a HU-17. Ver sección correspondiente.

---

## Criterios de verificación

### Escenarios de aceptación (Gherkin)

```gherkin
Funcionalidad: Colocación de orden Limit

  Antecedentes:
    Dado que "ana@test.com" tiene JWT válido y saldo_disponible suficiente

  Escenario: Orden Limit de compra exitosa
    Cuando se envía POST /api/ordenes con { simbolo: "AAPL", tipoOrden: "LIMIT", lado: "COMPRA", cantidad: 10, precioLimite: 185.00 }
    Entonces el sistema responde 200 OK con OrdenDTO
    Y la orden tiene precio_limite=185.00
    Y la comisión se calculó sobre 185.00 × 10

  Escenario: precioLimite ausente retorna 400
    Cuando se envía POST /api/ordenes con tipoOrden="LIMIT" sin precioLimite
    Entonces el sistema responde 400 Bad Request

  Escenario: Previsualización usa precioLimite como precio estimado
    Cuando se envía POST /api/ordenes/previsualizar con tipoOrden="LIMIT" y precioLimite=185.00
    Entonces precioEstimado en la respuesta es 185.00
```

---

## Definición de terminado

- [x] `POST /api/ordenes` con LIMIT crea orden con `precio_limite` almacenado.
- [x] `precioLimite` ausente o inválido retorna 400.
- [x] Comisión calculada sobre `precioLimite × cantidad`.
- [x] Alpaca recibe `type: "limit"` con `limit_price`.
- [x] Previsualización usa `precioLimite` como precio estimado.
- [x] `docs/PROGRESO.md` marcado con ✅ para HU-18.

---

## Historial de cambios

| Versión | Fecha | Descripción | Razón |
|---|---|---|---|
| 1.0 | 2026-05-24 | Refactorización a estructura SDD del proyecto. | Unificación de todos los SPEC.md bajo plantilla canónica SDD del proyecto |
| 1.1 | 2026-05-26 | Auditoría SDD: `activoId` → `simbolo` en ejemplos de request. HTTP 201→200. | Código real usa `simbolo` en `CrearOrdenRequestDTO`. |
