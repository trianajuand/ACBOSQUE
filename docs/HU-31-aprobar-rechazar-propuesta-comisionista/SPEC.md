# SPEC — Aprobación o rechazo de propuesta del comisionista

---

## Ficha de la historia

| Campo | Valor |
|---|---|
| ID | HU-31 |
| Sprint | 4 |
| Prioridad MoSCoW | Should Have |
| Estado | Completada |
| Épica | Órdenes / Comisionista |
| CU asociado | CU-31 |
| Autor | Juan Diego Triana Mejia |
| Creada | 2026-05-20 |
| Última revisión | 2026-05-24 |
| Versión de esta spec | 1.0 |

**Trazabilidad:**

| Tipo | ID | Descripción |
|---|---|---|
| Requerimiento funcional | RF-30 | Inversionista aprueba o rechaza propuesta creada por comisionista |
| Escenario de calidad | EC-12 | Trazabilidad de PROPUESTA_ORDEN_APROBADA / PROPUESTA_ORDEN_RECHAZADA |
| Historia que precede a esta | HU-30 | El comisionista crea la propuesta con estado PENDIENTE_APROBACION |
| Historia que sigue | HU-32 | El comisionista firma y envía la propuesta aprobada al mercado |

---

## Historia de usuario

**Como** inversionista autenticado,
**quiero** ver las propuestas de orden que me ha enviado mi comisionista y decidir aprobarlas o rechazarlas con un comentario opcional,
**para** mantener el control final sobre las operaciones que se ejecutan en mi nombre.

---

## Motivación y contexto

### Por qué existe esta historia

El flujo de comisionista garantiza que ninguna operación se ejecute en nombre del inversionista sin su consentimiento explícito. Tras la propuesta (HU-30), el inversionista tiene control total: aprueba o rechaza. Solo las propuestas aprobadas avanzan a firma y envío (HU-32). No se reservan fondos ni se interactúa con Alpaca en este paso.

### Ciclo de vida de la propuesta

```
PENDIENTE_APROBACION (HU-30)
    ↓ aprueba         ↓ rechaza
APROBADA             RECHAZADA
    ↓ HU-32
ENVIADA / EN_COLA / EJECUTADA
```

---

## Actores y precondiciones

### Actores

| Actor | Rol en el sistema | Participación |
|---|---|---|
| Inversionista autenticado | `INVERSIONISTA` | Toma la decisión de aprobación |
| `OrdenService` | Módulo `ordenes` | Cambia el estado de la orden |
| `AuditLogService` | Módulo `trazabilidad` | Registra la decisión |

### Precondiciones

- JWT válido con rol `INVERSIONISTA`.
- La propuesta existe en `propuesta_orden` y pertenece al inversionista autenticado (`inversionistaId = inversionista.id`).
- La propuesta tiene estado `PENDIENTE_APROBACION`.

### Postcondiciones (aprobación)

- `PropuestaOrden.estado = APROBADA`.
- `PropuestaOrden.aprobadaEn = now()`.
- `PropuestaOrden.comentarioInversionista` almacenado (puede ser null).
- Evento `PROPUESTA_ORDEN_APROBADA` registrado.

### Postcondiciones (rechazo)

- `PropuestaOrden.estado = RECHAZADA`.
- `PropuestaOrden.rechazadaEn = now()`.
- `PropuestaOrden.comentarioInversionista` almacenado (puede ser null).
- Evento `PROPUESTA_ORDEN_RECHAZADA` registrado.

---

## Flujo principal — Consultar propuestas pendientes

1. Inversionista navega a la sección de propuestas.
2. Frontend envía `GET /api/propuestas` con JWT.
3. `OrdenService.obtenerPropuestasPendientesInversionista(inversionistaId)` consulta registros en `propuesta_orden` con `estado = PENDIENTE_APROBACION` y `inversionistaId = inversionistaId`.
4. Retorna `List<OrdenDTO>`.

## Flujo principal — Aprobar propuesta

1. Inversionista revisa la propuesta y decide aprobar.
2. Frontend envía `POST /api/propuestas/{propuestaId}/aprobar` con JWT y body opcional `DecisionPropuestaDTO`.
3. `OrdenService.aprobarPropuesta(propuestaId, inversionistaId, comentario)`:
   a. Busca la propuesta en `propuesta_orden` por `propuestaId`. Si no existe → 404.
   b. Valida que `propuesta.inversionistaId == inversionistaId`. Si no → 403.
   c. Valida que `propuesta.estado == PENDIENTE_APROBACION`. Si no → 400.
   d. Actualiza `estado = APROBADA`, `aprobadaEn = now()`, `comentarioInversionista`.
   e. Persiste la orden.
