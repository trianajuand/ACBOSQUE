# SPEC — Cancelación de orden pendiente

---

## Ficha de la historia

| Campo | Valor |
|---|---|
| ID | HU-21 |
| Sprint | 3 |
| Prioridad MoSCoW | Must Have |
| Estado | Completada |
| Épica | Órdenes / Gestión |
| CU asociado | CU-21 |
| Autor | Juan Diego Triana Mejia |
| Creada | 2026-05-20 |
| Última revisión | 2026-05-24 |
| Versión de esta spec | 1.0 |

**Trazabilidad:**

| Tipo | ID | Descripción |
|---|---|---|
| Requerimiento funcional | RF-20 | Cancelación de órdenes que no han sido ejecutadas |
| Escenario de calidad | EC-12 | Trazabilidad de ORDEN_CANCELADA |
| Historia que precede a esta | HU-17..20 | Las órdenes a cancelar son creadas en estas historias |

---

## Historia de usuario

**Como** inversionista autenticado,
**quiero** cancelar una orden que aún no se ha ejecutado (PENDIENTE, ENVIADA o EN_COLA),
**para** liberar los fondos reservados y evitar que la operación llegue al mercado.

---

## Actores y precondiciones

### Actores

| Actor | Rol en el sistema | Participación en este flujo |
|---|---|---|
| Inversionista autenticado | `INVERSIONISTA` | Iniciador |
| `OrdenService` | Módulo `ordenes` | Valida estado de la orden, cancela en Alpaca si corresponde, libera fondos |
| `AlpacaAdapter` | Módulo `integracion` | Cancela la orden en Alpaca si fue enviada |
| `AuditLogService` | Módulo `trazabilidad` | Registra ORDEN_CANCELADA |

### Precondiciones

- JWT válido.
- La orden existe y pertenece al inversionista autenticado.
- La orden tiene estado `PENDIENTE`, `ENVIADA` o `EN_COLA` (no `EJECUTADA` ni `CANCELADA`).

### Postcondiciones

- `orden.estado = CANCELADA`.
- Si la orden era de compra: `fondos_reservados -= totalADebitar`, `saldo_disponible += totalADebitar`.
- Si la orden estaba en Alpaca (`ENVIADA`): se envía cancelación a Alpaca.
- Evento `ORDEN_CANCELADA` registrado en auditoría.

---

## Flujo principal

1. Usuario selecciona una orden activa y presiona "Cancelar".
2. Frontend envía `DELETE /api/ordenes/{ordenId}` con JWT.

**Backend — `OrdenService.cancelarOrden(ordenId, correo)`:**

3. Busca la orden por `ordenId`. Si no existe: 404.
4. Verifica que `orden.inversionistaId == inversionista.id` (la orden pertenece al inversionista autenticado). Si no: 403.
5. Verifica que `orden.estado IN (PENDIENTE, ENVIADA, EN_COLA)`. Si no: 400.
6. Si `orden.estado == ENVIADA` y `orden.alpacaOrderId != null`: llama `AlpacaAdapter.cancelarOrden(alpacaOrderId)`.
7. Actualiza `orden.estado = CANCELADA`.
8. Si era orden de compra: libera fondos (`saldo_disponible += totalADebitar`, `fondos_reservados -= totalADebitar`).
9. `IAuditLog.registrar(ORDEN_CANCELADA, correo, "Orden {ordenId} cancelada")`.
10. Responde `200 OK` con `RespuestaDTO{mensaje: "Orden cancelada exitosamente"}`.

---

## Flujos de error

### Error 1 — No autenticado

| Campo | Valor |
|---|---|
| Condición | JWT ausente o inválido |
| HTTP | 401 Unauthorized |

### Error 2 — Orden no encontrada

| Campo | Valor |
|---|---|
| Condición | No existe orden con el `ordenId` proporcionado |
| Excepción Java | `OrdenNoEncontradaException` |
| HTTP | 404 Not Found |
| Cuerpo | `RespuestaDTO{error: "Orden no encontrada"}` |

