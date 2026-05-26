# Plan de implementación — HU-29: Consulta de órdenes de cliente asignado (comisionista)

## Contexto

HU-29 complementa a HU-28. Si HU-28 da al comisionista visibilidad del portafolio, HU-29 le da visibilidad de la actividad operativa: órdenes activas e historial de órdenes del cliente. Ambas historias comparten el mismo control de acceso (EC-18) y el mismo controlador (`ComisionistaController`).

Estado actual: **Completada**. Endpoints implementados.

---

## Objetivo

Exponer dos endpoints adicionales en `ComisionistaController`:
1. `GET /api/comisionista/clientes/{clienteId}/ordenes/activas` — órdenes activas del cliente.
2. `GET /api/comisionista/clientes/{clienteId}/ordenes/historial` — historial con filtros opcionales.

---

## Decisiones de diseño

| Decisión | Elección | Motivo |
|---|---|---|
| Reutilizar métodos de `IOrden` | `obtenerOrdenesActivas(clienteId)` y `obtenerHistorialOrdenes(clienteId)` — mismos métodos que usa el inversionista | Evita duplicar lógica de negocio; el comisionista ve los mismos datos que el inversionista |
| Validación de asignación | Idéntica a HU-28: `IAsignacionComisionista.validarClienteAsignado()` antes de cualquier consulta | Consistencia en el mecanismo de control de acceso |
| Filtros del historial | La implementación actual pasa `clienteId` sin filtros adicionales; los filtros opcionales (desde/hasta/estado/ticker) son mejora futura | Alcance mínimo funcional; los filtros del comisionista se pueden agregar sin romper la API |
| Módulo | `ordenes` — `ComisionistaController` | Extensión natural del mismo controlador de HU-28 |

---

## Componentes involucrados

| Capa | Clase | Responsabilidad |
|---|---|---|
| Controller | `ComisionistaController` | Valida asignación y delega a `IOrden` |
| Interface | `IAsignacionComisionista` | Valida acceso al cliente |
| Interface | `IOrden.obtenerOrdenesActivas(Long clienteId)` | Retorna órdenes activas del cliente |
| Interface | `IOrden.obtenerHistorialOrdenes(Long clienteId)` | Retorna historial del cliente |

---

## Flujo de datos

```
Comisionista
  → GET /api/comisionista/clientes/{clienteId}/ordenes/activas  [JWT COMISIONISTA]
      → ComisionistaController.ordenesActivasCliente(correo, clienteId)
          → resolverComisionista(correo)
          → IAsignacionComisionista.validarClienteAsignado(comisionistaId, clienteId)
          → IOrden.obtenerOrdenesActivas(clienteId)
              ← List<OrdenDTO> (estados: PENDIENTE, ENVIADA, EN_COLA, PENDIENTE_APROBACION, APROBADA)
      ← 200 OK con RespuestaDTO{data: [...]}
```

---

## Consideraciones de calidad

- Los estados que se muestran en "órdenes activas" del cliente incluyen `PENDIENTE_APROBACION` y `APROBADA` además de los estados estándar, ya que el comisionista necesita ver el estado de sus propias propuestas.
- El historial del cliente visto por el comisionista incluye todas las órdenes (propias del inversionista y las que el comisionista firmó), para dar una vista completa de la actividad.