4. `IAuditLog.registrar(PROPUESTA_ORDEN_APROBADA, correo_inversionista, "Propuesta {propuestaId} aprobada")`.
5. Responde `200 OK` con `OrdenDTO` actualizada.

## Flujo principal — Rechazar propuesta

1. Inversionista revisa la propuesta y decide rechazar.
2. Frontend envía `POST /api/propuestas/{propuestaId}/rechazar` con JWT y body opcional `DecisionPropuestaDTO`.
3. `OrdenService.rechazarPropuesta(propuestaId, inversionistaId, comentario)`:
   a. Busca la propuesta en `propuesta_orden` por `propuestaId`. Si no existe → 404.
   b. Valida que `propuesta.inversionistaId == inversionistaId`. Si no → 403.
   c. Valida que `propuesta.estado == PENDIENTE_APROBACION`. Si no → 400.
   d. Actualiza `estado = RECHAZADA`, `rechazadaEn = now()`, `comentarioInversionista`.
   e. Persiste la orden.
4. `IAuditLog.registrar(PROPUESTA_ORDEN_RECHAZADA, correo_inversionista, "Propuesta {propuestaId} rechazada")`.
5. Responde `200 OK` con `OrdenDTO` actualizada.

---

## Flujos de error

### Error 1 — No autenticado o rol incorrecto

| Campo | Valor |
|---|---|
| Condición | JWT ausente o rol ≠ INVERSIONISTA |
| HTTP | 401 / 403 |

### Error 2 — Propuesta no encontrada

| Campo | Valor |
|---|---|
| Condición | `propuestaId` no existe en BD |
| HTTP | 404 Not Found |
| Cuerpo | `RespuestaDTO{error: "Orden no encontrada"}` |

### Error 3 — Propuesta de otro inversionista

| Campo | Valor |
|---|---|
| Condición | `propuesta_orden.inversionistaId ≠ inversionista.id` |
| HTTP | 403 Forbidden |
| Cuerpo | `RespuestaDTO{error: "No tienes permiso para gestionar esta propuesta"}` |

### Error 4 — Estado incorrecto

| Campo | Valor |
|---|---|
| Condición | `orden.estado ≠ PENDIENTE_APROBACION` |
| HTTP | 400 Bad Request |
| Cuerpo | `RespuestaDTO{error: "La propuesta no está en estado PENDIENTE_APROBACION"}` |

---

## Contrato de API

### Endpoint 1 — `GET /api/propuestas`

```yaml
GET /api/propuestas:
  summary: Lista las propuestas pendientes del inversionista autenticado
  security:
    - bearerAuth: []  # Solo INVERSIONISTA
  responses:
    '200':
      description: Lista de propuestas en estado PENDIENTE_APROBACION
      content:
        application/json:
          schema:
            type: array
            items:
              $ref: '#/components/schemas/OrdenDTO'
          example:
            - id: 42
              activoId: 1
              ticker: "AAPL"
              tipoOrden: "LIMIT"
              lado: "COMPRA"
              cantidad: 5
              precioLimite: 185.00
              estado: "PENDIENTE_APROBACION"
              comentarioComisionista: "Recomiendo comprar AAPL; precio de entrada favorable"
              comisionistaId: 7
    '401':
      description: No autenticado
    '403':
      description: Rol incorrecto
```

### Endpoint 2 — `POST /api/propuestas/{propuestaId}/aprobar`

```yaml
POST /api/propuestas/{propuestaId}/aprobar:
  summary: Aprueba una propuesta de orden del comisionista
  security:
    - bearerAuth: []  # Solo INVERSIONISTA
  parameters:
    - name: propuestaId
      in: path
      required: true
      schema:
        type: integer
  requestBody:
    required: false
    content:
      application/json:
        schema:
          $ref: '#/components/schemas/DecisionPropuestaDTO'
        example:
          comentario: "De acuerdo con la recomendación"
  responses:
    '200':
      description: Propuesta aprobada exitosamente
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/OrdenDTO'
    '400':
      description: La propuesta no está en estado PENDIENTE_APROBACION
    '403':
      description: No autorizado para gestionar esta propuesta
    '404':
      description: Propuesta no encontrada

components:
  schemas:
    DecisionPropuestaDTO:
      type: object
      properties:
        comentario:
          type: string
          nullable: true
          description: Comentario opcional del inversionista sobre su decisión
```

