# SPEC — Configuración de feriados por mercado

---

## Ficha de la historia

| Campo | Valor |
|---|---|
| ID | HU-34 |
| Sprint | 4 |
| Prioridad MoSCoW | Should Have |
| Estado | Completada |
| Épica | Administración / Parámetros |
| CU asociado | CU-34 |
| Autor | Juan Diego Triana Mejia |
| Creada | 2026-05-20 |
| Última revisión | 2026-05-24 |
| Versión de esta spec | 1.0 |

**Trazabilidad:**

| Tipo | ID | Descripción |
|---|---|---|
| Requerimiento funcional | RF-33 | Administrador gestiona feriados de mercado |
| Escenario de calidad | EC-12 | Trazabilidad de PARAMETRO_ADMIN_ACTUALIZADO |
| Historia relacionada | HU-33 | La tabla `mercado_config` es referenciada por `feriado_mercado` |
| Historia relacionada | HU-23 | La cola de órdenes verifica feriados antes de procesar |

---

## Historia de usuario

**Como** administrador autenticado,
**quiero** gestionar el calendario de feriados de cada mercado,
**para** que el sistema no intente enviar órdenes en días no hábiles y las mantenga encoladas correctamente.

---

## Actores y precondiciones

### Actores

| Actor | Rol en el sistema | Participación |
|---|---|---|
| Administrador autenticado | `ADMINISTRADOR` | Consulta, crea y elimina feriados |
| `AdministracionService` | Módulo `administracion` | Lógica de negocio y persistencia |
| `AuditLogService` | Módulo `trazabilidad` | Registra cambios de configuración |

### Precondiciones

- JWT válido con rol `ADMINISTRADOR`.
- MFA completado.
- El mercado identificado por `codigoMercado` existe en `mercado_config`.

---

## Flujo principal — Consultar feriados de un mercado

1. Frontend envía `GET /api/admin/mercados/{codigoMercado}/feriados` con JWT.
2. `AdministracionService` resuelve el `id` de `mercado_config` a partir de `codigoMercado` y consulta `feriado_mercado` filtrado por `mercado_config_id`.
3. Responde `200 OK` con `List<FeriadoDTO>`.

## Flujo principal — Registrar feriado

1. Administrador selecciona mercado y fecha del feriado.
2. Frontend envía `POST /api/admin/mercados/{codigoMercado}/feriados` con JWT y `FeriadoDTO`.
3. `AdministracionService.agregarFeriado(codigoMercado, dto)`:
   a. Valida que el mercado existe. Si no → 404.
   b. Valida que la fecha no esté ya registrada para ese mercado (duplicado → 409).
   c. Persiste el registro en `feriado_mercado`.
4. `IAuditLog.registrar(PARAMETRO_ADMIN_ACTUALIZADO, correo_admin, "Feriado {fecha} agregado a {codigoMercado}")`.
5. Responde `201 Created` con `FeriadoDTO`.

## Flujo principal — Eliminar feriado

1. Frontend envía `DELETE /api/admin/mercados/{codigoMercado}/feriados/{feriadoId}` con JWT.
2. `AdministracionService.eliminarFeriado(codigoMercado, feriadoId)`:
   a. Valida que el feriado existe y pertenece al mercado. Si no → 404.
   b. Elimina el registro.
3. `IAuditLog.registrar(PARAMETRO_ADMIN_ACTUALIZADO, correo_admin, "Feriado {feriadoId} eliminado de {codigoMercado}")`.
4. Responde `200 OK`.

---

## Flujos de error

### Error 1 — No autenticado o rol incorrecto

| Campo | Valor |
|---|---|
| Condición | JWT ausente o rol ≠ ADMINISTRADOR |
| HTTP | 401 / 403 |

### Error 2 — Mercado no encontrado

| Campo | Valor |
|---|---|
| Condición | `codigoMercado` no existe en `mercado_config` |
| HTTP | 404 Not Found |
| Cuerpo | `RespuestaDTO{error: "Mercado no encontrado: {codigoMercado}"}` |

### Error 3 — Feriado duplicado

| Campo | Valor |
|---|---|
| Condición | La fecha ya está registrada para ese mercado |
| HTTP | 409 Conflict |
| Cuerpo | `RespuestaDTO{error: "Ya existe un feriado registrado para {fecha} en {codigoMercado}"}` |

### Error 4 — Feriado no encontrado (para DELETE)

| Campo | Valor |
|---|---|
| Condición | `feriadoId` no existe o no pertenece al `codigoMercado` |
| HTTP | 404 Not Found |
| Cuerpo | `RespuestaDTO{error: "Feriado no encontrado"}` |

### Error 5 — Fecha inválida

