# Tasks — HU-25: Historial de órdenes filtrado por tipo de orden y activo

---

## Leyenda

| Símbolo | Significado |
|---|---|
| ✅ | Completada |
| 🔄 | En progreso |
| ⬜ | Pendiente |
| ❌ | Bloqueada |

---

## Bloque 1 — OrdenSpecification: predicados de tipo y ticker

| # | Tarea | Estado | Archivo(s) afectado(s) | Notas |
|---|---|:-:|---|---|
| T1.1 | Añadir predicado `porTipo(TipoOrden tipo)` en `OrdenSpecification` | ✅ | `ordenes/repository/OrdenSpecification.java` | Retornar `null` si `tipo == null` para que `Specification.and(null)` lo ignore |
| T1.2 | Añadir predicado `porTicker(String ticker)` en `OrdenSpecification` | ✅ | `ordenes/repository/OrdenSpecification.java` | JOIN con `activo`; `UPPER(activo.ticker) = UPPER(:ticker)` |
| T1.3 | Verificar que `Orden.activo` es `@ManyToOne` a entidad `Activo` con campo `ticker` | ✅ | `ordenes/model/Orden.java`, `ordenes/model/Activo.java` o `mercado/model/Activo.java` | Si `Activo` está en el módulo mercado, acceder via tabla directamente o verificar diseño |

---

## Bloque 2 — Controller

| # | Tarea | Estado | Archivo(s) afectado(s) | Notas |
|---|---|:-:|---|---|
| T2.1 | Añadir `@RequestParam(required = false) TipoOrden tipoOrden` en `@GetMapping("/historial")` | ✅ | `ordenes/controller/OrdenController.java` | Spring convierte String → enum y retorna 400 automáticamente si el valor es inválido |
| T2.2 | Añadir `@RequestParam(required = false) String ticker` en el mismo método | ✅ | `ordenes/controller/OrdenController.java` | Sin transformación adicional; el service lo normaliza a uppercase |
| T2.3 | Pasar `tipoOrden` y `ticker` al service | ✅ | `ordenes/controller/OrdenController.java` | El service ya acepta estos parámetros si HU-24 se implementó correctamente |

---

## Bloque 3 — Service

| # | Tarea | Estado | Archivo(s) afectado(s) | Notas |
|---|---|:-:|---|---|
| T3.1 | Verificar que `OrdenService.listarHistorial` incorpora `porTipo` y `porTicker` en la `Specification` combinada | ✅ | `ordenes/service/OrdenService.java` | Si no: añadir `.and(OrdenSpecification.porTipo(tipo)).and(OrdenSpecification.porTicker(ticker))` |

---

## Bloque 4 — Validaciones

| # | Tarea | Estado | Archivo(s) afectado(s) | Notas |
|---|---|:-:|---|---|
| T4.1 | Verificar que `TipoOrden` inválido retorna 400 con mensaje descriptivo | ✅ | `shared/exception/GlobalExceptionHandler.java` | Spring lanza `MethodArgumentTypeMismatchException` al convertir enum; capturar en handler |
| T4.2 | Verificar que `ticker` vacío o con solo espacios no aplica el filtro | ✅ | `ordenes/repository/OrdenSpecification.java` | Condición `ticker == null || ticker.isBlank()` → retornar null en el predicado |

---

## Bloque 5 — Frontend

| # | Tarea | Estado | Archivo(s) afectado(s) | Notas |
|---|---|:-:|---|---|
| T5.1 | Añadir selector de tipo de orden (dropdown: Todos, MARKET, LIMIT, STOP_LOSS, TAKE_PROFIT) en el componente de historial Angular | ✅ | `frontend/src/app/dashboard/` o equivalente | |
| T5.2 | Añadir campo de texto para filtrar por ticker en el componente de historial | ✅ | Template Angular | |
| T5.3 | Enviar `tipoOrden` y `ticker` como query params al llamar al backend | ✅ | `ApiService` o equivalente | Omitir el param si está vacío/null |

---

## Bloque 6 — Pruebas manuales

| # | Tarea | Estado | Herramienta | Escenario |
|---|---|:-:|---|---|
| T6.1 | GET historial?tipoOrden=MARKET → solo órdenes MARKET | ✅ | Postman | |
| T6.2 | GET historial?ticker=AAPL → solo órdenes de AAPL | ✅ | Postman | |
| T6.3 | GET historial?ticker=aapl (minúscula) → mismos resultados que AAPL | ✅ | Postman | Case insensitive |
| T6.4 | GET historial?tipoOrden=LIMIT&ticker=AAPL&desde=2026-05-01 → intersección de todos los filtros | ✅ | Postman | |
| T6.5 | GET historial?tipoOrden=INVALIDO → 400 Bad Request | ✅ | Postman | |
| T6.6 | Verificar UI Angular: dropdown tipo + campo ticker filtran la lista visualmente | ✅ | Navegador | |

---

## Bloque 7 — Documentación y cierre

| # | Tarea | Estado | Archivo(s) afectado(s) | Notas |
|---|---|:-:|---|---|
| T7.1 | Marcar HU-25 como ✅ en `docs/PROGRESO.md` | ✅ | `docs/PROGRESO.md` | |
| T7.2 | Crear `docs/HU-25-historial-ordenes-filtro-tipo-activo/plan.md` y `tasks.md` | ✅ | Este archivo | |

---

## Notas de implementación

- El predicado `porTicker` hace un JOIN con `activo`. Si `Activo` está en el módulo `mercado` y `ordenes` no puede importar sus clases internas directamente, verificar que la entidad `Activo` está en un paquete compartido o que `Orden` tiene acceso directo a la tabla via `@JoinColumn`. Alternativa: añadir campo `ticker` desnormalizado en `orden` (menos preferible dado el diseño 3NF).
- Si HU-24 se implementó con `@Query` JPQL en lugar de `Specification`, extender la query JPQL con condiciones `AND (:tipoOrden IS NULL OR o.tipoOrden = :tipoOrden) AND (:ticker IS NULL OR UPPER(a.ticker) = UPPER(:ticker))`.
- Para el frontend, los filtros deben aplicarse en el momento en que el usuario confirma la búsqueda (botón "Filtrar" o `debounceTime` en el campo de ticker), no en cada keystroke, para evitar llamadas excesivas al backend.
