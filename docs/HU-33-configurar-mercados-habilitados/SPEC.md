# SPEC — Configuración de mercados habilitados y horarios

---

## Ficha de la historia

| Campo | Valor |
|---|---|
| ID | HU-33 |
| Sprint | 4 |
| Prioridad MoSCoW | Should Have |
| Estado | Completada |
| Épica | Administración / Parámetros |
| CU asociado | CU-33 |
| Autor | Juan Diego Triana Mejia |
| Creada | 2026-05-20 |
| Última revisión | 2026-05-24 |
| Versión de esta spec | 1.0 |

**Trazabilidad:**

| Tipo | ID | Descripción |
|---|---|---|
| Requerimiento funcional | RF-32 | Administrador configura mercados habilitados y sus horarios de operación |
| Escenario de calidad | EC-12 | Trazabilidad de PARAMETRO_ADMIN_ACTUALIZADO |
| Historia relacionada | HU-23 | La cola de órdenes usa `mercado_config` para determinar si el mercado está abierto |
| Historia relacionada | HU-34 | Feriados de mercado usan esta misma tabla como referencia |

---

## Historia de usuario

**Como** administrador autenticado,
**quiero** configurar qué mercados están habilitados y sus horarios de operación,
**para** que el sistema pueda determinar correctamente si está en horario de mercado al procesar órdenes.

---

## Actores y precondiciones

### Actores

| Actor | Rol en el sistema | Participación |
|---|---|---|
| Administrador autenticado | `ADMINISTRADOR` | Consulta y actualiza configuración de mercados |
| `AdministracionService` | Módulo `administracion` | Lógica de negocio y persistencia |
| `AuditLogService` | Módulo `trazabilidad` | Registra cambios de configuración |

### Precondiciones

- JWT válido con rol `ADMINISTRADOR`.
- MFA completado (el rol ADMINISTRADOR requiere MFA obligatorio).
- El mercado identificado por `codigo` existe en la tabla `mercado_config`.

---

## Flujo principal — Consultar mercados

1. Administrador navega a la sección de configuración de mercados.
2. Frontend envía `GET /api/admin/mercados` con JWT.
3. `AdministracionService` retorna todos los registros de `mercado_config`.
4. Responde `200 OK` con `List<MercadoConfigDTO>`.

## Flujo principal — Actualizar mercado

1. Administrador selecciona un mercado y modifica sus parámetros.
2. Frontend envía `PUT /api/admin/mercados/{codigo}` con JWT y `MercadoConfigDTO`.
3. `AdministracionService.actualizarMercado(codigo, dto)`:
   a. Busca el mercado por `codigo`. Si no existe → 404.
   b. Actualiza los campos: `habilitado`, `horaApertura`, `horaCierre`, `zonaHoraria`, `diasOperacion`.
   c. Persiste el registro actualizado.
4. `IAuditLog.registrar(PARAMETRO_ADMIN_ACTUALIZADO, correo_admin, "Mercado {codigo} actualizado")`.
5. Responde `200 OK` con `MercadoConfigDTO` actualizado.

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
| Condición | `codigo` no existe en `mercado_config` |
| HTTP | 404 Not Found |
| Cuerpo | `RespuestaDTO{error: "Mercado no encontrado: {codigo}"}` |

### Error 3 — Campos inválidos

| Campo | Valor |
|---|---|
| Condición | Hora en formato incorrecto o zona horaria inválida |
| HTTP | 400 Bad Request |

---

## Contrato de API

### Endpoint 1 — `GET /api/admin/mercados`

```yaml
GET /api/admin/mercados:
  summary: Lista la configuración de todos los mercados
  security:
    - bearerAuth: []  # Solo ADMINISTRADOR
  responses:
    '200':
      description: Lista de configuraciones de mercado
      content:
        application/json:
          schema:
            type: array
            items:
              $ref: '#/components/schemas/MercadoConfigDTO'
          example:
            - codigo: "NYSE"
              nombre: "New York Stock Exchange"
              habilitado: true
              horaApertura: "09:30"
              horaCierre: "16:00"
              zonaHoraria: "America/New_York"
              diasOperacion: ["LUNES","MARTES","MIERCOLES","JUEVES","VIERNES"]
            - codigo: "NASDAQ"
              nombre: "NASDAQ"
              habilitado: true
              horaApertura: "09:30"
              horaCierre: "16:00"
              zonaHoraria: "America/New_York"
              diasOperacion: ["LUNES","MARTES","MIERCOLES","JUEVES","VIERNES"]
    '401':
      description: No autenticado
    '403':
      description: Rol incorrecto
```

