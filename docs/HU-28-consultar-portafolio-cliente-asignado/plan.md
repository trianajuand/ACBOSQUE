# Plan de implementación — HU-28: Consulta de portafolio de cliente asignado (comisionista)

## Contexto

HU-28 es la primera historia del flujo de comisionista. Permite que un comisionista autenticado consulte el portafolio de sus clientes asignados. La restricción principal (EC-18 — Encapsulate) garantiza que un comisionista solo accede a los datos de sus propios clientes, no a los de otros comisionistas.

Estado actual: **Completada**. Endpoints implementados y validados.

---

## Objetivo

Exponer dos endpoints bajo `/api/comisionista`:
1. `GET /api/comisionista/clientes` — lista los clientes asignados al comisionista autenticado.
2. `GET /api/comisionista/clientes/{clienteId}/portafolio` — retorna el portafolio del cliente, previa validación de asignación.

---

## Decisiones de diseño

| Decisión | Elección | Motivo |
|---|---|---|
| Validación de asignación | `IAsignacionComisionista.validarClienteAsignado()` lanza excepción si no asignado | Encapsula la lógica de acceso dentro del módulo autenticación; el módulo ordenes no necesita saber cómo se gestionan las asignaciones |
| Portafolio reutilizado | `IOrden.obtenerPortafolio(clienteId)` — mismo método que usa el inversionista para su propio portafolio | Reutilización de lógica existente; el comisionista ve exactamente la misma vista que ve el inversionista |
| Rol requerido | `COMISIONISTA` validado en `ComisionistaController.resolverComisionista()` | Centraliza la validación de rol para todos los endpoints del controlador |
| Módulo | `ordenes` — `ComisionistaController` | Los datos de portafolio pertenecen al módulo de órdenes |

---

## Componentes involucrados

| Capa | Clase | Responsabilidad |
|---|---|---|
| Controller | `ComisionistaController` | Valida rol, resuelve ID del comisionista, llama a los servicios |
| Interface | `IAsignacionComisionista` | Valida que el cliente está asignado al comisionista |
| Interface | `IOrden.obtenerPortafolio(Long clienteId)` | Retorna el portafolio del cliente |
| Service | `PortafolioService` (a través de `IOrden`) | Lógica de composición del portafolio con precios actuales |
| Trazabilidad | `IAuditLog` | Registra `ACCESO_DENEGADO_CLIENTE_NO_ASIGNADO` si corresponde |

---

## Flujo de datos

```
Comisionista
  → GET /api/comisionista/clientes/{clienteId}/portafolio  [JWT COMISIONISTA]
      → ComisionistaController.portafolioCliente(correo, clienteId)
          → resolverComisionista(correo)  [valida rol COMISIONISTA]
          → IAsignacionComisionista.validarClienteAsignado(comisionistaId, clienteId)
              [403 si no asignado + auditoría ACCESO_DENEGADO]
          → IOrden.obtenerPortafolio(clienteId)
              ← PortafolioDTO (holdings, precio promedio, valor total, P&L)
      ← 200 OK con RespuestaDTO{data: PortafolioDTO}
```

---

## Escenario de calidad materializado

**EC-18 — Encapsulate (Acceso restringido a recursos propios):**
La validación `IAsignacionComisionista.validarClienteAsignado` lanza `403 Forbidden` si el `clienteId` no pertenece al comisionista autenticado. Esta comprobación ocurre antes de cualquier consulta de datos, garantizando que no hay fugas de información.

---

## Consideraciones de calidad

- El mismo controlador gestiona todos los endpoints del comisionista, centralizando la validación de rol en `resolverComisionista()`.
- La tabla `asignacion_comisionista` debe estar indexada por `comisionista_id` para rendimiento.
- El portafolio se compone con precios de caché para no impactar APIs externas en cada consulta.
