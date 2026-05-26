# SPEC — Firma y envío de orden aprobada al mercado (comisionista)

---

## Ficha de la historia

| Campo | Valor |
|---|---|
| ID | HU-32 |
| Sprint | 4 |
| Prioridad MoSCoW | Should Have |
| Estado | Completada |
| Épica | Órdenes / Comisionista |
| CU asociado | CU-32 |
| Autor | Juan Diego Triana Mejia |
| Creada | 2026-05-20 |
| Última revisión | 2026-05-24 |
| Versión de esta spec | 1.0 |

**Trazabilidad:**

| Tipo | ID | Descripción |
|---|---|---|
| Requerimiento funcional | RF-31 | Comisionista firma y envía orden aprobada por el inversionista |
| Escenario de calidad | EC-12 | Trazabilidad de PROPUESTA_ORDEN_FIRMADA y eventos de ejecución |
| Escenario de calidad | EC-04 | Disponibilidad: cola persiste en BD si mercado está cerrado |
| Historia que precede a esta | HU-31 | El inversionista aprueba la propuesta (estado = APROBADA) |

---

## Historia de usuario

**Como** comisionista autenticado,
**quiero** ver las propuestas que mis clientes han aprobado y firmarlas para enviarlas al mercado,
**para** completar el flujo de dos etapas y ejecutar la operación en nombre del cliente con su consentimiento explícito.

---

## Motivación y contexto

### Por qué existe esta historia

Tras la aprobación del inversionista (HU-31), la propuesta tiene estado `APROBADA` pero aún no ha interactuado con el mercado. El comisionista debe dar el paso final: confirmar la orden (firma), recalcular el monto con el precio actual de mercado, reservar fondos del cliente y enviar a Alpaca (o encolar si el mercado está cerrado). Este paso cierra el ciclo de tres pasos: proponer → aprobar → firmar y enviar.

### Ciclo de vida de la propuesta

```
APROBADA (HU-31)
    ↓ comisionista firma y envía (HU-32)
    ├─ Mercado abierto + símbolo US → ENVIADA (Alpaca)
    ├─ Mercado abierto + símbolo global → EJECUTADA (simulada)
    └─ Mercado cerrado → EN_COLA (procesada en HU-23)
```

---

## Actores y precondiciones

### Actores

| Actor | Rol en el sistema | Participación |
|---|---|---|
| Comisionista autenticado | `COMISIONISTA` | Firma y envía la propuesta aprobada |
| `OrdenService` | Módulo `ordenes` | Recalcula, reserva fondos y envía/encola |
| `IAsignacionComisionista` | Módulo `autenticacion` | Valida que el cliente está asignado |
| `IIntegracionAlpaca` | Módulo `integracion` | Envía la orden al mercado real |
| `AuditLogService` | Módulo `trazabilidad` | Registra todos los eventos del proceso |

### Precondiciones

- JWT válido con rol `COMISIONISTA`.
- La propuesta existe en `propuesta_orden` con estado `APROBADA`.
- La propuesta fue creada por el comisionista autenticado (`propuesta_orden.comisionistaId = comisionista.id`).
- El cliente (`propuesta_orden.inversionistaId`) está asignado al comisionista.
- El cliente tiene fondos suficientes para el monto recalculado.

### Postcondiciones

- Se crea registro en `orden` con estado `ENVIADA`, `EN_COLA` o `EJECUTADA` y montos recalculados.
- `PropuestaOrden.firmadaEn = now()`.
- `PropuestaOrden.ordenId` se popula con el ID de la `Orden` recién creada.
- Fondos reservados en `cuenta_fondos` del cliente.
- Evento `PROPUESTA_ORDEN_FIRMADA` registrado, seguido del evento de estado resultante.

---

## Flujo principal — Consultar propuestas aprobadas

1. Comisionista navega a su panel de propuestas aprobadas.
2. Frontend envía `GET /api/comisionista/propuestas/aprobadas` con JWT.
3. Sistema retorna registros de `propuesta_orden` con `estado = APROBADA` donde `comisionistaId = comisionista.id`.
4. Retorna `List<OrdenDTO>`.

## Flujo principal — Firmar y enviar

