# Plan de implementación — HU-4: Verificación de segundo factor (MFA)

| Campo | Valor |
|---|---|
| Historia | HU-4 — Autenticación multifactor (MFA) |
| Sprint | 1 |
| Estado | Completada |
| Módulo principal | `autenticacion` |
| Módulos de soporte | `trazabilidad` |

---

## Objetivo

Completar el flujo de login para usuarios que requieren MFA (roles privilegiados: COMISIONISTA, ADMINISTRADOR, RESPONSABLE_LEGAL — siempre; INVERSIONISTA — si `mfa_habilitado = true`). El usuario envía el código de 6 dígitos junto con el `mfaToken` recibido en HU-3. El sistema valida el token intermedio, verifica el código en BD y emite el JWT de sesión completa.

---

## Estrategia general

1. **Dos tokens distintos:**
   - `mfaToken`: JWT de corto plazo (10 min, claim `tipo = "MFA"`) emitido en HU-3. Autentica el paso intermedio del login.
   - JWT de sesión: JWT completo (1h, claims `sub + rol`) emitido al completar MFA exitosamente en HU-4.
2. **Validación manual del mfaToken:** el endpoint `/mfa/verify` está en la lista pública de `SecurityConfig`. El `mfaToken` se valida manualmente en `AutenticacionService.verificarMfa` (no pasa por el filtro JWT estándar).
3. **Código de 6 dígitos en BD:** consumido desde `codigo_verificacion` con `tipo = "MFA"`. Creado en HU-3 al generar el `mfaToken`. Se marca `usado = true` al validar exitosamente.
4. **Sin reintentos ilimitados:** el `mfaToken` expira en 10 min, acotando la ventana de ataque. No hay bloqueo específico por intentos en `/mfa/verify` (deuda técnica documentada).
5. **Auditoría doble:** se registran `MFA_VERIFICADO` y `LOGIN_EXITOSO` en secuencia al completar el flujo.

---

## Fases de implementación

### Fase 1 — DTO

- Verificar o crear `MFARequestDTO` con `codigo` (`@NotBlank @Size(min=6, max=6)`).

### Fase 2 — Servicio

- `AutenticacionService.verificarMfa(String mfaToken, String codigo)`:
  1. `JwtUtil.esValido(mfaToken)` → lanzar `InvalidTokenException` si inválido o expirado → 400.
  2. Extraer `correo` del claim `sub` del `mfaToken`.
  3. `MFAService.validarCodigo(correo, codigo, "MFA")`:
     - Buscar código no usado y no expirado.
     - Si no existe: lanzar `InvalidMfaException("Código no encontrado o ya utilizado.")` → 401.
     - Si expirado: lanzar `InvalidMfaException("El código ha expirado.")` → 401.
     - Si no coincide: lanzar `InvalidMfaException("Código incorrecto.")` → 401.
     - Si válido: marcar `usado = true`.
  4. Cargar `Usuario` por correo → obtener `rol`.
  5. `JwtUtil.generarToken(correo, rol)` → JWT de sesión completa.
  6. Auditar `MFA_VERIFICADO` con `"MFA validado"`.
  7. Auditar `LOGIN_EXITOSO` con `"Login via MFA"`.
  8. Retornar `LoginResponseDTO{token: jwt, requiereMfa: false, mfaToken: null, rol: rol}`.

### Fase 3 — Controlador

- `AuthController.verificarMfa(@RequestHeader("Authorization") String authHeader, @Valid @RequestBody MFARequestDTO dto)`:
  - Extraer el token raw del header `Authorization: Bearer <mfaToken>`.
  - Delegar a `AutenticacionService.verificarMfa(mfaToken, dto.getCodigo())`.
  - Responder `200 OK` con `LoginResponseDTO`.
- Configurar `POST /api/auth/mfa/verify` como público en `SecurityConfig`.

### Fase 4 — Frontend

- Crear o verificar `MfaComponent` en `/mfa/verificar`:
  - Recuperar `mfaToken` de `sessionStorage`.
  - Campo de código de 6 dígitos; botón "Verificar".
  - `POST /api/auth/mfa/verify` con `Authorization: Bearer <mfaToken>` y body `{codigo}`.
  - Si 200: guardar JWT en `localStorage`, eliminar `mfaToken` de `sessionStorage`, navegar según `rol`.
  - Si 401: mostrar mensaje de error bajo el campo.
  - Si 400 (mfaToken expirado): mostrar `"Tu sesión temporal expiró. Inicia sesión de nuevo."`, navegar a `/login`.

### Fase 5 — Verificación

- Ejecutar escenarios Gherkin del SPEC: verificación exitosa, código incorrecto, expirado, mfaToken expirado, código ya usado, sin Authorization.
- Verificar que `codigo_verificacion` queda con `usado = true` tras verificación exitosa.
- Verificar eventos `MFA_VERIFICADO` y `LOGIN_EXITOSO` en `logs/audit.log`.

---

## Dependencias externas

| Dependencia | Requerida para | Estado |
|---|---|---|
| `mfaToken` generado por HU-3 | Autenticación del paso intermedio | Disponible |
| Tabla `codigo_verificacion` (HU-1) | Código MFA a validar | Disponible |
| `JwtUtil` (HU-3) | Validar `mfaToken` y generar JWT final | Disponible |
| `IAuditLog` impl. | Trazabilidad | Disponible |

---

## Decisiones de diseño clave

- **`mfaToken` validado manualmente:** el endpoint es público en Spring Security; la autenticación del paso intermedio se hace en el servicio, no en el filtro JWT global.
- **Sin bloqueo en MFA:** `mfaToken` expira en 10 min. No hay bloqueo de cuenta por intentos fallidos en este endpoint (riesgo documentado R2).
- **Código de uso único:** `usado = true` al primer intento válido; evita reutilización del mismo código.

---

## Riesgos principales

| Riesgo | Impacto | Mitigación |
|---|---|---|
| Código expirado → usuario debe reiniciar login completo | UX degradada | TTL configurable; mostrar indicador de tiempo en UI |
| Sin límite de intentos en `/mfa/verify` | Brute force sobre código 6 dígitos | `mfaToken` expira en 10 min; implementar límite post-MVP |
