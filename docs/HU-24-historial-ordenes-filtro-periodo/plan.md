# Plan de Implementación — HU-24: Historial de órdenes filtrado por período

---

## Resumen ejecutivo

HU-24 implementa `GET /api/ordenes/historial` con parámetros opcionales `desde` y `hasta` (fecha ISO 8601). Retorna todas las órdenes del inversionista autenticado cuya `fecha_creacion` cae en el rango especificado, ordenadas por fecha descendente. Sin parámetros, retorna todo el historial. Este endpoint es compartido con HU-25 (filtros por tipo y activo) y HU-26 (filtro por estado); los tres filtros se pueden combinar.

---

## Alcance

| Incluido | Excluido |
|---|---|
| `GET /api/ordenes/historial?desde=&hasta=` | Paginación (implementación futura) |
| Filtro opcional por rango de fechas | Exportación a CSV/PDF (eso es HU-27) |
| Lista vacía → 200 con `[]` | Filtros de HU-25 y HU-26 (aunque comparten endpoint, cada HU documenta su parte) |
| Ordenado por `fecha_creacion DESC` | |
| Solo órdenes del inversionista autenticado | |

---

## Decisiones técnicas

| Decisión | Justificación |
|---|---|
| Endpoint compartido `GET /api/ordenes/historial` para HU-24, HU-25, HU-26 | Los filtros son ortogonales y se combinan naturalmente como query params; un endpoint único es más simple y RESTful |
| Parámetros opcionales (sin default) | Si no se especifican fechas, retornar todo el historial es el comportamiento más útil para el usuario |
| `LocalDate` como tipo de los parámetros | Fechas sin hora; el filtro aplica `fecha_creacion >= desde 00:00:00` y `fecha_creacion <= hasta 23:59:59` |
| Query dinámica con `Specification` o `@Query` JPQL | Los filtros opcionales se construyen dinámicamente; `Specification` de Spring Data JPA permite componerlos sin condicionales de String |
| Sin auditoría | Consulta de solo lectura |

---

## Módulos involucrados

| Módulo | Componente | Cambio |
|---|---|---|
| `ordenes` | `OrdenController` | `@GetMapping("/historial")` con `@RequestParam` opcionales |
| `ordenes` | `OrdenService` | `listarHistorial(String correo, LocalDate desde, LocalDate hasta, TipoOrden tipo, String ticker, EstadoOrden estado)` |
| `ordenes` | `OrdenRepository` | Query dinámica con `JpaSpecificationExecutor` o método `@Query` con parámetros opcionales |
| `ordenes` | `OrdenDTO` | Ya definido en HU-22; verificar que incluye todos los campos del historial |

---

## Modelo de datos relevante

```sql
-- Consulta base con filtro de período
SELECT o.*, a.ticker
FROM orden o
JOIN activo a ON a.id = o.activo_id
WHERE o.inversionista_id = :inversionistaId
  AND (:desde IS NULL OR o.fecha_creacion >= :desde)
  AND (:hasta IS NULL OR o.fecha_creacion <= :hasta::date + INTERVAL '1 day')
ORDER BY o.fecha_creacion DESC;
```

---

## Campos de `OrdenDTO` para el historial

| Campo | Fuente | Uso en UI |
|---|---|---|
| `id` | `orden.id` | |
| `ticker` | `activo.ticker` | |
| `tipoOrden` | `orden.tipo_orden` | |
| `lado` | `orden.lado` | |
| `cantidad` | `orden.cantidad` | |
| `precioEjecucion` | `orden.precio_ejecucion` | Precio al que se ejecutó |
| `montoTotal` | `orden.monto_total` | |
| `comision` | `orden.comision` | |
| `estado` | `orden.estado` | |
| `fechaCreacion` | `orden.fecha_creacion` | |
| `fechaEjecucion` | `orden.fecha_ejecucion` | Puede ser null si no ejecutada |

---

## Criterios de aceptación (resumen ejecutivo)

1. `GET /api/ordenes/historial?desde=2026-05-01&hasta=2026-05-31` retorna solo órdenes del período.
2. `GET /api/ordenes/historial` sin parámetros retorna todo el historial.
3. Período sin órdenes retorna 200 con `[]`.
4. Fecha inválida (ej. `desde=foobar`) retorna 400.
5. Sin JWT retorna 401.
6. Solo incluye órdenes del inversionista autenticado.

---

## Estrategia de pruebas

| Tipo | Herramienta | Escenario clave |
|---|---|---|
| Integración manual | Postman | GET historial con fechas → solo órdenes del rango |
| Integración manual | Postman | GET historial sin parámetros → todo el historial |
| Integración manual | Postman | Fecha inválida → 400 |
| UI | Dashboard Angular | Selectores de fecha aplicados → lista se filtra |

---

## Estimación

| Tarea | Puntos de historia |
|---|---|
| Controller con `@RequestParam` opcionales | 1 |
| Service: método con filtros opcionales | 2 |
| Repository: query dinámica | 3 |
| Validación de formato de fecha | 1 |
| Pruebas manuales | 1 |
| **Total** | **8** |

---

## Estado

- [x] Implementación completada
- [x] Pruebas manuales pasadas
- [x] `PROGRESO.md` actualizado