| Campo | Valor |
|---|---|
| Condición | La fecha no tiene formato `YYYY-MM-DD` (formato inválido en el body) |
| HTTP | 400 Bad Request |
| Nota | El código actual **no valida** que la fecha sea futura. Se acepta registrar feriados pasados. Ver pregunta abierta PA-1. |

---

## Contrato de API

### Endpoint 1 — `GET /api/admin/mercados/{codigoMercado}/feriados`

```yaml
GET /api/admin/mercados/{codigoMercado}/feriados:
  summary: Lista los feriados registrados para un mercado
  security:
    - bearerAuth: []  # Solo ADMINISTRADOR
  parameters:
    - name: codigoMercado
      in: path
      required: true
      schema:
        type: string
      example: "NYSE"
  responses:
    '200':
      description: Lista de feriados del mercado
      content:
        application/json:
          schema:
            type: array
            items:
              $ref: '#/components/schemas/FeriadoDTO'
          example:
            - id: 1
              mercadoCodigo: "NYSE"
              fecha: "2026-07-04"
              descripcion: "Independence Day"
            - id: 2
              mercadoCodigo: "NYSE"
              fecha: "2026-12-25"
              descripcion: "Christmas Day"
    '404':
      description: Mercado no encontrado
```

### Endpoint 2 — `POST /api/admin/mercados/{codigoMercado}/feriados`

```yaml
POST /api/admin/mercados/{codigoMercado}/feriados:
  summary: Registra un nuevo feriado para un mercado
  security:
    - bearerAuth: []  # Solo ADMINISTRADOR
  parameters:
    - name: codigoMercado
      in: path
      required: true
      schema:
        type: string
  requestBody:
    required: true
    content:
      application/json:
        schema:
          $ref: '#/components/schemas/FeriadoDTO'
        example:
          fecha: "2026-07-04"
          descripcion: "Independence Day"
  responses:
    '201':
      description: Feriado registrado exitosamente
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/FeriadoDTO'
    '400':
      description: Fecha inválida
    '404':
      description: Mercado no encontrado
    '409':
      description: Fecha ya registrada para este mercado

components:
  schemas:
    FeriadoDTO:
      type: object
      properties:
        id:
          type: integer
          description: Generado por el sistema; no requerido en POST
        mercadoConfigId:
          type: integer
          format: int64
          description: "ID de mercado_config (no el código textual)"
        mercadoCodigo:
          type: string
          description: "Código del mercado (ej. NYSE) — incluido en respuesta para legibilidad, derivado por JOIN"
          readOnly: true
        fecha:
          type: string
          format: date
          example: "2026-07-04"
        descripcion:
          type: string
          nullable: true
```

### Endpoint 3 — `DELETE /api/admin/mercados/{codigoMercado}/feriados/{feriadoId}`

```yaml
DELETE /api/admin/mercados/{codigoMercado}/feriados/{feriadoId}:
  summary: Elimina un feriado registrado
  security:
    - bearerAuth: []  # Solo ADMINISTRADOR
  parameters:
    - name: codigoMercado
      in: path
      required: true
      schema:
        type: string
    - name: feriadoId
      in: path
      required: true
      schema:
        type: integer
  responses:
    '200':
      description: Feriado eliminado exitosamente
    '404':
      description: Feriado o mercado no encontrado
```

---

## Modelo de datos

```sql
-- Tabla feriado_mercado (DDL completo definido en HU-23)
CREATE TABLE feriado_mercado (
    id                BIGSERIAL PRIMARY KEY,
    mercado_config_id BIGINT NOT NULL REFERENCES mercado_config(id),  -- FK por ID, no por código
    fecha             DATE NOT NULL,
    descripcion       VARCHAR(200),
    UNIQUE (mercado_config_id, fecha)
);

-- Datos iniciales (feriados NYSE/NASDAQ 2026)
-- Asume mercado_config.id=1 para NYSE y id=2 para NASDAQ (ver seed de HU-33)
INSERT INTO feriado_mercado (mercado_config_id, fecha, descripcion) VALUES
    (1, '2026-01-01', 'New Year''s Day'),
    (2, '2026-01-01', 'New Year''s Day'),
    (1, '2026-07-04', 'Independence Day'),
    (2, '2026-07-04', 'Independence Day'),
    (1, '2026-12-25', 'Christmas Day'),
    (2, '2026-12-25', 'Christmas Day');
```

**Decisiones de esquema:**
- `mercado_config_id BIGINT` reemplaza `mercado_codigo VARCHAR(20)`. La FK ahora apunta al `id` numérico de `mercado_config`, no al código textual. Esto sigue el patrón de integridad referencial estricta (normalización 3NF).
- El `UNIQUE (mercado_config_id, fecha)` reemplaza el antiguo `UNIQUE (mercado_codigo, fecha)`.

---

## Módulos y arquitectura

