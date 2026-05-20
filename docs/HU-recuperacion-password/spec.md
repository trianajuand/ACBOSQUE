# Historia de Usuario

## Título
Recuperación y restablecimiento de contraseña.

## Descripción
Como usuario registrado
Quiero recibir un código de recuperación y definir una nueva contraseña
Para recuperar acceso si olvidé mis credenciales.

## Contexto
El flujo se implementa con dos pantallas Angular y dos endpoints públicos. Usa `CodigoVerificacion` con tipo `RECUPERACION_PASSWORD`, TTL propio y un solo uso.

## Flujo funcional
1. Usuario abre `/recuperar`.
2. Envía correo con `POST /api/auth/forgot-password`.
3. Backend valida usuario, elimina tokens previos y genera token de 6 dígitos.
4. Se envía token por correo y se audita solicitud.
5. Frontend guarda `reset_correo` y navega a `/reset-password`.
6. Usuario ingresa token y nueva contraseña.
7. `POST /api/auth/reset-password` valida token, expiración y uso.
8. Backend marca token usado y guarda contraseña BCrypt nueva.

## Reglas de negocio
- Solo correos existentes pueden solicitar recuperación.
- El token anterior se reemplaza por uno nuevo.
- El token expira según `app.seguridad.recuperacion-expiracion-minutos`.
- La nueva contraseña debe tener mínimo 8 caracteres.

## Componentes involucrados
- `frontend/src/app/auth/recuperar.component.ts`
- `frontend/src/app/auth/reset-password.component.ts`
- `backend/.../autenticacion/controller/RecuperacionController.java`
- `backend/.../autenticacion/service/RecuperacionPasswordService.java`
- `backend/.../autenticacion/dto/RecuperarPasswordDTO.java`
- `backend/.../autenticacion/dto/ResetPasswordDTO.java`
- `backend/.../autenticacion/model/CodigoVerificacion.java`

## Backend
`RecuperacionPasswordService` maneja solicitud y reset dentro de transacciones. Usa `PasswordEncoder` para persistir la nueva contraseña.

## Frontend
El correo se captura en `/recuperar`; el código y nueva contraseña en `/reset-password`. Los mensajes usan `ToastService`.

## Base de datos
Tabla `codigo_verificacion` con tipo `RECUPERACION_PASSWORD`. Tabla `usuario` actualiza `contrasenia` y `fecha_actualizacion`.

## API / Endpoints
- `POST /api/auth/forgot-password`
- `POST /api/auth/reset-password`

## Validaciones
- Correo requerido y formato email.
- Token requerido.
- Nueva contraseña mínima de 8 caracteres.

## Seguridad
No se devuelve si el token existe más allá del mensaje de error controlado. El token queda invalidado con `usado=true`. Se auditan solicitud y reseteo.

## Consideraciones técnicas
Aunque la convención original menciona 64 hex para recuperación, el código real usa 6 dígitos numéricos.

## Dependencias
Depende de SMTP, `CodigoVerificacionRepository`, `UsuarioRepository` y BCrypt.

## Criterios de aceptación
- [ ] Usuario existente recibe código de recuperación.
- [ ] Token correcto permite cambiar contraseña.
- [ ] Token expirado, usado o incorrecto es rechazado.
- [ ] La nueva contraseña queda hasheada.

## Notas
El correo se mantiene temporalmente en `localStorage` bajo `reset_correo`.
