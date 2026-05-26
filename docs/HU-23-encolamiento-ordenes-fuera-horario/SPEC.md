# SPEC — Encolamiento de órdenes fuera de horario de mercado

---

## Ficha de la historia

| Campo | Valor |
|---|---|
| ID | HU-23 |
| Sprint | 3 |
| Prioridad MoSCoW | Must Have |
| Estado | Completada |
| Épica | Órdenes / Gestión |
| CU asociado | CU-23 |
| Autor | Juan Diego Triana Mejia |
| Creada | 2026-05-20 |
| Última revisión | 2026-05-24 |
| Versión de esta spec | 1.0 |

**Trazabilidad:**

| Tipo | ID | Descripción |
|---|---|---|
| Requerimiento funcional | RF-22 | Encolamiento automático de órdenes fuera de horario |
| Escenario de calidad | EC-04 | Disponibilidad — las órdenes no se pierden cuando el mercado está cerrado |
| Escenario de calidad | EC-12 | Trazabilidad de ORDEN_EN_COLA, ORDEN_ENVIADA_ALPACA, ORDEN_EJECUTADA |
| Historia que precede a esta | HU-17..20 | Las órdenes se encolan cuando el mercado está cerrado |
| Historia relacionada | HU-22 | Las órdenes EN_COLA aparecen en la lista de órdenes activas |

---

## Historia de usuario

**Como** inversionista autenticado,
**quiero** colocar órdenes fuera del horario de mercado,
**para** que el sistema las procese automáticamente cuando el mercado abra sin que yo tenga que estar presente.

---

## Motivación y contexto

### Por qué existe esta historia

Los mercados internacionales tienen horarios limitados. Un inversionista puede querer operar después del horario de cierre o antes de la apertura. Esta historia garantiza que las órdenes no se pierden: se persisten en BD con estado `EN_COLA` y un proceso programado (`@Scheduled`) las procesa cuando el mercado abre.

### Definición de horario de mercado

- **NYSE / NASDAQ**: Lunes a viernes, 9:30 AM - 4:00 PM EST (America/New_York).
- Feriados: configurables en `feriado_mercado` (HU-34).
- Mercados globales (con punto, ej. `RIO.LON`, `TM.TSE`): ejecución interna simulada.

---

## Actores y precondiciones

### Actores

| Actor | Rol en el sistema | Participación en este flujo |
|---|---|---|
| `ColaOrdenesService` | Módulo `ordenes` | Job programado que procesa la cola al abrir el mercado |
| `AlpacaAdapter` | Módulo `integracion` | Envía órdenes encoladas de símbolos US a Alpaca |
| `INotificacion` | Módulo `integracion` | Notifica al inversionista cuando su orden se procesa |
| `AuditLogService` | Módulo `trazabilidad` | Registra eventos de procesamiento |

### Precondiciones

- Existen órdenes en estado `EN_COLA` en BD.
- El mercado US (NYSE/NASDAQ) está abierto en el momento de verificación.
- La cuenta del inversionista sigue activa.
- Los fondos siguen disponibles/reservados.

### Postcondiciones (procesamiento exitoso)

- `orden.estado = ENVIADA` (símbolo US enviado a Alpaca) o `EJECUTADA` (símbolo global).
- El inversionista recibe notificación de apertura de mercado.
- Eventos `ORDEN_ENVIADA_ALPACA` o `ORDEN_EJECUTADA` registrados.

---

## Flujo principal (sin endpoint REST — proceso interno)

**Proceso programado — `ColaOrdenesService.procesarColaAlAbrirMercado()` (cada 60 segundos):**

1. Verifica si el mercado US (NYSE/NASDAQ) está abierto (lunes a viernes, 9:30-16:00 EST).
2. Si el mercado está cerrado: no hace nada; espera al siguiente ciclo.
3. Si el mercado está abierto:
   a. Consulta todas las órdenes con `estado = EN_COLA`.
   b. Para cada orden:
      - Verifica que `usuario.estado_cuenta = ACTIVA` o `OPERACIONES_RESTRINGIDAS`. Si no: cancela la orden.
      - Notifica al usuario que el mercado abrió: `INotificacion.notificarAperturaConOrdenesEnCola(correo)`.
      - **Símbolo US (sin punto)**: envía a Alpaca → `estado = ENVIADA`, guarda `alpaca_order_id` → audit `ORDEN_ENVIADA_ALPACA`.
      - **Símbolo global (con punto)**: ejecuta internamente al precio de caché → `estado = EJECUTADA` → actualiza holding, libera fondos → audit `ORDEN_EJECUTADA`.
      - Si falla: registra error en logs, continúa con la siguiente orden (tolerancia a fallos individuales).