| Módulo | Rol | Componentes |
|---|---|---|
| `administracion` | Coordinador | `AdminController`, `AdministracionService`, `MercadoFeriadoRepository` |
| `trazabilidad` | Auditoría | `AuditLogService` vía `IAuditLog` |

### Interfaces utilizadas

| Interfaz | Módulo proveedor | Método |
|---|---|---|
| `IAuditLog` | `trazabilidad` | `registrar(evento, correo, detalle)` |

### Escenarios de calidad y tácticas materializadas

| EC | Táctica | Cómo se materializa en HU-34 |
|---|---|---|
| EC-12 | Audit Trail | PARAMETRO_ADMIN_ACTUALIZADO por cada creación o eliminación de feriado |
| EC-04 | Queue Availability | El scheduler de HU-23 consulta `feriado_mercado` para no procesar en días festivos |

---

## Eventos y efectos transversales

| Evento | Cuándo | Consumidor |
|---|---|---|
| `PARAMETRO_ADMIN_ACTUALIZADO` | Al agregar o eliminar un feriado | `trazabilidad` |

Efecto transversal: el scheduler de `ColaOrdenesService` (HU-23) consulta `feriado_mercado` en cada tick. Feriados agregados son efectivos en el siguiente ciclo del scheduler (hasta 60 segundos de latencia).

---

## Riesgos

| # | Riesgo | P | I | Mitigación |
|---|---|:-:|:-:|---|
| R1 | Admin elimina un feriado que ya pasó, sin efecto real | Baja | Ninguno | Advertencia en UI si la fecha es pasada; la operación sigue siendo válida |
| R2 | Feriado no cargado para el año siguiente | Media | Medio | Seed con feriados del año en curso; administrador es responsable de actualizar anualmente |

---

## Criterios de verificación

### Escenarios de aceptación (Gherkin)

```gherkin
Funcionalidad: Configuración de feriados de mercado

  Antecedentes:
    Dado que "admin@test.com" tiene JWT válido con rol=ADMINISTRADOR y MFA completado

  Escenario: Consulta de feriados de NYSE
    Cuando se envía GET /api/admin/mercados/NYSE/feriados
    Entonces el sistema responde 200 OK
    Y la lista incluye feriados con formato de fecha YYYY-MM-DD

  Escenario: Registro exitoso de feriado
    Cuando se envía POST /api/admin/mercados/NYSE/feriados con fecha="2026-11-26" y descripcion="Thanksgiving"
    Entonces el sistema responde 201 Created
    Y el feriado aparece en GET /api/admin/mercados/NYSE/feriados
    Y se emite evento PARAMETRO_ADMIN_ACTUALIZADO en auditoría

  Escenario: Feriado duplicado retorna 409
    Dado que ya existe feriado para NYSE en fecha="2026-07-04"
    Cuando se envía POST /api/admin/mercados/NYSE/feriados con fecha="2026-07-04"
    Entonces el sistema responde 409 Conflict

  Escenario: Eliminación de feriado
    Dado que existe feriadoId=1 para NYSE
    Cuando se envía DELETE /api/admin/mercados/NYSE/feriados/1
    Entonces el sistema responde 200 OK
    Y el feriado ya no aparece en el listado
    Y se emite evento PARAMETRO_ADMIN_ACTUALIZADO en auditoría

  Escenario: Mercado no encontrado retorna 404
    Cuando se envía GET /api/admin/mercados/INEXISTENTE/feriados
    Entonces el sistema responde 404 Not Found
```

---

## Interfaz de usuario

- Vista por mercado: selector de mercado → tabla de feriados (fecha, descripción, acción eliminar).
- Botón "Agregar feriado" con selector de fecha y campo de descripción.
- Confirmación antes de eliminar un feriado futuro.

---

## Fuera de alcance

- Importación masiva de feriados desde archivo CSV o API externa.
- Notificación a inversionistas sobre feriados próximos.
- Feriados parciales (mercado que cierra a mediodía).

---

## Definición de terminado

- [x] `GET /api/admin/mercados/{codigo}/feriados` retorna feriados del mercado.
- [x] `POST /api/admin/mercados/{codigo}/feriados` crea feriado; retorna 201.
- [x] Feriado duplicado retorna 409.
- [x] `DELETE /api/admin/mercados/{codigo}/feriados/{id}` elimina el feriado.
- [x] Feriado o mercado no encontrado retorna 404.
- [x] Evento `PARAMETRO_ADMIN_ACTUALIZADO` registrado en cada mutación.
- [x] `docs/PROGRESO.md` marcado con ✅ para HU-34.

---

## Historial de cambios

| Versión | Fecha | Descripción | Razón |
|---|---|---|---|
| 1.0 | 2026-05-24 | Refactorización a estructura SDD del proyecto. | Unificación de todos los SPEC.md bajo plantilla canónica SDD del proyecto |
