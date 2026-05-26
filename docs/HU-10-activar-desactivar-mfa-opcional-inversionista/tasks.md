# Tareas — HU-10: Activar/desactivar MFA opcional (Inversionista)

> Estado general: **COMPLETADA**

---

## Checklist de implementación

### Modelo de datos

- [x] **Tabla `usuario`** — campo añadido:
  - `mfa_habilitado BOOLEAN NOT NULL DEFAULT FALSE`

- [x] **Entidad `Usuario`** — campo Java `mfaHabilitado` con anotación JPA

### Backend

- [x] **Service** — `PerfilService.toggleMfa(String correo, boolean activar)`
  - Extrae el rol del usuario desde `SecurityContextHolder` o lo recibe del controller
  - Valida que rol sea `INVERSIONISTA`; si no, lanza `AccionNoPermitidaException` → 403
  - Carga `Usuario` por correo
  - Actualiza `usuario.mfaHabilitado = activar`
  - Persiste con `usuarioRepository.save`
  - Llama `IAuditLog.registrar(activar ? MFA_ACTIVADO : MFA_DESACTIVADO, correo, "MFA " + (activar ? "activado" : "desactivado"))`
  - Retorna `RespuestaDTO{mensaje: "MFA activado/desactivado exitosamente"}`

- [x] **Controller** — `PerfilController.toggleMfa(@RequestParam boolean activar)`
  - Verbo: `PUT /api/perfil/mfa`
  - Parámetro: `?activar=true` o `?activar=false` (required)
  - Extrae correo y rol de `Authentication`
  - Delega a service, retorna 200 OK con `RespuestaDTO`

- [x] **Manejo de errores** — `GlobalExceptionHandler` captura:
  - `AccionNoPermitidaException` → 403 con mensaje descriptivo
  - `MissingServletRequestParameterException` → 400 cuando falta `activar`

- [x] **Auditoría** — eventos `MFA_ACTIVADO` y `MFA_DESACTIVADO` emitidos diferenciadamente

### Integración con HU-3 (login)

- [x] `AutenticacionService.iniciarSesion` lee `usuario.mfaHabilitado` para determinar si requiere paso MFA
- [x] `LoginResponseDTO` contiene `requiereMfa: true/false` según el estado del campo

### Frontend (dashboard.html / Angular)

- [x] Toggle switch en sección "Seguridad" del perfil, visible solo para rol `INVERSIONISTA`
- [x] Llamada `PUT /api/perfil/mfa?activar={true|false}` con JWT
- [x] Mensaje de confirmación tras 200 OK
- [x] Toggle inicializado con el valor actual de `mfaHabilitado` desde `GET /api/perfil`

### Documentación

- [x] `SPEC.md` creado/actualizado
- [x] `plan.md` creado
- [x] `tasks.md` creado (este archivo)
- [x] `docs/PROGRESO.md` marcado con ✅ para HU-10

---

## Criterios de aceptación verificados

| Criterio | Estado |
|---|:-:|
| `PUT /api/perfil/mfa?activar=true` actualiza `mfa_habilitado = true` y devuelve 200 | ✅ |
| `PUT /api/perfil/mfa?activar=false` actualiza `mfa_habilitado = false` y devuelve 200 | ✅ |
| Rol distinto a `INVERSIONISTA` recibe 403 con mensaje | ✅ |
| Sin JWT devuelve 401 | ✅ |
| Siguiente login con `mfa_habilitado = true` retorna `requiereMfa: true` | ✅ |
| Evento `MFA_ACTIVADO` / `MFA_DESACTIVADO` en audit.log | ✅ |

---

## Archivos modificados / creados

| Archivo | Tipo | Descripción |
|---|---|---|
| `autenticacion/model/Usuario.java` | Modificado | Campo `mfaHabilitado` añadido |
| `autenticacion/service/PerfilService.java` | Modificado | Método `toggleMfa` añadido |
| `autenticacion/controller/PerfilController.java` | Modificado | `PUT /api/perfil/mfa` añadido |
| `autenticacion/service/AutenticacionService.java` | Modificado | Login lee `mfaHabilitado` para decidir flujo MFA |
