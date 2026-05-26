# Tasks — HU-24: Historial de órdenes filtrado por período

---

## Leyenda

| Símbolo | Significado |
|---|---|
| ✅ | Completada |
| 🔄 | En progreso |
| ⬜ | Pendiente |
| ❌ | Bloqueada |

---

## Bloque 1 — Repository: query dinámica

| # | Tarea | Estado | Archivo(s) afectado(s) | Notas |
|---|---|:-:|---|---|
| T1.1 | Decidir estrategia de query dinámica: `JpaSpecificationExecutor` o `@Query` JPQL con `IS NULL OR` | ✅ | `ordenes/repository/OrdenRepository.java` | Ambas son válidas; `Specification` es más limpio para 4+ filtros combinables |
| T1.2 | Si se usa `Specification`: extender `JpaSpecificationExecutor<Orden>` en `OrdenRepository` | ✅ | `ordenes/repository/OrdenRepository.java` | |
| T1.3 | Crear `OrdenSpecification` o método builder de `Specification<Orden>` | ✅ | `ordenes/repository/OrdenSpecification.java` o equivalente | Métodos: `porInversionista`, `desdeFecha`, `hastaFecha`, `porTipo`, `porTicker`, `porEstado` |
| T1.4 | Verificar que el JOIN con `activo` para obtener `ticker` no genera N+1 | ✅ | `ordenes/repository/OrdenRepository.java` | Usar `JOIN FETCH` o `EntityGraph` |

---

## Bloque 2 — Service

| # | Tarea | Estado | Archivo(s) afectado(s) | Notas |
|---|---|:-:|---|---|
| T2.1 | Implementar `OrdenService.listarHistorial(String correo, LocalDate desde, LocalDate hasta, TipoOrden tipo, String ticker, EstadoOrden estado)` | ✅ | `ordenes/service/OrdenService.java` | Aceptar nulls para parámetros opcionales; no aplicar filtros null |
| T2.2 | Construir `Specification` combinando solo los filtros no-null | ✅ | `ordenes/service/OrdenService.java` | `Specification.where(spec1).and(spec2)` |
| T2.3 | Ejecutar query ordenada por `fecha_creacion DESC` | ✅ | `ordenes/service/OrdenService.java` | `Sort.by(Sort.Direction.DESC, "fechaCreacion")` |
| T2.4 | Mapear resultado a `List<OrdenDTO>` | ✅ | `ordenes/service/OrdenService.java` | |

---

## Bloque 3 — Controller

| # | Tarea | Estado | Archivo(s) afectado(s) | Notas |
|---|---|:-:|---|---|
| T3.1 | Añadir `@GetMapping("/historial")` en `OrdenController` con `@RequestParam` opcionales | ✅ | `ordenes/controller/OrdenController.java` | `@RequestParam(required = false)` para cada filtro |
| T3.2 | Parsear `desde` y `hasta` como `LocalDate` (Spring convierte automáticamente con formato `yyyy-MM-dd`) | ✅ | `ordenes/controller/OrdenController.java` | Si el parsing falla, Spring retorna 400 automáticamente |
| T3.3 | Pasar todos los filtros al service; incluir `tipoOrden`, `ticker` y `estado` para HU-25/HU-26 | ✅ | `ordenes/controller/OrdenController.java` | El mismo endpoint sirve para HU-24, HU-25 y HU-26 |

---

## Bloque 4 — Validación de parámetros

| # | Tarea | Estado | Archivo(s) afectado(s) | Notas |
|---|---|:-:|---|---|
| T4.1 | Verificar que `desde > hasta` retorna 400 con mensaje de error claro | ✅ | `ordenes/service/OrdenService.java` | Validar en el service antes de ejecutar la query |
| T4.2 | Verificar que fecha con formato incorrecto (ej. `31/05/2026`) retorna 400 | ✅ | `shared/exception/GlobalExceptionHandler.java` | Spring lanza `MethodArgumentTypeMismatchException` → capturar en handler |

---

## Bloque 5 — DTO

| # | Tarea | Estado | Archivo(s) afectado(s) | Notas |
|---|---|:-:|---|---|
| T5.1 | Añadir campos de historial a `OrdenDTO` si faltan: `precioEjecucion`, `montoTotal`, `comision`, `fechaEjecucion` | ✅ | `ordenes/dto/OrdenDTO.java` | `fechaEjecucion` puede ser null para órdenes no ejecutadas |

---

## Bloque 6 — Frontend

| # | Tarea | Estado | Archivo(s) afectado(s) | Notas |
|---|---|:-:|---|---|
| T6.1 | Verificar que el componente Angular de historial llama `GET /api/ordenes/historial?desde=&hasta=` con los selectores de fecha | ✅ | `frontend/src/app/dashboard/` o equivalente | |
| T6.2 | Mostrar lista vacía con mensaje "No hay órdenes en el período seleccionado" | ✅ | Template Angular | |

---

## Bloque 7 — Pruebas manuales

| # | Tarea | Estado | Herramienta | Escenario |
|---|---|:-:|---|---|
| T7.1 | GET historial?desde=2026-05-01&hasta=2026-05-31 → solo órdenes de mayo | ✅ | Postman | Verificar fechas en response |
| T7.2 | GET historial sin parámetros → todo el historial del usuario | ✅ | Postman | |
| T7.3 | GET historial?desde=2020-01-01&hasta=2020-12-31 → lista vacía 200 | ✅ | Postman | |
| T7.4 | GET historial?desde=foobar → 400 Bad Request | ✅ | Postman | |
| T7.5 | GET historial sin JWT → 401 | ✅ | Postman | |
| T7.6 | Verificar que no retorna órdenes de otro inversionista | ✅ | Postman | Crear orden con usuario B, consultar con usuario A |

---

## Bloque 8 — Documentación y cierre

| # | Tarea | Estado | Archivo(s) afectado(s) | Notas |
|---|---|:-:|---|---|
| T8.1 | Marcar HU-24 como ✅ en `docs/PROGRESO.md` | ✅ | `docs/PROGRESO.md` | |
| T8.2 | Crear `docs/HU-24-historial-ordenes-filtro-periodo/plan.md` y `tasks.md` | ✅ | Este archivo | |

---

## Notas de implementación

- El endpoint `/historial` es el mismo para HU-24, HU-25 y HU-26. Implementar los parámetros de todas las HU en el mismo método del controller y service para no duplicar código.
- El filtro `hasta` debe incluir el día completo: si `hasta = 2026-05-31`, la query debe incluir órdenes hasta `2026-05-31 23:59:59`. Usar `hasta.atTime(LocalTime.MAX)` al construir la `Specification`.
- Usar `@Transactional(readOnly = true)` en el método de servicio para optimizar la lectura en PostgreSQL.
