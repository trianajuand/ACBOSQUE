# Plan de Implementación — HU-25: Historial de órdenes filtrado por tipo de orden y activo

---

## Resumen ejecutivo

HU-25 añade los parámetros `tipoOrden` y `ticker` al endpoint `GET /api/ordenes/historial` implementado en HU-24. Permite al inversionista buscar órdenes por tipo (MARKET, LIMIT, STOP_LOSS, TAKE_PROFIT) y/o por símbolo del activo. Ambos filtros son opcionales, case insensitive para `ticker`, y se combinan con los filtros de período (HU-24) y estado (HU-26). No requiere nuevo endpoint ni nuevas tablas.

---

## Alcance

| Incluido | Excluido |
|---|---|
| Parámetro `tipoOrden` en `GET /api/ordenes/historial` | Nuevo endpoint REST |
| Parámetro `ticker` (case insensitive, JOIN con tabla `activo`) | Filtro por nombre de empresa |
| Combinación con filtros de HU-24 y HU-26 | Búsqueda de texto libre en órdenes |
| Validación de `tipoOrden` como enum válido | |

---

## Decisiones técnicas

| Decisión | Justificación |
|---|---|
| `ticker` resuelto via JOIN con tabla `activo` | `orden.activo_id` → `activo.ticker`; no hay campo ticker directo en `orden` después de la normalización 3NF |
| Comparación case insensitive para `ticker` | Los tickers son uppercase (AAPL, TSLA) pero el usuario puede escribir en minúscula |
| `tipoOrden` como enum en el parámetro del controller | Spring convierte automáticamente el String a enum y retorna 400 si el valor no es válido |
| Filtros reutilizados de `OrdenSpecification` (HU-24) | Los métodos `porTipo` y `porTicker` de `OrdenSpecification` se añaden al mismo builder |

---

## Módulos involucrados

| Módulo | Componente | Cambio |
|---|---|---|
| `ordenes` | `OrdenController` | Añadir `@RequestParam(required = false) TipoOrden tipoOrden` y `@RequestParam(required = false) String ticker` |
| `ordenes` | `OrdenService` | El método `listarHistorial` ya acepta estos parámetros (definido en HU-24) |
| `ordenes` | `OrdenSpecification` | Añadir predicados `porTipo(TipoOrden tipo)` y `porTicker(String ticker)` |
| `ordenes` | `OrdenRepository` | Sin cambio si usa `JpaSpecificationExecutor` (ya habilitado en HU-24) |

---

## Especificaciones de los predicados

```java
// Predicado para tipo de orden
public static Specification<Orden> porTipo(TipoOrden tipo) {
    return (root, query, cb) ->
        tipo == null ? null : cb.equal(root.get("tipoOrden"), tipo);
}

// Predicado para ticker (case insensitive, JOIN con activo)
public static Specification<Orden> porTicker(String ticker) {
    return (root, query, cb) -> {
        if (ticker == null || ticker.isBlank()) return null;
        Join<Orden, Activo> activo = root.join("activo", JoinType.INNER);
        return cb.equal(cb.upper(activo.get("ticker")), ticker.toUpperCase());
    };
}
```

---

## Dependencias

- HU-24 completada (endpoint `/historial` base en su lugar, `OrdenSpecification` creada).
- `Orden.activo` debe ser relación `@ManyToOne` a entidad `Activo` con campo `ticker`.
- Enum `TipoOrden` incluye `MARKET`, `LIMIT`, `STOP_LOSS`, `TAKE_PROFIT`.

---

## Criterios de aceptación (resumen ejecutivo)

1. `GET /api/ordenes/historial?tipoOrden=MARKET` → solo órdenes de tipo MARKET.
2. `GET /api/ordenes/historial?ticker=AAPL` → solo órdenes del activo AAPL (case insensitive).
3. `GET /api/ordenes/historial?tipoOrden=LIMIT&ticker=AAPL&desde=2026-05-01` → combinación correcta de filtros.
4. `tipoOrden=INVALIDO` → 400 Bad Request.
5. Sin JWT → 401.

---

## Estrategia de pruebas

| Tipo | Herramienta | Escenario clave |
|---|---|---|
| Integración manual | Postman | GET historial?tipoOrden=MARKET → solo MARKET |
| Integración manual | Postman | GET historial?ticker=aapl (minúscula) → retorna AAPL |
| Integración manual | Postman | Combinación tipoOrden + ticker + desde → intersección correcta |
| Integración manual | Postman | tipoOrden=INVALIDO → 400 |
| UI | Dashboard Angular | Selector de tipo de orden filtra la lista |

---

## Estimación

| Tarea | Puntos de historia |
|---|---|
| Predicados `porTipo` y `porTicker` en OrdenSpecification | 2 |
| Controller: añadir `@RequestParam` para tipo y ticker | 1 |
| Pruebas manuales combinadas | 2 |
| **Total** | **5** |

---

## Estado

- [x] Implementación completada
- [x] Pruebas manuales pasadas
- [x] `PROGRESO.md` actualizado
