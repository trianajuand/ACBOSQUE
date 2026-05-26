# SPEC — Colocación de orden Market

---

## Ficha de la historia

| Campo | Valor |
|---|---|
| ID | HU-17 |
| Sprint | 3 |
| Prioridad MoSCoW | Must Have |
| Estado | Completada |
| Épica | Órdenes / Ejecución |
| CU asociado | CU-17 |
| Autor | Juan Diego Triana Mejia |
| Creada | 2026-05-20 |
| Última revisión | 2026-05-24 |
| Versión de esta spec | 1.0 |

**Trazabilidad:**

| Tipo | ID | Descripción |
|---|---|---|
| Requerimiento funcional | RF-16 | Colocación de órdenes de compra/venta Market |
| Escenario de calidad | EC-13 | Previsualización de comisión antes de confirmar la orden |
| Escenario de calidad | EC-12 | Trazabilidad de eventos de orden |
| Historia relacionada | HU-23 | Si el mercado está cerrado, la orden se encola (HU-23) |
| Historia relacionada | HU-15 | El portafolio refleja las posiciones abiertas |

---

## Historia de usuario

**Como** inversionista autenticado,
**quiero** colocar una orden Market de compra o venta,
**para** operar al precio actual de mercado de forma inmediata.

---

## Motivación y contexto

### Por qué existe esta historia

Una orden Market es la operación más básica de trading: compra o vende a precio de mercado actual. El sistema calcula la comisión, reserva fondos (si es compra), envía la orden a Alpaca (mercados US) o la ejecuta internamente (mercados globales), y permite al usuario ver el desglose de la operación antes de confirmarla (EC-13).

### Definición de orden Market

- Se ejecuta al precio actual del mercado.
- No requiere `precioLimite` ni `precioStop`.
- Si el mercado está cerrado, la orden se encola (HU-23) para ejecutarse cuando abra.
- Para símbolos con punto (mercados globales, p. ej. `RIO.LON`): la ejecución es interna al precio de caché.
- Para símbolos sin punto (NYSE/NASDAQ): se envía a Alpaca API.

### Cálculo de comisión

```
montoBase = precio_actual × cantidad
montoComision = montoBase × porcentajeComision (2% por defecto)
montoPlatforma = montoComision × 60%
montoComisionista = montoComision × 40% (si hay comisionista asignado, sino 0%)
totalADebitar (compra) = montoBase + montoComision
totalARecibir (venta) = montoBase - montoComision
```

---

## Actores y precondiciones

### Actores

| Actor | Rol en el sistema | Participación en este flujo |
|---|---|---|
| Inversionista autenticado | `INVERSIONISTA` | Iniciador — envía la solicitud de orden |
| `OrdenService` | Módulo `ordenes` | Valida, calcula comisión, reserva fondos y envía a Alpaca |
| `AlpacaAdapter` | Módulo `integracion` | Envía la orden al broker externo |
| `IGestorParametros` | Módulo `administracion` | Provee `porcentajeComision`, splits de comisión |
| `AuditLogService` | Módulo `trazabilidad` (vía `IAuditLog`) | Registra eventos de la orden |

### Precondiciones

- JWT válido, `estado_cuenta = ACTIVA` o `OPERACIONES_RESTRINGIDAS`.
- Para compra: `cuenta_fondos.saldo_disponible >= totalADebitar`.
- Para venta: `holding.cantidad >= cantidad` solicitada.
- Símbolo válido y operable.

### Postcondiciones (mercado abierto, símbolo US)

- `orden.estado = ENVIADA`.
- Fondos reservados (compra) o holding reducido (venta).
- Orden enviada a Alpaca con `alpaca_order_id` almacenado.
- Evento `ORDEN_ENVIADA_ALPACA` registrado.

### Postcondiciones (mercado cerrado)

- `orden.estado = EN_COLA`.
- Fondos reservados.
- Evento `ORDEN_EN_COLA` registrado (procesada en HU-23).

---

## Flujo principal

**Paso 1 — Previsualización (EC-13):**

1. Frontend envía `POST /api/ordenes/previsualizar` con `CrearOrdenRequestDTO`.
2. `OrdenService.previsualizarOrden` calcula la comisión y retorna `ResumenComisionDTO`.
3. El usuario revisa el desglose y confirma o cancela.

**Paso 2 — Confirmación:**

4. Frontend envía `POST /api/ordenes` con el mismo `CrearOrdenRequestDTO` y `X-Forwarded-For` / IP.

