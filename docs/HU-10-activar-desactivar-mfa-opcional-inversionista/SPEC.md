# Historia de Usuario

## Título
Activación o desactivación de MFA opcional.

## Descripción
Como inversionista regular
Quiero activar o desactivar MFA en mi cuenta
Para controlar el nivel de seguridad adicional de mi login.

## Contexto
Implementa HU-10. El toggle está en el dashboard y modifica `mfa_habilitado` para usuarios con rol `INVERSIONISTA`.

## Flujo funcional
1. Usuario abre perfil/configuración.
2. Frontend lee `mfaHabilitado`.
3. Usuario activa o desactiva el toggle.
4. Angular envía `PUT /api/perfil/mfa?activar=true|false`.
5. Backend valida que el rol sea `INVERSIONISTA`.
6. Guarda el flag y audita el evento.
7. Próximo login exigirá MFA si quedó activo.

## Reglas de negocio
- Solo inversionistas regulares pueden modificar MFA opcional.
- Roles administrativos o comisionista tienen MFA obligatorio y no dependen del toggle.
- Cada cambio actualiza `fechaActualizacion`.

## Componentes involucrados
- `frontend/src/app/dashboard/dashboard.component.ts`
- `backend/.../autenticacion/controller/PerfilController.java`
- `backend/.../autenticacion/service/PerfilService.java`
- `backend/.../autenticacion/model/Rol.java`
- `backend/.../autenticacion/model/Usuario.java`

## Backend
`PerfilService.toggleMfa` valida rol, persiste el flag y registra `MFA_ACTIVADO` o `MFA_DESACTIVADO`.

## Frontend
`toggleMfa` invierte el valor actual del perfil, llama endpoint y recarga perfil.

## Base de datos
Tabla `usuario`: `mfa_habilitado`, `fecha_actualizacion`.

## API / Endpoints
- `PUT /api/perfil/mfa?activar={boolean}`

## Validaciones
- Usuario autenticado.
- Rol debe ser `INVERSIONISTA`; de lo contrario retorna 403.

## Seguridad
El endpoint está protegido por JWT. La decisión afecta el flujo posterior de `POST /api/auth/login`.

## Consideraciones técnicas
El control de MFA obligatorio vive en `AutenticacionService`, no en el perfil.

## Dependencias
Depende de login y MFA.

## Criterios de aceptación
- [ ] Inversionista puede activar MFA.
- [ ] Inversionista puede desactivar MFA.
- [ ] Roles no inversionistas reciben 403.
- [ ] El siguiente login respeta el nuevo estado.

## Notas
Un usuario premium activa MFA automáticamente al confirmar pago Stripe.
