# SPEC — Cierre de sesión desde el dashboard

---

## Ficha de la historia

| Campo | Valor |
|---|---|
| ID | HU-5 |
| Sprint | 1 |
| Prioridad MoSCoW | Must Have |
| Estado | **Pendiente** |
| Épica | Autenticación / Acceso al sistema |
| CU asociado | CU-05 |
| Autor | Juan Diego Triana Mejia |
| Creada | 2026-05-20 |
| Última revisión | 2026-05-24 |
| Versión de esta spec | 1.0 |

> **⚠ Estado Pendiente:** La implementación actual de `cerrarSesion` solo registra el evento `LOGOUT` en auditoría pero **no revoca el JWT en BD**. La tabla `token_revocado` está referenciada en `CONVENCIONES.md §2.2` pero no ha sido creada. El JWT emitido en HU-3 sigue siendo válido hasta su expiración (`app.jwt.expiracion-ms`) incluso después del logout. Esta deuda técnica debe resolverse antes de marcar HU-5 como Completada.

**Trazabilidad:**

| Tipo | ID | Descripción |
|---|---|---|
| Requerimiento funcional | RF-04 | Cierre de sesión con invalidación del token |
| Escenario de calidad | EC-10 | Tokens revocados almacenados en BD para prevenir reutilización |
| Escenario de calidad | EC-12 | Trazabilidad de evento LOGOUT |
| Historia que precede a esta | HU-3 | Emite el JWT que debe ser revocado aquí |

---

## Historia de usuario

**Como** usuario autenticado,
**quiero** cerrar sesión desde el dashboard,
**para** que mi token quede invalidado y nadie pueda usarlo después de que yo salga.

---

## Motivación y contexto

### Por qué existe esta historia

Un JWT sin mecanismo de revocación es válido hasta su expiración (1 hora). Si el usuario cierra sesión, un atacante que haya obtenido el token podría seguir usándolo. La solución es persistir el token en una tabla `token_revocado` y verificar su presencia en el filtro JWT de Spring Security en cada request. Esta historia implementa el lado del servidor del cierre de sesión.

### Deuda técnica activa

La implementación actual **no revoca el JWT**. `AuthController.cerrarSesion` extrae el correo del Bearer token y llama a `AutenticacionService.cerrarSesion`, que solo audita `LOGOUT`. El filtro JWT no consulta `token_revocado`. El endpoint devuelve 200 pero el token sigue siendo válido.

**Trabajo pendiente:**
1. Crear tabla `token_revocado` en BD.
2. En `cerrarSesion`, extraer el token raw de la cabecera y persistirlo en `token_revocado` con su fecha de expiración.
3. En `JwtFilter` (o `JwtUtil`), verificar que el token no esté en `token_revocado` antes de autenticar cada request.
4. (Opcional) Job periódico para limpiar tokens expirados de `token_revocado`.

### Dependencias hacia atrás

| Componente | Qué provee | Sin esto... |
|---|---|---|
| JWT válido de HU-3 | Token a revocar | No hay nada que cerrar |
| Tabla `token_revocado` (pendiente) | Almacenamiento de tokens revocados | El logout no tiene efecto en seguridad |

---

## Actores y precondiciones

### Actores

| Actor | Rol en el sistema | Participación en este flujo |
|---|---|---|
| Usuario autenticado | Cualquier rol | Iniciador — presiona "Cerrar sesión" |
| `AutenticacionService` | Módulo `autenticacion` | Procesa el logout, persiste token revocado (pendiente), audita |
| `AuditLogService` | Módulo `trazabilidad` (vía `IAuditLog`) | Registra evento LOGOUT |

### Precondiciones

- El usuario tiene JWT válido (no expirado) en `localStorage`.
- El backend está operativo.

### Postcondiciones (estado objetivo — pendiente implementar)

- Token añadido a `token_revocado` en BD.
- Cualquier request con ese token recibe 401 Unauthorized.
- JWT eliminado del `localStorage` del frontend.
- Evento `LOGOUT` registrado en auditoría.