**Backend — `OrdenService.crearOrden`:**

5. Valida que el símbolo es operable y el usuario puede operar.
6. Obtiene precio actual del mercado.
7. Calcula comisión (2% de `montoBase`).
8. Para **compra**: verifica `saldo_disponible >= totalADebitar`; si no, lanza `FondosInsuficientesException` → 402.
9. Para **venta**: verifica `holding.cantidad >= cantidad`; si no, lanza `HoldingInsuficienteException` → 422.
10. Persiste `Orden` con `estado = PENDIENTE`.
11. Reserva fondos (compra): `saldo_disponible -= totalADebitar`, `fondos_reservados += totalADebitar`.
12. `IAuditLog.registrar(ORDEN_PENDIENTE, correo, "Orden MARKET creada")`.
13. Si el mercado está abierto:
    - Símbolo US → `AlpacaAdapter.crearOrden(...)` → `orden.estado = ENVIADA`, guarda `alpaca_order_id` → audit `ORDEN_ENVIADA_ALPACA`.
    - Símbolo global → ejecución interna al precio de caché → `orden.estado = EJECUTADA` → audit `ORDEN_EJECUTADA`.
14. Si el mercado está cerrado: `orden.estado = EN_COLA` → audit `ORDEN_EN_COLA`.
15. Responde `201 Created` con `OrdenDTO`.

---

## Flujos de error

### Error 1 — No autenticado

| Campo | Valor |
|---|---|
| Condición | JWT ausente, inválido o expirado |
| HTTP | 401 Unauthorized |

### Error 2 — Fondos insuficientes (compra)

| Campo | Valor |
|---|---|
| Condición | `saldo_disponible < totalADebitar` |
| Excepción Java | `FondosInsuficientesException` |
| HTTP | 402 Payment Required |
| Cuerpo | `RespuestaDTO{error: "Fondos insuficientes para ejecutar la orden"}` |
| Estado final | Sin cambios en fondos ni orden persistida |
| Evento de auditoría | Ninguno |

### Error 3 — Holding insuficiente (venta)

| Campo | Valor |
|---|---|
| Condición | `holding.cantidad < cantidad` solicitada |
| Excepción Java | `HoldingInsuficienteException` |
| HTTP | 422 Unprocessable Entity |
| Cuerpo | `RespuestaDTO{error: "No tienes suficientes acciones para vender"}` |
| Estado final | Sin cambios en holding ni orden persistida |
| Evento de auditoría | Ninguno |

### Error 4 — Símbolo inválido

| Campo | Valor |
|---|---|
| Condición | Símbolo no reconocido por el sistema o Alpha Vantage |
| Excepción Java | `SimboloInvalidoException` |
| HTTP | 400 Bad Request |
| Cuerpo | `RespuestaDTO{error: "Símbolo no válido: {simbolo}"}` |

### Error 5 — Cuenta no puede operar

| Campo | Valor |
|---|---|
| Condición | `estado_cuenta = SUSPENDIDA, ELIMINADA` o cuenta no activa para trading |
| HTTP | 403 Forbidden |
| Cuerpo | `RespuestaDTO{error: "La cuenta no puede realizar operaciones"}` |

### Error 6 — Fallo de Alpaca

| Campo | Valor |
|---|---|
| Condición | `AlpacaAdapter.crearOrden` retorna null o lanza excepción |
| HTTP | 201 Created (la orden queda en estado `PENDIENTE` o `EN_COLA`) |
| Cuerpo | `OrdenDTO` con estado `PENDIENTE` |
| Evento de auditoría | `ORDEN_FALLO_ALPACA` |

### Error 7 — Campos obligatorios ausentes

| Campo | Valor |
|---|---|
| Condición | `simbolo`, `tipoOrden`, `lado` o `cantidad` ausentes |
| HTTP | 400 Bad Request |
| Cuerpo | `RespuestaDTO{error: "...validación..."}` |

---

## Contrato de API

### Endpoint 1 — `POST /api/ordenes/previsualizar` (EC-13)

