# SPEC — Asignación manual de comisionista a inversionista

---

## Ficha de la historia

| Campo | Valor |
|---|---|
| ID | HU-37 |
| Sprint | 4 |
| Prioridad MoSCoW | Should Have |
| Estado | En desarrollo |
| Épica | Administración / Gestión de Cuentas |
| CU asociado | CU-37 |
| Autor | Juan Diego Triana Mejia |
| Creada | 2026-05-20 |
| Última revisión | 2026-05-24 |
| Versión de esta spec | 1.0 |

> ⚠️ **Estado En desarrollo:** La funcionalidad está implementada pero aún en fase de pruebas y validación. El endpoint existe y persiste la asignación, pero los criterios de verificación completos no han sido validados en entorno integrado.

**Trazabilidad:**

| Tipo | ID | Descripción |
|---|---|---|
| Requerimiento funcional | RF-36 | Administrador asigna comisionista a inversionista |
| Escenario de calidad | EC-12 | Trazabilidad del evento de asignación |
| Historia que sigue | HU-28 | El comisionista asignado puede consultar portafolio del cliente |
| Historia que sigue | HU-30 | El comisionista asignado puede crear propuestas para el cliente |

---

## Historia de usuario

**Como** administrador autenticado,
**quiero** asignar un comisionista a un inversionista,
**para** que el comisionista pueda gestionar las operaciones del inversionista con su consentimiento.

---

## Actores y precondiciones

### Actores

| Actor | Rol en el sistema | Participación |
|---|---|---|
| Administrador autenticado | `ADMINISTRADOR` | Crea la asignación |
| `AdministracionService` | Módulo `administracion` | Coordina la operación |
| `IGestionCuentas` | Módulo `autenticacion` | Persiste la asignación en `asignacion_comisionista` |
| `AuditLogService` | Módulo `trazabilidad` | Registra la asignación |

### Precondiciones

- JWT válido con rol `ADMINISTRADOR`.
- MFA completado.
- `inversionistaId` existe con `rol = INVERSIONISTA`.
- `comisionistaId` existe con `rol = COMISIONISTA`.
- Si existe una asignación previa activa, el sistema la desactiva automáticamente antes de crear la nueva (no bloquea la operación). Un inversionista tiene máximo una asignación activa en todo momento.

---

## Flujo principal

1. Administrador selecciona inversionista y comisionista en el panel.
2. Frontend envía `PUT /api/admin/inversionistas/{inversionistaId}/comisionista/{comisionistaId}` con JWT.
3. `AdministracionService.asignarComisionista(inversionistaId, comisionistaId, adminCorreo)`:
   a. Valida rol de administrador.
   b. Delega a `IGestionCuentas.asignarComisionista(inversionistaId, comisionistaId)`.
   c. Valida existencia de ambos usuarios con roles correctos.
   d. Si ya existe asignación activa → **la desactiva** (`activa = false`) y crea una nueva asignación activa (política real: reemplazar, nunca retornar 409 por reasignación).
   e. Persiste registro en `asignacion_comisionista`.
4. `IAuditLog.registrar(COMISIONISTA_ASIGNADO, correo_inversionista, "Asignado comisionista {comisionistaId} por {adminCorreo}")`.
5. Responde `200 OK` con `RespuestaDTO{mensaje: "Comisionista asignado exitosamente"}`.

---

## Flujos de error

### Error 1 — No autenticado o rol incorrecto

| Campo | Valor |
|---|---|
| HTTP | 401 / 403 |

### Error 2 — Inversionista o comisionista no encontrado

| Campo | Valor |
|---|---|
| HTTP | 404 Not Found |
| Cuerpo | `RespuestaDTO{error: "Usuario no encontrado"}` |

### Error 3 — Roles incorrectos

| Campo | Valor |
|---|---|
| Condición | `inversionistaId` tiene rol distinto a INVERSIONISTA, o `comisionistaId` tiene rol distinto a COMISIONISTA |
| HTTP | 400 Bad Request |

