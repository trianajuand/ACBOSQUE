# Plan de implementación — HU-26: Historial de órdenes filtrado por estado

## Contexto

HU-26 es la tercera historia del grupo de filtros del historial de órdenes (junto con HU-24 — período y HU-25 — tipo/activo). Los tres filtros se combinan en un único endpoint `GET /api/ordenes/historial`. Esta historia agrega el parámetro `estado` a ese endpoint ya existente.

Estado actual: **Completada**. El filtro por estado está implementado en backend y frontend.

---

## Objetivo

Permitir que el inversionista filtre su historial de órdenes por estado (`PENDIENTE`, `ENVIADA`, `EN_COLA`, `EJECUTADA`, `CANCELADA`), combinable con los filtros de período (HU-24) y tipo/activo (HU-25).

---

## Decisiones de diseño

| Decisión | Elección | Motivo |
|---|---|---|
| Endpoint único vs. endpoints separados | Endpoint único con parámetros opcionales | Simplicidad en el frontend; los filtros son ortogonales y se combinan con AND |
| Validación del enum | Dejar que Spring deserialice el `String → EstadoOrden`; excepción de deserialización devuelve 400 automáticamente | Evita código extra de validación manual |
| Módulo | `ordenes` — `OrdenService` + `OrdenController` | El historial pertenece al módulo de órdenes |

---

## Componentes involucrados

| Capa | Clase | Responsabilidad |
|---|---|---|
| Controller | `OrdenController` | Recibe `@RequestParam(required = false) String estado` y lo pasa al servicio |
| Service | `OrdenService.obtenerHistorialOrdenes(...)` | Aplica filtro `WHERE o.estado = :estado` si el parámetro no es nulo |
| Repository | `OrdenRepository` | Consulta JPA con `@Query` dinámica o `Specification` |
| DTO | `OrdenDTO` | Incluye el campo `estado` en la respuesta |

---

## Flujo de datos

```
Frontend
  → GET /api/ordenes/historial?estado=EJECUTADA  [JWT]
      → OrdenController.historial(..., String estado)
          → OrdenService.obtenerHistorialOrdenes(usuarioId, desde, hasta, tipoOrden, simbolo, estado)
              → OrdenRepository (filtro combinado)
                  ← List<Orden>
              ← List<OrdenDTO>
          ← RespuestaDTO{data: [...]}
      ← 200 OK
  ← tabla de órdenes filtradas
```

---

## Consideraciones de calidad

- No aplica escenario EC especifico para esta historia, pero el filtro se apoya en el índice `idx_orden_estado` para rendimiento.
- Estado inválido genera `400 Bad Request` por deserialización automática del enum (no requiere manejo manual).
- El endpoint ya estaba cubierto por auditoría de autenticación (JWT obligatorio).