### Endpoint 3 — `POST /api/propuestas/{propuestaId}/rechazar`

```yaml
POST /api/propuestas/{propuestaId}/rechazar:
  summary: Rechaza una propuesta de orden del comisionista
  security:
    - bearerAuth: []  # Solo INVERSIONISTA
  parameters:
    - name: propuestaId
      in: path
      required: true
      schema:
        type: integer
  requestBody:
    required: false
    content:
      application/json:
        schema:
          $ref: '#/components/schemas/DecisionPropuestaDTO'
        example:
          comentario: "No estoy de acuerdo con el precio de entrada"
  responses:
    '200':
      description: Propuesta rechazada exitosamente
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/OrdenDTO'
    '400':
      description: La propuesta no está en estado PENDIENTE_APROBACION
    '403':
      description: No autorizado para gestionar esta propuesta
    '404':
      description: Propuesta no encontrada
```

---

## Modelo de datos

Los campos necesarios para esta historia están en la tabla `propuesta_orden` (ver DDL completo en HU-30):

```sql
-- Campos relevantes de propuesta_orden para el flujo de aprobación
inversionista_id          BIGINT NOT NULL REFERENCES inversionista(id),
comentario_inversionista  TEXT,           -- comentario opcional al aprobar o rechazar
aprobada_en               TIMESTAMP,      -- timestamp de aprobación
rechazada_en              TIMESTAMP,      -- timestamp de rechazo
estado                    VARCHAR(50),    -- 'PENDIENTE_APROBACION', 'APROBADA', 'RECHAZADA'
```

No se crean tablas adicionales en esta historia. Los estados de propuesta viven exclusivamente en `propuesta_orden`, no en `orden`.

---

## Módulos y arquitectura

| Módulo | Rol | Componentes |
|---|---|---|
| `ordenes` | Coordinador | `PropuestaController`, `OrdenService` |
| `trazabilidad` | Auditoría | `AuditLogService` vía `IAuditLog` |

### Interfaces utilizadas

| Interfaz | Módulo proveedor | Método |
|---|---|---|
| `IAuditLog` | `trazabilidad` | `registrar(evento, correo, detalle)` |

### Escenarios de calidad y tácticas materializadas

| EC | Táctica | Cómo se materializa en HU-31 |
|---|---|---|
| EC-12 | Audit Trail | PROPUESTA_ORDEN_APROBADA / PROPUESTA_ORDEN_RECHAZADA con correo del inversionista |
| EC-18 | Encapsulate | Validación de que la propuesta pertenece al inversionista autenticado antes de permitir la acción |

---

## Eventos y efectos transversales

| Evento | Cuándo | Consumidor |
|---|---|---|
| `PROPUESTA_ORDEN_APROBADA` | Al aprobar la propuesta | `trazabilidad` |
| `PROPUESTA_ORDEN_RECHAZADA` | Al rechazar la propuesta | `trazabilidad` |

Nota: No hay efecto sobre fondos ni sobre Alpaca en este paso. Los fondos se reservan en HU-32 al momento de firmar y enviar.

---

## Riesgos

| # | Riesgo | P | I | Mitigación |
|---|---|:-:|:-:|---|
| R1 | Doble aprobación por clic doble | Baja | Medio | Validación de estado `PENDIENTE_APROBACION` como guarda idempotente |
| R2 | Comisionista ve comentario del rechazo | — | — | El comentario del inversionista se devuelve en OrdenDTO; el comisionista puede consultarlo vía sus endpoints propios |

---

## Criterios de verificación

### Escenarios de aceptación (Gherkin)

