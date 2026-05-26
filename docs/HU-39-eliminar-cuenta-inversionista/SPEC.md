# SPEC — Baja lógica de inversionistas y comisionistas

---

## Ficha de la historia

| Campo | Valor |
|---|---|
| ID | HU-39 |
| Sprint | 4 |
| Prioridad MoSCoW | Should Have |
| Estado | En desarrollo |
| Épica | Administración / Gestión de Cuentas |
| CU asociado | CU-39 |
| Autor | Juan Diego Triana Mejia |
| Creada | 2026-05-20 |
| Última revisión | 2026-05-24 |
| Versión de esta spec | 1.0 |

> ⚠️ **Estado En desarrollo:** La funcionalidad está implementada pero aún en fase de pruebas y validación. El endpoint existe y realiza la baja lógica, pero los criterios de verificación completos no han sido validados en entorno integrado.

**Trazabilidad:**

| Tipo | ID | Descripción |
|---|---|---|
| Requerimiento funcional | RF-38 | Administrador elimina lógicamente una cuenta |
| Escenario de calidad | EC-12 | Trazabilidad del evento de eliminación de usuario |

---

## Historia de usuario

**Como** administrador autenticado,
**quiero** realizar la baja lógica de un inversionista o comisionista,
**para** que no pueda acceder al sistema sin perder el historial de sus operaciones.

---

## Actores y precondiciones

### Actores

| Actor | Rol en el sistema | Participación |
|---|---|---|
| Administrador autenticado | `ADMINISTRADOR` | Solicita la baja lógica |
| `AdministracionService` | Módulo `administracion` | Coordina la operación |
| `IGestionCuentas` | Módulo `autenticacion` | Marca el usuario como ELIMINADO |
| `AuditLogService` | Módulo `trazabilidad` | Registra la baja |

### Precondiciones

- JWT válido con rol `ADMINISTRADOR`.
- MFA completado.
- `usuarioId` existe y no tiene estado `ELIMINADA` ya.
- El usuario no tiene órdenes activas (EN_COLA, PENDIENTE, ENVIADA) — opcional según política.

---

## Flujo principal

1. Administrador selecciona un usuario y confirma la baja.
2. Frontend envía `DELETE /api/admin/usuarios/{usuarioId}` con JWT.
3. `AdministracionService.eliminarUsuario(usuarioId, adminCorreo)`:
   a. Valida rol de administrador.
   b. Delega a `IGestionCuentas.eliminarUsuario(usuarioId)`.
   c. Actualiza `usuario.estado_cuenta = 'INACTIVA'`. El registro permanece en BD (baja lógica). **Nota:** el enum `EstadoCuenta` real usa `INACTIVA` para baja lógica; no existe el valor `ELIMINADA` en el enum.
4. `IAuditLog.registrar(USUARIO_ADMIN_GESTIONADO, correo_usuario, "Baja lógica por {adminCorreo}")`.
5. Responde `200 OK` con `RespuestaDTO{mensaje: "Cuenta eliminada exitosamente"}`.

---

## Flujos de error

### Error 1 — No autenticado o rol incorrecto

| Campo | Valor |
|---|---|
| HTTP | 401 / 403 |

### Error 2 — Usuario no encontrado

| Campo | Valor |
|---|---|
| HTTP | 404 Not Found |
| Cuerpo | `RespuestaDTO{error: "Usuario no encontrado"}` |

### Error 3 — Usuario ya dado de baja

| Campo | Valor |
|---|---|
| Condición | `usuario.estado_cuenta == 'INACTIVA'` (ya fue dado de baja) |
| HTTP | 409 Conflict |
| Cuerpo | `RespuestaDTO{error: "La cuenta ya fue eliminada"}` |
| Nota (auditoría 2026-05-25) | El código actual (`GestionCuentasService.eliminarUsuario`) NO valida si el usuario ya está INACTIVA; siempre aplica la baja. Esta validación de 409 es un comportamiento pendiente de implementar. |

