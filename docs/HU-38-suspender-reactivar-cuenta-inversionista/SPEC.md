# SPEC — Suspensión, reactivación y restricción operativa de cuentas

---

## Ficha de la historia

| Campo | Valor |
|---|---|
| ID | HU-38 |
| Sprint | 4 |
| Prioridad MoSCoW | Should Have |
| Estado | En desarrollo |
| Épica | Administración / Gestión de Cuentas |
| CU asociado | CU-38 |
| Autor | Juan Diego Triana Mejia |
| Creada | 2026-05-20 |
| Última revisión | 2026-05-24 |
| Versión de esta spec | 1.0 |

> ⚠️ **Estado En desarrollo:** La funcionalidad está implementada pero aún en fase de pruebas y validación. El endpoint existe y cambia el estado, pero los criterios de verificación completos no han sido validados en entorno integrado.

**Trazabilidad:**

| Tipo | ID | Descripción |
|---|---|---|
| Requerimiento funcional | RF-37 | Administrador suspende o reactiva cuentas de inversionistas |
| Escenario de calidad | EC-12 | Trazabilidad de cambios de estado de cuenta |

---

## Historia de usuario

**Como** administrador autenticado,
**quiero** suspender o reactivar la cuenta de un inversionista o comisionista,
**para** gestionar el ciclo de vida de las cuentas sin eliminarlas permanentemente.

---

## Actores y precondiciones

### Actores

| Actor | Rol en el sistema | Participación |
|---|---|---|
| Administrador autenticado | `ADMINISTRADOR` | Cambia el estado de la cuenta |
| `AdministracionService` | Módulo `administracion` | Coordina la operación |
| `IGestionCuentas` | Módulo `autenticacion` | Actualiza `estado_cuenta` en `usuario` |
| `AuditLogService` | Módulo `trazabilidad` | Registra el cambio de estado |

### Precondiciones

- JWT válido con rol `ADMINISTRADOR`.
- MFA completado.
- `usuarioId` existe en la tabla `usuario`.
- El nuevo estado es diferente al estado actual.

### Comportamiento sobre órdenes activas al suspender (auditoría 2026-05-25)

`GestionCuentasService.cambiarEstadoUsuario()` **solo cambia `estado_cuenta`**; no cancela ni modifica órdenes pendientes/EN_COLA. Sin embargo, `ColaOrdenesService` verifica `EstadoCuenta.ACTIVA` antes de procesar cada orden encolada: si el usuario está INACTIVA u otro estado no-ACTIVA, la orden se cancela automáticamente en el siguiente ciclo del scheduler y se liberan los fondos reservados. Las órdenes `PENDIENTE` o `ENVIADA` directas (no en cola) no son canceladas automáticamente al suspender.

**Pregunta abierta PA-1 (decisión de negocio):** ¿Debe la suspensión cancelar inmediatamente también las órdenes PENDIENTE y ENVIADA del usuario? El código actual no lo hace.

---

## Flujo principal

1. Administrador selecciona un usuario y el nuevo estado.
2. Frontend envía `PUT /api/admin/usuarios/{usuarioId}/estado` con JWT y `CambiarEstadoCuentaDTO`.
3. `AdministracionService.cambiarEstadoUsuario(usuarioId, dto, adminCorreo)`:
   a. Valida rol de administrador.
   b. Delega a `IGestionCuentas.cambiarEstadoUsuario(usuarioId, nuevoEstado)`.
   c. Actualiza `usuario.estado_cuenta` al nuevo estado.
4. `IAuditLog.registrar(CAMBIO_ESTADO_CUENTA, correo_usuario, "Estado cambiado a {nuevoEstado} por {adminCorreo}")`.
5. Responde `200 OK` con `RespuestaDTO{mensaje: "Estado actualizado exitosamente"}`.

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

### Error 3 — Estado inválido

| Campo | Valor |
|---|---|
| Condición | `nuevoEstado` no es un valor válido del enum `EstadoCuenta` |
| HTTP | 400 Bad Request |

---

## Contrato de API

### Endpoint — `PUT /api/admin/usuarios/{usuarioId}/estado`

