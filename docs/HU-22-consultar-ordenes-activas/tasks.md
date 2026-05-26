# Tasks — HU-22: Consulta de órdenes activas

---

## Leyenda

| Símbolo | Significado |
|---|---|
| ✅ | Completada |
| 🔄 | En progreso |
| ⬜ | Pendiente |
| ❌ | Bloqueada |

---

## Bloque 1 — Repository

| # | Tarea | Estado | Archivo(s) afectado(s) | Notas |
|---|---|:-:|---|---|
| T1.1 | Añadir/verificar `findByInversionistaIdAndEstadoIn(Long id, List<EstadoOrden> estados)` en `OrdenRepository` | ✅ | `ordenes/repository/OrdenRepository.java` | Spring Data JPA genera la query automáticamente; alternativamente usar `@Query` con JPQL |

---

## Bloque 2 — Service

| # | Tarea | Estado | Archivo(s) afectado(s) | Notas |
|---|---|:-:|---|---|
| T2.1 | Implementar `OrdenService.listarOrdenesActivas(String correo)` | ✅ | `ordenes/service/OrdenService.java` | Resolver `inversionistaId` desde correo vía `IConsultaInversionista` |
| T2.2 | Llamar al repository con `estados = [PENDIENTE, ENVIADA, EN_COLA]` | ✅ | `ordenes/service/OrdenService.java` | |
| T2.3 | Mapear lista de `Orden` a lista de `OrdenDTO` | ✅ | `ordenes/service/OrdenService.java` | Verificar que `OrdenDTO` incluye `ticker` (requiere JOIN con `activo`) |

---

## Bloque 3 — Controller

| # | Tarea | Estado | Archivo(s) afectado(s) | Notas |
|---|---|:-:|---|---|
| T3.1 | Añadir `@GetMapping("/activas")` en `OrdenController` | ✅ | `ordenes/controller/OrdenController.java` | Retorna `ResponseEntity<List<OrdenDTO>>` |
| T3.2 | Extraer correo del JWT y pasarlo a `OrdenService.listarOrdenesActivas` | ✅ | `ordenes/controller/OrdenController.java` | Usar `SecurityContextHolder` o `@AuthenticationPrincipal` |

---

## Bloque 4 — DTO

| # | Tarea | Estado | Archivo(s) afectado(s) | Notas |
|---|---|:-:|---|---|
| T4.1 | Verificar que `OrdenDTO` contiene: `id`, `ticker`, `tipoOrden`, `lado`, `cantidad`, `precioLimite`, `precioStop`, `estado`, `fechaCreacion` | ✅ | `ordenes/dto/OrdenDTO.java` | Añadir campos faltantes si no existen |
| T4.2 | Verificar que `ticker` se resuelve correctamente (JOIN con tabla `activo` o campo desnormalizado) | ✅ | `ordenes/dto/OrdenDTO.java` o mapper | Si `Orden` tiene `activoId`, el mapper debe buscar el `Activo` para obtener el ticker |

---

## Bloque 5 — Pruebas manuales

| # | Tarea | Estado | Herramienta | Escenario |
|---|---|:-:|---|---|
| T5.1 | Crear 2 órdenes EN_COLA y 1 ENVIADA; GET /activas → lista con 3 elementos | ✅ | Postman | Verificar que todas tienen estado activo |
| T5.2 | Con cuenta sin órdenes activas: GET /activas → `[]` | ✅ | Postman | |
| T5.3 | Sin JWT: GET /activas → 401 | ✅ | Postman | |
| T5.4 | Verificar que órdenes EJECUTADA/CANCELADA no aparecen | ✅ | Postman | Crear una ejecutada y una cancelada antes de llamar |
| T5.5 | Verificar UI: panel "Órdenes activas" en `dashboard.html` o componente Angular muestra la lista correctamente | ✅ | Navegador | Botón "Cancelar" visible por orden |

---

## Bloque 6 — Documentación y cierre

| # | Tarea | Estado | Archivo(s) afectado(s) | Notas |
|---|---|:-:|---|---|
| T6.1 | Marcar HU-22 como ✅ en `docs/PROGRESO.md` | ✅ | `docs/PROGRESO.md` | |
| T6.2 | Crear `docs/HU-22-consultar-ordenes-activas/plan.md` y `tasks.md` | ✅ | Este archivo | |

---

## Notas de implementación

- Este endpoint es consultado periódicamente por el frontend para mantener actualizado el panel de órdenes en curso. Si el frontend hace polling, asegurarse de que la query sea liviana (índice en `(inversionista_id, estado)`).
- No se registra evento de auditoría para esta consulta (es de solo lectura).
- Si `Orden.activo` es una relación `@ManyToOne` con `fetch = LAZY`, agregar JOIN FETCH en la query del repository para evitar N+1 queries al mapear el `ticker`.
