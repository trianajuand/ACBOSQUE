# Historia de Usuario

## Título
Autenticación multifactor por correo.

## Descripción
Como usuario con MFA requerido o habilitado
Quiero validar un código temporal enviado a mi correo
Para completar el inicio de sesión con un segundo factor.

## Contexto
La MFA cubre RF-04 y EC-10. El sistema genera códigos de 6 dígitos persistidos en BD con TTL, no en memoria.

## Flujo funcional
1. Login determina si el usuario requiere MFA.
2. `MFAService` elimina códigos MFA previos y genera uno nuevo.
3. `DespachadorNotificaciones` envía el código por email.
4. Backend retorna `requiereMfa=true` y `mfaToken`.
5. El usuario ingresa el código en la misma pantalla.
6. Angular envía `POST /api/auth/mfa/verify`.
7. Backend valida token MFA, código, expiración y uso.
8. Se marca el código como usado y se emite JWT final.

## Reglas de negocio
- MFA obligatorio para `COMISIONISTA`, `ADMINISTRADOR` y `RESPONSABLE_LEGAL`.
- MFA opcional para inversionista regular si `mfaHabilitado=true`.
- Código MFA expira según `app.seguridad.codigo-expiracion-minutos`.
- Cada código es de un solo uso.

## Componentes involucrados
- `frontend/src/app/auth/login.component.ts`
- `backend/.../autenticacion/service/AutenticacionService.java`
- `backend/.../autenticacion/service/MFAService.java`
- `backend/.../autenticacion/model/CodigoVerificacion.java`
- `backend/.../integracion/notificaciones/DespachadorNotificaciones.java`
- `backend/.../autenticacion/security/JwtUtil.java`

## Backend
`JwtUtil.generarTokenMfa` emite token temporal con claim `tipo=MFA`. `MFAService.validarCodigo` valida existencia, expiración, igualdad y marca `usado=true`.

## Frontend
El formulario MFA aparece en `LoginComponent` cuando el backend responde `requiereMfa`. Al validar, guarda el JWT definitivo.

## Base de datos
Tabla `codigo_verificacion`: `correo`, `codigo`, `tipo=MFA`, `expiracion`, `usado`.

## API / Endpoints
- `POST /api/auth/login`
- `POST /api/auth/mfa/verify`

## Validaciones
- Código obligatorio de 6 caracteres.
- `mfaToken` obligatorio.
- Token MFA debe ser válido y no expirado.

## Seguridad
El código se envía por canal controlado, se invalida al usarse y queda auditado con `MFA_ENVIADO`, `MFA_VERIFICADO` y `LOGIN_EXITOSO`.

## Consideraciones técnicas
La implementación reutiliza `CodigoVerificacion` para registro, MFA y recuperación, discriminando por enum `TipoCodigo`.

## Dependencias
Depende de login, SMTP, JWT y repositorio de códigos.

## Criterios de aceptación
- [ ] Login con MFA retorna `mfaToken` sin JWT final.
- [ ] Código correcto entrega JWT final.
- [ ] Código incorrecto, usado o expirado es rechazado.
- [ ] El evento queda auditado.

## Notas
No hay reenvío de código MFA como endpoint separado.