### Postcondiciones (estado actual — implementado parcialmente)

- Evento `LOGOUT` registrado en auditoría.
- JWT **no** revocado en BD (sigue siendo válido hasta expiración).
- Frontend elimina JWT de `localStorage`.

---

## Flujo principal (objetivo — con revocación)

1. Usuario presiona "Cerrar sesión" en el dashboard.
2. Frontend envía `POST /api/auth/logout` con cabecera `Authorization: Bearer <jwt>`.

**Backend — `AutenticacionService.cerrarSesion` (objetivo):**

3. Extrae correo del claim `sub` del JWT.
4. Extrae el token raw de la cabecera `Authorization`.
5. Extrae la fecha de expiración del token (`JwtUtil.extraerExpiracion`).
6. Persiste en `token_revocado`: `{token, correo, expiracion}`.
7. `IAuditLog.registrar(LOGOUT, correo, "Sesión cerrada")`.
8. Responde `200 OK` con `RespuestaDTO{mensaje: "Sesión cerrada exitosamente"}`.

**Frontend:**

9. Elimina JWT de `localStorage`, elimina `rol` de `localStorage`, navega a `/login`.

---

## Flujo actual (implementado — sin revocación)

Pasos 1–2 idénticos.

**Backend — `AutenticacionService.cerrarSesion` (actual):**

3. Extrae correo del Bearer token (sin persistir en `token_revocado`).
4. `IAuditLog.registrar(LOGOUT, correo, "Sesión cerrada")`.
5. Responde `200 OK` con `RespuestaDTO{mensaje: "Sesión cerrada exitosamente"}`.

> El JWT sigue siendo válido hasta su expiración.

---

## Flujos de error

### Error 1 — Cabecera Authorization ausente o inválida

| Campo | Valor |
|---|---|
| Condición | Request sin cabecera `Authorization: Bearer ...` o token malformado |
| Excepción Java | `AccessDeniedException` / filtro JWT rechaza |
| HTTP | 401 Unauthorized |
| Cuerpo | Respuesta estándar de Spring Security |
| Estado final | Sin cambios |
| Evento de auditoría | Ninguno |

### Error 2 — JWT expirado al momento del logout

| Campo | Valor |
|---|---|
| Condición | El usuario intenta logout con token ya expirado |
| HTTP | 401 Unauthorized (filtro JWT rechaza antes de llegar al controller) |
| Cuerpo | Respuesta estándar de Spring Security |
| Estado final | Sin cambios (token ya expirado, sin relevancia funcional) |
| Evento de auditoría | Ninguno |

### Error 3 — Error técnico al persistir token revocado (objetivo)

| Campo | Valor |
|---|---|
| Condición | Falla al escribir en `token_revocado` (BD no disponible, etc.) |
| HTTP | 500 Internal Server Error |
| Cuerpo | `RespuestaDTO{error: "Error interno del servidor"}` |
| Estado final | Token no revocado; auditoría puede o no haberse registrado |
| Evento de auditoría | Ninguno garantizado |

---

## Contrato de API

### Endpoint — `POST /api/auth/logout`

```yaml
POST /api/auth/logout:
  summary: Cierra la sesión del usuario autenticado
  security:
    - bearerAuth: []  # Requiere JWT de sesión válido
  requestBody:
    required: false  # Sin body — el token viene en la cabecera Authorization
  responses:
    '200':
      description: Sesión cerrada exitosamente
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'
          example:
            mensaje: "Sesión cerrada exitosamente"
    '401':
      description: Token ausente, inválido o expirado
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'
    '500':
      description: Error interno del servidor
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'
```

---

## Modelo de datos

### Tabla `token_revocado` (pendiente de crear)

