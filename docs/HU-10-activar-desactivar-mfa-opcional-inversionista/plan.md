# Plan de implementación — HU-10: Activar/desactivar MFA opcional (Inversionista)

## Contexto

MFA es obligatorio para COMISIONISTA, ADMINISTRADOR y RESPONSABLE_LEGAL (forzado en HU-3). Para el rol INVERSIONISTA es opcional: el usuario decide activarlo o desactivarlo desde su perfil. Esta historia provee el toggle. El cambio es efectivo en el **próximo login** (la sesión actual no se ve afectada). Solo aplica a rol `INVERSIONISTA`; cualquier otro rol recibe 403.

---

## Estado

**Completada** — implementación en `PerfilController` + `PerfilService`. Campo `mfa_habilitado` en tabla `usuario`.

---

## Decisiones de diseño

| Decisión | Justificación |
|---|---|
| Solo `INVERSIONISTA` puede usar este endpoint | COMISIONISTA/ADMIN tienen MFA obligatorio por EC-10; no pueden desactivarlo. Verificación de rol en `PerfilService` (no solo en Spring Security) para respuesta semántica clara (403 con mensaje, no 403 genérico) |
| Parámetro `?activar=true/false` en query string, no en body | Operación booleana simple; no justifica un DTO. Patrón establecido en proyectos previos para toggles |
| Cambio efectivo en próximo login | Evitar invalidar la sesión actual del usuario (sería disruptivo). El JWT actual ya pasó la fase MFA; el cambio aplica a partir de la siguiente autenticación completa |
| `mfa_habilitado` en tabla `usuario`, no en `inversionista` | MFA es un atributo de seguridad de la autenticación (capa de `usuario`), no del perfil financiero. Así `AutenticacionService` puede leerlo directamente sin cruzar módulos |
| Auditoría distingue `MFA_ACTIVADO` vs `MFA_DESACTIVADO` | Permite detectar patrones sospechosos (e.g., desactivación antes de operaciones de alto valor) |

---

## Módulos involucrados

| Módulo | Componente | Rol |
|---|---|---|
| `autenticacion` | `PerfilController` | Recibe `PUT /api/perfil/mfa?activar={boolean}` |
| `autenticacion` | `PerfilService` | Valida rol, actualiza `mfaHabilitado`, persiste |
| `autenticacion` | `Usuario` (entidad) | Contiene `mfa_habilitado BOOLEAN NOT NULL DEFAULT FALSE` |
| `autenticacion` | `AutenticacionService` | Lee `mfaHabilitado` en HU-3 para decidir si pedir código |
| `trazabilidad` | `AuditLogService` (vía `IAuditLog`) | Registra `MFA_ACTIVADO` o `MFA_DESACTIVADO` |

---

## Flujo de implementación

```
PUT /api/perfil/mfa?activar={true|false}
  → JwtFilter valida token, SecurityContext contiene correo + rol
  → PerfilController.toggleMfa(@RequestParam boolean activar)
    → extrae correo y rol de Authentication
    → delega a PerfilService.toggleMfa(correo, activar)
      → if rol != INVERSIONISTA → throw AccionNoPermitidaException("Solo los inversionistas pueden gestionar MFA opcional") → 403
      → usuarioRepository.findByCorreo(correo)
      → usuario.mfaHabilitado = activar
      → usuarioRepository.save(usuario)
      → IAuditLog.registrar(activar ? MFA_ACTIVADO : MFA_DESACTIVADO, correo, "MFA " + (activar ? "activado" : "desactivado"))
      → return RespuestaDTO{mensaje: "MFA " + (activar ? "activado" : "desactivado") + " exitosamente"}
    → 200 OK con RespuestaDTO
```

---

## Interacción con HU-3 (login)

```
POST /api/auth/login (AutenticacionService.iniciarSesion)
  → credenciales OK
  → load usuario
  → if usuario.rol in {COMISIONISTA, ADMINISTRADOR, RESPONSABLE_LEGAL}
       OR (usuario.rol == INVERSIONISTA AND usuario.mfaHabilitado == true)
    → return LoginResponseDTO{requiereMfa: true, token: null}
  → else
    → generarJWT y return LoginResponseDTO{requiereMfa: false, token: jwt}
```

HU-10 controla el campo `mfaHabilitado`. HU-3 lo lee. Sin acoplamiento directo.

---

## Modelo de datos (tabla `usuario`)

| Columna | Tipo SQL | Default | Descripción |
|---|---|---|---|
| `mfa_habilitado` | `BOOLEAN NOT NULL` | `FALSE` | Si true, el inversionista necesita código MFA en cada login |

---

## Contrato resumido

| Verbo | URL | Auth | Parámetro | Respuesta exitosa |
|---|---|---|---|---|
| PUT | `/api/perfil/mfa` | Bearer JWT (INVERSIONISTA) | `?activar=true` o `?activar=false` | 200 `RespuestaDTO{mensaje}` |

**Códigos de error:**
- `400` — parámetro `activar` ausente o no booleano
- `401` — JWT ausente, inválido o expirado
- `403` — rol distinto a `INVERSIONISTA`
- `500` — error técnico genérico

---

## Escenarios de calidad cubiertos

| EC | Táctica | Materialización |
|---|---|---|
| EC-10 | Authenticate Actors | MFA activado → siguiente login del inversionista requiere código de 6 dígitos además de contraseña |
| EC-12 | Audit Trail | `MFA_ACTIVADO` o `MFA_DESACTIVADO` diferenciados en auditoría |

---

## Notas para el desarrollador

- **Inversionista premium y MFA:** cuando un inversionista activa el plan premium (HU-11), el sistema fuerza `mfa_habilitado = true`. En ese caso, el inversionista premium NO puede desactivar MFA con HU-10 (porque HU-10 verifica el rol pero no el estado premium). Esto es una limitación documentada en el SPEC; si se quiere corregir, se debe añadir validación `if (usuario.esPremium) → 403` al desactivar.
- La actualización de `mfa_habilitado` no invalida el JWT actual. El usuario sigue logueado normalmente hasta que expire o haga logout.
- El parámetro `activar` es `required = true` en Spring MVC. Si no se envía → 400 automático de Spring.