---

## Contrato de API

### Endpoint — `DELETE /api/admin/usuarios/{usuarioId}`

```yaml
DELETE /api/admin/usuarios/{usuarioId}:
  summary: Realiza la baja lógica de un usuario (inversionista o comisionista)
  security:
    - bearerAuth: []  # Solo ADMINISTRADOR
  parameters:
    - name: usuarioId
      in: path
      required: true
      schema:
        type: integer
  responses:
    '200':
      description: Cuenta eliminada exitosamente (baja lógica)
    '403':
      description: No autorizado
    '404':
      description: Usuario no encontrado
    '409':
      description: Usuario ya fue eliminado
```

---

## Modelo de datos

Se usa el campo existente `estado_cuenta` en la tabla `usuario`:

```sql
-- Baja lógica: el registro no se borra físicamente
UPDATE usuario SET estado_cuenta = 'INACTIVA' WHERE id = ?;

-- Consecuencias:
-- Login: AuthService rechaza con 403 si estado_cuenta = 'INACTIVA'
-- Historial de órdenes: conservado y visible para auditoría
-- Asignación comisionista: desactivada si aplica
```

---

## Módulos y arquitectura

| Módulo | Rol | Componentes |
|---|---|---|
| `administracion` | Coordinador | `AdminController`, `AdministracionService` |
| `autenticacion` | Persistencia | `IGestionCuentas.eliminarUsuario()` |
| `trazabilidad` | Auditoría | `AuditLogService` vía `IAuditLog` |

### Interfaces utilizadas

| Interfaz | Módulo proveedor | Método |
|---|---|---|
| `IGestionCuentas` | `autenticacion` | `eliminarUsuario(usuarioId)` |
| `IAuditLog` | `trazabilidad` | `registrar(evento, correo, detalle)` |

---

## Criterios de verificación

### Escenarios de aceptación (Gherkin)

```gherkin
Funcionalidad: Baja lógica de usuarios

  Antecedentes:
    Dado que "admin@test.com" tiene JWT válido con rol=ADMINISTRADOR y MFA completado
    Y el usuario con id=5 existe con estado_cuenta=ACTIVA

  Escenario: Baja lógica exitosa
    Cuando se envía DELETE /api/admin/usuarios/5
    Entonces el sistema responde 200 OK
    Y usuario.estado_cuenta = 'INACTIVA'
    Y el registro aún existe en la tabla usuario
    Y se emite evento USUARIO_ADMIN_GESTIONADO en auditoría

  Escenario: Usuario ya eliminado retorna 409
    Dado que usuario id=5 ya tiene estado_cuenta=ELIMINADA
    Cuando se envía DELETE /api/admin/usuarios/5
    Entonces el sistema responde 409 Conflict

  Escenario: Usuario con cuenta eliminada no puede iniciar sesión
    Dado que usuario "ana@test.com" tiene estado_cuenta=ELIMINADA
    Cuando intenta iniciar sesión con credenciales correctas
    Entonces el sistema responde 403 Forbidden
```

---

## Definición de terminado

- [x] `DELETE /api/admin/usuarios/{id}` cambia `estado_cuenta = 'INACTIVA'` (baja lógica, no borrado físico).
- [x] Usuario no encontrado retorna 404.
- [x] Usuario ya eliminado retorna 409.
- [x] Sin JWT o rol incorrecto retorna 401/403.
- [x] Usuario con estado ELIMINADA no puede iniciar sesión.
- [x] Evento `USUARIO_ADMIN_GESTIONADO` registrado en auditoría.
- [ ] Validación completa en entorno integrado con frontend.
- [ ] `docs/PROGRESO.md` marcado con ✅ para HU-39.

---

## Historial de cambios

| Versión | Fecha | Descripción | Razón |
|---|---|---|---|
| 1.0 | 2026-05-24 | Refactorización a estructura SDD del proyecto. | Unificación de todos los SPEC.md bajo plantilla canónica SDD del proyecto |