---

## Flujos de error / edge cases

### Error 1 — Cuenta inactiva al procesar cola

| Campo | Valor |
|---|---|
| Condición | `usuario.estado_cuenta = SUSPENDIDA / ELIMINADA` al intentar procesar la orden encolada |
| Comportamiento | La orden se cancela automáticamente; fondos reservados se liberan |
| Evento de auditoría | `ORDEN_RECHAZADA_FONDOS` o `ORDEN_CANCELADA` |

### Error 2 — Fondos insuficientes al procesar (raro — ya fueron reservados)

| Campo | Valor |
|---|---|
| Condición | El saldo disponible + reservado cambió entre el encolamiento y la apertura |
| Comportamiento | La orden se cancela; fondos reservados se liberan |
| Evento de auditoría | `ORDEN_RECHAZADA_FONDOS` |

### Error 3 — Fallo de Alpaca al procesar orden encolada

| Campo | Valor |
|---|---|
| Condición | `AlpacaAdapter` retorna null o excepción |
| Comportamiento | La orden queda en `EN_COLA` (reintento en el próximo ciclo de 60s) |
| Evento de auditoría | `ORDEN_FALLO_ALPACA` |

---

## Contrato de API

**HU-23 no expone endpoints REST.** Es un proceso interno del módulo `ordenes`.

Las órdenes encoladas se crean mediante los endpoints de HU-17 a HU-20 cuando el mercado está cerrado.
Las órdenes en cola se consultan mediante HU-22 (`GET /api/ordenes/activas`).

---

## Modelo de datos

### Campos relevantes en tabla `orden`

```sql
-- estado = 'EN_COLA' cuando el mercado estaba cerrado al crear la orden
-- fecha_creacion registra cuándo fue encolada
-- El procesamiento actualiza estado + fecha_ejecucion
```

### Configuración de horario de mercado

```sql
CREATE TABLE mercado_config (
    id              BIGSERIAL PRIMARY KEY,
    codigo          VARCHAR(20) NOT NULL UNIQUE,  -- NYSE, NASDAQ, TSE, LSE, ASX
    nombre          VARCHAR(100),
    timezone        VARCHAR(50) NOT NULL,         -- America/New_York
    hora_apertura   TIME NOT NULL,                -- 09:30
    hora_cierre     TIME NOT NULL,                -- 16:00
    activo          BOOLEAN DEFAULT TRUE
);

CREATE TABLE feriado_mercado (
    id                BIGSERIAL PRIMARY KEY,
    mercado_config_id BIGINT REFERENCES mercado_config(id) NOT NULL,
    fecha             DATE NOT NULL,
    descripcion       VARCHAR(255),
    creado_en         TIMESTAMP NOT NULL,
    UNIQUE (mercado_config_id, fecha)
);
-- Nota de auditoría (2026-05-25): el nombre real de la tabla en la entidad JPA es
-- `feriado_mercado` (ver FeriadoMercado.java @Table(name = "feriado_mercado")).
-- La columna FK es `mercado_config_id` (BIGINT), no `mercado_id`.
```

---

## Módulos y arquitectura

### Módulos involucrados

| Módulo | Rol | Componentes específicos |
|---|---|---|
| `ordenes` | Proceso programado | `ColaOrdenesService` (`@Scheduled`), `OrdenService` |
| `integracion` | Envío a Alpaca | `AlpacaAdapter` |
| `integracion` | Notificaciones | `DespachadorNotificaciones` (vía `INotificacion`) |
| `administracion` | Configuración de mercados | `AdministracionService` (vía `IAdministracion`) |
| `trazabilidad` | Auditoría | `AuditLogService` |

### Escenarios de calidad y tácticas materializadas

| EC | Táctica | Cómo se materializa en HU-23 |
|---|---|---|
| EC-04 | Disponibilidad — cola persistida en BD | Las órdenes sobreviven reinicios del servidor; se procesan cuando el mercado abre |
| EC-12 | Audit Trail | `ORDEN_EN_COLA`, `ORDEN_ENVIADA_ALPACA`, `ORDEN_EJECUTADA`, `ORDEN_RECHAZADA_FONDOS` |

