# SPEC — Configuración de porcentaje y split de comisión

---

## Ficha de la historia

| Campo | Valor |
|---|---|
| ID | HU-35 |
| Sprint | 4 |
| Prioridad MoSCoW | Should Have |
| Estado | Completada |
| Épica | Administración / Parámetros |
| CU asociado | CU-35 |
| Autor | Juan Diego Triana Mejia |
| Creada | 2026-05-20 |
| Última revisión | 2026-05-24 |
| Versión de esta spec | 1.0 |

**Trazabilidad:**

| Tipo | ID | Descripción |
|---|---|---|
| Requerimiento funcional | RF-34 | Administrador configura porcentaje de comisión y split plataforma/comisionista |
| Escenario de calidad | EC-12 | Trazabilidad de PARAMETRO_ADMIN_ACTUALIZADO |
| Escenario de calidad | EC-13 | Previsualización de comisión usa valores de esta tabla |
| Historia relacionada | HU-17 | OrdenService lee parámetros de comisión al crear/previsualizar orden |

---

## Historia de usuario

**Como** administrador autenticado,
**quiero** configurar el porcentaje de comisión y el split entre la plataforma y el comisionista,
**para** que todas las órdenes calculen las comisiones con los valores vigentes definidos centralmente.

---

## Actores y precondiciones

### Actores

| Actor | Rol en el sistema | Participación |
|---|---|---|
| Administrador autenticado | `ADMINISTRADOR` | Consulta y actualiza parámetros de comisión |
| `AdministracionService` | Módulo `administracion` | Lógica de negocio y persistencia |
| `AuditLogService` | Módulo `trazabilidad` | Registra cambios de configuración |

### Precondiciones

- JWT válido con rol `ADMINISTRADOR`.
- MFA completado.
- Existe al menos un registro en `parametro_comision` (seed inicial).

---

## Flujo principal — Consultar parámetros

1. Frontend envía `GET /api/admin/comisiones` con JWT.
2. `AdministracionService` retorna el registro vigente de `parametro_comision` (el que tiene `fecha_fin IS NULL`).
3. Responde `200 OK` con `ParametroComisionDTO`.

## Flujo principal — Actualizar parámetros

1. Administrador modifica los valores de comisión.
2. Frontend envía `PUT /api/admin/comisiones` con JWT y `ParametroComisionDTO`.
3. `AdministracionService.actualizarParametrosComision(dto)`:
   a. Valida que `porcentajeComision > 0 && <= 100`.
   b. Valida que `porcentajePlataforma + porcentajeComisionista == 100`.
   c. Cierra el registro vigente actual: `UPDATE parametro_comision SET fecha_fin = CURRENT_DATE - 1 WHERE fecha_fin IS NULL`.
   d. Inserta nuevo registro con `fecha_inicio = CURRENT_DATE`, `fecha_fin = NULL`.
4. `IAuditLog.registrar(PARAMETRO_ADMIN_ACTUALIZADO, correo_admin, "Comisión actualizada: {porcentaje}%")`.
5. Responde `200 OK` con `ParametroComisionDTO` actualizado.

---

## Flujos de error

### Error 1 — No autenticado o rol incorrecto

| Campo | Valor |
|---|---|
| Condición | JWT ausente o rol ≠ ADMINISTRADOR |
| HTTP | 401 / 403 |

### Error 2 — Porcentaje de comisión inválido

| Campo | Valor |
|---|---|
| Condición | `porcentajeComision <= 0` o `> 100` |
| HTTP | 400 Bad Request |
| Cuerpo | `RespuestaDTO{error: "El porcentaje de comisión debe estar entre 0.01 y 100"}` |

### Error 3 — Split incoherente

| Campo | Valor |
|---|---|
| Condición | `porcentajePlataforma + porcentajeComisionista ≠ 100` |
| HTTP | 400 Bad Request |
| Cuerpo | `RespuestaDTO{error: "La suma del split debe ser 100%"}` |

---

## Contrato de API

### Endpoint 1 — `GET /api/admin/comisiones`

```yaml
GET /api/admin/comisiones:
  summary: Consulta los parámetros de comisión vigentes
  security:
    - bearerAuth: []  # Solo ADMINISTRADOR
  responses:
    '200':
      description: Parámetros de comisión vigentes
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ParametroComisionDTO'
          example:
            porcentajeComision: 2.0
            porcentajePlataforma: 60.0
            porcentajeComisionista: 40.0
    '401':
      description: No autenticado
    '403':
      description: Rol incorrecto
```

### Endpoint 2 — `PUT /api/admin/comisiones`