1. Comisionista selecciona una propuesta aprobada y confirma el envío.
2. Frontend envía `POST /api/comisionista/propuestas/{propuestaId}/firmar-enviar` con JWT.
3. **`OrdenService.firmarYEnviarPropuesta(propuestaId, comisionistaId)`:**
   a. Busca la propuesta en `propuesta_orden`. Si no existe → 404.
   b. Valida `propuesta.comisionistaId == comisionistaId`. Si no → 403.
   c. Valida `propuesta.estado == APROBADA`. Si no → 400.
   d. Obtiene precio actual del caché de precios (JOIN con `activo` por `propuesta.activoId`).
   e. Recalcula `montoBase = precioActual × propuesta.cantidad`.
   f. Recalcula `montoComision = montoBase × porcentajeComision`.
   g. Valida fondos del cliente: `saldoDisponible >= montoBase + montoComision`. Si no → 402.
   h. Reserva fondos del cliente: `saldoDisponible -= (montoBase + montoComision)`.
   i. Crea registro en `orden` con `inversionistaId = propuesta.inversionistaId`, `activoId = propuesta.activoId`, montos recalculados.
   j. Actualiza `propuesta.firmadaEn = now()`, `propuesta.ordenId = orden.id`.
   k. Determina destino según horario y ticker del activo:
      - Mercado abierto + símbolo US → llama `IAlpacaAdapter.enviarOrden(...)` → `estado = ENVIADA`.
      - Mercado abierto + símbolo global → simula ejecución → `estado = EJECUTADA`.
      - Mercado cerrado → `estado = EN_COLA`.
4. `IAuditLog.registrar(PROPUESTA_ORDEN_FIRMADA, correo_cliente, "Firmada por {correo_comisionista}")`.
5. `IAuditLog.registrar(ORDEN_ENVIADA_ALPACA | ORDEN_EN_COLA | ORDEN_EJECUTADA, ...)`.
6. Responde `200 OK` con `OrdenDTO` actualizada.

---

## Flujos de error

### Error 1 — No autenticado o rol incorrecto

| Campo | Valor |
|---|---|
| Condición | JWT ausente o rol ≠ COMISIONISTA |
| HTTP | 401 / 403 |

### Error 2 — Propuesta no encontrada

| Campo | Valor |
|---|---|
| Condición | `propuestaId` no existe en BD |
| HTTP | 404 Not Found |
| Cuerpo | `RespuestaDTO{error: "Orden no encontrada"}` |

### Error 3 — Propuesta de otro comisionista

| Campo | Valor |
|---|---|
| Condición | `propuesta_orden.comisionistaId ≠ comisionista.id` |
| HTTP | 403 Forbidden |
| Cuerpo | `RespuestaDTO{error: "No tienes permiso para gestionar esta propuesta"}` |

### Error 4 — Estado incorrecto

| Campo | Valor |
|---|---|
| Condición | `propuesta_orden.estado ≠ APROBADA` |
| HTTP | 400 Bad Request |
| Cuerpo | `RespuestaDTO{error: "La propuesta no está en estado APROBADA"}` |

### Error 5 — Fondos insuficientes del cliente

| Campo | Valor |
|---|---|
| Condición | `saldoDisponible < montoBase + montoComision` |
| HTTP | 402 Payment Required |
| Cuerpo | `RespuestaDTO{error: "Fondos insuficientes del cliente"}` |
| Evento de auditoría | `ORDEN_RECHAZADA_FONDOS` |

### Error 6 — Fallo en Alpaca

| Campo | Valor |
|---|---|
| Condición | `IAlpacaAdapter` lanza excepción |
| HTTP | 502 Bad Gateway |
| Cuerpo | `RespuestaDTO{error: "Error al comunicarse con el mercado"}` |
| Acción | Fondos reservados se liberan; `estado` no cambia a ENVIADA |
| Evento de auditoría | `ORDEN_FALLO_ALPACA` |

---

## Contrato de API

### Endpoint 1 — `GET /api/comisionista/propuestas/aprobadas`

```yaml
GET /api/comisionista/propuestas/aprobadas:
  summary: Lista propuestas aprobadas pendientes de firma por el comisionista
  security:
    - bearerAuth: []  # Solo COMISIONISTA
  responses:
    '200':
      description: Lista de propuestas con estado APROBADA del comisionista
      content:
        application/json:
          schema:
            type: array
            items:
              $ref: '#/components/schemas/OrdenDTO'
    '401':
      description: No autenticado
    '403':
      description: Rol incorrecto
```

### Endpoint 2 — `POST /api/comisionista/propuestas/{propuestaId}/firmar-enviar`

```yaml
POST /api/comisionista/propuestas/{propuestaId}/firmar-enviar:
  summary: Firma y envía al mercado una propuesta aprobada por el inversionista
  security:
    - bearerAuth: []  # Solo COMISIONISTA
  parameters:
    - name: propuestaId
      in: path
      required: true
      schema:
        type: integer
  responses:
    '200':
      description: Orden enviada, encolada o ejecutada exitosamente
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/OrdenDTO'
          example:
            id: 42
            estado: "ENVIADA"
            firmadaEn: "2026-05-24T15:30:00"
            montoBase: 925.00
            montoComision: 18.50
    '400':
      description: La propuesta no está en estado APROBADA
    '402':
      description: Fondos insuficientes del cliente
    '403':
      description: No autorizado para gestionar esta propuesta
    '404':
      description: Propuesta no encontrada
    '502':
      description: Error al comunicarse con Alpaca
```

