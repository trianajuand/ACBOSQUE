# SPEC — Propuesta de orden para cliente (comisionista)

---

## Ficha de la historia

| Campo | Valor |
|---|---|
| ID | HU-30 |
| Sprint | 4 |
| Prioridad MoSCoW | Should Have |
| Estado | Completada |
| Épica | Órdenes / Comisionista |
| CU asociado | CU-30 |
| Autor | Juan Diego Triana Mejia |
| Creada | 2026-05-20 |
| Última revisión | 2026-05-24 |
| Versión de esta spec | 1.0 |

**Trazabilidad:**

| Tipo | ID | Descripción |
|---|---|---|
| Requerimiento funcional | RF-29 | Comisionista crea propuesta de orden para cliente asignado |
| Escenario de calidad | EC-12 | Trazabilidad de PROPUESTA_ORDEN_CREADA |
| Historia que sigue | HU-31 | El inversionista aprueba o rechaza la propuesta |
| Historia que sigue | HU-32 | El comisionista firma y envía la propuesta aprobada |

---

## Historia de usuario

**Como** comisionista autenticado,
**quiero** crear una propuesta de orden para uno de mis clientes asignados con mi comentario de recomendación,
**para** que el inversionista pueda revisar la recomendación y decidir si aprobarla o rechazarla.

---

## Motivación y contexto

### Por qué existe esta historia

El flujo de comisionista introduce un mecanismo de aprobación de dos etapas: el comisionista propone y el inversionista tiene el control final. Esto garantiza que ninguna operación se ejecute en nombre del inversionista sin su consentimiento explícito. La propuesta NO reserva fondos ni envía nada al mercado — solo registra la intención con estado `PENDIENTE_APROBACION`.

### Ciclo de vida de la propuesta

```
PENDIENTE_APROBACION (HU-30)
    ↓ Inversionista aprueba (HU-31)    ↓ Inversionista rechaza (HU-31)
APROBADA                               RECHAZADA
    ↓ Comisionista firma y envía (HU-32)
ENVIADA / EN_COLA / EJECUTADA
```

---

## Actores y precondiciones

### Actores

| Actor | Rol en el sistema | Participación |
|---|---|---|
| Comisionista autenticado | `COMISIONISTA` | Iniciador — crea la propuesta |
| `OrdenService` | Módulo `ordenes` | Persiste la propuesta |
| `IAsignacionComisionista` | Módulo `autenticacion` | Valida que el cliente está asignado |
| `AuditLogService` | Módulo `trazabilidad` | Registra PROPUESTA_ORDEN_CREADA |

### Precondiciones

- JWT válido con rol `COMISIONISTA`.
- `clienteId` está asignado al comisionista.
- Símbolo válido y operable.
- La propuesta NO verifica fondos del cliente en este momento.

### Postcondiciones

- `PropuestaOrden` persistida con:
  - `estado = PENDIENTE_APROBACION`
  - `inversionistaId = clienteId`
  - `comisionistaId = comisionista.id`
  - `comentarioComisionista` almacenado
  - `ipOrigen` del request
- Evento `PROPUESTA_ORDEN_CREADA` registrado.

---

## Flujo principal

1. Comisionista selecciona un cliente y llena el formulario de propuesta.
2. Frontend envía `POST /api/comisionista/clientes/{clienteId}/propuestas` con JWT y `CrearPropuestaOrdenDTO`.

**Backend — `OrdenService.crearPropuestaOrden(comisionistaId, clienteId, dto, ipOrigen)`:**

3. Valida rol del comisionista.
4. `IAsignacionComisionista.validarClienteAsignado(comisionistaId, clienteId)`. Si no → 403.
5. Valida que el cliente puede operar (`estado_cuenta = ACTIVA`).
6. Valida símbolo operable.
7. Calcula `montoBase = cantidad × precioEfectivo` (precio de caché para estimación).
8. Calcula `comision = montoBase × porcentajeComision`.
9. Persiste `PropuestaOrden` con `estado = PENDIENTE_APROBACION` en la tabla `propuesta_orden`.
10. `IAuditLog.registrar(PROPUESTA_ORDEN_CREADA, correo_cliente, "Propuesta creada por {correo_comisionista}")`.
11. Responde `201 Created` con `OrdenDTO`.

