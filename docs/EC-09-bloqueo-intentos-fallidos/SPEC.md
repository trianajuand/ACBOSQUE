# Historia de Usuario

## Título
Bloqueo temporal por intentos fallidos de login.

## Descripción
Como sistema de autenticación
Quiero bloquear temporalmente una cuenta tras varios intentos fallidos
Para mitigar ataques de fuerza bruta sobre credenciales.

## Contexto
Implementa RF-21, RNF-06 y EC-09. La lógica está separada en `MonitorIntentosService` y se invoca desde login.

## Flujo funcional
1. Usuario intenta iniciar sesión.
2. Antes de validar credenciales, backend consulta si el correo está bloqueado.
3. Si está bloqueado, retorna 423.
4. Si credenciales fallan, aumenta contador.
5. Al alcanzar `app.seguridad.max-intentos`, define `bloqueadoHasta`.
6. Si se bloquea un usuario existente, envía notificación de bloqueo.
7. Login exitoso reinicia contador y desbloqueo.

## Reglas de negocio
- Máximo de intentos configurable por `app.seguridad.max-intentos`.
- Duración configurable por `app.seguridad.bloqueo-minutos`.
- El contador se guarda por correo.
- Un login exitoso reinicia contador y `bloqueadoHasta`.

## Componentes involucrados
- `backend/.../autenticacion/service/MonitorIntentosService.java`
- `backend/.../autenticacion/service/AutenticacionService.java`
- `backend/.../autenticacion/model/IntentoFallido.java`
- `backend/.../autenticacion/repository/IntentoFallidoRepository.java`
- `backend/.../integracion/notificaciones/DespachadorNotificaciones.java`
- `backend/.../shared/exceptions/AccountLockedException.java`

## Backend
`MonitorIntentosService` controla estado de bloqueo y contador. `AutenticacionService` audita `LOGIN_FALLIDO` y `CUENTA_BLOQUEADA`, y usa el despachador para notificar.

## Frontend
`LoginComponent` muestra el error recibido por `ApiService` como toast.

## Base de datos
Tabla `intento_fallido`: `correo`, `contador`, `bloqueado_hasta`, `ultimo_intento`.

## API / Endpoints
- `POST /api/auth/login`

## Validaciones
- Correo requerido.
- Bloqueo se revisa antes de buscar usuario.

## Seguridad
Retorna HTTP 423 para cuenta bloqueada. Audita fallos de login. No expone contraseña ni detalles de hash.

## Consideraciones técnicas
El contador no implementa ventana móvil explícita; aumenta hasta reinicio o bloqueo.

## Dependencias
Depende de login, auditoría y correo de notificación.

## Criterios de aceptación
- [ ] Cinco intentos fallidos bloquean temporalmente.
- [ ] Cuenta bloqueada no puede iniciar sesión.
- [ ] Login exitoso reinicia contador.
- [ ] Se notifica al titular si existe usuario.

## Notas
La notificación solo se envía si se tiene entidad `Usuario`; correo inexistente se audita como fallo.
