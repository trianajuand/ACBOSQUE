# SPEC — Consulta de órdenes de cliente asignado (comisionista)

---

## Ficha de la historia

| Campo | Valor |
|---|---|
| ID | HU-29 |
| Sprint | 4 |
| Prioridad MoSCoW | Should Have |
| Estado | Completada |
| Épica | Órdenes / Comisionista |
| CU asociado | CU-29 |
| Autor | Juan Diego Triana Mejia |
| Creada | 2026-05-20 |
| Última revisión | 2026-05-24 |
| Versión de esta spec | 1.0 |

**Trazabilidad:**

| Tipo | ID | Descripción |
|---|---|---|
| Requerimiento funcional | RF-28 | Comisionista consulta órdenes activas e históricas de sus clientes |
| Escenario de calidad | EC-18 | Acceso restringido a clientes asignados únicamente |
| Historia que precede a esta | HU-28 | Mismo control de acceso de clientes asignados |

---

## Historia de usuario

**Como** comisionista autenticado,
**quiero** consultar las órdenes activas e históricas de mis clientes asignados,
**para** hacer seguimiento de su actividad operativa y ofrecer recomendaciones informadas.

---

## Actores y precondiciones

Idénticos a HU-28.

### Precondiciones adicionales

- Para historial: parámetros de filtro opcionales (`desde`, `hasta`, `estado`, `simbolo`).

---

## Flujo principal

1. Comisionista selecciona un cliente y navega a su historial de órdenes.
2. Frontend envía `GET /api/comisionista/clientes/{clienteId}/ordenes/activas` o `/historial` con JWT.
3. Sistema valida asignación comisionista-cliente.
4. Retorna `List<OrdenDTO>` filtrada.

---

## Flujos de error

Idénticos a HU-28.

---

## Contrato de API

### Endpoint 1 — `GET /api/comisionista/clientes/{clienteId}/ordenes/activas`

```yaml
GET /api/comisionista/clientes/{clienteId}/ordenes/activas:
  summary: Órdenes activas de un cliente asignado
  security:
    - bearerAuth: []
  parameters:
    - name: clienteId
      in: path
      required: true
      schema:
        type: integer
  responses:
    '200':
      description: Lista de órdenes activas del cliente (PENDIENTE, ENVIADA, EN_COLA, PENDIENTE_APROBACION, APROBADA)
    '403':
      description: Cliente no asignado
    '404':
      description: Cliente no encontrado
```

### Endpoint 2 — `GET /api/comisionista/clientes/{clienteId}/ordenes/historial`

```yaml
GET /api/comisionista/clientes/{clienteId}/ordenes/historial:
  summary: Historial de órdenes de un cliente asignado con filtros opcionales
  security:
    - bearerAuth: []
  parameters:
    - name: clienteId
      in: path
      required: true
      schema:
        type: integer
    - name: desde
      in: query
      required: false
      schema:
        type: string
        format: date
    - name: hasta
      in: query
      required: false
      schema:
        type: string
        format: date
    - name: estado
      in: query
      required: false
      schema:
        type: string
    - name: ticker
      in: query
      required: false
      schema:
        type: string
      description: "Filtrar por ticker del activo (ej. AAPL) — resuelto vía JOIN con tabla activo"
  responses:
    '200':
      description: Historial de órdenes del cliente
    '403':
      description: Cliente no asignado
```

---

## Módulos y arquitectura

Idénticos a HU-28.

---

## Criterios de verificación

### Escenarios de aceptación (Gherkin)

```gherkin
Funcionalidad: Consulta de órdenes de cliente asignado

  Escenario: Órdenes activas del cliente
    Dado que "comis@test.com" tiene cliente asignado id=5
    Cuando se envía GET /api/comisionista/clientes/5/ordenes/activas
    Entonces el sistema responde 200 OK
    Y solo aparecen órdenes del cliente 5

  Escenario: Historial filtrado por período
    Cuando se envía GET /api/comisionista/clientes/5/ordenes/historial?desde=2026-05-01
    Entonces solo aparecen órdenes desde 2026-05-01

  Escenario: Acceso denegado a cliente no asignado
    Cuando se intenta consultar órdenes de clienteId=99 (no asignado)
    Entonces el sistema responde 403 Forbidden
```

---

## Definición de terminado

- [x] `GET /api/comisionista/clientes/{id}/ordenes/activas` retorna órdenes activas del cliente.
- [x] `GET /api/comisionista/clientes/{id}/ordenes/historial` retorna historial con filtros.
- [x] Acceso a cliente no asignado retorna 403.
- [x] `docs/PROGRESO.md` marcado con ✅ para HU-29.

---

## Historial de cambios

| Versión | Fecha | Descripción | Razón |
|---|---|---|---|
| 1.0 | 2026-05-24 | Refactorización a estructura SDD del proyecto. | Unificación de todos los SPEC.md bajo plantilla canónica SDD del proyecto |
