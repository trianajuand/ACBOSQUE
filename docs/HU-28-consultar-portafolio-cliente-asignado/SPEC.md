# SPEC — Consulta de portafolio de cliente asignado (comisionista)

---

## Ficha de la historia

| Campo | Valor |
|---|---|
| ID | HU-28 |
| Sprint | 4 |
| Prioridad MoSCoW | Should Have |
| Estado | Completada |
| Épica | Órdenes / Comisionista |
| CU asociado | CU-28 |
| Autor | Juan Diego Triana Mejia |
| Creada | 2026-05-20 |
| Última revisión | 2026-05-24 |
| Versión de esta spec | 1.0 |

**Trazabilidad:**

| Tipo | ID | Descripción |
|---|---|---|
| Requerimiento funcional | RF-27 | Comisionista consulta portafolio de clientes asignados |
| Escenario de calidad | EC-18 | Encapsulate — acceso restringido a clientes asignados únicamente |
| Historia que precede a esta | HU-37 | La asignación de comisionista debe existir antes |

---

## Historia de usuario

**Como** comisionista autenticado,
**quiero** ver la lista de mis clientes asignados y el portafolio de cada uno,
**para** hacer seguimiento de sus inversiones sin poder ver inversionistas que no me corresponden.

---

## Actores y precondiciones

### Actores

| Actor | Rol en el sistema | Participación |
|---|---|---|
| Comisionista autenticado | `COMISIONISTA` | Iniciador |
| `OrdenService` / `PortafolioService` | Módulo `ordenes` | Consulta de datos del cliente |
| `IAsignacionComisionista` | Módulo `autenticacion` | Valida que el cliente está asignado al comisionista |

### Precondiciones

- JWT válido con rol `COMISIONISTA`.
- El `clienteId` solicitado está asignado al comisionista en `asignacion_comisionista`.

---

## Flujo principal

1. Comisionista navega a su panel de clientes.
2. Frontend envía `GET /api/comisionista/clientes` con JWT.
3. El sistema retorna la lista de clientes asignados al comisionista.
4. Comisionista selecciona un cliente y el frontend envía `GET /api/comisionista/clientes/{clienteId}/portafolio`.
5. `IAsignacionComisionista.validarClienteAsignado(comisionistaId, clienteId)`: verifica asignación. Si no está asignado → 403.
6. Retorna `PortafolioDTO` del cliente.

---

## Flujos de error

### Error 1 — No autenticado o rol incorrecto

| Campo | Valor |
|---|---|
| Condición | JWT ausente o rol ≠ COMISIONISTA |
| HTTP | 401 / 403 |

### Error 2 — Cliente no asignado al comisionista

| Campo | Valor |
|---|---|
| Condición | `clienteId` no está en la lista de clientes asignados |
| HTTP | 403 Forbidden |
| Cuerpo | `RespuestaDTO{error: "No tienes acceso a este cliente"}` |
| Evento de auditoría | `ACCESO_DENEGADO_CLIENTE_NO_ASIGNADO` |

### Error 3 — Cliente no encontrado

| Campo | Valor |
|---|---|
| HTTP | 404 Not Found |

---

## Contrato de API

### Endpoint 1 — `GET /api/comisionista/clientes`

```yaml
GET /api/comisionista/clientes:
  summary: Lista todos los clientes asignados al comisionista
  security:
    - bearerAuth: []  # Solo COMISIONISTA
  responses:
    '200':
      description: Lista de clientes asignados
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'
```

### Endpoint 2 — `GET /api/comisionista/clientes/{clienteId}/portafolio`

```yaml
GET /api/comisionista/clientes/{clienteId}/portafolio:
  summary: Consulta el portafolio de un cliente asignado
  security:
    - bearerAuth: []  # Solo COMISIONISTA
  parameters:
    - name: clienteId
      in: path
      required: true
      schema:
        type: integer
  responses:
    '200':
      description: Portafolio del cliente
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/PortafolioDTO'
    '403':
      description: Cliente no asignado al comisionista
    '404':
      description: Cliente no encontrado
```

---

## Módulos y arquitectura

| Módulo | Rol | Componentes |
|---|---|---|
| `ordenes` | Endpoints y lógica | `ComisionistaController`, `PortafolioService` |
| `autenticacion` | Validación de asignación | `IAsignacionComisionista` |
| `trazabilidad` | Auditoría de acceso denegado | `AuditLogService` |

### Escenarios de calidad y tácticas materializadas

| EC | Táctica | Cómo se materializa en HU-28 |
|---|---|---|
| EC-18 | Encapsulate | La validación de asignación garantiza que un comisionista solo ve sus clientes |

---

## Criterios de verificación

### Escenarios de aceptación (Gherkin)

```gherkin
Funcionalidad: Consulta de portafolio de cliente asignado

  Antecedentes:
    Dado que "comis@test.com" tiene JWT válido con rol=COMISIONISTA
    Y "ana@test.com" está asignada como cliente de "comis@test.com" (clienteId=5)

  Escenario: Consulta exitosa del portafolio del cliente
    Cuando se envía GET /api/comisionista/clientes/5/portafolio con JWT de comisionista
    Entonces el sistema responde 200 OK con PortafolioDTO del cliente 5

  Escenario: Acceso denegado a cliente no asignado
    Cuando se envía GET /api/comisionista/clientes/99/portafolio
    Entonces el sistema responde 403 Forbidden
    Y se emite evento ACCESO_DENEGADO_CLIENTE_NO_ASIGNADO en auditoría
```

---

## Definición de terminado

- [x] `GET /api/comisionista/clientes` lista los clientes asignados.
- [x] `GET /api/comisionista/clientes/{clienteId}/portafolio` retorna portafolio solo si el cliente está asignado.
- [x] Acceso a cliente no asignado retorna 403 y audita el evento.
- [x] `docs/PROGRESO.md` marcado con ✅ para HU-28.

---

## Historial de cambios

| Versión | Fecha | Descripción | Razón |
|---|---|---|---|
| 1.0 | 2026-05-24 | Refactorización a estructura SDD del proyecto. | Unificación de todos los SPEC.md bajo plantilla canónica SDD del proyecto |