### Error 3 — Orden no pertenece al inversionista

| Campo | Valor |
|---|---|
| Condición | `orden.inversionistaId != inversionista.id` del JWT |
| HTTP | 403 Forbidden |
| Cuerpo | `RespuestaDTO{error: "No tienes permiso para cancelar esta orden"}` |

### Error 4 — Orden ya ejecutada o cancelada

| Campo | Valor |
|---|---|
| Condición | `orden.estado IN (EJECUTADA, CANCELADA)` |
| HTTP | 400 Bad Request |
| Cuerpo | `RespuestaDTO{error: "La orden no puede cancelarse en su estado actual"}` |

---

## Contrato de API

### Endpoint — `DELETE /api/ordenes/{ordenId}`

```yaml
DELETE /api/ordenes/{ordenId}:
  summary: Cancela una orden pendiente del inversionista autenticado
  security:
    - bearerAuth: []
  parameters:
    - name: ordenId
      in: path
      required: true
      schema:
        type: integer
      example: 42
  responses:
    '200':
      description: Orden cancelada exitosamente
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'
          example:
            mensaje: "Orden cancelada exitosamente"
    '400':
      description: La orden no puede cancelarse (ejecutada o ya cancelada)
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'
    '401':
      description: No autenticado
    '403':
      description: La orden no pertenece al inversionista
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'
    '404':
      description: Orden no encontrada
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'
```

---

## Módulos y arquitectura

| Módulo | Rol | Componentes específicos |
|---|---|---|
| `ordenes` | Coordinador | `OrdenController`, `OrdenService` |
| `integracion` | Cancelación en Alpaca | `AlpacaAdapter` |
| `trazabilidad` | Auditoría | `AuditLogService` |

### Escenarios de calidad y tácticas materializadas

| EC | Táctica | Cómo se materializa en HU-21 |
|---|---|---|
| EC-12 | Audit Trail | `ORDEN_CANCELADA` registrado con ID de orden |

---

## Criterios de verificación

### Escenarios de aceptación (Gherkin)

```gherkin
Funcionalidad: Cancelación de orden pendiente

  Antecedentes:
    Dado que "ana@test.com" tiene JWT válido y orden EN_COLA id=42

  Escenario: Cancelación exitosa de orden en cola
    Cuando se envía DELETE /api/ordenes/42 con JWT de "ana@test.com"
    Entonces el sistema responde 200 OK
    Y orden 42 tiene estado=CANCELADA
    Y los fondos reservados de "ana@test.com" disminuyeron
    Y se emite evento ORDEN_CANCELADA en auditoría

  Escenario: No se puede cancelar orden ejecutada
    Dado que orden 43 tiene estado=EJECUTADA
    Cuando se envía DELETE /api/ordenes/43
    Entonces el sistema responde 400 Bad Request

  Escenario: Orden de otro usuario retorna 403
    Dado que orden 44 pertenece a otro usuario
    Cuando "ana@test.com" envía DELETE /api/ordenes/44
    Entonces el sistema responde 403 Forbidden

  Escenario: Orden inexistente retorna 404
    Cuando se envía DELETE /api/ordenes/9999
    Entonces el sistema responde 404 Not Found
```

---

## Definición de terminado

- [x] `DELETE /api/ordenes/{id}` cancela orden PENDIENTE/ENVIADA/EN_COLA y retorna 200.
- [x] Fondos reservados liberados en órdenes de compra canceladas.
- [x] Orden EJECUTADA o ya CANCELADA retorna 400.
- [x] Orden de otro usuario retorna 403.
- [x] Orden inexistente retorna 404.
- [x] Evento `ORDEN_CANCELADA` en auditoría.
- [x] `docs/PROGRESO.md` marcado con ✅ para HU-21.

---

## Historial de cambios

| Versión | Fecha | Descripción | Razón |
|---|---|---|---|
| 1.0 | 2026-05-24 | Refactorización a estructura SDD del proyecto. | Unificación de todos los SPEC.md bajo plantilla canónica SDD del proyecto |