```yaml
POST /api/ordenes/previsualizar:
  summary: Previsualiza el desglose de comisión de una orden sin confirmarla
  security:
    - bearerAuth: []
  requestBody:
    required: true
    content:
      application/json:
        schema:
          $ref: '#/components/schemas/CrearOrdenRequestDTO'
        example:
          simbolo: "AAPL"
          tipoOrden: "MARKET"
          lado: "COMPRA"
          cantidad: 10
  responses:
    '200':
      description: Desglose de comisión sin crear la orden
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ResumenComisionDTO'
          example:
            simbolo: "AAPL"
            tipoOrden: "MARKET"
            lado: "COMPRA"
            cantidad: 10
            precioEstimado: 189.50
            montoBase: 1895.00
            porcentajeComision: 2.0
            montoComision: 37.90
            montoPlataforma: 22.74
            montoComisionista: 15.16
            totalADebitar: 1932.90
            mercadoAbierto: true
            advertencia: null
    '401':
      description: No autenticado
    '400':
      description: Parámetros inválidos
```

### Endpoint 2 — `POST /api/ordenes`

```yaml
POST /api/ordenes:
  summary: Crea y envía una orden de compra o venta
  security:
    - bearerAuth: []
  requestBody:
    required: true
    content:
      application/json:
        schema:
          $ref: '#/components/schemas/CrearOrdenRequestDTO'
        example:
          simbolo: "AAPL"
          tipoOrden: "MARKET"
          lado: "COMPRA"
          cantidad: 10
  responses:
    '201':
      description: Orden creada exitosamente
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/OrdenDTO'
    '400':
      description: Datos inválidos o símbolo no válido
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'
    '401':
      description: No autenticado
    '402':
      description: Fondos insuficientes
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'
    '403':
      description: Cuenta no puede operar
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'
    '422':
      description: Holding insuficiente para venta
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'
    '500':
      description: Error interno del servidor

components:
  schemas:
    CrearOrdenRequestDTO:
      type: object
      required: [simbolo, tipoOrden, lado, cantidad]
      properties:
        simbolo:
          type: string
          description: "Ticker del activo (ej. AAPL, TSLA). El servicio lo resuelve a activo_id internamente."
        tipoOrden:
          type: string
          enum: [MARKET, LIMIT, STOP_LOSS, TAKE_PROFIT]
        lado:
          type: string
          enum: [COMPRA, VENTA]
        cantidad:
          type: number
          format: double
          minimum: 0.0001
        precioLimite:
          type: number
          format: double
          nullable: true
          description: "Requerido para LIMIT y TAKE_PROFIT"
        precioStop:
          type: number
          format: double
          nullable: true
          description: "Requerido para STOP_LOSS"
    ResumenComisionDTO:
      type: object
      properties:
        simbolo:
          type: string
          description: "Ticker del activo (ej. AAPL)"
        tipoOrden:
          type: string
        lado:
          type: string
        cantidad:
          type: number
        precioEstimado:
          type: number
        montoBase:
          type: number
        porcentajeComision:
          type: number
        montoComision:
          type: number
        montoPlataforma:
          type: number
        montoComisionista:
          type: number
        totalADebitar:
          type: number
          nullable: true
          description: "Para COMPRA"
        totalARecibir:
          type: number
          nullable: true
          description: "Para VENTA"
        mercadoAbierto:
          type: boolean
        advertencia:
          type: string
          nullable: true
    OrdenDTO:
      type: object
      properties:
        id:
          type: integer
        simbolo:
          type: string
          description: "Ticker del activo"
        tipoOrden:
          type: string
        lado:
          type: string
        cantidad:
          type: number
        precioEjecucion:
          type: number
          nullable: true
        montoComision:
          type: number
        estado:
          type: string
          enum: [PENDIENTE, ENVIADA, EN_COLA, EJECUTADA, CANCELADA]
        alpacaOrderId:
          type: string
          nullable: true
        fechaCreacion:
          type: string
          format: date-time
        fechaEjecucion:
          type: string
          format: date-time
          nullable: true
```

---

## Modelo de datos

### Tabla `activo` (catálogo de instrumentos financieros — ver DDL completo en HU-15)

```sql
-- activo.ticker reemplaza el campo simbolo VARCHAR en orden y holding
CREATE TABLE activo (
    id       BIGSERIAL PRIMARY KEY,
    ticker   VARCHAR(20)  NOT NULL UNIQUE,
    nombre   VARCHAR(255),
    mercado  VARCHAR(50),
    tipo     VARCHAR(50)
);
```

### Tabla `orden` (módulo `ordenes`)

