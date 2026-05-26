# Plan de implementación — HU-3: Inicio de sesión con credenciales y JWT

| Campo | Valor |
|---|---|
| Historia | HU-3 — Inicio de sesión con credenciales |
| Sprint | 1 |
| Estado | Completada |
| Módulo principal | `autenticacion` |
| Módulos de soporte | `integracion`, `trazabilidad` |

---

## Objetivo

Permitir que cualquier usuario registrado (inversionista, comisionista, administrador) inicie sesión con correo y contraseña. Si las credenciales son válidas, emitir un JWT de sesión completa para roles sin MFA requerido, o un `mfaToken` de corto plazo para roles con MFA obligatorio o inversionistas con MFA habilitado. Proteger la cuenta con bloqueo temporal tras 5 intentos fallidos (EC-09) y registrar trazabilidad de todos los eventos (EC-12).

---

## Estrategia general

1. **Verificación por BCrypt:** `passwordEncoder.matches(contrasenia, hashAlmacenado)`. Nunca comparar texto plano.
2. **Bloqueo por intentos:** `MonitorIntentosService` mantiene el contador en `intento_fallido`. Tras `max-intentos` (configurable), bloquea durante `bloqueo-minutos` (configurable). Bloqueo persiste en BD, no en RAM.
3. **JWT firmado:** `JwtUtil.generarToken(correo, rol)` con HMAC-SHA256, secret en `application.properties`, TTL desde `app.jwt.expiracion-ms`. Claims: `sub = correo`, `rol`, `iat`, `exp`.
4. **MFA token intermedio:** `JwtUtil.generarTokenMfa(correo)` con claim `tipo = "MFA"` y TTL fijo de 10 min. El flujo MFA se completa en HU-4.
5. **Mensaje de error genérico:** ambos casos de credenciales inválidas (usuario no encontrado y contraseña incorrecta) devuelven el mismo mensaje `"Credenciales inválidas"` para prevenir user enumeration (R3).
6. **Notificación de bloqueo:** al alcanzar el límite de intentos, se envía correo al usuario vía `INotificacion.notificarBloqueo` y se audita `CUENTA_BLOQUEADA`.

---

## Fases de implementación

### Fase 1 — Modelo y persistencia

- Crear tabla `intento_fallido` con columnas: `id`, `correo` (UNIQUE), `contador`, `ultimo_intento`, `bloqueado_hasta`.
- Crear entidad `IntentoFallido` y `IntentoFallidoRepository` con `findByCorreo(String correo)`.

### Fase 2 — Servicio de monitoreo de intentos

- `MonitorIntentosService.verificarBloqueo(correo)`: si existe registro con `bloqueado_hasta > now()`, lanza `AccountLockedException`.
- `MonitorIntentosService.registrarIntentoFallido(correo)`: crea o actualiza registro; incrementa `contador`; si `contador >= max-intentos`, establece `bloqueado_hasta = now() + bloqueo-minutos`.
- `MonitorIntentosService.seBloqueo(correo)`: retorna true si el último intento activó el bloqueo.
- `MonitorIntentosService.reiniciarIntentos(correo)`: establece `contador = 0`, `bloqueado_hasta = null`.

### Fase 3 — JwtUtil

- `JwtUtil.generarToken(correo, rol)`: JWT HMAC-SHA256 con claims `sub`, `rol`, `iat`, `exp` (TTL de `app.jwt.expiracion-ms`).
- `JwtUtil.generarTokenMfa(correo)`: JWT con claim adicional `tipo = "MFA"`, TTL fijo de 10 min.
- `JwtUtil.esValido(token)`: retorna true si el token no está expirado y la firma es válida.
- `JwtUtil.extraerCorreo(token)`, `JwtUtil.extraerRol(token)`.

### Fase 4 — Servicio de autenticación

- `AutenticacionService.iniciarSesion(LoginRequestDTO dto)`:
  1. `MonitorIntentosService.verificarBloqueo(correo)` → 423 si bloqueado.
  2. Buscar usuario por correo → 401 + registrar intento fallido si no existe.
  3. Verificar `estadoCuenta == BLOQUEADA` → 423.
  4. Verificar `estadoCuenta != ACTIVA && != OPERACIONES_RESTRINGIDAS` → 403.
  5. `passwordEncoder.matches` → si falla: registrar intento, notificar y auditar si se bloqueó, lanzar excepción 401.
  6. Reiniciar intentos.
  7. Evaluar MFA: `mfaHabilitado == true` OR `rol IN (COMISIONISTA, ADMINISTRADOR, RESPONSABLE_LEGAL)`.
  8. Sin MFA: generar JWT, auditar `LOGIN_EXITOSO`, responder 200 con `LoginResponseDTO{token, requiereMfa: false}`.
  9. Con MFA: `MFAService.generarYGuardarCodigo(correo, "MFA")`, enviar código, generar `mfaToken`, auditar `MFA_ENVIADO`, responder 200 con `LoginResponseDTO{requiereMfa: true, mfaToken}`.

### Fase 5 — Controlador y configuración de seguridad

- `AuthController.login(@Valid @RequestBody LoginRequestDTO)` → delega a `AutenticacionService.iniciarSesion`.
- Configurar `POST /api/auth/login` como público en `SecurityConfig`.
- Configurar `JwtAuthenticationFilter` para todos los endpoints protegidos.

### Fase 6 — Frontend

- `LoginComponent`: formulario de correo y contraseña; manejo de estados: enviando, éxito directo (navegar a `/dashboard`), MFA requerido (guardar `mfaToken` en `sessionStorage`, navegar a `/mfa/verificar`), error 401/403/423 con mensajes descriptivos.

### Fase 7 — Verificación

- Ejecutar todos los escenarios Gherkin del SPEC.
- Verificar que el JWT decodificado en `jwt.io` contiene solo `sub`, `rol`, `iat`, `exp` (sin contraseña).
- Verificar que el bloqueo persiste tras reinicio del servidor (almacenado en BD).

---

## Dependencias externas

| Dependencia | Requerida para | Estado |
|---|---|---|
| `app.jwt.secret` (256 bits) | Firma del JWT | Configurado |
| `app.jwt.expiracion-ms` | TTL del JWT | Configurado |
| `app.seguridad.max-intentos` | Límite de intentos | Configurado |
| `app.seguridad.bloqueo-minutos` | Duración del bloqueo | Configurado |
| `INotificacion` impl. | Notificación de bloqueo y código MFA | Disponible |
| `IAuditLog` impl. | Trazabilidad | Disponible |
| Tabla `intento_fallido` | Control de bloqueo | Creada |

---

## Decisiones de diseño clave

- **Mismo mensaje de error** para usuario no encontrado y contraseña incorrecta: previene user enumeration.
- **Bloqueo en BD, no en memoria:** `bloqueado_hasta` persiste reinicios del servidor.
- **`mfaToken` TTL hardcodeado (10 min):** deuda técnica — hacer configurable con `app.jwt.mfa-expiracion-ms` (pregunta abierta #1).
- **Claims mínimos en JWT:** `sub`, `rol`, `iat`, `exp`. No incluir datos sensibles.

---

## Riesgos principales

| Riesgo | Impacto | Mitigación |
|---|---|---|
| `app.jwt.secret` en texto plano en properties | Crítico en producción | Configurar como variable de entorno en despliegue |
| SMTP falla en notificación de bloqueo | Usuario bloqueado no recibe aviso | El bloqueo ocurre igual; notificación es informativa |
| TTL mfaToken hardcodeado | No configurable sin recompilar | Deuda técnica — `app.jwt.mfa-expiracion-ms` pendiente |