```sql
CREATE TABLE token_revocado (
    id          BIGSERIAL PRIMARY KEY,
    token       TEXT         NOT NULL UNIQUE,
    correo      VARCHAR(255) NOT NULL,
    expiracion  TIMESTAMP    NOT NULL,
    revocado_en TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_token_revocado_token ON token_revocado (token);
CREATE INDEX idx_token_revocado_expiracion ON token_revocado (expiracion);
```

**Decisiones de esquema:**
- `token TEXT`: los JWT son strings de longitud variable (~200-400 chars con claims estándar); `TEXT` es más seguro que `VARCHAR(500)`.
- `expiracion`: permite un job de limpieza periódica (`DELETE FROM token_revocado WHERE expiracion < now()`) para evitar crecimiento indefinido.
- `idx_token_revocado_token`: el filtro JWT consultará esta tabla en cada request autenticado → índice crítico para rendimiento.

---

## Módulos y arquitectura

### Módulos involucrados

| Módulo | Rol | Componentes específicos |
|---|---|---|
| `autenticacion` | Coordinador del flujo | `AuthController`, `AutenticacionService`, `JwtUtil`, `TokenRevocadoRepository` (pendiente) |
| `trazabilidad` | Registro de eventos | `AuditLogService` (impl. de `IAuditLog`) |

### Interfaces consumidas en este flujo

| Interfaz | Módulo dueño | Métodos usados | Cuándo |
|---|---|---|---|
| `IAuditLog` | `trazabilidad` | `registrar(LOGOUT, correo, detalle)` | Al procesar el logout |

### Escenarios de calidad y tácticas materializadas

| EC | Táctica | Cómo se materializa en HU-5 |
|---|---|---|
| EC-10 | Authenticate Actors | Token revocado en BD previene reutilización post-logout (**pendiente implementar**) |
| EC-12 | Audit Trail | `LOGOUT` registrado en auditoría (implementado) |

---

## Eventos y efectos transversales

### Eventos de auditoría emitidos

| Evento (`TipoEvento`) | Cuándo se emite | Datos en `detalle` |
|---|---|---|
| `LOGOUT` | Logout procesado exitosamente | `"Sesión cerrada"` |

---

## Riesgos

| # | Riesgo | P | I | Mitigación | Test que lo cubre |
|---|---|:-:|:-:|---|---|
| R1 | JWT no revocado — token sigue válido hasta expiración tras logout | Alta (actual) | Alto | Implementar `token_revocado` (trabajo pendiente). TTL de 1h limita la ventana de riesgo | Manual: logout y reusar JWT en request a endpoint protegido |
| R2 | Tabla `token_revocado` crece indefinidamente sin job de limpieza | Media (futuro) | Bajo | Añadir `@Scheduled` job que elimine tokens con `expiracion < now()` | No hay test |
| R3 | El filtro JWT añadirá latencia al consultar `token_revocado` en cada request | Baja | Bajo | Índice en `token` y limpieza periódica mantienen la tabla pequeña | Benchmark post-implementación |

---

## Criterios de verificación

### Escenarios de aceptación (Gherkin)

```gherkin
Funcionalidad: Cierre de sesión

  Antecedentes:
    Dado que el backend está corriendo en http://localhost:8080
    Y existe usuario "ana@test.com" con sesión JWT activa

  Escenario: Logout exitoso (actual — sin revocación)
    Cuando se envía POST /api/auth/logout con Authorization: Bearer <jwt_valido>
    Entonces el sistema responde 200 OK
    Y el cuerpo contiene { "mensaje": "Sesión cerrada exitosamente" }
    Y se emite evento LOGOUT en auditoría para "ana@test.com"

  Escenario: JWT sigue válido tras logout (deuda técnica documentada)
    Dado que se realizó logout exitoso con <jwt>
    Cuando se envía GET /api/perfil con Authorization: Bearer <jwt>
    Entonces el sistema responde 200 OK  # BUG CONOCIDO — debe ser 401 cuando se implemente token_revocado
    Y se documenta como pendiente en HU-5

  Escenario: Logout sin cabecera Authorization
    Cuando se envía POST /api/auth/logout sin cabecera Authorization
    Entonces el sistema responde 401 Unauthorized

  Escenario: Logout con JWT expirado
    Dado que el JWT de "ana@test.com" está expirado
    Cuando se envía POST /api/auth/logout con ese JWT
    Entonces el sistema responde 401 Unauthorized

  # Escenarios objetivo (cuando se implemente token_revocado):
  Escenario: JWT invalidado tras logout (objetivo)
    Dado que se realizó logout exitoso con <jwt>
    Cuando se envía GET /api/perfil con Authorization: Bearer <jwt>
    Entonces el sistema responde 401 Unauthorized
    Y el cuerpo contiene error de token revocado
```

