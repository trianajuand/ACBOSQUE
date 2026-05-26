# plan.md — EC-09 Bloqueo por Intentos Fallidos de Login
> Derivado de `docs/EC-09-bloqueo-intentos-fallidos/SPEC.md`.
> Estado: PENDIENTE DE APROBACIÓN HUMANA.

---

## 1. Qué construye esta historia

Implementa la táctica de seguridad **Lock Computer** (EC-09, RNF-06): tras **5 intentos fallidos** de login sobre la misma cuenta, el sistema bloquea el acceso durante **15 minutos** (configurable) y notifica al titular por correo. El estado de bloqueo se persiste en la tabla `intento_fallido` en BD (NO en `ConcurrentHashMap` en memoria). Un login exitoso reinicia el contador y levanta el bloqueo.

El escenario de calidad exige que el bloqueo se aplique en ≤ 1 segundo tras el 5.º intento.

---

## 2. Decisiones técnicas

| # | Decisión | Justificación |
|---|---|---|
| 1 | Estado de bloqueo en tabla `intento_fallido` en BD, NO en `ConcurrentHashMap` | Regla dura del proyecto (CLAUDE.md §7, CONVENCIONES.md §2.4). Un `ConcurrentHashMap` estático no escala en múltiples instancias ni sobrevive reinicios. |
| 2 | Máximo de intentos y duración configurables en `application.properties` (`app.seguridad.max-intentos`, `app.seguridad.bloqueo-minutos`) | Permite ajuste sin redespliegue; consistente con táctica Defer Binding del proyecto. |
| 3 | HTTP 423 (Locked) para cuenta bloqueada | Estándar definido en `CONVENCIONES.md §1.6` para este escenario. |
| 4 | Notificación al titular vía `INotificacion.notificarBloqueo()` solo si el usuario existe | SPEC indica que para correos inexistentes se audita como LOGIN_FALLIDO pero no se notifica. |
| 5 | Se registra `CUENTA_BLOQUEADA` vía `IAuditLog` | Bloqueo de cuenta es evento auditable obligatorio (CONVENCIONES.md §2.7). |
| 6 | El bloqueo se verifica ANTES de buscar/validar credenciales | Evita exponer timing attacks y cumple el flujo funcional del SPEC. |

---

## 3. Cambios de dependencias

Ningún cambio en `pom.xml`. Los valores configurables (`app.seguridad.max-intentos`, `app.seguridad.bloqueo-minutos`) deben declararse en `application.properties` si no existen.

---

## 4. Deuda técnica o hallazgos previos

| Hallazgo | Acción |
|---|---|
| El SPEC indica que el contador no implementa ventana móvil explícita (solo aumenta hasta reinicio o bloqueo) | Esto es una limitación vs. el estándar del CLAUDE.md §14 que menciona "ventana móvil de 15 min". Documentar en §9 si se debe añadir lógica de ventana deslizante. |
| `IntentoFallido` en el SPEC tiene campos: `correo`, `contador`, `bloqueado_hasta`, `ultimo_intento` | La arquitectura en ARQUITECTURA.md §3.1 menciona `intento_fallido(correo, ip, timestamp, exitoso boolean)`. Hay discrepancia de esquema. Exponer en §9. |

---

## 5. Arquitectura de la solución

### 5a. Mapeo de componentes (backend)

| Capa | Componente | Módulo | Responsabilidad |
|---|---|---|---|
| Controller | `AuthController` | `autenticacion` | Recibe `POST /api/auth/login`; delega a `AutenticacionService`. |
| Service | `MonitorIntentosService` | `autenticacion` | Verifica si correo bloqueado, registra intento fallido, incrementa contador, define `bloqueadoHasta`, reinicia en login exitoso. |
| Service | `AutenticacionService` | `autenticacion` | Llama `MonitorIntentosService` antes y después de validar credenciales. Audita eventos. |
| Repository | `IntentoFallidoRepository` | `autenticacion` | `findByCorreo(String)`, `save()`. |
| Model | `IntentoFallido` | `autenticacion` | Entidad con `correo`, `contador`, `bloqueadoHasta`, `ultimoIntento`. |
| Interface | `INotificacion` | `integracion` | `notificarBloqueo(String correo, String nombreCompleto)` — llamado cuando se bloquea un usuario existente. |
| Interface | `IAuditLog` | `trazabilidad` | `registrar(LOGIN_FALLIDO, ...)`, `registrar(CUENTA_BLOQUEADA, ...)`. |
| Exception | `AccountLockedException` | `shared/exceptions` | Lanzada cuando `bloqueadoHasta > now()` → HTTP 423. |

### 5b. Mapeo de componentes (frontend)

| Componente | Archivo | Responsabilidad |
|---|---|---|
| `LoginComponent` | `auth/login.component.ts` | Muestra mensaje de error recibido (toast). Para HTTP 423, muestra mensaje específico de cuenta bloqueada. |
| `ApiService` | `core/api.service.ts` | Propaga error HTTP al componente. |

### 5c. Modelo de datos

Tabla: `intento_fallido` (módulo `autenticacion`).

```
intento_fallido
  id             BIGINT PK (IDENTITY)
  correo         VARCHAR NOT NULL  -- índice para búsqueda rápida
  contador       INT NOT NULL DEFAULT 0
  bloqueado_hasta TIMESTAMPTZ NULL  -- NULL = no bloqueado
  ultimo_intento TIMESTAMPTZ NOT NULL
```

