# Tareas de implementación — HU-28: Consulta de portafolio de cliente asignado

## Estado general: COMPLETADA

---

## Tareas backend

- [x] **T1 — Crear `ComisionistaController` en `ordenes/controller/`**
  - Mapping base: `@RequestMapping("/api/comisionista")`
  - Constructor: inyección de `UsuarioRepository`, `IAsignacionComisionista`, `IOrden`.
  - Método privado `resolverComisionista(String correo)`: busca el usuario por correo y verifica que su rol sea `COMISIONISTA`; lanza `403` si no.

- [x] **T2 — Endpoint `GET /api/comisionista/clientes`**
  - Llama a `IAsignacionComisionista.listarClientesAsignados(comisionistaId)`.
  - Retorna `RespuestaDTO.exito(List<ClienteAsignadoDTO>)`.

- [x] **T3 — Endpoint `GET /api/comisionista/clientes/{clienteId}/portafolio`**
  - Llama a `IAsignacionComisionista.validarClienteAsignado(comisionistaId, clienteId)` — lanza `403` si no asignado.
  - Llama a `IOrden.obtenerPortafolio(clienteId)`.
  - Retorna `RespuestaDTO.exito(PortafolioDTO)`.

- [x] **T4 — Implementar `IAsignacionComisionista.validarClienteAsignado()` en `autenticacion`**
  - Consulta `asignacion_comisionista` buscando el par `(comisionistaId, clienteId)`.
  - Si no existe → lanza excepción que el controlador convierte en 403.
  - Registra evento `ACCESO_DENEGADO_CLIENTE_NO_ASIGNADO` en auditoría.

- [x] **T5 — Implementar `IAsignacionComisionista.listarClientesAsignados()` en `autenticacion`**
  - Retorna `List<ClienteAsignadoDTO>` con los datos básicos de cada cliente (id, nombre, correo).

- [x] **T6 — Registrar endpoints en `SecurityConfig`**
  - Ruta `/api/comisionista/**` requiere rol `COMISIONISTA`.

---

## Tareas frontend (Angular)

- [x] **T7 — Agregar panel de clientes en la vista del comisionista**
  - Lista los clientes con datos básicos (nombre, correo).
  - Al seleccionar un cliente → carga su portafolio.

- [x] **T8 — Mostrar portafolio del cliente seleccionado**
  - Misma estructura visual que el portafolio del inversionista: holdings, precio promedio, precio mercado, valor total, P&L.

---

## Tareas de verificación

- [x] **T9 — Probar `GET /api/comisionista/clientes` con JWT de comisionista**
  - Resultado esperado: lista de clientes asignados.

- [x] **T10 — Probar `GET /api/comisionista/clientes/{id}/portafolio` con cliente asignado**
  - Resultado esperado: 200 OK con PortafolioDTO.

- [x] **T11 — Probar con cliente NO asignado**
  - Resultado esperado: 403 Forbidden.

- [x] **T12 — Probar con JWT de inversionista (rol incorrecto)**
  - Resultado esperado: 403 Forbidden.

- [x] **T13 — Actualizar `docs/PROGRESO.md`**
  - Marcar HU-28 con ✅ en la tabla del Sprint 4.