---

## Contrato de API

### Endpoint — `PUT /api/admin/inversionistas/{inversionistaId}/comisionista/{comisionistaId}`

```yaml
PUT /api/admin/inversionistas/{inversionistaId}/comisionista/{comisionistaId}:
  summary: Asigna un comisionista a un inversionista
  security:
    - bearerAuth: []  # Solo ADMINISTRADOR
  parameters:
    - name: inversionistaId
      in: path
      required: true
      schema:
        type: integer
    - name: comisionistaId
      in: path
      required: true
      schema:
        type: integer
  responses:
    '200':
      description: Comisionista asignado exitosamente
    '400':
      description: Roles incorrectos
    '403':
      description: No autorizado
    '404':
      description: Inversionista o comisionista no encontrado
```

---

## Modelo de datos

```sql
CREATE TABLE asignacion_comisionista (
    id                  BIGSERIAL PRIMARY KEY,
    inversionista_id    BIGINT NOT NULL REFERENCES inversionista(id),  -- FK a inversionista (no a usuario)
    comisionista_id     BIGINT NOT NULL REFERENCES usuario(id),
    asignado_en         TIMESTAMP NOT NULL DEFAULT NOW(),
    asignado_por        VARCHAR(255),  -- correo del admin
    activa              BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (inversionista_id)  -- un inversionista tiene máximo un comisionista activo
);
```

**Decisiones de esquema:**
- `inversionista_id` referencia `inversionista(id)` (antes `usuario(id)`). Esto refuerza que la asignación es a un perfil de inversionista, no a cualquier usuario del sistema.

---

## Módulos y arquitectura

| Módulo | Rol | Componentes |
|---|---|---|
| `administracion` | Coordinador | `AdminController`, `AdministracionService` |
| `autenticacion` | Persistencia | `IGestionCuentas.asignarComisionista()`, `IAsignacionComisionista` |
| `trazabilidad` | Auditoría | `AuditLogService` vía `IAuditLog` |

### Interfaces utilizadas

| Interfaz | Módulo proveedor | Método |
|---|---|---|
| `IGestionCuentas` | `autenticacion` | `asignarComisionista(inversionistaId, comisionistaId)` |
| `IAuditLog` | `trazabilidad` | `registrar(evento, correo, detalle)` |

---

## Criterios de verificación

### Escenarios de aceptación (Gherkin)

```gherkin
Funcionalidad: Asignación de comisionista a inversionista

  Antecedentes:
    Dado que "admin@test.com" tiene JWT válido con rol=ADMINISTRADOR y MFA completado
    Y inversionistaId=5 existe con rol=INVERSIONISTA
    Y comisionistaId=7 existe con rol=COMISIONISTA

  Escenario: Asignación exitosa
    Cuando se envía PUT /api/admin/inversionistas/5/comisionista/7
    Entonces el sistema responde 200 OK
    Y existe asignación activa entre inversionista=5 y comisionista=7
    Y se emite evento COMISIONISTA_ASIGNADO en auditoría

  Escenario: Inversionista no encontrado retorna 404
    Cuando se envía PUT /api/admin/inversionistas/999/comisionista/7
    Entonces el sistema responde 404 Not Found
```

---

## Definición de terminado

- [x] `PUT /api/admin/inversionistas/{id}/comisionista/{id}` crea registro en `asignacion_comisionista`.
- [x] Usuarios no encontrados retornan 404.
- [x] Sin JWT o rol incorrecto retorna 401/403.
- [x] Evento `COMISIONISTA_ASIGNADO` registrado en auditoría.
- [ ] Validación completa en entorno integrado con frontend.
- [ ] `docs/PROGRESO.md` marcado con ✅ para HU-37.

---

## Historial de cambios

| Versión | Fecha | Descripción | Razón |
|---|---|---|---|
| 1.0 | 2026-05-24 | Refactorización a estructura SDD del proyecto. | Unificación de todos los SPEC.md bajo plantilla canónica SDD del proyecto |