---

## Flujos de error

### Error 1 — No autenticado o rol incorrecto

| Campo | Valor |
|---|---|
| HTTP | 401 / 403 |

### Error 2 — Cliente no asignado al comisionista

| Campo | Valor |
|---|---|
| Condición | `clienteId` no en lista de clientes del comisionista |
| HTTP | 403 Forbidden |
| Cuerpo | `RespuestaDTO{error: "No tienes acceso a este cliente"}` |
| Evento de auditoría | `ACCESO_DENEGADO_CLIENTE_NO_ASIGNADO` |

### Error 3 — Símbolo inválido

| Campo | Valor |
|---|---|
| HTTP | 400 Bad Request |
| Cuerpo | `RespuestaDTO{error: "Símbolo no válido: {simbolo}"}` |

### Error 4 — Campos obligatorios ausentes

| Campo | Valor |
|---|---|
| HTTP | 400 Bad Request |

---

## Contrato de API

### Endpoint — `POST /api/comisionista/clientes/{clienteId}/propuestas`

```yaml
POST /api/comisionista/clientes/{clienteId}/propuestas:
  summary: Crea una propuesta de orden para un cliente asignado
  security:
    - bearerAuth: []  # Solo COMISIONISTA
  parameters:
    - name: clienteId
      in: path
      required: true
      schema:
        type: integer
  requestBody:
    required: true
    content:
      application/json:
        schema:
          $ref: '#/components/schemas/CrearPropuestaOrdenDTO'
        example:
          simbolo: "AAPL"
          tipoOrden: "LIMIT"
          lado: "COMPRA"
          cantidad: 5
          precioLimite: 185.00
          comentarioComisionista: "Recomiendo comprar AAPL; precio de entrada favorable dado el soporte técnico en $185"
  responses:
    '200':
      description: Propuesta creada exitosamente
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/OrdenDTO'
    '400':
      description: Campos inválidos o símbolo no válido
    '403':
      description: Cliente no asignado al comisionista
    '500':
      description: Error interno

components:
  schemas:
    CrearPropuestaOrdenDTO:
      type: object
      required: [simbolo, tipoOrden, lado, cantidad]
      properties:
        simbolo:
          type: string
          description: "Ticker del activo (ej. AAPL, TSLA)"
        tipoOrden:
          type: string
          enum: [MARKET, LIMIT, STOP_LOSS, TAKE_PROFIT]
        lado:
          type: string
          enum: [COMPRA, VENTA]
        cantidad:
          type: number
          format: double
          minimum: 0.000001
        precioLimite:
          type: number
          nullable: true
        precioStop:
          type: number
          nullable: true
        comentarioComisionista:
          type: string
          nullable: true
```

---

## Modelo de datos

### Tabla `propuesta_orden` (separada de `orden` — normalización 3NF)

```sql
CREATE TABLE propuesta_orden (
    id                        BIGSERIAL PRIMARY KEY,
    inversionista_id          BIGINT NOT NULL REFERENCES inversionista(id),
    comisionista_id           BIGINT NOT NULL REFERENCES usuario(id),
    activo_id                 BIGINT NOT NULL REFERENCES activo(id),
    tipo_orden                VARCHAR(50)  NOT NULL,
    lado                      VARCHAR(20)  NOT NULL,
    cantidad                  DECIMAL(12,4) NOT NULL,
    precio_limite             DECIMAL(12,4),
    precio_stop               DECIMAL(12,4),
    monto_base_estimado       DECIMAL(15,4),
    monto_comision_estimado   DECIMAL(15,4),
    estado                    VARCHAR(50)  NOT NULL DEFAULT 'PENDIENTE_APROBACION',
    comentario_comisionista   TEXT,
    comentario_inversionista  TEXT,
    aprobada_en               TIMESTAMP,
    rechazada_en              TIMESTAMP,
    firmada_en                TIMESTAMP,
    orden_id                  BIGINT REFERENCES orden(id),  -- enlace a la orden creada al firmar
    ip_origen                 VARCHAR(100),
    fecha_creacion            TIMESTAMP    NOT NULL,
    fecha_actualizacion       TIMESTAMP
);

CREATE INDEX idx_propuesta_inversionista ON propuesta_orden (inversionista_id);
CREATE INDEX idx_propuesta_comisionista ON propuesta_orden (comisionista_id);
CREATE INDEX idx_propuesta_estado ON propuesta_orden (estado);
```

