# Plan de Implementación — HU-20: Colocación de orden Take Profit

---

## Resumen ejecutivo

HU-20 extiende el endpoint de órdenes (implementado en HU-17/HU-18) para soportar el tipo `TAKE_PROFIT`. No requiere nuevo endpoint ni nuevas tablas: reutiliza `POST /api/ordenes` con `tipoOrden = "TAKE_PROFIT"` y delega a `OrdenService`, que ya maneja `precioLimite` desde HU-18. La diferencia es puramente semántica para el usuario y de trazabilidad en auditoría.

---

## Alcance

| Incluido | Excluido |
|---|---|
| Validar `precioLimite != null && > 0` para `TAKE_PROFIT` | Lógica de ejecución automática en Alpaca (la gestiona Alpaca internamente) |
| Mapear `TAKE_PROFIT → type: "limit"` al enviar a Alpaca | Cambios al modelo `Orden` (ya tiene `precioLimite`) |
| Auditar `ORDEN_ENVIADA_ALPACA` con `tipoOrden = TAKE_PROFIT` | Nuevo endpoint REST |
| Previsualización de comisión vía `POST /api/ordenes/previsualizar` | |

---

## Decisiones técnicas

| Decisión | Justificación |
|---|---|
| Mismo endpoint que HU-17/HU-18 | El discriminador `tipoOrden` en el body ya enruta la lógica en `OrdenService` |
| `TAKE_PROFIT` → `"limit"` en Alpaca | Alpaca solo distingue `"market"`, `"limit"`, `"stop"`, `"stop_limit"`. Take Profit es semánticamente un limit de venta |
| Validación en `OrdenService`, no en controller | Consistente con el patrón del proyecto; el controller solo parsea el JWT y delega |

---

## Módulos involucrados

| Módulo | Componente | Cambio |
|---|---|---|
| `ordenes` | `OrdenService` | Añadir rama `TAKE_PROFIT` al switch de tipos si no existe; validar `precioLimite` |
| `ordenes` | `OrdenController` | Sin cambio (ya acepta el body completo) |
| `integracion` | `AlpacaAdapter` | Confirmar que el mapeo `TAKE_PROFIT → limit` ya está implementado |
| `trazabilidad` | `AuditLogService` | Sin cambio (ya registra `tipoOrden` como string) |

---

## Dependencias

- HU-17 y HU-18 completadas (endpoint base y lógica de `precioLimite` en su lugar).
- `Orden.tipoOrden` acepta el valor `TAKE_PROFIT` en el enum.
- `AlpacaAdapter.enviarOrden` acepta el parámetro `type` como string configurable por `tipoOrden`.

---

## Criterios de aceptación (resumen ejecutivo)

1. `POST /api/ordenes` con `tipoOrden: "TAKE_PROFIT"` y `precioLimite: 210.00` retorna 201.
2. La orden persiste con `precio_limite = 210.00` en BD.
3. Alpaca recibe `type: "limit"` con `limit_price: 210.00`.
4. `POST /api/ordenes` con `tipoOrden: "TAKE_PROFIT"` sin `precioLimite` retorna 400.
5. Evento `ORDEN_ENVIADA_ALPACA` en `audit.log` con `tipoOrden = TAKE_PROFIT`.

---

## Estrategia de pruebas

| Tipo | Herramienta | Escenario clave |
|---|---|---|
| Integración manual | Postman / `dashboard.html` | POST con TAKE_PROFIT + precioLimite → 201 |
| Integración manual | Postman | POST con TAKE_PROFIT sin precioLimite → 400 |
| Trazabilidad | `logs/audit.log` | Verificar `ORDEN_ENVIADA_ALPACA` con tipo correcto |

---

## Estimación

| Tarea | Puntos de historia |
|---|---|
| Verificar/añadir rama TAKE_PROFIT en OrdenService | 1 |
| Confirmar mapeo en AlpacaAdapter | 1 |
| Pruebas manuales y correcciones | 1 |
| **Total** | **3** |

---

## Estado

- [x] Implementación completada
- [x] Pruebas manuales pasadas
- [x] `PROGRESO.md` actualizado