Nota: si la tabla ya existe con el esquema del ARQUITECTURA.md (`correo, ip, timestamp, exitoso`), evaluar si migrar al esquema del SPEC o mantener ambos. Ver §9.

### 5d. Contratos de API

```
POST /api/auth/login
Content-Type: application/json
{ "correo": "...", "contrasenia": "..." }

Responses:
200 OK    → LoginResponseDTO (JWT, rol, nombre)
401       → { "error": "Credenciales inválidas" }
423 Locked → { "error": "Cuenta bloqueada temporalmente. Intente en X minutos." }
```

---

## 6. Grafo de dependencias entre tareas

```
T1.1 (verificar/ajustar modelo IntentoFallido y repositorio)
    └─► T1.2 (verificar application.properties con parámetros)
            └─► T2.1 (implementar/verificar MonitorIntentosService)
                    └─► T2.2 (integrar con AutenticacionService)
                            └─► T2.3 (test unitario)
                                    └─► T3.1 (test integración endpoint login)
                                            └─► T4.1 (validación frontend)
                                                    └─► T4.2 (DoD + PROGRESO.md)
```

---

## 7. Estrategia de tests

- **Unitario `MonitorIntentosService`:**
  - `login_5IntentosFallidos_bloqueaCuenta15Min` (test clave de CONVENCIONES.md §8).
  - `login_cuentaBloqueada_lanzaAccountLockedException`.
  - `login_exitoso_reiniciaContador`.
  - `login_correoInexistente_soloAuditaNoNotifica`.
- **Integración `MockMvc`:**
  - 5 peticiones `POST /api/auth/login` con credenciales erróneas → la 5.ª retorna 423.
  - Petición con cuenta bloqueada → 423.
  - Login exitoso tras 15 min → 200 (simular tiempo con `bloqueadoHasta = now() - 1min`).
- **Naming:** `login_5IntentosFallidos_bloqueaCuenta15Min`, `login_cuentaBloqueada_retorna423`.

---

## 8. Trazabilidad criterios de aceptación → artefacto

| Criterio (SPEC) | Test o mecanismo |
|---|---|
| 5 intentos fallidos bloquean temporalmente | `login_5IntentosFallidos_bloqueaCuenta15Min` |
| Cuenta bloqueada no puede iniciar sesión | `login_cuentaBloqueada_retorna423` |
| Login exitoso reinicia contador | `login_exitoso_reiniciaContador` |
| Notificación al titular si existe usuario | Verificar invocación de `INotificacion.notificarBloqueo` en test unitario. |
| Bloqueo ≤ 1 segundo tras 5.º intento | Test de integración mide tiempo de respuesta del 5.º intento. |
| Estado de bloqueo persiste en BD, no en RAM | Inspección: `MonitorIntentosService` no usa `ConcurrentHashMap`; usa `IntentoFallidoRepository`. |

---

## 9. Preguntas abiertas

| # | Pregunta | Propuesta |
|---|---|---|
| 1 | El SPEC no implementa ventana móvil de 15 min (solo contador acumulativo), pero CLAUDE.md §14 la menciona explícitamente. ¿Se debe agregar? | Propuesta: agregar lógica de ventana: solo contar intentos de los últimos 15 min. Requiere filtrar por `ultimo_intento >= now() - 15min` en el repositorio. Decisión del equipo. |
| 2 | Discrepancia de esquema: ARQUITECTURA.md define `intento_fallido(correo, ip, timestamp, exitoso)` y el SPEC define `(correo, contador, bloqueado_hasta, ultimo_intento)`. ¿Cuál es el esquema canónico? | Propuesta: adoptar el esquema del SPEC (más funcional para bloqueo) y documentar en ARQUITECTURA.md. Alternativamente, mantener ambos esquemas con propósitos distintos. |
| 3 | ¿El bloqueo aplica por correo únicamente, o también por IP? | SPEC dice "por correo". Si se quiere protección por IP también (rate limiting), requeriría una tabla o mecanismo adicional. |

---

## 10. Definition of Done

- [ ] 5 intentos fallidos consecutivos bloquean la cuenta durante 15 minutos (tiempo configurable).
- [ ] Estado de bloqueo persistido en tabla `intento_fallido` en BD (sin `ConcurrentHashMap`).
- [ ] HTTP 423 retornado al intentar login con cuenta bloqueada.
- [ ] Login exitoso reinicia contador y `bloqueadoHasta`.
- [ ] Notificación al titular enviada vía `INotificacion.notificarBloqueo()` si el usuario existe.
- [ ] Eventos `LOGIN_FALLIDO` y `CUENTA_BLOQUEADA` registrados vía `IAuditLog`.
- [ ] `app.seguridad.max-intentos` y `app.seguridad.bloqueo-minutos` declarados en `application.properties`.
- [ ] Test unitario `login_5IntentosFallidos_bloqueaCuenta15Min` en verde.
- [ ] Tests de integración MockMvc en verde.
- [ ] `LoginComponent` muestra mensaje específico para HTTP 423.
- [ ] Preguntas §9 sobre ventana móvil y esquema de tabla respondidas.
- [ ] `docs/PROGRESO.md` actualizado.