### Endpoint 2 — `PUT /api/admin/mercados/{codigo}`

```yaml
PUT /api/admin/mercados/{codigo}:
  summary: Actualiza la configuración de un mercado
  security:
    - bearerAuth: []  # Solo ADMINISTRADOR
  parameters:
    - name: codigo
      in: path
      required: true
      schema:
        type: string
      example: "NYSE"
  requestBody:
    required: true
    content:
      application/json:
        schema:
          $ref: '#/components/schemas/MercadoConfigDTO'
        example:
          habilitado: true
          horaApertura: "09:30"
          horaCierre: "16:00"
          zonaHoraria: "America/New_York"
          diasOperacion: ["LUNES","MARTES","MIERCOLES","JUEVES","VIERNES"]
  responses:
    '200':
      description: Configuración actualizada exitosamente
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/MercadoConfigDTO'
    '400':
      description: Campos inválidos
    '403':
      description: No autorizado
    '404':
      description: Mercado no encontrado

components:
  schemas:
    MercadoConfigDTO:
      type: object
      properties:
        codigo:
          type: string
          description: Código del mercado (NYSE, NASDAQ, etc.)
        nombre:
          type: string
        habilitado:
          type: boolean
        horaApertura:
          type: string
          description: Formato HH:mm
          example: "09:30"
        horaCierre:
          type: string
          description: Formato HH:mm
          example: "16:00"
        zonaHoraria:
          type: string
          description: ZoneId de Java (ej. America/New_York)
        diasOperacion:
          type: array
          items:
            type: string
          description: Días de la semana habilitados
          example: ["LUNES","MARTES","MIERCOLES","JUEVES","VIERNES"]
```

---

## Modelo de datos

```sql
-- Tabla mercado_config (DDL completo definido en HU-23)
CREATE TABLE mercado_config (
    id              BIGSERIAL PRIMARY KEY,
    codigo          VARCHAR(20) UNIQUE NOT NULL,  -- 'NYSE', 'NASDAQ', 'BMV', etc.
    nombre          VARCHAR(100) NOT NULL,
    habilitado      BOOLEAN NOT NULL DEFAULT TRUE,
    hora_apertura   TIME NOT NULL,                -- 09:30:00
    hora_cierre     TIME NOT NULL,                -- 16:00:00
    zona_horaria    VARCHAR(50) NOT NULL,          -- 'America/New_York'
    dias_operacion  VARCHAR(100) NOT NULL          -- 'LUNES,MARTES,MIERCOLES,JUEVES,VIERNES'
);

-- Datos iniciales (seed)
INSERT INTO mercado_config (codigo, nombre, habilitado, hora_apertura, hora_cierre, zona_horaria, dias_operacion)
VALUES
    ('NYSE',   'New York Stock Exchange', TRUE, '09:30', '16:00', 'America/New_York', 'LUNES,MARTES,MIERCOLES,JUEVES,VIERNES'),
    ('NASDAQ', 'NASDAQ',                  TRUE, '09:30', '16:00', 'America/New_York', 'LUNES,MARTES,MIERCOLES,JUEVES,VIERNES');
```

---

## Módulos y arquitectura

| Módulo | Rol | Componentes |
|---|---|---|
| `administracion` | Coordinador | `AdminController`, `AdministracionService`, `MercadoConfigRepository` |
| `trazabilidad` | Auditoría | `AuditLogService` vía `IAuditLog` |

### Interfaces utilizadas

| Interfaz | Módulo proveedor | Método |
|---|---|---|
| `IAuditLog` | `trazabilidad` | `registrar(evento, correo, detalle)` |

### Escenarios de calidad y tácticas materializadas