---

## Modelo de datos

Al firmar y enviar, se actualiza `propuesta_orden` y se crea un nuevo registro en `orden`:

```sql
-- En propuesta_orden (actualización al firmar):
firmada_en        TIMESTAMP,              -- timestamp de firma
orden_id          BIGINT,                 -- FK al registro de orden creado

-- Nuevo registro en orden (creado al firmar, con valores efectivos):
inversionista_id      BIGINT,                 -- del inversionista cliente
activo_id             BIGINT,                 -- del activo de la propuesta
parametro_comision_id BIGINT,                 -- snapshot del parámetro vigente
monto_base            DECIMAL(15,4),          -- recalculado con precio actual
monto_comision        DECIMAL(15,4),
monto_plataforma      DECIMAL(15,4),          -- 60% de monto_comision
monto_comisionista    DECIMAL(15,4),          -- 40% de monto_comision
precio_ejecucion      DECIMAL(12,4),          -- precio de caché al momento de firma
alpaca_order_id       VARCHAR(255),           -- ID retornado por Alpaca si se envía
estado                VARCHAR(50),            -- ENVIADA | EN_COLA | EJECUTADA
```

La reserva de fondos se actualiza en `cuenta_fondos.fondos_reservados` del cliente (inversionista).

---

## Módulos y arquitectura

| Módulo | Rol | Componentes |
|---|---|---|
| `ordenes` | Coordinador | `ComisionistaController`, `OrdenService` |
| `integracion` | Adaptador Alpaca | `IAlpacaAdapter`, `AlpacaAdapter` |
| `autenticacion` | Validación asignación | `IAsignacionComisionista` |
| `trazabilidad` | Auditoría | `AuditLogService` vía `IAuditLog` |

### Interfaces utilizadas

| Interfaz | Módulo proveedor | Método |
|---|---|---|
| `IIntegracionAlpaca` | `integracion` | `crearOrden(accountId, simbolo, tipoOrden, lado, cantidad, precioLimite, precioStop)` |
| `IAuditLog` | `trazabilidad` | `registrar(evento, correo, detalle)` |
| `IAsignacionComisionista` | `autenticacion` | `validarClienteAsignado(comisionistaId, clienteId)` |

### Escenarios de calidad y tácticas materializadas

| EC | Táctica | Cómo se materializa en HU-32 |
|---|---|---|
| EC-12 | Audit Trail | PROPUESTA_ORDEN_FIRMADA + evento de estado final, con correo del cliente |
| EC-04 | Queue Availability | Si mercado cerrado → EN_COLA en BD; procesada por @Scheduled de HU-23 |
| EC-14 | Orchestrate | `OrdenService` orquesta: validar → recalcular → reservar fondos → enviar/encolar |

---

## Eventos y efectos transversales

| Evento | Cuándo | Consumidor |
|---|---|---|
| `PROPUESTA_ORDEN_FIRMADA` | Al completar la firma | `trazabilidad` |
| `ORDEN_ENVIADA_ALPACA` | Si se envía a Alpaca exitosamente | `trazabilidad` |
| `ORDEN_EN_COLA` | Si mercado está cerrado | `trazabilidad` |
| `ORDEN_EJECUTADA` | Si se ejecuta inmediatamente (símbolo global) | `trazabilidad` |
| `ORDEN_FALLO_ALPACA` | Si Alpaca falla | `trazabilidad` |
| `ORDEN_RECHAZADA_FONDOS` | Si fondos insuficientes | `trazabilidad` |

---

## Riesgos

| # | Riesgo | P | I | Mitigación |
|---|---|:-:|:-:|---|
| R1 | Precio cambia entre propuesta y firma, fondos del cliente ya no alcanzan | Alta | Alto | Recalcular montoBase con precio actual al momento de firma; validar fondos antes de reservar |
| R2 | Doble firma por doble clic | Baja | Alto | Validación de estado `APROBADA` como guarda idempotente |
| R3 | Alpaca falla después de reservar fondos | Baja | Alto | Transacción: si Alpaca falla → liberar fondos reservados y retornar 502 |

---

## Criterios de verificación

### Escenarios de aceptación (Gherkin)

