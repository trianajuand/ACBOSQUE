# Tareas — HU-5: Cierre de sesión con invalidación de JWT

| Campo | Valor |
|---|---|
| Historia | HU-5 |
| Sprint | 1 |
| Estado | **Pendiente — deuda técnica activa** |

> Las tareas marcadas con `[x]` están implementadas. Las marcadas con `[ ]` son el trabajo pendiente para cerrar la historia.

---

## Tareas ya implementadas

- [x] `POST /api/auth/logout` existe y responde 200 con `"Sesión cerrada exitosamente"`.
- [x] `POST /api/auth/logout` sin Authorization responde 401.
- [x] Evento `LOGOUT` registrado en `logs/audit.log` al llamar al endpoint.
- [x] Frontend elimina `auth_token` y `rol` de `localStorage` al hacer logout.
- [x] Frontend navega a `/login` tras logout exitoso o error.

---

## Tareas pendientes — Revocación de JWT

### Modelo y persistencia

- [ ] Crear entidad `TokenRevocado` (`autenticacion/model/TokenRevocado.java`) con campos:
  - `id` (BIGSERIAL PK)
  - `token` (TEXT, UNIQUE, NOT NULL)
  - `correo` (VARCHAR 255, NOT NULL)
  - `expiracion` (TIMESTAMP, NOT NULL)
  - `revocadoEn` (TIMESTAMP, NOT NULL, default `now()`)
- [ ] Crear `TokenRevocadoRepository` con métodos:
  - `boolean existsByToken(String token)`
  - `void deleteByExpiracionBefore(LocalDateTime fecha)`
- [ ] Verificar creación de tabla `token_revocado` en PostgreSQL con índice UNIQUE en `token` e índice en `expiracion`.

### JwtUtil (si no existe)

- [ ] Añadir método `JwtUtil.extraerExpiracion(String token)` que retorna `LocalDateTime` de la fecha de expiración del claim `exp`.

### AutenticacionService — actualizar cerrarSesion

- [ ] Modificar `AutenticacionService.cerrarSesion(String authHeader)`:
  - [ ] Extraer token raw del header (strip `"Bearer "`).
  - [ ] Extraer `correo` con `JwtUtil.extraerCorreo(token)`.
  - [ ] Extraer `expiracion` con `JwtUtil.extraerExpiracion(token)`.
  - [ ] Persistir en `token_revocado`: `{token, correo, expiracion, revocadoEn: now()}`.
  - [ ] Auditar `LOGOUT` con `"Sesión cerrada"`.
  - [ ] Responder `200 OK`.
- [ ] Añadir `@Transactional` al método `cerrarSesion` (por el write en `token_revocado`).

### JwtAuthenticationFilter — verificar revocación

- [ ] Modificar `JwtAuthenticationFilter.doFilterInternal`:
  - [ ] Tras validar la firma y expiración del token, consultar `tokenRevocadoRepository.existsByToken(token)`.
  - [ ] Si el token está en `token_revocado`: no configurar `SecurityContextHolder`; continuar la cadena sin autenticación (el endpoint protegido retornará 401 automáticamente).
  - [ ] Inyectar `TokenRevocadoRepository` en el filtro por constructor.

### Job de limpieza

- [ ] Crear método `@Scheduled(cron = "0 0 * * * *")` en `AutenticacionService` o clase dedicada:
  - [ ] Llama `tokenRevocadoRepository.deleteByExpiracionBefore(LocalDateTime.now())`.
  - [ ] Log de cuántos tokens eliminados (`log.info("Limpieza token_revocado: {} eliminados", count)`).
- [ ] Habilitar `@EnableScheduling` en `AccionesElBosqueApplication` o en `SecurityConfig` si no está habilitado.

---

## Tareas de verificación (cuando se implementen los pendientes)

- [ ] **Revocación efectiva:** login → logout → reusar el mismo JWT en `GET /api/perfil` → verificar `401 Unauthorized`.
- [ ] **Token en tabla:** tras logout, verificar `SELECT token FROM token_revocado WHERE correo = 'X'` retorna el JWT usado.
- [ ] **Frontend limpia sesión:** tras logout, verificar que `localStorage.getItem('auth_token')` es null en el navegador.
- [ ] **JWT expirado en logout:** `POST /api/auth/logout` con token ya expirado → verificar `401`.
- [ ] **Job de limpieza:** forzar expiración de tokens en BD → ejecutar job manualmente → verificar que la tabla queda vacía.
- [ ] Verificar tiempo de respuesta del logout ≤ 500 ms (medición Postman).

---

## Actualización de documentación

- [ ] Marcar HU-5 como `✅` en `docs/PROGRESO.md` **solo cuando todos los ítems pendientes estén completos**.
- [ ] Registrar en `docs/PROGRESO.md` la decisión de implementar revocación con tabla `token_revocado`.
