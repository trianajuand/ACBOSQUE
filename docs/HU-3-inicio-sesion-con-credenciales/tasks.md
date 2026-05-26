# Tareas — HU-3: Inicio de sesión con credenciales y JWT

| Campo | Valor |
|---|---|
| Historia | HU-3 |
| Sprint | 1 |
| Estado | Completada |

---

## Tareas de backend

### Modelo y persistencia

- [x] Crear entidad `IntentoFallido` (`autenticacion/model/IntentoFallido.java`) con campos: `id`, `correo` (UNIQUE), `contador`, `ultimoIntento`, `bloqueadoHasta`.
- [x] Crear `IntentoFallidoRepository` con método `findByCorreo(String correo)`.
- [x] Verificar que Hibernate crea tabla `intento_fallido` con índice UNIQUE en `correo`.

### DTOs

- [x] Crear `LoginRequestDTO` con `correo` (`@NotBlank @Email`) y `contrasenia` (`@NotBlank`).
- [x] Crear `LoginResponseDTO` con `token`, `requiereMfa`, `mfaToken`, `rol`, `mensaje`.

### JwtUtil

- [x] Implementar `JwtUtil.generarToken(String correo, String rol)`: HMAC-SHA256, claims `sub=correo`, `rol`, TTL desde `app.jwt.expiracion-ms`.
- [x] Implementar `JwtUtil.generarTokenMfa(String correo)`: JWT con claim `tipo="MFA"`, TTL fijo de `10 * 60 * 1000L` ms.
- [x] Implementar `JwtUtil.esValido(String token)`: retorna false si expirado o firma inválida.
- [x] Implementar `JwtUtil.extraerCorreo(String token)` y `JwtUtil.extraerRol(String token)`.
- [x] Verificar que `app.jwt.secret` y `app.jwt.expiracion-ms` se leen desde `application.properties` con `@Value`.

### MonitorIntentosService

- [x] Implementar `verificarBloqueo(String correo)`: busca registro en `intento_fallido`; si `bloqueadoHasta != null && bloqueadoHasta.isAfter(now())`, lanza `AccountLockedException`.
- [x] Implementar `registrarIntentoFallido(String correo)`: crea o actualiza registro, incrementa `contador`; si `contador >= max-intentos`, establece `bloqueadoHasta = now().plusMinutes(bloqueoMinutos)`.
- [x] Implementar `seBloqueo(String correo)`: retorna true si el registro tiene `bloqueadoHasta` en el futuro.
- [x] Implementar `reiniciarIntentos(String correo)`: `contador = 0`, `bloqueadoHasta = null`, guardar.
- [x] Leer `app.seguridad.max-intentos` y `app.seguridad.bloqueo-minutos` con `@Value`.

### AutenticacionService

- [x] Implementar `iniciarSesion(LoginRequestDTO dto)` con la secuencia completa:
  - [x] Verificar bloqueo → lanzar `AccountLockedException` (423) si bloqueado.
  - [x] Buscar usuario por correo → lanzar `InvalidCredentialsException` (401) + registrar intento fallido si no existe.
  - [x] Verificar estado `BLOQUEADA` → lanzar `AccountLockedException` (423).
  - [x] Verificar estado `ACTIVA` o `OPERACIONES_RESTRINGIDAS` → lanzar excepción 403 con mensaje `"La cuenta aun no esta activa"` si ninguno.
  - [x] `passwordEncoder.matches` → si falla: registrar intento; si `seBloqueo()`: auditar `CUENTA_BLOQUEADA` + notificar bloqueo + lanzar `AccountLockedException`; si no: auditar `LOGIN_FALLIDO` + lanzar `InvalidCredentialsException`.
  - [x] Reiniciar intentos.
  - [x] Evaluar si requiere MFA: `mfaHabilitado == true` OR `rol IN (COMISIONISTA, ADMINISTRADOR, RESPONSABLE_LEGAL)`.
  - [x] Rama sin MFA: generar JWT, auditar `LOGIN_EXITOSO` con `"Login directo"`, responder con `LoginResponseDTO{token, requiereMfa: false, rol}`.
  - [x] Rama con MFA: `MFAService.generarYGuardarCodigo(correo, "MFA")`, `INotificacion.enviarCodigoMfa(correo, nombre, codigo)`, generar `mfaToken`, auditar `MFA_ENVIADO`, responder con `LoginResponseDTO{requiereMfa: true, mfaToken}`.
