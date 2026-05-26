# SPEC — Consulta de órdenes activas

---

## Ficha de la historia

| Campo | Valor |
|---|---|
| ID | HU-22 |
| Sprint | 3 |
| Prioridad MoSCoW | Must Have |
| Estado | Completada |
| Épica | Órdenes / Gestión |
| CU asociado | CU-22 |
| Autor | Juan Diego Triana Mejia |
| Creada | 2026-05-20 |
| Última revisión | 2026-05-24 |
| Versión de esta spec | 1.0 |

**Trazabilidad:**

| Tipo | ID | Descripción |
|---|---|---|
| Requerimiento funcional | RF-21 | Consulta de órdenes activas (PENDIENTE, ENVIADA, EN_COLA) |
| Historia que precede a esta | HU-17..20 | Las órdenes activas son las creadas y no ejecutadas |
| Historia relacionada | HU-21 | Desde aquí el inversionista puede cancelar una orden activa |

---

## Historia de usuario

**Como** inversionista autenticado,
**quiero** ver la lista de mis órdenes activas (pendientes, enviadas o en cola),
**para** monitorear mis operaciones en curso y decidir si cancelar alguna.

---

## Actores y precondiciones

### Actores

| Actor | Rol en el sistema | Participación |
|---|---|---|
| Inversionista autenticado | `INVERSIONISTA` | Iniciador |
| `OrdenService` | Módulo `ordenes` | Consulta órdenes activas del inversionista |

### Precondiciones

- JWT válido.

### Postcondiciones

- Respuesta 200 con lista de `OrdenDTO` en estados PENDIENTE, ENVIADA o EN_COLA.

---

## Flujo principal

1. Frontend llama `GET /api/ordenes/activas` con JWT.
2. `OrdenService` filtra `orden` donde `inversionista_id = inversionista.id` (del JWT) y `estado IN (PENDIENTE, ENVIADA, EN_COLA)`.
3. Retorna lista de `OrdenDTO`.

---

## Flujos de error

### Error 1 — No autenticado

| Campo | Valor |
|---|---|
| Condición | JWT ausente o inválido |
| HTTP | 401 Unauthorized |

### Error 2 — Sin órdenes activas

| Campo | Valor |
|---|---|
| Condición | No hay órdenes en estado activo |
| HTTP | 200 OK |
| Cuerpo | Lista vacía `[]` |

---

## Contrato de API

### Endpoint — `GET /api/ordenes/activas`

```yaml
GET /api/ordenes/activas:
  summary: Retorna las órdenes activas del inversionista autenticado
  security:
    - bearerAuth: []
  responses:
    '200':
      description: Lista de órdenes activas (puede estar vacía)
      content:
        application/json:
          schema:
            type: array
            items:
              $ref: '#/components/schemas/OrdenDTO'
          example:
            - id: 42
              ticker: "AAPL"
              tipoOrden: "MARKET"
              lado: "COMPRA"
              cantidad: 10
              estado: "EN_COLA"
              fechaCreacion: "2026-05-24T14:30:00"
    '401':
      description: No autenticado
    '500':
      description: Error interno
```

---

## Criterios de verificación

### Escenarios de aceptación (Gherkin)

```gherkin
Funcionalidad: Consulta de órdenes activas

  Escenario: Inversionista con órdenes activas
    Dado que "ana@test.com" tiene 2 órdenes EN_COLA y 1 ENVIADA
    Cuando se envía GET /api/ordenes/activas con JWT
    Entonces el sistema responde 200 OK
    Y la lista contiene 3 elementos
    Y todos tienen estado PENDIENTE, ENVIADA o EN_COLA

  Escenario: Sin órdenes activas retorna lista vacía
    Dado que "ana@test.com" no tiene órdenes activas
    Cuando se envía GET /api/ordenes/activas
    Entonces el sistema responde 200 OK
    Y la lista está vacía

  Escenario: Sin JWT — 401
    Cuando se envía GET /api/ordenes/activas sin Authorization
    Entonces el sistema responde 401 Unauthorized
```

---

## Definición de terminado

- [x] `GET /api/ordenes/activas` retorna solo órdenes en estado PENDIENTE, ENVIADA o EN_COLA.
- [x] Lista vacía retorna 200 con array vacío.
- [x] Solo muestra órdenes del inversionista autenticado.
- [x] Sin JWT responde 401.
- [x] `docs/PROGRESO.md` marcado con ✅ para HU-22.

---

## Historial de cambios

| Versión | Fecha | Descripción | Razón |
|---|---|---|---|
| 1.0 | 2026-05-24 | Refactorización a estructura SDD del proyecto. | Unificación de todos los SPEC.md bajo plantilla canónica SDD del proyecto |