---

## Eventos y efectos transversales

### Eventos de auditoría emitidos

| Evento (`TipoEvento`) | Cuándo se emite |
|---|---|
| `ORDEN_EN_COLA` | Al crear la orden fuera de horario (en HU-17..20) |
| `ORDEN_ENVIADA_ALPACA` | Al procesar la orden encolada (símbolo US) |
| `ORDEN_EJECUTADA` | Al procesar la orden encolada (símbolo global) |
| `ORDEN_RECHAZADA_FONDOS` | Cuenta inactiva o fondos insuficientes al procesar |
| `ORDEN_FALLO_ALPACA` | Error en la API de Alpaca |

### Notificaciones enviadas

| Trigger | Canal | Contenido |
|---|---|---|
| Mercado abre con órdenes en cola | Email/Notificación | "El mercado ha abierto. Tus órdenes en cola están siendo procesadas" |

---

## Riesgos

| # | Riesgo | P | I | Mitigación | Test que lo cubre |
|---|---|:-:|:-:|---|---|
| R1 | El ciclo de 60s puede procesar la cola varias veces si el mercado acaba de abrir y el ciclo se ejecuta múltiples veces rápido | Baja | Bajo | La verificación de `estado = EN_COLA` es idempotente; una vez procesada, la orden cambia de estado | Manual: monitorear logs al abrir el mercado |
| R2 | Feriados de mercado no configurados → órdenes procesadas en días que el mercado está cerrado | Media | Medio | HU-34 provee la gestión de feriados; el check incluye `esFeriadoMercado(fecha)` | Manual: crear feriado y verificar que las órdenes no se procesan |

---

## Criterios de verificación

### Escenarios de aceptación (Gherkin)

```gherkin
Funcionalidad: Encolamiento de órdenes fuera de horario

  Antecedentes:
    Dado que el mercado NYSE está cerrado

  Escenario: Orden creada fuera de horario queda EN_COLA
    Cuando "ana@test.com" envía POST /api/ordenes con símbolo AAPL
    Entonces el sistema responde 201 Created
    Y la orden tiene estado="EN_COLA"
    Y cuenta_fondos.fondos_reservados aumentó

  Escenario: Orden EN_COLA se procesa al abrir el mercado (símbolo US)
    Dado que existe orden EN_COLA de AAPL para "ana@test.com"
    Cuando el mercado NYSE abre (9:30 EST) y ColaOrdenesService.procesarCola se ejecuta
    Entonces la orden cambia a estado="ENVIADA"
    Y alpaca_order_id no es null
    Y "ana@test.com" recibe notificación de procesamiento
    Y se emite evento ORDEN_ENVIADA_ALPACA en auditoría

  Escenario: Orden global EN_COLA se ejecuta internamente al abrir
    Dado que existe orden EN_COLA de RIO.LON para "ana@test.com"
    Cuando el mercado abre y la cola se procesa
    Entonces la orden cambia a estado="EJECUTADA"
    Y holding de RIO.LON para "ana@test.com" aumentó

  Escenario: Orden cancelada antes de que abra el mercado
    Dado que existe orden EN_COLA id=42
    Cuando "ana@test.com" envía DELETE /api/ordenes/42 (antes de que abra)
    Entonces la orden tiene estado="CANCELADA"
    Y los fondos reservados fueron liberados
```

---

## Definición de terminado

- [x] Órdenes con mercado cerrado se crean con estado `EN_COLA` (en HU-17..20).
- [x] `ColaOrdenesService` verifica apertura del mercado cada 60 segundos.
- [x] Órdenes encoladas de símbolos US se envían a Alpaca al abrir.
- [x] Órdenes encoladas de símbolos globales se ejecutan internamente al abrir.
- [x] Orden de cuenta inactiva se cancela y libera fondos.
- [x] Inversionista recibe notificación cuando su cola se procesa.
- [x] Feriados de mercado considerados en la verificación de apertura.
- [x] `docs/PROGRESO.md` marcado con ✅ para HU-23.

---

## Historial de cambios

| Versión | Fecha | Descripción | Razón |
|---|---|---|---|
| 1.0 | 2026-05-24 | Refactorización a estructura SDD del proyecto. Documenta proceso interno `@Scheduled`, DDL de mercado y feriados, flujos de error del job. | Unificación de todos los SPEC.md bajo plantilla canónica SDD del proyecto |