- [x] Implementar `AutenticacionService` con inyección por constructor (no `@Autowired` en campo).
- [x] Anotar `iniciarSesion` con `@Transactional` (por los writes en `intento_fallido`).

### Controlador y seguridad

- [x] Crear `AuthController.login(@Valid @RequestBody LoginRequestDTO)` → delega a `AutenticacionService.iniciarSesion`, retorna `ResponseEntity<LoginResponseDTO>`.
- [x] Configurar `POST /api/auth/login` como endpoint público en `SecurityConfig` (sin requerir JWT).
- [x] Implementar (o verificar) `JwtAuthenticationFilter extends OncePerRequestFilter`: extrae `Authorization: Bearer`, valida con `JwtUtil.esValido`, carga `SecurityContextHolder`.

### Manejo de errores

- [x] `AccountLockedException` mapeada a `423 Locked` en `GlobalExceptionHandler`.
- [x] `InvalidCredentialsException` (o excepción de credenciales) mapeada a `401 Unauthorized` con mensaje `"Credenciales inválidas"`.
- [x] Excepción de cuenta no activa mapeada a `403 Forbidden` con mensaje `"La cuenta aun no esta activa"`.
- [x] `MethodArgumentNotValidException` mapeada a `400 Bad Request`.

---

## Tareas de frontend

- [x] Crear `LoginComponent` (`frontend/src/app/auth/login.component.ts/.html`):
  - [x] Formulario reactivo con campos `correo` y `contrasenia`.
  - [x] Botón "Iniciar sesión": llama `POST /api/auth/login`.
  - [x] Si `requiereMfa: false`: guardar JWT en `localStorage` (`auth_token`), guardar `rol`, navegar a `/dashboard` (inversionista), `/comisionista` o `/admin` según rol.
  - [x] Si `requiereMfa: true`: guardar `mfaToken` en `sessionStorage`, navegar a `/mfa/verificar`.
  - [x] Error 401: mostrar `"Credenciales inválidas"` bajo el formulario.
  - [x] Error 403: mostrar `"La cuenta aún no está activa"`.
  - [x] Error 423: mostrar `"Cuenta bloqueada temporalmente. Intente de nuevo más tarde."`.
  - [x] Estado de carga: deshabilitar botón mientras se procesa.
- [x] Registrar ruta `/login` en `app.routes.ts` como ruta pública.

---

## Tareas de verificación

- [x] **Login exitoso sin MFA:** `POST /api/auth/login` con inversionista sin MFA → 200 con `token` no nulo, `requiereMfa: false`, `rol: "INVERSIONISTA"`.
- [x] **Login con MFA:** `POST /api/auth/login` con administrador → 200 con `requiereMfa: true`, `mfaToken` no nulo, `token: null`.
- [x] **Contraseña incorrecta:** → 401 con `"Credenciales inválidas"`.
- [x] **Usuario no encontrado:** → 401 con `"Credenciales inválidas"` (mismo mensaje que contraseña incorrecta).
- [x] **Cuenta no activa:** → 403 con `"La cuenta aun no esta activa"`.
- [x] **Bloqueo al quinto intento:** → 423 tras 5 intentos fallidos.
- [x] **Bloqueo persiste tras reinicio:** reiniciar backend con cuenta bloqueada → `POST /api/auth/login` → 423.
- [x] **Reinicio del contador:** login exitoso tras 3 intentos fallidos → verificar `contador = 0` en BD.
- [x] **JWT no contiene contraseña:** decodificar en `jwt.io` → solo `sub`, `rol`, `iat`, `exp`.
- [x] Verificar eventos en `logs/audit.log`: `LOGIN_EXITOSO`, `LOGIN_FALLIDO`, `CUENTA_BLOQUEADA`, `MFA_ENVIADO`.

---

## Deudas técnicas

- [ ] Hacer configurable el TTL del `mfaToken` con propiedad `app.jwt.mfa-expiracion-ms` en `application.properties` (actualmente hardcodeado como 10 min en `JwtUtil`).

---

## Actualización de documentación

- [x] Marcar HU-3 como `✅` en `docs/PROGRESO.md`.