```gherkin
Funcionalidad: Aprobación o rechazo de propuesta del comisionista

  Antecedentes:
    Dado que "comis@test.com" creó una propuesta para "ana@test.com" (propuestaId=42, estado=PENDIENTE_APROBACION)
    Y "ana@test.com" tiene JWT válido con rol=INVERSIONISTA

  Escenario: Consulta de propuestas pendientes
    Cuando se envía GET /api/propuestas con JWT de "ana@test.com"
    Entonces el sistema responde 200 OK
    Y la lista incluye la propuesta con id=42 y estado="PENDIENTE_APROBACION"
    Y se muestra el comentarioComisionista de la propuesta

  Escenario: Aprobación exitosa sin comentario
    Cuando se envía POST /api/propuestas/42/aprobar con JWT de "ana@test.com" y body vacío
    Entonces el sistema responde 200 OK
    Y la orden retornada tiene estado="APROBADA"
    Y aprobadaEn no es nulo
    Y se emite evento PROPUESTA_ORDEN_APROBADA en auditoría

  Escenario: Aprobación exitosa con comentario
    Cuando se envía POST /api/propuestas/42/aprobar con comentario="De acuerdo"
    Entonces la orden retornada tiene comentarioInversionista="De acuerdo"

  Escenario: Rechazo exitoso con comentario
    Cuando se envía POST /api/propuestas/42/rechazar con comentario="No estoy de acuerdo con el precio"
    Entonces el sistema responde 200 OK
    Y la orden retornada tiene estado="RECHAZADA"
    Y rechazadaEn no es nulo
    Y se emite evento PROPUESTA_ORDEN_RECHAZADA en auditoría

  Escenario: Propuesta de otro inversionista retorna 403
    Dado que "otro@test.com" tiene JWT válido con rol=INVERSIONISTA
    Cuando "otro@test.com" intenta aprobar POST /api/propuestas/42/aprobar
    Entonces el sistema responde 403 Forbidden

  Escenario: Propuesta ya aprobada no puede aprobarse de nuevo
    Dado que la propuesta 42 ya tiene estado="APROBADA"
    Cuando se intenta POST /api/propuestas/42/aprobar
    Entonces el sistema responde 400 Bad Request

  Escenario: Propuesta no encontrada retorna 404
    Cuando se envía POST /api/propuestas/999/aprobar
    Entonces el sistema responde 404 Not Found
```

---

## Interfaz de usuario

- Lista de propuestas pendientes con: símbolo, tipo de orden, lado, cantidad, precio límite/stop estimado, comisión estimada, comentario del comisionista.
- Botones de acción por propuesta: **Aprobar** y **Rechazar**.
- Modal de confirmación con campo de texto opcional para el comentario del inversionista.
- Tras la acción: la propuesta desaparece de la lista de pendientes.

---

## Fuera de alcance

- Notificación push al comisionista cuando el inversionista toma la decisión (HU-41).
- Envío de la orden al mercado (HU-32).
- Modificación de la propuesta por el inversionista (no existe — solo aprobar o rechazar).

---

## Decisiones y preguntas abiertas

| # | Pregunta | Decisión |
|---|---|---|
| D1 | ¿El inversionista puede ver propuestas ya resueltas (APROBADA/RECHAZADA)? | No en este endpoint — solo PENDIENTE_APROBACION. El historial de órdenes incluye todos los estados. |
| D2 | ¿Se notifica al comisionista automáticamente? | Fuera de alcance de HU-31 — se maneja en HU-41. |

---

## Definición de terminado

- [x] `GET /api/propuestas` retorna propuestas con estado `PENDIENTE_APROBACION` del inversionista autenticado.
- [x] `POST /api/propuestas/{id}/aprobar` cambia estado a `APROBADA` y registra `aprobadaEn`.
- [x] `POST /api/propuestas/{id}/rechazar` cambia estado a `RECHAZADA` y registra `rechazadaEn`.
- [x] `comentarioInversionista` se persiste correctamente (null si no se provee).
- [x] Propuesta de otro inversionista retorna 403.
- [x] Propuesta no en `PENDIENTE_APROBACION` retorna 400.
- [x] Propuesta inexistente retorna 404.
- [x] Evento `PROPUESTA_ORDEN_APROBADA` / `PROPUESTA_ORDEN_RECHAZADA` registrado en auditoría.
- [x] `docs/PROGRESO.md` marcado con ✅ para HU-31.

---

## Historial de cambios

| Versión | Fecha | Descripción | Razón |
|---|---|---|---|
| 1.0 | 2026-05-24 | Refactorización a estructura SDD del proyecto. | Unificación de todos los SPEC.md bajo plantilla canónica SDD del proyecto |
