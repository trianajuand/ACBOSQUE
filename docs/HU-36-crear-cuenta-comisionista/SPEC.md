# SPEC — Creación de cuenta de comisionista por administrador

---

## Ficha de la historia

| Campo | Valor |
|---|---|
| ID | HU-36 |
| Sprint | 4 |
| Prioridad MoSCoW | Should Have |
| Estado | En desarrollo |
| Épica | Administración / Gestión de Cuentas |
| CU asociado | CU-36 |
| Autor | Juan Diego Triana Mejia |
| Creada | 2026-05-20 |
| Última revisión | 2026-05-24 |
| Versión de esta spec | 1.0 |

> ⚠️ **Estado En desarrollo:** La funcionalidad está implementada pero aún en fase de pruebas y validación. El endpoint existe y persiste la cuenta, pero los criterios de verificación completos no han sido validados en entorno integrado.

**Trazabilidad:**

| Tipo | ID | Descripción |
|---|---|---|
| Requerimiento funcional | RF-35 | Administrador crea cuentas de comisionista |
| Escenario de calidad | EC-12 | Trazabilidad del evento de creación de usuario |
| Historia que sigue | HU-37 | El comisionista creado puede ser asignado a inversionistas |

---

## Historia de usuario

**Como** administrador autenticado,
**quiero** crear una cuenta de comisionista en el sistema,
**para** que el comisionista pueda autenticarse y operar en nombre de sus clientes asignados.

---

## Actores y precondiciones

### Actores

| Actor | Rol en el sistema | Participación |
|---|---|---|
| Administrador autenticado | `ADMINISTRADOR` | Crea la cuenta del comisionista |
| `AdministracionService` | Módulo `administracion` | Coordina la operación |
| `IGestionCuentas` | Módulo `autenticacion` | Persiste el usuario con rol COMISIONISTA |
| `AuditLogService` | Módulo `trazabilidad` | Registra la creación |

### Precondiciones

- JWT válido con rol `ADMINISTRADOR`.
- MFA completado.
- El correo del comisionista no existe en la tabla `usuario`.

---

## Flujo principal

1. Administrador completa el formulario de nuevo comisionista.
2. Frontend envía `POST /api/admin/comisionistas` con JWT y `CrearComisionistaDTO`.
3. `AdministracionService.crearComisionista(dto, adminCorreo)`:
   a. Valida rol de administrador vía `validarAdministrador(adminCorreo)`.
   b. Delega a `IGestionCuentas.crearComisionista(dto)`.
   c. `IGestionCuentas` verifica que el correo no esté registrado; si existe → 409.
   d. Crea `Usuario` con `rol = COMISIONISTA`, `mfa_habilitado = true` (obligatorio), `estado_cuenta = ACTIVA`.
   e. Contraseña hasheada con BCrypt.
4. `IAuditLog.registrar(USUARIO_CREADO, correo_comisionista, "Creado por {adminCorreo}")`.
5. Responde `201 Created` con `RespuestaDTO{mensaje: "Comisionista creado exitosamente"}`.

---

## Flujos de error

### Error 1 — No autenticado o rol incorrecto

| Campo | Valor |
|---|---|
| HTTP | 401 / 403 |

### Error 2 — Correo ya registrado

| Campo | Valor |
|---|---|
| Condición | El correo ya existe en `usuario` |
| HTTP | 409 Conflict |
| Cuerpo | `RespuestaDTO{error: "El correo ya está registrado"}` |

### Error 3 — Campos obligatorios ausentes

| Campo | Valor |
|---|---|
| HTTP | 400 Bad Request |

---

## Contrato de API

### Endpoint — `POST /api/admin/comisionistas`

