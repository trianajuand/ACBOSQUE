# SPEC — Historial de órdenes filtrado por período

---

## Ficha de la historia

| Campo | Valor |
|---|---|
| ID | HU-24 |
| Sprint | 3 |
| Prioridad MoSCoW | Must Have |
| Estado | Completada |
| Épica | Órdenes / Historial |
| CU asociado | CU-24 |
| Autor | Juan Diego Triana Mejia |
| Creada | 2026-05-20 |
| Última revisión | 2026-05-24 |
| Versión de esta spec | 1.0 |

**Trazabilidad:**

| Tipo | ID | Descripción |
|---|---|---|
| Requerimiento funcional | RF-23 | Historial de órdenes con filtro por período |
| Historia relacionada | HU-25 | Filtro por tipo de orden y símbolo (mismo endpoint) |
| Historia relacionada | HU-26 | Filtro por estado (mismo endpoint) |

---

## Historia de usuario

**Como** inversionista autenticado,
**quiero** consultar el historial de mis órdenes filtrando por un rango de fechas,
**para** revisar las operaciones que realicé en un período específico.

---

## Actores y precondiciones

### Actores

| Actor | Rol en el sistema | Participación |
|---|---|---|
| Inversionista autenticado | `INVERSIONISTA` | Iniciador |
| `OrdenService` | Módulo `ordenes` | Consulta historial con filtros |

### Precondiciones

- JWT válido.
- Parámetros `desde` y `hasta` en formato ISO 8601 (`yyyy-MM-dd`) si se proveen.

---

## Flujo principal

1. Frontend llama `GET /api/ordenes/historial?desde=2026-05-01&hasta=2026-05-31` con JWT.
2. `OrdenService` consulta órdenes del usuario filtrando por `fecha_creacion BETWEEN desde AND hasta`.
3. Retorna lista paginada de `OrdenDTO` ordenada por `fecha_creacion DESC`.

---

## Flujos de error

### Error 1 — No autenticado

| Campo | Valor |
|---|---|
| HTTP | 401 Unauthorized |

### Error 2 — Formato de fecha inválido

| Campo | Valor |
|---|---|
| Condición | `desde` o `hasta` no parseable como fecha |
| HTTP | 400 Bad Request |

### Error 3 — Sin órdenes en el período

| Campo | Valor |
|---|---|
| Condición | No hay órdenes que cumplan los filtros |
| HTTP | 200 OK con lista vacía |

---

## Contrato de API

### Endpoint — `GET /api/ordenes/historial`

> **Nota:** HU-24, HU-25 y HU-26 comparten este mismo endpoint. Los parámetros se pueden combinar.

```yaml
GET /api/ordenes/historial:
  summary: Consulta el historial de órdenes del inversionista con filtros opcionales
  security:
    - bearerAuth: []
  parameters:
    - name: desde
      in: query
      required: false
      schema:
        type: string
        format: date
      description: "Fecha de inicio del período (yyyy-MM-dd)"
      example: "2026-05-01"
    - name: hasta
      in: query
      required: false
      schema:
        type: string
        format: date
      description: "Fecha de fin del período (yyyy-MM-dd)"
      example: "2026-05-31"
    - name: tipoOrden
      in: query
      required: false
      schema:
        type: string
        enum: [MARKET, LIMIT, STOP_LOSS, TAKE_PROFIT]
      description: "Filtro por tipo de orden (HU-25)"
    - name: simbolo
      in: query
      required: false
      schema:
        type: string
      description: "Filtro por símbolo del activo (HU-25) — parámetro real en OrdenController"
      example: "AAPL"
    - name: estado
      in: query
      required: false
      schema:
        type: string
        enum: [PENDIENTE, ENVIADA, EN_COLA, EJECUTADA, CANCELADA]
      description: "Filtro por estado de la orden (HU-26)"
  responses:
    '200':
      description: Historial de órdenes (puede estar vacío)
      content:
        application/json:
          schema:
            type: array
            items:
              $ref: '#/components/schemas/OrdenDTO'
    '400':
      description: Parámetros de fecha inválidos
    '401':
      description: No autenticado
    '500':
      description: Error interno
```

---

## Criterios de verificación

### Escenarios de aceptación (Gherkin)

```gherkin
Funcionalidad: Historial de órdenes por período

  Antecedentes:
    Dado que "ana@test.com" tiene órdenes en mayo 2026 y junio 2026

  Escenario: Filtro por período retorna solo órdenes del rango
    Cuando se envía GET /api/ordenes/historial?desde=2026-05-01&hasta=2026-05-31
    Entonces el sistema responde 200 OK
    Y todas las órdenes tienen fecha_creacion entre 2026-05-01 y 2026-05-31

  Escenario: Sin parámetros retorna todo el historial
    Cuando se envía GET /api/ordenes/historial sin parámetros
    Entonces el sistema responde 200 OK
    Y se retornan todas las órdenes del inversionista

  Escenario: Período sin órdenes retorna lista vacía
    Cuando se envía GET /api/ordenes/historial?desde=2020-01-01&hasta=2020-12-31
    Entonces el sistema responde 200 OK con lista vacía
```

---

## Definición de terminado

- [x] `GET /api/ordenes/historial?desde=&hasta=` filtra por rango de fechas.
- [x] Sin parámetros retorna todo el historial del inversionista.
- [x] Sin JWT responde 401.
- [x] Solo muestra órdenes del inversionista autenticado.
- [x] `docs/PROGRESO.md` marcado con ✅ para HU-24.

---

## Historial de cambios

| Versión | Fecha | Descripción | Razón |
|---|---|---|---|
| 1.0 | 2026-05-24 | Refactorización a estructura SDD del proyecto. | Unificación de todos los SPEC.md bajo plantilla canónica SDD del proyecto |
| 1.1 | 2026-05-26 | Auditoría SDD: parámetro `ticker` → `simbolo` en la definición del endpoint (compartido con HU-25). | Parámetro real en `OrdenController` es `simbolo`. |