```yaml
PUT /api/admin/usuarios/{usuarioId}/estado:
  summary: Cambia el estado de la cuenta de un usuario
  security:
    - bearerAuth: []  # Solo ADMINISTRADOR
  parameters:
    - name: usuarioId
      in: path
      required: true
      schema:
        type: integer
  requestBody:
    required: true
    content:
      application/json:
        schema:
          $ref: '#/components/schemas/CambiarEstadoCuentaDTO'
        example:
          nuevoEstado: "INACTIVA"
          motivo: "Actividad sospechosa detectada"
  responses:
    '200':
      description: Estado actualizado exitosamente
    '400':
      description: Estado inválido
    '403':
      description: No autorizado
    '404':
      description: Usuario no encontrado

components:
  schemas:
    CambiarEstadoCuentaDTO:
      type: object
      required: [nuevoEstado]
      properties:
        nuevoEstado:
          type: string
          enum: [ACTIVA, INACTIVA, BLOQUEADA, OPERACIONES_RESTRINGIDAS, PENDIENTE_VERIFICACION]
          # Nota: estos son los valores reales del enum EstadoCuenta. Los valores SUSPENDIDA y RESTRINGIDA NO existen en el código.
        motivo:
          type: string
          nullable: true
          description: Razón del cambio de estado (opcional)
```

---

## Modelo de datos

Utiliza el campo `estado_cuenta` de la tabla `usuario`:

```sql
-- Valores reales del enum EstadoCuenta (auditado 2026-05-25):
-- 'ACTIVA'                    → cuenta operativa
-- 'INACTIVA'                  → baja lógica o suspensión por admin (HU-39)
-- 'BLOQUEADA'                 → bloqueada por intentos fallidos (EC-09)
-- 'OPERACIONES_RESTRINGIDAS'  → puede autenticarse pero no operar (equivale a la "restricción" conceptual)
-- 'PENDIENTE_VERIFICACION'    → pendiente de verificar correo (HU-1)
-- NOTA: Los valores 'SUSPENDIDA', 'RESTRINGIDA' y 'ELIMINADA' NO existen en el enum real.
```

---

## Módulos y arquitectura

| Módulo | Rol | Componentes |
|---|---|---|
| `administracion` | Coordinador | `AdminController`, `AdministracionService` |
| `autenticacion` | Persistencia | `IGestionCuentas.cambiarEstadoUsuario()` |
| `trazabilidad` | Auditoría | `AuditLogService` vía `IAuditLog` |

### Interfaces utilizadas

| Interfaz | Módulo proveedor | Método |
|---|---|---|
| `IGestionCuentas` | `autenticacion` | `cambiarEstadoUsuario(usuarioId, nuevoEstado)` |
| `IAuditLog` | `trazabilidad` | `registrar(evento, correo, detalle)` |

---

## Criterios de verificación

### Escenarios de aceptación (Gherkin)

```gherkin
Funcionalidad: Suspensión y reactivación de cuentas

  Antecedentes:
    Dado que "admin@test.com" tiene JWT válido con rol=ADMINISTRADOR y MFA completado
    Y el usuario con id=5 existe con estado_cuenta=ACTIVA

  Escenario: Suspensión exitosa
    Cuando se envía PUT /api/admin/usuarios/5/estado con nuevoEstado="INACTIVA"
    Entonces el sistema responde 200 OK
    Y usuario.estado_cuenta = 'INACTIVA'
    Y se emite evento CAMBIO_ESTADO_CUENTA en auditoría

  Escenario: Reactivación de cuenta suspendida
    Dado que usuario id=5 tiene estado_cuenta=INACTIVA
    Cuando se envía PUT /api/admin/usuarios/5/estado con nuevoEstado="ACTIVA"
    Entonces el sistema responde 200 OK
    Y usuario.estado_cuenta = 'ACTIVA'

  Escenario: Usuario no encontrado retorna 404
    Cuando se envía PUT /api/admin/usuarios/999/estado con nuevoEstado="INACTIVA"
    Entonces el sistema responde 404 Not Found
```

---

## Definición de terminado

- [x] `PUT /api/admin/usuarios/{id}/estado` actualiza `estado_cuenta` del usuario.
- [x] Usuario no encontrado retorna 404.
- [x] Estado inválido retorna 400.
- [x] Sin JWT o rol incorrecto retorna 401/403.
- [x] Evento `CAMBIO_ESTADO_CUENTA` registrado en auditoría.
- [ ] Validación completa en entorno integrado con frontend.
- [ ] `docs/PROGRESO.md` marcado con ✅ para HU-38.

---

## Historial de cambios

| Versión | Fecha | Descripción | Razón |
|---|---|---|---|
| 1.0 | 2026-05-24 | Refactorización a estructura SDD del proyecto. | Unificación de todos los SPEC.md bajo plantilla canónica SDD del proyecto |