```yaml
POST /api/admin/comisionistas:
  summary: Crea una nueva cuenta de comisionista
  security:
    - bearerAuth: []  # Solo ADMINISTRADOR
  requestBody:
    required: true
    content:
      application/json:
        schema:
          $ref: '#/components/schemas/CrearComisionistaDTO'
        example:
          correo: "comisionista@ejemplo.com"
          nombreCompleto: "Carlos López"
          contrasena: "Segura123!"
          telefono: "+573001234567"
  responses:
    '201':
      description: Comisionista creado exitosamente
    '400':
      description: Campos inválidos
    '403':
      description: No autorizado
    '409':
      description: Correo ya registrado

components:
  schemas:
    CrearComisionistaDTO:
      type: object
      required: [correo, nombreCompleto, contrasena]
      properties:
        correo:
          type: string
          format: email
        nombreCompleto:
          type: string
        contrasena:
          type: string
        telefono:
          type: string
          nullable: true
```

---

## Modelo de datos

Se reutiliza la tabla `usuario` con `rol = 'COMISIONISTA'` y `mfa_habilitado = true`. No se crean tablas adicionales (el comisionista no tiene perfil `inversionista` ni `suscripcion`).

```sql
-- El comisionista creado tendrá en tabla usuario:
-- rol = 'COMISIONISTA'
-- mfa_habilitado = TRUE  (obligatorio por reglas del sistema)
-- estado_cuenta = 'ACTIVA'
-- contrasena = BCrypt(contraseña_proporcionada)
-- (no hay registro en inversionista, suscripcion ni integracion_inversionista)
```

---

## Módulos y arquitectura

| Módulo | Rol | Componentes |
|---|---|---|
| `administracion` | Coordinador | `AdminController`, `AdministracionService` |
| `autenticacion` | Persistencia | `IGestionCuentas.crearComisionista()` |
| `trazabilidad` | Auditoría | `AuditLogService` vía `IAuditLog` |

### Interfaces utilizadas

| Interfaz | Módulo proveedor | Método |
|---|---|---|
| `IGestionCuentas` | `autenticacion` | `crearComisionista(CrearComisionistaDTO)` |
| `IAuditLog` | `trazabilidad` | `registrar(evento, correo, detalle)` |

### Escenarios de calidad y tácticas materializadas

| EC | Táctica | Cómo se materializa en HU-36 |
|---|---|---|
| EC-12 | Audit Trail | USUARIO_CREADO registrado con correo del nuevo comisionista y del admin |

---

## Criterios de verificación

### Escenarios de aceptación (Gherkin)

```gherkin
Funcionalidad: Creación de cuenta de comisionista

  Antecedentes:
    Dado que "admin@test.com" tiene JWT válido con rol=ADMINISTRADOR y MFA completado

  Escenario: Creación exitosa de comisionista
    Cuando se envía POST /api/admin/comisionistas con correo="comis@test.com" y datos válidos
    Entonces el sistema responde 201 Created
    Y el usuario "comis@test.com" existe con rol=COMISIONISTA y mfa_habilitado=true
    Y se emite evento USUARIO_CREADO en auditoría

  Escenario: Correo ya registrado retorna 409
    Dado que "comis@test.com" ya existe en el sistema
    Cuando se envía POST /api/admin/comisionistas con correo="comis@test.com"
    Entonces el sistema responde 409 Conflict

  Escenario: Sin JWT retorna 401
    Cuando se envía POST /api/admin/comisionistas sin Authorization
    Entonces el sistema responde 401 Unauthorized
```

---

## Definición de terminado

- [x] `POST /api/admin/comisionistas` crea usuario con `rol = COMISIONISTA` y `mfa_habilitado = true`.
- [x] Correo duplicado retorna 409.
- [x] Contraseña hasheada con BCrypt.
- [x] Sin JWT o rol incorrecto retorna 401/403.
- [x] Evento `USUARIO_CREADO` registrado en auditoría.
- [ ] Validación completa en entorno integrado con frontend.
- [ ] `docs/PROGRESO.md` marcado con ✅ para HU-36.

---

## Historial de cambios

| Versión | Fecha | Descripción | Razón |
|---|---|---|---|
| 1.0 | 2026-05-24 | Refactorización a estructura SDD del proyecto. | Unificación de todos los SPEC.md bajo plantilla canónica SDD del proyecto |