```sql
CREATE TABLE orden (
    id                    BIGSERIAL PRIMARY KEY,
    inversionista_id      BIGINT NOT NULL REFERENCES inversionista(id),
    activo_id             BIGINT NOT NULL REFERENCES activo(id),
    parametro_comision_id BIGINT REFERENCES parametro_comision(id),  -- snapshot del parámetro usado
    tipo_orden            VARCHAR(50)  NOT NULL,  -- MARKET, LIMIT, STOP_LOSS, TAKE_PROFIT
    lado                  VARCHAR(20)  NOT NULL,  -- COMPRA, VENTA
    cantidad              DECIMAL(12,4) NOT NULL,
    precio_limite         DECIMAL(12,4),
    precio_stop           DECIMAL(12,4),
    precio_ejecucion      DECIMAL(12,4),
    monto_base            DECIMAL(15,4),
    porcentaje_comision   DECIMAL(5,4),
    monto_comision        DECIMAL(15,4),
    monto_plataforma      DECIMAL(15,4),
    monto_comisionista    DECIMAL(15,4),
    estado                VARCHAR(50)  NOT NULL,  -- PENDIENTE, ENVIADA, EN_COLA, EJECUTADA, CANCELADA
    alpaca_order_id       VARCHAR(255),
    ip_origen             VARCHAR(100),
    fecha_creacion        TIMESTAMP    NOT NULL,
    fecha_ejecucion       TIMESTAMP
);

CREATE INDEX idx_orden_inversionista_id ON orden (inversionista_id);
CREATE INDEX idx_orden_estado ON orden (estado);
CREATE INDEX idx_orden_activo_id ON orden (activo_id);
```

**Decisiones de esquema:**
- `inversionista_id` reemplaza `usuario_id`: las órdenes pertenecen al perfil de inversionista, no al usuario genérico.
- `activo_id` en la tabla referencia al catálogo `activo`. El campo `simbolo` del DTO de entrada es el ticker en String (ej. `"AAPL"`); el service lo resuelve a `activo_id` internamente vía `ActivoRepository.findByTicker(simbolo)`. El DTO de respuesta (`OrdenDTO`) devuelve el campo `simbolo` con el ticker.
- `parametro_comision_id`: snapshot de los parámetros de comisión usados al calcular la orden (trazabilidad).
- El flujo de propuestas de comisionista (estados `PENDIENTE_APROBACION`, `APROBADA`, `RECHAZADA`) se gestiona en la tabla separada `propuesta_orden` (ver HU-30).

---

## Módulos y arquitectura

### Módulos involucrados

| Módulo | Rol | Componentes específicos |
|---|---|---|
| `ordenes` | Coordinador del flujo | `OrdenController`, `OrdenService` |
| `administracion` | Parámetros de comisión | `IGestorParametros` (impl. en `AdministracionService`) |
| `integracion` | Envío a Alpaca | `AlpacaAdapter` (vía `IEnvioOrden` o similar) |
| `trazabilidad` | Registro de eventos | `AuditLogService` (impl. de `IAuditLog`) |

### Interfaces consumidas en este flujo

| Interfaz | Módulo dueño | Métodos usados | Cuándo |
|---|---|---|---|
| `IGestorParametros` | `administracion` | `obtenerPorcentajeComision()`, `obtenerSplitPlataforma()`, `obtenerSplitComisionista()` | Al calcular comisión |
| `IAuditLog` | `trazabilidad` | `registrar(TipoEvento, correo, detalle)` | En cada transición de estado |

### Escenarios de calidad y tácticas materializadas

| EC | Táctica | Cómo se materializa en HU-17 |
|---|---|---|
| EC-13 | Previsualizar antes de confirmar | `POST /api/ordenes/previsualizar` retorna desglose sin crear la orden |
| EC-12 | Audit Trail | `ORDEN_PENDIENTE`, `ORDEN_ENVIADA_ALPACA`, `ORDEN_EN_COLA`, `ORDEN_EJECUTADA` registrados |

---

## Eventos y efectos transversales

### Eventos de auditoría emitidos

| Evento (`TipoEvento`) | Cuándo se emite |
|---|---|
| `ORDEN_PENDIENTE` | Orden creada en BD |
| `ORDEN_ENVIADA_ALPACA` | Enviada a Alpaca exitosamente (mercado US abierto) |
| `ORDEN_EN_COLA` | Mercado cerrado — encolada para HU-23 |
| `ORDEN_EJECUTADA` | Símbolo global ejecutado internamente |
| `ORDEN_FALLO_ALPACA` | Alpaca retornó null o excepción |

---

## Riesgos