**Decisiones de esquema:**
- `propuesta_orden` está separada de `orden`. Anteriormente los campos de propuesta (`comisionista_id`, `comentario_comisionista`, etc.) vivían como columnas extras en `orden`, lo que violaba 3NF. Ahora la propuesta tiene su propio ciclo de vida.
- `orden_id` se popula cuando el comisionista firma y envía (HU-32), creando el enlace entre la propuesta aprobada y la orden efectiva en el mercado.
- Estados posibles: `PENDIENTE_APROBACION`, `APROBADA`, `RECHAZADA`.

---

## Módulos y arquitectura

| Módulo | Rol | Componentes |
|---|---|---|
| `ordenes` | Coordinador | `ComisionistaController`, `OrdenService` |
| `autenticacion` | Validación asignación | `IAsignacionComisionista` |
| `trazabilidad` | Auditoría | `AuditLogService` |

### Escenarios de calidad y tácticas materializadas

| EC | Táctica | Cómo se materializa en HU-30 |
|---|---|---|
| EC-12 | Audit Trail | `PROPUESTA_ORDEN_CREADA` registrado con correo del cliente |
| EC-18 | Encapsulate | Acceso solo a clientes asignados |

---

## Criterios de verificación

### Escenarios de aceptación (Gherkin)

```gherkin
Funcionalidad: Propuesta de orden para cliente

  Antecedentes:
    Dado que "comis@test.com" tiene rol=COMISIONISTA y cliente asignado id=5 ("ana@test.com")

  Escenario: Propuesta creada exitosamente
    Cuando se envía POST /api/comisionista/clientes/5/propuestas con datos válidos
    Entonces el sistema responde 200 OK con OrdenDTO
    Y la orden tiene estado="PENDIENTE_APROBACION"
    Y la propuesta_orden tiene comisionista_id del comisionista
    Y no se reservaron fondos de "ana@test.com"
    Y se emite evento PROPUESTA_ORDEN_CREADA en auditoría

  Escenario: Cliente no asignado retorna 403
    Cuando se envía POST /api/comisionista/clientes/99/propuestas
    Entonces el sistema responde 403 Forbidden
```

---

## Definición de terminado

- [x] `POST /api/comisionista/clientes/{id}/propuestas` crea propuesta con `PENDIENTE_APROBACION`.
- [x] No se reservan fondos ni se envía nada al mercado al crear la propuesta.
- [x] `comentarioComisionista` almacenado.
- [x] Cliente no asignado retorna 403.
- [x] Evento `PROPUESTA_ORDEN_CREADA` en auditoría.
- [x] `docs/PROGRESO.md` marcado con ✅ para HU-30.

---

## Historial de cambios

| Versión | Fecha | Descripción | Razón |
|---|---|---|---|
| 1.0 | 2026-05-24 | Refactorización a estructura SDD del proyecto. | Unificación de todos los SPEC.md bajo plantilla canónica SDD del proyecto |
| 1.1 | 2026-05-26 | Auditoría SDD: `activoId` → `simbolo` en schema y ejemplo de `CrearPropuestaOrdenDTO`. HTTP 201→200 en respuesta. | Código real usa `simbolo` en `CrearPropuestaOrdenDTO`. |
