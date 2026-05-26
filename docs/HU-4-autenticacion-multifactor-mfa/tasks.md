# Tareas — HU-4: Verificación de segundo factor (MFA)

| Campo | Valor |
|---|---|
| Historia | HU-4 |
| Sprint | 1 |
| Estado | Completada |

---

## Tareas de backend

### DTO

- [x] Verificar o crear `MFARequestDTO` (`autenticacion/dto/MFARequestDTO.java`) con campo `codigo` (`@NotBlank @Size(min=6, max=6)`).

### AutenticacionService — método verificarMfa

- [x] Implementar `verificarMfa(String mfaToken, String codigo)`:
  - [x] Llamar `JwtUtil.esValido(mfaToken)` → lanzar `InvalidTokenException` si false.
  - [x] Extraer `correo` del claim `sub` del `mfaToken` con `JwtUtil.extraerCorreo`.
  - [x] Llamar `MFAService.validarCodigo(correo, codigo, "MFA")`:
    - [x] Verifica código no expirado y no usado.
    - [x] Verifica coincidencia exacta.
    - [x] Marca `usado = true` si válido.
    - [x] Lanza `InvalidMfaException("Código no encontrado o ya utilizado.")` si no existe o ya usado.
    - [x] Lanza `InvalidMfaException("El código ha expirado.")` si expirado.
    - [x] Lanza `InvalidMfaException("Código incorrecto.")` si no coincide.
  - [x] Cargar `Usuario` por correo → obtener `rol`.
  - [x] Generar JWT de sesión completa con `JwtUtil.generarToken(correo, rol)`.
  - [x] Auditar `MFA_VERIFICADO` con detalle `"MFA validado"`.
  - [x] Auditar `LOGIN_EXITOSO` con detalle `"Login via MFA"`.
  - [x] Retornar `LoginResponseDTO{token: jwt, requiereMfa: false, mfaToken: null, rol: rol}`.

### Controlador

- [x] Implementar `AuthController.verificarMfa(@RequestHeader("Authorization") String authHeader, @Valid @RequestBody MFARequestDTO dto)`:
  - [x] Extraer token raw del header (formato `"Bearer <token>"`).
  - [x] Delegar a `AutenticacionService.verificarMfa(token, dto.getCodigo())`.
  - [x] Responder `200 OK` con `LoginResponseDTO`.
- [x] Configurar `POST /api/auth/mfa/verify` como endpoint público en `SecurityConfig` (no requiere JWT del filtro estándar).

### Manejo de errores (ya presentes, verificar cobertura)

- [x] `InvalidTokenException` mapeada a `400 Bad Request` con mensaje `"Token inválido o expirado"`.
- [x] `InvalidMfaException` mapeada a `401 Unauthorized`.
- [x] `MethodArgumentNotValidException` mapeada a `400 Bad Request` (campo `codigo` ausente).

---

## Tareas de frontend

- [x] Crear o verificar `MfaComponent` (`frontend/src/app/auth/mfa.component.ts/.html`):
  - [x] Al cargar: recuperar `mfaToken` de `sessionStorage`; si no existe, navegar a `/login`.
  - [x] Campo de código de 6 dígitos con validación de longitud.
  - [x] Botón "Verificar": llama `POST /api/auth/mfa/verify` con `Authorization: Bearer <mfaToken>` y `{codigo}`.
  - [x] Si 200 OK: guardar JWT en `localStorage` (`auth_token`), guardar `rol`, eliminar `mfaToken` de `sessionStorage`, navegar a ruta según `rol` (`/dashboard`, `/comisionista`, `/admin`).
  - [x] Si 401: mostrar mensaje de error del backend bajo el campo código.
  - [x] Si 400 (mfaToken expirado): mostrar `"Tu sesión temporal expiró. Inicia sesión de nuevo."`, navegar a `/login`.
  - [x] Estado de carga: deshabilitar botón mientras se procesa.
- [x] Registrar ruta `/mfa/verificar` en `app.routes.ts` como ruta pública.

---

## Tareas de verificación

- [x] **Verificación exitosa (administrador):** `POST /api/auth/mfa/verify` con código válido y mfaToken válido → 200 con `token` no nulo, `requiereMfa: false`, `rol: "ADMINISTRADOR"`.
- [x] **Código incorrecto:** → 401 con `"Código incorrecto."`.
- [x] **Código expirado:** → 401 con `"El código ha expirado."`.
- [x] **mfaToken expirado (> 10 min desde login):** → 400 con `"Token inválido o expirado"`.
- [x] **Código ya utilizado:** intentar usar el mismo código dos veces → 401 con `"Código no encontrado o ya utilizado."` en el segundo intento.
- [x] **Sin cabecera Authorization:** → 401.
- [x] **Campo codigo ausente:** → 400 con mensaje de validación.
- [x] Verificar que `codigo_verificacion` queda con `usado = true` tras verificación exitosa: `SELECT usado FROM codigo_verificacion WHERE tipo = 'MFA' AND correo = 'X'`.
- [x] Verificar eventos en `logs/audit.log`: `MFA_VERIFICADO` seguido de `LOGIN_EXITOSO`.
- [x] Verificar que el JWT de sesión completa contiene `sub = correo`, `rol`, `iat`, `exp` (decodificar en `jwt.io`).
- [x] Flujo completo end-to-end: login admin → navegar a `/mfa/verificar` → ingresar código → navegar a `/admin`.

---

## Deudas técnicas

- [ ] Implementar límite de intentos en `/mfa/verify` (bloqueo o invalidación del `mfaToken` tras N intentos fallidos). Prioridad: post-MVP.

---

## Actualización de documentación

- [x] Marcar HU-4 como `✅` en `docs/PROGRESO.md`.