| # | Riesgo | P | I | Mitigación | Test que lo cubre |
|---|---|:-:|:-:|---|---|
| R1 | Fondos reservados pero Alpaca falla → fondos quedan bloqueados indefinidamente | Baja | Alto | HU-21 (cancelar orden) libera fondos reservados | Manual: simular fallo Alpaca y verificar fondos en `fondos_reservados` |
| R2 | Precio de mercado varía entre previsualización y confirmación — el `totalADebitar` real puede diferir | Media | Medio | La previsualización es estimada; la orden real usa el precio en el momento de la ejecución. UI debe advertirlo | Manual: verificar que el precio de ejecución puede diferir del previsualizado |

---

## Criterios de verificación

### Escenarios de aceptación (Gherkin)

```gherkin
Funcionalidad: Colocación de orden Market

  Antecedentes:
    Dado que "ana@test.com" tiene JWT válido, saldo_disponible=10000 y mercado NASDAQ abierto

  Escenario: Previsualización de orden Market de compra (EC-13)
    Cuando se envía POST /api/ordenes/previsualizar con { simbolo: "AAPL", tipoOrden: "MARKET", lado: "COMPRA", cantidad: 10 }
    Entonces el sistema responde 200 OK
    Y el cuerpo contiene montoBase, montoComision, totalADebitar
    Y no se crea ninguna orden en BD

  Escenario: Orden Market de compra exitosa (mercado abierto)
    Cuando se envía POST /api/ordenes con { simbolo: "AAPL", tipoOrden: "MARKET", lado: "COMPRA", cantidad: 10 }
    Entonces el sistema responde 200 OK con OrdenDTO
    Y la orden tiene estado "ENVIADA" (o "EJECUTADA" si es global)
    Y cuenta_fondos.fondos_reservados aumentó en el totalADebitar
    Y se emite evento ORDEN_ENVIADA_ALPACA en auditoría

  Escenario: Fondos insuficientes retorna 402
    Dado que saldo_disponible < precio × cantidad
    Cuando se envía POST /api/ordenes con compra de AAPL
    Entonces el sistema responde 402 Payment Required

  Escenario: Venta sin holding suficiente retorna 422
    Dado que "ana@test.com" no tiene holding de AAPL
    Cuando se envía POST /api/ordenes con { lado: "VENTA", simbolo: "AAPL", cantidad: 1 }
    Entonces el sistema responde 422 Unprocessable Entity

  Escenario: Mercado cerrado — orden encolada
    Dado que el mercado NASDAQ está cerrado
    Cuando se envía POST /api/ordenes con orden de compra de simbolo="AAPL"
    Entonces el sistema responde 200 OK
    Y la orden tiene estado "EN_COLA"
    Y se emite evento ORDEN_EN_COLA en auditoría
```

---

## Fuera de alcance

- **Cola de órdenes y procesamiento al abrir mercado** — HU-23.
- **Cancelar orden** — HU-21.
- **Historial de órdenes** — HU-24 a HU-26.
- **Flujo de comisionista** — HU-30 a HU-32.

---

## Definición de terminado

- [x] `POST /api/ordenes/previsualizar` retorna desglose sin crear orden.
- [x] `POST /api/ordenes` con MARKET crea orden y la envía a Alpaca (mercado abierto).
- [x] Fondos reservados inmediatamente en compra.
- [x] Holding verificado en venta.
- [x] Orden encolada si mercado cerrado.
- [x] Fondos insuficientes → 402; holding insuficiente → 422.
- [x] Eventos de auditoría emitidos en cada transición de estado.
- [x] `docs/PROGRESO.md` marcado con ✅ para HU-17.

---

## Historial de cambios

| Versión | Fecha | Descripción | Razón |
|---|---|---|---|
| 1.0 | 2026-05-24 | Refactorización a estructura SDD del proyecto. Incluye EC-13 (previsualización), modelo de datos de `orden`, y flujos de error con HTTP codes. | Unificación de todos los SPEC.md bajo plantilla canónica SDD del proyecto |
| 1.1 | 2026-05-26 | Auditoría SDD: `activoId` (Long) → `simbolo` (String) en `CrearOrdenRequestDTO`, `ResumenComisionDTO` y `OrdenDTO`. El campo `simbolo` es el ticker en texto (ej. "AAPL"); el servicio lo resuelve a `activo_id` internamente. Endpoint `POST /api/ordenes` retorna `200 OK` (no 201). | Código real usa `simbolo`, no `activoId`. |
