# Plan de implementación — HU-5: Cierre de sesión con invalidación de JWT

| Campo | Valor |
|---|---|
| Historia | HU-5 — Cierre de sesión |
| Sprint | 1 |
| Estado | **Pendiente (deuda técnica activa)** |
| Módulo principal | `autenticacion` |
| Módulos de soporte | `trazabilidad` |

---

## Objetivo

Permitir que un usuario autenticado cierre sesión de forma segura, invalidando el JWT activo de modo que cualquier solicitud posterior con ese token sea rechazada con 401, incluso si el token no ha expirado naturalmente.

---

## Estado actual (implementado parcialmente)

El endpoint `POST /api/auth/logout` existe y funciona pero **no revoca el JWT**:

- Extrae el correo del Bearer token.
- Audita el evento `LOGOUT`.
- Responde `200 OK` con `"Sesión cerrada exitosamente"`.
- **El JWT sigue siendo válido hasta su expiración (1h).** Un atacante que tenga el token puede seguir usándolo.

El frontend elimina el JWT de `localStorage` correctamente, pero la seguridad real depende de la revocación en BD, que está pendiente.

---

## Estrategia general (objetivo completo)

1. **Tabla `token_revocado`:** almacena los tokens invalidados con su fecha de expiración. El filtro JWT la consulta en cada request autenticado.
2. **Verificación en `JwtAuthenticationFilter`:** antes de aceptar cualquier token, consultar si está en `token_revocado`. Si está presente → rechazar con 401.
3. **Limpieza periódica:** job `@Scheduled` que borra tokens con `expiracion < now()` para evitar crecimiento indefinido de la tabla.
4. **Frontend independiente del backend:** el frontend siempre elimina el JWT de `localStorage` tras el logout, independientemente de la respuesta HTTP.

---

## Fases de implementación (trabajo pendiente)

### Fase 1 — Modelo y persistencia (PENDIENTE)

- Crear entidad `TokenRevocado` con campos: `id`, `token` (UNIQUE TEXT), `correo`, `expiracion`, `revocadoEn`.
- Crear `TokenRevocadoRepository` con `existsByToken(String token)` y `deleteByExpiracionBefore(LocalDateTime fecha)`.
- Verificar que Hibernate crea tabla `token_revocado` con índices en `token` (UNIQUE) y `expiracion`.

### Fase 2 — Servicio de logout (PENDIENTE)

- Modificar `AutenticacionService.cerrarSesion(String authHeader)`:
  1. Extraer el token raw de `authHeader` (strip `"Bearer "`).
  2. Extraer `correo` con `JwtUtil.extraerCorreo(token)`.
  3. Extraer `expiracion` con `JwtUtil.extraerExpiracion(token)`.
  4. Persistir en `token_revocado`: `{token, correo, expiracion}`.
  5. Auditar `LOGOUT` con `"Sesión cerrada"`.
  6. Responder `200 OK`.

### Fase 3 — Filtro JWT actualizado (PENDIENTE)

- Modificar `JwtAuthenticationFilter.doFilterInternal`:
  - Después de validar la firma y expiración del token, consultar `tokenRevocadoRepository.existsByToken(token)`.
  - Si existe (token revocado) → continuar la cadena de filtros sin autenticar (el endpoint protegido rechazará con 401).
  - Documentar: esta consulta añade latencia lineal con el tamaño de `token_revocado`; el índice en `token` es crítico.

### Fase 4 — Job de limpieza (PENDIENTE)

- Crear `@Scheduled` job en `AutenticacionService` o clase dedicada:
  - `@Scheduled(cron = "0 0 * * * *")` (cada hora).
  - Llama `tokenRevocadoRepository.deleteByExpiracionBefore(LocalDateTime.now())`.

### Fase 5 — Verificación (PENDIENTE)

- Ejecutar: login → logout → reusar el JWT → verificar `401 Unauthorized`.
- Verificar que `token_revocado` contiene el token tras el logout.
- Verificar que el job de limpieza elimina tokens expirados.
- Verificar evento `LOGOUT` en `logs/audit.log`.

---

## Fases ya implementadas (no tocar)

- `AuthController.cerrarSesion` con endpoint `POST /api/auth/logout` ya funciona (sin revocación).
- Frontend: elimina `auth_token` y `rol` de `localStorage`, navega a `/login`.
- Auditoría `LOGOUT` ya se emite.

---

## Dependencias

| Dependencia | Requerida para | Estado |
|---|---|---|
| JWT válido de HU-3 | Token a revocar | Disponible |
| `JwtUtil.extraerExpiracion` | TTL del token para tabla | Puede requerir método nuevo en `JwtUtil` |
| Tabla `token_revocado` | Almacenamiento | **PENDIENTE — no creada** |
| `TokenRevocadoRepository` | Persistencia y consulta | **PENDIENTE — no creado** |

---

## Decisiones de diseño

- **`token TEXT`** (no `VARCHAR`): los JWT son strings largos variables; `TEXT` es más seguro.
- **Índice en `token`:** el filtro JWT consulta esta columna en cada request autenticado; el índice es crítico para rendimiento.
- **Índice en `expiracion`:** el job de limpieza necesita filtrar por expiración eficientemente.
- **Frontend limpia localStorage independientemente:** garantiza UX consistente aunque el backend falle.

---

## Riesgos principales

| Riesgo | Impacto actual | Mitigación pendiente |
|---|---|---|
| JWT no revocado tras logout | **Crítico** — token válido 1h post-logout | Implementar `token_revocado` (trabajo pendiente) |
| Tabla crece indefinidamente | Bajo (futuro) | Job de limpieza periódica |
| Latencia por consulta en cada request | Bajo | Índice en `token`; tabla pequeña tras limpieza |
