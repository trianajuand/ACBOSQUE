# Historia de Usuario

## Título
Inicio de sesión con credenciales y JWT.

## Descripción
Como usuario registrado
Quiero iniciar sesión con correo y contraseña
Para acceder de forma segura al dashboard según mi estado y rol.

## Contexto
El login implementa RF-02 y RNF-08. Verifica estado de cuenta, bloqueo temporal, contraseña BCrypt y genera JWT firmado cuando no se requiere MFA.

## Flujo funcional
1. El usuario ingresa correo y contraseña en `/login`.
2. Angular envía `POST /api/auth/login`.
3. El backend verifica bloqueo temporal.
4. Busca el usuario por correo.
5. Rechaza cuentas no activas o bloqueadas.
6. Compara contraseña con BCrypt.
7. Si no requiere MFA, emite JWT con `sub` y `rol`.
8. El frontend guarda token en `localStorage` y navega a `/dashboard`.

## Reglas de negocio
- Solo cuentas `ACTIVA` pueden iniciar sesión.
- Cuentas `PENDIENTE_VERIFICACION` reciben 403.
- Cinco intentos fallidos bloquean temporalmente.
- Un login exitoso reinicia contador de intentos.

## Componentes involucrados
- `frontend/src/app/auth/login.component.ts`
- `frontend/src/app/auth/login.component.html`
- `frontend/src/app/core/api.service.ts`
- `backend/.../autenticacion/controller/AuthController.java`
- `backend/.../autenticacion/service/AutenticacionService.java`
- `backend/.../autenticacion/security/JwtUtil.java`
- `backend/.../autenticacion/security/JwtAuthenticationFilter.java`
- `backend/.../autenticacion/service/MonitorIntentosService.java`

## Backend
`AutenticacionService.iniciarSesion` centraliza reglas. `JwtUtil` firma tokens HMAC-SHA con expiración configurada. `SecurityConfig` permite login público.

## Frontend
`LoginComponent` usa `ReactiveForms`, muestra errores con `ToastService`, guarda token mediante `ApiService.setToken` y redirige al dashboard.

## Base de datos
Tabla `usuario` para credenciales, rol y estado. Tabla `intento_fallido` para contador y bloqueo.

## API / Endpoints
- `POST /api/auth/login`

## Validaciones
- DTO: correo obligatorio y formato email; contraseña obligatoria.
- Frontend: `Validators.required` y `Validators.email`.

## Seguridad
Contraseña nunca se retorna. JWT via `Authorization: Bearer`. Eventos `LOGIN_EXITOSO` y `LOGIN_FALLIDO` se auditan.

## Consideraciones técnicas
El token se guarda en `localStorage` por simplicidad académica. No existe lista de revocación efectiva en el filtro.

## Dependencias
Depende de registro, BCrypt, monitor de intentos y JWT.

## Criterios de aceptación
- [ ] Credenciales válidas generan JWT si no hay MFA.
- [ ] Credenciales inválidas retornan 401.
- [ ] Cuenta no activa retorna 403.
- [ ] Login exitoso permite navegar al dashboard.

## Notas
La respuesta puede ser JWT o requerimiento de MFA según rol/configuración del usuario.