```yaml
PUT /api/admin/comisiones:
  summary: Actualiza los parámetros de comisión
  security:
    - bearerAuth: []  # Solo ADMINISTRADOR
  requestBody:
    required: true
    content:
      application/json:
        schema:
          $ref: '#/components/schemas/ParametroComisionDTO'
        example:
          porcentajeComision: 1.5
          porcentajePlataforma: 65.0
          porcentajeComisionista: 35.0
  responses:
    '200':
      description: Parámetros actualizados exitosamente
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ParametroComisionDTO'
    '400':
      description: Validación fallida (porcentaje inválido o split incoherente)
    '401':
      description: No autenticado
    '403':
      description: Rol incorrecto

components:
  schemas:
    ParametroComisionDTO:
      type: object
      required: [porcentajeComision, porcentajePlataforma, porcentajeComisionista]
      properties:
        porcentajeComision:
          type: number
          format: double
          description: Porcentaje total de comisión sobre el monto base (ej. 2.0 = 2%)
          minimum: 0.01
          maximum: 100
          example: 2.0
        porcentajePlataforma:
          type: number
          format: double
          description: Porcentaje del total de comisión que va a la plataforma (ej. 60.0 = 60%)
          minimum: 0
          maximum: 100
          example: 60.0
        porcentajeComisionista:
          type: number
          format: double
          description: Porcentaje del total de comisión que va al comisionista (ej. 40.0 = 40%)
          minimum: 0
          maximum: 100
          example: 40.0
```

---

## Modelo de datos

```sql
CREATE TABLE parametro_comision (
    id                        BIGSERIAL PRIMARY KEY,
    porcentaje_comision       DECIMAL(5,4) NOT NULL DEFAULT 0.0200,  -- 2.00%
    split_plataforma          DECIMAL(6,2) NOT NULL DEFAULT 60.00,   -- 60% (columna real: split_plataforma)
    split_comisionista        DECIMAL(6,2) NOT NULL DEFAULT 40.00,   -- 40% (columna real: split_comisionista)
    fecha_inicio              DATE         NOT NULL,                  -- inicio de vigencia
    fecha_fin                 DATE,                                   -- null = vigente actualmente
    actualizado_por           VARCHAR(255)    -- correo del admin
);

-- Seed inicial — parámetro vigente desde hoy sin fecha de fin
INSERT INTO parametro_comision (porcentaje_comision, split_plataforma, split_comisionista, fecha_inicio)
VALUES (0.0200, 0.6000, 0.4000, CURRENT_DATE);
```

**Decisiones de esquema:**
- `fecha_inicio DATE NOT NULL` + `fecha_fin DATE NULL` reemplazan `activo BOOLEAN + actualizado_en TIMESTAMP`.
- El parámetro vigente es el registro con `fecha_fin IS NULL` (o `fecha_fin >= CURRENT_DATE`).
- Al actualizar parámetros, el registro anterior recibe `fecha_fin = CURRENT_DATE - 1 day` y se inserta un nuevo registro con `fecha_inicio = CURRENT_DATE`. Esto mantiene historial de vigencias para trazabilidad y para órdenes encoladas que se procesan días después.

**Regla de cálculo derivada** (usada en `OrdenService`):

```
montoBase      = precioEfectivo × cantidad
montoComision  = montoBase × porcentaje_comision
montoPlatforma = montoComision × porcentaje_plataforma
montoComisionista = montoComision × porcentaje_comisionista
```

---

## Módulos y arquitectura

| Módulo | Rol | Componentes |
|---|---|---|
| `administracion` | Coordinador | `AdminController`, `AdministracionService`, `ParametroComisionRepository` |
| `ordenes` | Consumidor | `OrdenService` lee via `IGestorParametros.obtenerPorcentajeComision() / obtenerSplitPlataforma() / obtenerSplitComisionista()` |
| `trazabilidad` | Auditoría | `AuditLogService` vía `IAuditLog` |

### Interfaces utilizadas

| Interfaz | Módulo proveedor | Método |
|---|---|---|
| `IAuditLog` | `trazabilidad` | `registrar(evento, correo, detalle)` |
| `IGestorParametros` | `administracion` | `obtenerPorcentajeComision()`, `obtenerSplitPlataforma()`, `obtenerSplitComisionista()` → `BigDecimal` cada uno |

### Escenarios de calidad y tácticas materializadas

| EC | Táctica | Cómo se materializa en HU-35 |
|---|---|---|
| EC-12 | Audit Trail | PARAMETRO_ADMIN_ACTUALIZADO con valor anterior y nuevo |
| EC-13 | Commission Preview | `OrdenService.previsualizarOrden` usa `IGestorParametros.obtenerPorcentajeComision() / obtenerSplitPlataforma() / obtenerSplitComisionista()` — siempre el valor actual |

---

## Eventos y efectos transversales

| Evento | Cuándo | Consumidor |
|---|---|---|
| `PARAMETRO_ADMIN_ACTUALIZADO` | Al actualizar parámetros de comisión | `trazabilidad` |