### Criterios no funcionales

| Criterio | Métrica | Cómo se verifica |
|---|---|---|
| Tiempo de respuesta logout | ≤ 500 ms | Medición con Postman |
| Token revocado rechazado (objetivo) | 401 en request posterior con token revocado | Test manual post-implementación |

---

## Interfaz de usuario

### Vistas afectadas

| Ruta Angular | Componente | Cambio introducido en HU-5 |
|---|---|---|
| `/dashboard` (o cualquier layout autenticado) | `NavbarComponent` | Botón "Cerrar sesión" que envía POST logout y limpia localStorage |

### Comportamiento del frontend tras logout

1. Envía `POST /api/auth/logout` con JWT.
2. Independientemente de la respuesta (200 o error), elimina `auth_token` y `rol` de `localStorage`.
3. Navega a `/login`.

> El frontend limpia el estado local incluso si el backend falla — esto es correcto desde la perspectiva del UX. La seguridad real depende de la revocación en BD (pendiente).

---

## Fuera de alcance

- **Revocación de `mfaToken`** — no aplica (TTL de 10 min, uso único).
- **Cerrar todas las sesiones activas** — no implementado; no hay gestión de sesiones múltiples.
- **Invalidación de tokens en todas las instancias** — no aplica (sin Docker, instancia única).

---

## Decisiones y preguntas abiertas

| # | Pregunta / Decisión | Responsable | Fecha | Estado |
|---|---|---|---|---|
| 1 | **Deuda técnica:** Implementar `token_revocado` + consulta en `JwtFilter` + job de limpieza periódica. ¿En qué sprint se aborda? | Juan Diego Triana Mejia | 2026-05-24 | **Abierta — bloquea cierre de HU-5** |
| 2 | **Decisión tomada:** El frontend limpia `localStorage` independientemente de la respuesta del backend. Garantiza que el usuario vea el logout como exitoso. | Juan Diego Triana Mejia | 2026-05-20 | Resuelta |

---

## Definición de terminado

- [x] `POST /api/auth/logout` responde 200 con mensaje de confirmación.
- [x] `POST /api/auth/logout` sin Authorization responde 401.
- [x] Evento `LOGOUT` registrado en auditoría.
- [x] Frontend elimina JWT de `localStorage` y navega a `/login`.
- [ ] **PENDIENTE:** Tabla `token_revocado` creada en BD.
- [ ] **PENDIENTE:** `cerrarSesion` persiste el token en `token_revocado`.
- [ ] **PENDIENTE:** `JwtFilter` verifica que el token no esté en `token_revocado`.
- [ ] **PENDIENTE:** JWT inválido tras logout (request posterior retorna 401).
- [ ] **PENDIENTE:** `docs/PROGRESO.md` marcado con ✅ para HU-5 (cuando se implementen los ítems pendientes).

---

## Historial de cambios

| Versión | Fecha | Descripción | Razón |
|---|---|---|---|
| 1.0 | 2026-05-24 | Refactorización a estructura SDD del proyecto. Spec narrativo reemplazado con contrato de API, flujos de error, modelo de datos objetivo (`token_revocado`), criterios Gherkin y deuda técnica documentada explícitamente. | Unificación de todos los SPEC.md bajo plantilla canónica SDD del proyecto |