| EC | Táctica | Cómo se materializa en HU-33 |
|---|---|---|
| EC-12 | Audit Trail | PARAMETRO_ADMIN_ACTUALIZADO registrado con correo del admin y mercado modificado |
| EC-04 | Queue Availability | La tabla `mercado_config` es la fuente de verdad para el scheduler de HU-23; cambios son inmediatos |

---

## Eventos y efectos transversales

| Evento | Cuándo | Consumidor |
|---|---|---|
| `PARAMETRO_ADMIN_ACTUALIZADO` | Al modificar la configuración de un mercado | `trazabilidad` |

Efecto transversal: el scheduler de `ColaOrdenesService` (HU-23) consulta `mercado_config` en cada ejecución. Cambios a `habilitado`, `horaApertura` o `horaCierre` son efectivos en el siguiente tick del scheduler (hasta 60 segundos de latencia).

---

## Riesgos

| # | Riesgo | P | I | Mitigación |
|---|---|:-:|:-:|---|
| R1 | Admin deshabilita un mercado mientras hay órdenes EN_COLA para ese mercado | Baja | Medio | Las órdenes EN_COLA permanecen; se procesan cuando el mercado vuelve a habilitarse |
| R2 | Zona horaria incorrecta afecta cálculo de horario | Baja | Alto | Validar que la zona horaria sea un ZoneId válido de Java antes de persistir |

---

## Criterios de verificación

### Escenarios de aceptación (Gherkin)

```gherkin
Funcionalidad: Configuración de mercados habilitados

  Antecedentes:
    Dado que "admin@test.com" tiene JWT válido con rol=ADMINISTRADOR y MFA completado

  Escenario: Consulta de mercados
    Cuando se envía GET /api/admin/mercados con JWT de admin
    Entonces el sistema responde 200 OK
    Y la respuesta contiene al menos los mercados NYSE y NASDAQ

  Escenario: Deshabilitar un mercado
    Cuando se envía PUT /api/admin/mercados/NYSE con body {habilitado: false}
    Entonces el sistema responde 200 OK
    Y la respuesta tiene habilitado=false
    Y se emite evento PARAMETRO_ADMIN_ACTUALIZADO en auditoría

  Escenario: Actualizar horario de mercado
    Cuando se envía PUT /api/admin/mercados/NYSE con body {horaApertura: "09:00", horaCierre: "17:00"}
    Entonces el sistema responde 200 OK
    Y la respuesta tiene horaApertura="09:00" y horaCierre="17:00"

  Escenario: Mercado no encontrado retorna 404
    Cuando se envía PUT /api/admin/mercados/INEXISTENTE con datos válidos
    Entonces el sistema responde 404 Not Found

  Escenario: Sin JWT retorna 401
    Cuando se envía GET /api/admin/mercados sin Authorization
    Entonces el sistema responde 401 Unauthorized
```

---

## Interfaz de usuario

- Tabla con todos los mercados: código, nombre, estado (habilitado/deshabilitado), horario, zona horaria, días.
- Toggle de habilitado/deshabilitado por mercado.
- Modal de edición con campos: hora apertura, hora cierre, zona horaria, días de operación.
- Confirmación con resumen del cambio antes de guardar.

---

## Fuera de alcance

- Creación de nuevos mercados vía UI (los mercados se insertan con datos iniciales).
- Eliminación de mercados.
- Notificación a inversionistas cuando un mercado se deshabilita.

---

## Definición de terminado

- [x] `GET /api/admin/mercados` retorna todos los mercados de `mercado_config`.
- [x] `PUT /api/admin/mercados/{codigo}` actualiza `habilitado`, `horaApertura`, `horaCierre`, `zonaHoraria`, `diasOperacion`.
- [x] Mercado no encontrado retorna 404.
- [x] Campos inválidos retornan 400.
- [x] Sin JWT o rol incorrecto retorna 401/403.
- [x] Evento `PARAMETRO_ADMIN_ACTUALIZADO` registrado en auditoría.
- [x] `docs/PROGRESO.md` marcado con ✅ para HU-33.

---

## Historial de cambios

| Versión | Fecha | Descripción | Razón |
|---|---|---|---|
| 1.0 | 2026-05-24 | Refactorización a estructura SDD del proyecto. | Unificación de todos los SPEC.md bajo plantilla canónica SDD del proyecto |
