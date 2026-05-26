# Plan de Implementación — HU-22: Consulta de órdenes activas

---

## Resumen ejecutivo

HU-22 expone `GET /api/ordenes/activas` que retorna la lista de órdenes del inversionista autenticado en los estados `PENDIENTE`, `ENVIADA` o `EN_COLA`. Es una consulta de solo lectura: no modifica datos, no llama a Alpaca y no emite eventos de auditoría. La lista vacía retorna 200 con array vacío (nunca 404). Este endpoint es la puerta de entrada para que el usuario visualice sus operaciones en curso y decida si cancela alguna (HU-21).

---

## Alcance

| Incluido | Excluido |
|---|---|
| `GET /api/ordenes/activas` con filtro por inversionista del JWT | Órdenes de otros inversionistas |
| Filtro de estados: `PENDIENTE`, `ENVIADA`, `EN_COLA` | Órdenes `EJECUTADA` o `CANCELADA` (ver HU-24/HU-26 para historial) |
| Respuesta `200` con lista de `OrdenDTO` (puede ser vacía) | Paginación (la cantidad de órdenes activas simultáneas es acotada) |
| Mapeo de entidad `Orden` a `OrdenDTO` | |

---

## Decisiones técnicas

| Decisión | Justificación |
|---|---|
| Endpoint separado `/activas` en lugar de filtrar el historial | Semántica más clara; el frontend lo usa en un panel dedicado ("Órdenes en curso") |
| Lista vacía → 200 con `[]`, no 404 | Estándar REST para colecciones: una colección vacía no es un error |
| Sin auditoría | Consultas de solo lectura no se auditan (solo se auditan mutaciones) |
| `OrdenDTO` en lugar de entidad `Orden` | Regla 11 del proyecto: nunca exponer entidades JPA al frontend |

---

## Módulos involucrados

| Módulo | Componente | Cambio |
|---|---|---|
| `ordenes` | `OrdenController` | `@GetMapping("/activas")` |
| `ordenes` | `OrdenService` | `listarOrdenesActivas(String correo)` → consulta con filtro de estados |
| `ordenes` | `OrdenRepository` | `findByInversionistaIdAndEstadoIn(Long inversionistaId, List<EstadoOrden> estados)` |
| `ordenes` | `OrdenDTO` | Verificar que incluye todos los campos necesarios para la UI |
| `autenticacion` | `IConsultaInversionista` | Obtener `inversionistaId` a partir del correo del JWT |

---

## Modelo de datos relevante

```sql
-- Consulta base
SELECT * FROM orden
WHERE inversionista_id = :inversionistaId
  AND estado IN ('PENDIENTE', 'ENVIADA', 'EN_COLA')
ORDER BY fecha_creacion DESC;
```

---

## Campos de `OrdenDTO` requeridos por la UI

| Campo | Fuente | Uso en UI |
|---|---|---|
| `id` | `orden.id` | Para botón "Cancelar" (HU-21) |
| `ticker` | `activo.ticker` (JOIN) | Mostrar símbolo en tabla |
| `tipoOrden` | `orden.tipo_orden` | Columna tipo |
| `lado` | `orden.lado` | COMPRA / VENTA |
| `cantidad` | `orden.cantidad` | |
| `precioLimite` | `orden.precio_limite` | Para órdenes LIMIT/TAKE_PROFIT |
| `precioStop` | `orden.precio_stop` | Para órdenes STOP_LOSS |
| `estado` | `orden.estado` | Badge de estado |
| `fechaCreacion` | `orden.fecha_creacion` | |

---

## Criterios de aceptación (resumen ejecutivo)

1. `GET /api/ordenes/activas` con JWT válido retorna 200 con lista de órdenes activas del inversionista.
2. La lista contiene únicamente órdenes en `PENDIENTE`, `ENVIADA` o `EN_COLA`.
3. No incluye órdenes de otros inversionistas.
4. Sin órdenes activas: retorna 200 con `[]`.
5. Sin JWT: retorna 401.

---

## Estrategia de pruebas

| Tipo | Herramienta | Escenario clave |
|---|---|---|
| Integración manual | Postman | GET /activas con JWT → lista no vacía |
| Integración manual | Postman | GET /activas sin órdenes activas → `[]` |
| Integración manual | Postman | GET /activas sin JWT → 401 |
| UI | `dashboard.html` / Angular | Panel "Órdenes activas" muestra la lista correctamente |

---

## Estimación

| Tarea | Puntos de historia |
|---|---|
| Endpoint en controller | 1 |
| Método en service + query en repository | 2 |
| Mapeo Orden → OrdenDTO | 1 |
| Pruebas manuales | 1 |
| **Total** | **5** |

---

## Estado

- [x] Implementación completada
- [x] Pruebas manuales pasadas
- [x] `PROGRESO.md` actualizado