Efecto transversal: `OrdenService` lee `parametro_comision` en cada creación y previsualización de orden. El cambio es efectivo de forma inmediata para todas las órdenes nuevas. Las órdenes ya ejecutadas conservan los valores con los que fueron calculadas (campos `monto_comision`, `monto_plataforma`, `monto_comisionista` en la tabla `orden`).

---

## Riesgos

| # | Riesgo | P | I | Mitigación |
|---|---|:-:|:-:|---|
| R1 | Cambio de comisión afecta órdenes en `EN_COLA` | Media | Medio | Las órdenes encoladas recalculan al momento de envío (HU-23 / HU-32); se notifica discrepancia |
| R3 | **Deuda técnica detectada (auditoría 2026-05-25):** `ColaOrdenesService` usa `@Value("${app.comision.porcentaje:2.0}")` (propiedad fija) en lugar de `IGestorParametros` para calcular comisiones de órdenes encoladas. Las órdenes encoladas NO usan el parámetro de BD al ejecutarse. | Alta | Medio | Migrar `ColaOrdenesService` para inyectar `IGestorParametros` y leer `obtenerPorcentajeComision()` en lugar de la propiedad hardcodeada. |
| R2 | Split configurado a 0% comisionista cuando no hay comisionistas | Baja | Ninguno | Válido — si no hay comisionista asignado, `monto_comisionista = 0` |

---

## Criterios de verificación

### Escenarios de aceptación (Gherkin)

```gherkin
Funcionalidad: Configuración de parámetros de comisión

  Antecedentes:
    Dado que "admin@test.com" tiene JWT válido con rol=ADMINISTRADOR y MFA completado

  Escenario: Consulta de parámetros vigentes
    Cuando se envía GET /api/admin/comisiones
    Entonces el sistema responde 200 OK
    Y la respuesta contiene porcentajeComision=2.0, porcentajePlataforma=60.0, porcentajeComisionista=40.0

  Escenario: Actualización exitosa de comisión
    Cuando se envía PUT /api/admin/comisiones con {porcentajeComision: 1.5, porcentajePlataforma: 65.0, porcentajeComisionista: 35.0}
    Entonces el sistema responde 200 OK
    Y la respuesta tiene porcentajeComision=1.5
    Y se emite evento PARAMETRO_ADMIN_ACTUALIZADO en auditoría

  Escenario: Split incoherente retorna 400
    Cuando se envía PUT /api/admin/comisiones con {porcentajePlataforma: 70.0, porcentajeComisionista: 40.0}
    Entonces el sistema responde 400 Bad Request
    Y el mensaje de error indica que la suma del split debe ser 100%

  Escenario: Porcentaje de comisión cero retorna 400
    Cuando se envía PUT /api/admin/comisiones con {porcentajeComision: 0.0}
    Entonces el sistema responde 400 Bad Request

  Escenario: Nueva orden usa el parámetro actualizado
    Dado que porcentajeComision fue actualizado a 1.5
    Cuando se crea una orden con montoBase=1000
    Entonces montoComision=15.0 (1.5% de 1000)

  Escenario: Sin JWT retorna 401
    Cuando se envía GET /api/admin/comisiones sin Authorization
    Entonces el sistema responde 401 Unauthorized
```

---

## Interfaz de usuario

- Panel de parámetros de comisión: campos editables de porcentaje total y split.
- Indicador en tiempo real de la suma del split (debe sumar 100%).
- Botón "Guardar cambios" con confirmación: "Este cambio aplicará a todas las órdenes nuevas. ¿Continuar?".
- Historial de cambios (últimas N actualizaciones con fecha y admin que lo realizó).

---

## Fuera de alcance

- Comisiones diferenciadas por tipo de orden, símbolo o mercado.
- Comisiones por volumen o por cliente (descuentos).
- Retroactividad de cambios a órdenes ya ejecutadas.

---

## Definición de terminado

- [x] `GET /api/admin/comisiones` retorna parámetros vigentes de `parametro_comision`.
- [x] `PUT /api/admin/comisiones` actualiza `porcentajeComision`, `porcentajePlataforma`, `porcentajeComisionista`.
- [x] Validación: porcentaje > 0 y ≤ 100; split suma 100%.
- [x] Cambio inválido retorna 400 con mensaje descriptivo.
- [x] Sin JWT o rol incorrecto retorna 401/403.
- [x] Evento `PARAMETRO_ADMIN_ACTUALIZADO` registrado en auditoría.
- [x] `OrdenService` lee parámetros vía `IGestorParametros.obtenerPorcentajeComision() / obtenerSplitPlataforma() / obtenerSplitComisionista()` (no hardcodeado).
- [x] `docs/PROGRESO.md` marcado con ✅ para HU-35.

---

## Historial de cambios

| Versión | Fecha | Descripción | Razón |
|---|---|---|---|
| 1.0 | 2026-05-24 | Refactorización a estructura SDD del proyecto. | Unificación de todos los SPEC.md bajo plantilla canónica SDD del proyecto |