```gherkin
Funcionalidad: Firma y envío de orden aprobada al mercado

  Antecedentes:
    Dado que "comis@test.com" tiene rol=COMISIONISTA
    Y la propuesta id=42 tiene estado="APROBADA" y comisionistaId="comis@test.com"
    Y el cliente "ana@test.com" tiene saldo suficiente

  Escenario: Consulta de propuestas aprobadas
    Cuando se envía GET /api/comisionista/propuestas/aprobadas con JWT del comisionista
    Entonces el sistema responde 200 OK
    Y la lista incluye la propuesta id=42 con estado="APROBADA"

  Escenario: Firma exitosa — mercado abierto, símbolo US
    Dado que el mercado NYSE está abierto
    Cuando se envía POST /api/comisionista/propuestas/42/firmar-enviar
    Entonces el sistema responde 200 OK
    Y la orden tiene estado="ENVIADA"
    Y firmadaEn no es nulo
    Y se emite evento PROPUESTA_ORDEN_FIRMADA en auditoría
    Y se emite evento ORDEN_ENVIADA_ALPACA en auditoría

  Escenario: Firma exitosa — mercado cerrado
    Dado que el mercado NYSE está cerrado
    Cuando se envía POST /api/comisionista/propuestas/42/firmar-enviar
    Entonces el sistema responde 200 OK
    Y la orden tiene estado="EN_COLA"
    Y se emite evento ORDEN_EN_COLA en auditoría

  Escenario: Fondos insuficientes del cliente retorna 402
    Dado que el cliente "ana@test.com" no tiene saldo suficiente
    Cuando se envía POST /api/comisionista/propuestas/42/firmar-enviar
    Entonces el sistema responde 402 Payment Required
    Y no se emite evento ORDEN_ENVIADA_ALPACA

  Escenario: Propuesta de otro comisionista retorna 403
    Dado que "otro-comis@test.com" tiene JWT válido con rol=COMISIONISTA
    Cuando "otro-comis@test.com" intenta POST /api/comisionista/propuestas/42/firmar-enviar
    Entonces el sistema responde 403 Forbidden

  Escenario: Propuesta no aprobada retorna 400
    Dado que la propuesta 42 tiene estado="PENDIENTE_APROBACION"
    Cuando se intenta firmar y enviar
    Entonces el sistema responde 400 Bad Request

  Escenario: Propuesta no encontrada retorna 404
    Cuando se envía POST /api/comisionista/propuestas/999/firmar-enviar
    Entonces el sistema responde 404 Not Found
```

---

## Interfaz de usuario

- Panel del comisionista con lista de propuestas aprobadas: símbolo, tipo, lado, cantidad, precio actual recalculado, monto estimado, nombre del cliente.
- Botón **Firmar y Enviar** por propuesta.
- Modal de confirmación con resumen de la orden (precio recalculado, monto total, comisión).
- Tras firma exitosa: la propuesta desaparece del panel y aparece en el historial de órdenes del cliente.

---

## Fuera de alcance

- Notificación al inversionista de que su orden fue enviada al mercado (HU-41).
- Modificación de la propuesta antes de firmar (no existe — solo firmar o no firmar).
- Cancelación de una propuesta aprobada (el comisionista solo puede firmar).

---

## Decisiones y preguntas abiertas

| # | Pregunta | Decisión |
|---|---|---|
| D1 | ¿Se recalcula precio al firmar o se usa el de la propuesta? | Se recalcula con precio actual del caché (EC-13 — preview es estimación, firma usa precio real). |
| D2 | ¿El inversionista puede ver que su orden fue enviada? | Sí — aparece en `/api/ordenes/activas` e historial con el estado ENVIADA/EN_COLA/EJECUTADA. |

---

## Definición de terminado

- [x] `GET /api/comisionista/propuestas/aprobadas` retorna propuestas con estado `APROBADA` del comisionista.
- [x] `POST /api/comisionista/propuestas/{id}/firmar-enviar` recalcula monto con precio actual.
- [x] Fondos del cliente reservados antes de enviar.
- [x] Mercado abierto + símbolo US → estado `ENVIADA` con `alpacaOrderId`.
- [x] Mercado cerrado → estado `EN_COLA`.
- [x] Fondos insuficientes del cliente → 402 sin modificar la orden.
- [x] Propuesta de otro comisionista → 403.
- [x] Propuesta no en estado `APROBADA` → 400.
- [x] Eventos `PROPUESTA_ORDEN_FIRMADA` + estado final registrados en auditoría.
- [x] `docs/PROGRESO.md` marcado con ✅ para HU-32.

---

## Historial de cambios

| Versión | Fecha | Descripción | Razón |
|---|---|---|---|
| 1.0 | 2026-05-24 | Refactorización a estructura SDD del proyecto. | Unificación de todos los SPEC.md bajo plantilla canónica SDD del proyecto |
