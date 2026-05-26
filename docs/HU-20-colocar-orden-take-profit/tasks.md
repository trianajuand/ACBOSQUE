# Tasks — HU-20: Colocación de orden Take Profit

---

## Leyenda

| Símbolo | Significado |
|---|---|
| ✅ | Completada |
| 🔄 | En progreso |
| ⬜ | Pendiente |
| ❌ | Bloqueada |

---

## Bloque 1 — Backend: validación y mapeo

| # | Tarea | Estado | Archivo(s) afectado(s) | Notas |
|---|---|:-:|---|---|
| T1.1 | Verificar que el enum `TipoOrden` incluye `TAKE_PROFIT` | ✅ | `ordenes/model/TipoOrden.java` o equivalente | Añadir si falta |
| T1.2 | Añadir/verificar rama `TAKE_PROFIT` en `OrdenService.crearOrden`: validar `precioLimite != null && > 0` | ✅ | `ordenes/service/OrdenService.java` | Si la validación de `precioLimite` ya es genérica para LIMIT y TAKE_PROFIT, confirmar que cubre ambos |
| T1.3 | Confirmar que `AlpacaAdapter.enviarOrden` mapea `TAKE_PROFIT → type: "limit"` con `limit_price = precioLimite` | ✅ | `integracion/adaptadores/alpaca/AlpacaAdapter.java` | No debe enviar `stop_price` para este tipo |
| T1.4 | Verificar que `OrdenService.previsualizarOrden` acepta `TAKE_PROFIT` y calcula comisión correctamente | ✅ | `ordenes/service/OrdenService.java` | Misma lógica que LIMIT |

---

## Bloque 2 — Auditoría

| # | Tarea | Estado | Archivo(s) afectado(s) | Notas |
|---|---|:-:|---|---|
| T2.1 | Confirmar que el evento `ORDEN_ENVIADA_ALPACA` incluye `tipoOrden` en el mensaje de auditoría | ✅ | `ordenes/service/OrdenService.java` | El log debe mostrar `TAKE_PROFIT` como tipo |

---

## Bloque 3 — Pruebas manuales

| # | Tarea | Estado | Herramienta | Escenario |
|---|---|:-:|---|---|
| T3.1 | POST `/api/ordenes/previsualizar` con TAKE_PROFIT + precioLimite → previsualización correcta | ✅ | Postman / dashboard.html | Verificar comisión 2% y monto total |
| T3.2 | POST `/api/ordenes` con TAKE_PROFIT + precioLimite → 201 Created, orden en BD con `precio_limite` | ✅ | Postman | Verificar estado `ENVIADA` o `EN_COLA` según horario |
| T3.3 | POST `/api/ordenes` con TAKE_PROFIT sin precioLimite → 400 Bad Request | ✅ | Postman | Verificar mensaje de error en español |
| T3.4 | Verificar `audit.log` contiene `ORDEN_ENVIADA_ALPACA` con `tipoOrden=TAKE_PROFIT` | ✅ | `logs/audit.log` | |

---

## Bloque 4 — Documentación y cierre

| # | Tarea | Estado | Archivo(s) afectado(s) | Notas |
|---|---|:-:|---|---|
| T4.1 | Marcar HU-20 como ✅ en `docs/PROGRESO.md` | ✅ | `docs/PROGRESO.md` | |
| T4.2 | Crear `docs/HU-20-colocar-orden-take-profit/plan.md` y `tasks.md` | ✅ | Este archivo | |

---

## Notas de implementación

- La implementación de HU-20 no agrega código net-new significativo: es la habilitación del valor `TAKE_PROFIT` en el flujo ya construido para `LIMIT` (HU-18).
- Si el switch de tipos en `OrdenService` usa un `if-else` por tipo de orden, verificar que `TAKE_PROFIT` comparte exactamente la misma rama que `LIMIT` (mismo mapeo a Alpaca, misma validación de `precioLimite`).
- El campo `precioStop` no se usa para `TAKE_PROFIT`; si el body lo incluye, ignorarlo.
