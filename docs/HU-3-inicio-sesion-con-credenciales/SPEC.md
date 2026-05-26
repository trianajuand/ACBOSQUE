# SPEC — Inicio de sesión con credenciales y JWT

---

## Ficha de la historia

| Campo | Valor |
|---|---|
| ID | HU-3 |
| Sprint | 1 |
| Prioridad MoSCoW | Must Have |
| Estado | Completada |
| Épica | Autenticación / Acceso al sistema |
| CU asociado | CU-03 |
| Autor | Juan Diego Triana Mejia |
| Creada | 2026-05-20 |
| Última revisión | 2026-05-24 |
| Versión de esta spec | 1.0 |

**Trazabilidad:**

| Tipo | ID | Descripción |
|---|---|---|
| Requerimiento funcional | RF-02 | Inicio de sesión con correo y contraseña |
| Escenario de calidad | EC-09 | Bloqueo de cuenta tras 5 intentos fallidos (Lock Computer) |
| Escenario de calidad | EC-10 | JWT firmado con HMAC-SHA256, claim de rol, expiración 1h |
| Escenario de calidad | EC-12 | Trazabilidad de LOGIN_EXITOSO, LOGIN_FALLIDO, CUENTA_BLOQUEADA |
| Historia que depende de esta | HU-4 | Necesita `mfaToken` emitido aquí para flujo MFA |
| Historia que precede a esta | HU-1 | Necesita cuenta con `estado_cuenta = ACTIVA` |

---

## Historia de usuario

**Como** usuario registrado (inversionista, comisionista, administrador o responsable legal),
**quiero** iniciar sesión con mi correo y contraseña,
**para** acceder al sistema y operar según mi rol, con protección contra accesos no autorizados.

---

## Motivación y contexto

### Por qué existe esta historia

El inicio de sesión es el mecanismo de acceso al sistema para todos los roles. Debe verificar la identidad mediante BCrypt, gestionar el estado de la cuenta, controlar intentos fallidos con bloqueo temporal (EC-09), y emitir un JWT firmado (EC-10) como prueba de autenticación. Para roles que requieren MFA obligatorio (COMISIONISTA, ADMINISTRADOR, RESPONSABLE_LEGAL) o inversionistas con MFA opcional habilitado, el resultado es un `mfaToken` de corto plazo en lugar del JWT de sesión completo; el flujo se completa en HU-4.

### Dependencias hacia atrás

| Componente | Qué provee | Sin esto... |
|---|---|---|
| Registro completado (HU-1) | Cuenta con `estado_cuenta = ACTIVA` | No hay usuario que autenticar |
| `PasswordEncoder` bean (BCrypt) | Verificación de contraseña hasheada | `iniciarSesion` falla con NPE en inyección |
| `JwtUtil` configurado | `app.jwt.secret` y `app.jwt.expiracion-ms` en properties | El JWT generado es inválido |
| `MonitorIntentosService` | Tabla `intento_fallido` en PostgreSQL | No se puede controlar bloqueo por intentos |
| `INotificacion` (SMTP) | Envío de notificación de bloqueo de cuenta | La notificación de bloqueo no llega al usuario |

### Historias que dependen de esta

| Historia | Qué consume de aquí |
|---|---|
| HU-4 | `mfaToken` emitido cuando `requiereMfa = true` |
| Todas las historias autenticadas | JWT emitido en login directo |

---

## Actores y precondiciones

### Actores

| Actor | Rol en el sistema | Participación en este flujo |
|---|---|---|
| Usuario registrado | INVERSIONISTA / COMISIONISTA / ADMINISTRADOR / RESPONSABLE_LEGAL | Iniciador — envía correo y contraseña |
| `AutenticacionService` | Módulo `autenticacion` | Orquesta validación de bloqueo, credenciales, estado y emisión de token |
| `MonitorIntentosService` | Módulo `autenticacion` | Controla conteo y bloqueo de intentos fallidos |
| `MFAService` | Módulo `autenticacion` | Genera y persiste código MFA cuando es requerido |
| `JwtUtil` | Módulo `autenticacion` (shared) | Firma y genera JWT y mfaToken |
| `DespachadorNotificaciones` | Módulo `integracion` (vía `INotificacion`) | Envía notificación de bloqueo y código MFA |
| `AuditLogService` | Módulo `trazabilidad` (vía `IAuditLog`) | Registra eventos auditables de login |

### Precondiciones

- Existe `Usuario` con el correo proporcionado, `rol` asignado y `estado_cuenta = ACTIVA` o `OPERACIONES_RESTRINGIDAS`.
- `app.jwt.secret` y `app.jwt.expiracion-ms` configurados en `application.properties`.
- `app.seguridad.max-intentos` y `app.seguridad.bloqueo-minutos` configurados.
- Tabla `intento_fallido` existente en PostgreSQL.

### Postcondiciones del flujo principal (login directo sin MFA)

- Se emite JWT firmado (HMAC-SHA256), claim `sub = correo`, claim `rol`, TTL = `app.jwt.expiracion-ms`.
- El contador de intentos fallidos del usuario queda en 0.
- Evento `LOGIN_EXITOSO` registrado en auditoría.

### Postcondiciones del flujo alternativo (MFA requerido)

- Se emite `mfaToken` (JWT de 10 min, claim `tipo = "MFA"`, `sub = correo`).
- Código MFA de 6 dígitos generado y enviado al correo del usuario.
- Evento `MFA_ENVIADO` registrado en auditoría.
- El login completo se finaliza en HU-4.

---

## Flujo principal (inversionista sin MFA habilitado)

1. Usuario abre `/login` en Angular e ingresa correo y contraseña.
2. Frontend envía `POST /api/auth/login` con `{correo, contrasenia}`.

**Backend — `AutenticacionService.iniciarSesion`:**

3. `MonitorIntentosService.verificarBloqueo(correo)`: si `bloqueadoHasta > now()`, lanza `AccountLockedException` → 423.
4. `usuarioRepository.findByCorreo(correo)`: si no existe, registra intento fallido y lanza `InvalidCredentialsException` → 401.
5. Si `usuario.estadoCuenta == BLOQUEADA`, lanza `AccountLockedException` → 423.
6. Si `usuario.estadoCuenta != ACTIVA && estadoCuenta != OPERACIONES_RESTRINGIDAS`, lanza excepción con mensaje "La cuenta aun no esta activa" → 403.
7. `passwordEncoder.matches(contrasenia, usuario.contrasenia)`: si no coincide:
   - `MonitorIntentosService.registrarIntentoFallido(correo)`
   - Si `seBloqueo()`: `IAuditLog.registrar(CUENTA_BLOQUEADA)` + `INotificacion.notificarBloqueo(correo)` + lanza `AccountLockedException` → 423.
   - Si no se bloqueó: `IAuditLog.registrar(LOGIN_FALLIDO)` + lanza `InvalidCredentialsException` → 401.
8. `MonitorIntentosService.reiniciarIntentos(correo)`.
9. Evalúa si requiere MFA: `usuario.mfaHabilitado == true` OR `usuario.rol IN (COMISIONISTA, ADMINISTRADOR, RESPONSABLE_LEGAL)`.
10. Como el inversionista no tiene MFA habilitado: genera JWT con HMAC-SHA256 (`app.jwt.secret`), TTL = `app.jwt.expiracion-ms`, claims `sub = correo`, `rol = INVERSIONISTA`.
11. `IAuditLog.registrar(LOGIN_EXITOSO, correo, "Login directo")`.
12. Responde `200 OK` con `LoginResponseDTO{token: "<jwt>", requiereMfa: false, mfaToken: null, rol: "INVERSIONISTA", mensaje: "Login exitoso"}`.

**Frontend:**

13. Guarda JWT en `localStorage` (clave `auth_token`), guarda `rol`, navega a `/dashboard`.

---

## Flujos alternativos

### Alternativo A — Login con MFA requerido (rol privilegiado o MFA habilitado)

**Condición:** `usuario.mfaHabilitado == true` OR `usuario.rol IN (COMISIONISTA, ADMINISTRADOR, RESPONSABLE_LEGAL)`.

Pasos 1–9 idénticos. En el paso 9:

10-A. `MFAService.generarYGuardarCodigo(correo, MFA)`: elimina código MFA previo, genera 6 dígitos con `SecureRandom`, persiste en `codigo_verificacion` con TTL = `app.seguridad.codigo-expiracion-minutos`.
11-A. `INotificacion.enviarCodigoMfa(correo, nombreCompleto, codigo)`.
12-A. `JwtUtil.generarTokenMfa(correo)`: JWT con `sub = correo`, claim `tipo = "MFA"`, TTL = 10 minutos (hardcodeado en `JwtUtil`).
13-A. `IAuditLog.registrar(MFA_ENVIADO, correo, "Código MFA generado para login")`.
14-A. Responde `200 OK` con `LoginResponseDTO{requiereMfa: true, mfaToken: "<jwt_mfa>", token: null, rol: null}`.

**Frontend:**

15-A. Guarda `mfaToken` en `sessionStorage`, navega a `/mfa/verificar` para completar el flujo (HU-4).

---

## Flujos de error

### Error 1 — Cuenta bloqueada temporalmente

| Campo | Valor |
|---|---|
| Condición | `intento_fallido.bloqueado_hasta > now()` al inicio de la solicitud |
| Excepción Java | `AccountLockedException` |
| HTTP | 423 Locked |
| Cuerpo | `RespuestaDTO{error: "Cuenta bloqueada temporalmente. Intente de nuevo más tarde."}` |
| Estado final | Sin cambios en intentos; bloqueo persiste hasta `bloqueado_hasta` |
| Evento de auditoría | Ninguno (el bloqueo ya fue auditado cuando se alcanzó el límite) |

### Error 2 — Credenciales inválidas (usuario no encontrado)

| Campo | Valor |
|---|---|
| Condición | No existe `Usuario` con el correo proporcionado |
| Excepción Java | `InvalidCredentialsException` (o `UsuarioNoEncontradoException` manejada como 401) |
| HTTP | 401 Unauthorized |
| Cuerpo | `RespuestaDTO{error: "Credenciales inválidas"}` |
| Estado final | Se registra intento fallido por el correo proporcionado |
| Evento de auditoría | `LOGIN_FALLIDO` con detalle `"Usuario no encontrado"` |

### Error 3 — Credenciales inválidas (contraseña incorrecta)

| Campo | Valor |
|---|---|
| Condición | `passwordEncoder.matches` retorna false |
| Excepción Java | `InvalidCredentialsException` |
| HTTP | 401 Unauthorized |
| Cuerpo | `RespuestaDTO{error: "Credenciales inválidas"}` |
| Estado final | Incrementa `intento_fallido.contador`; si llega a `max-intentos`, bloquea |
| Evento de auditoría | `LOGIN_FALLIDO`; si se bloqueó: adicionalmente `CUENTA_BLOQUEADA` |

### Error 4 — Cuenta bloqueada al alcanzar el límite de intentos

| Campo | Valor |
|---|---|
| Condición | `contador + 1 == max-intentos` tras contraseña incorrecta |
| Excepción Java | `AccountLockedException` |
| HTTP | 423 Locked |
| Cuerpo | `RespuestaDTO{error: "Cuenta bloqueada temporalmente. Intente de nuevo más tarde."}` |
| Estado final | `bloqueado_hasta = now() + bloqueo-minutos`; notificación enviada al correo |
| Evento de auditoría | `CUENTA_BLOQUEADA` + `LOGIN_FALLIDO` |

### Error 5 — Cuenta no activa (pendiente de verificación u otro estado)

| Campo | Valor |
|---|---|
| Condición | `usuario.estadoCuenta != ACTIVA && != OPERACIONES_RESTRINGIDAS` |
| Excepción Java | Excepción personalizada con mensaje "La cuenta aun no esta activa" |
| HTTP | 403 Forbidden |
| Cuerpo | `RespuestaDTO{error: "La cuenta aun no esta activa"}` |
| Estado final | Sin cambios; no se registra intento fallido |
| Evento de auditoría | Ninguno |

### Error 6 — Campos obligatorios ausentes o inválidos

| Campo | Valor |
|---|---|
| Condición | `correo` o `contrasenia` ausente o correo con formato inválido (Bean Validation) |
| Excepción Java | `MethodArgumentNotValidException` |
| HTTP | 400 Bad Request |
| Cuerpo | `RespuestaDTO{error: "{campo}: {mensaje de validación}"}` |
| Estado final | Sin cambios |
| Evento de auditoría | Ninguno |

### Error 7 — Error técnico genérico

| Campo | Valor |
|---|---|
| Condición | Cualquier excepción no manejada (falla BD, NPE, etc.) |
| Excepción Java | `Exception` (catch-all) |
| HTTP | 500 Internal Server Error |
| Cuerpo | `RespuestaDTO{error: "Error interno del servidor"}` |
| Estado final | Sin cambios garantizados (no hay `@Transactional` en `iniciarSesion`) |
| Evento de auditoría | Ninguno (stack trace en consola) |

---

## Contrato de API

### Endpoint — `POST /api/auth/login`

```yaml
POST /api/auth/login:
  summary: Inicia sesión con correo y contraseña
  security: []  # Endpoint público
  requestBody:
    required: true
    content:
      application/json:
        schema:
          $ref: '#/components/schemas/LoginRequestDTO'
        example:
          correo: "ana.gomez@correo.com"
          contrasenia: "Segura123"
  responses:
    '200':
      description: Autenticación exitosa (con o sin MFA pendiente)
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/LoginResponseDTO'
          examples:
            loginDirecto:
              summary: Inversionista sin MFA — JWT emitido
              value:
                token: "eyJhbGciOiJIUzI1NiJ9..."
                requiereMfa: false
                mfaToken: null
                rol: "INVERSIONISTA"
                mensaje: "Login exitoso"
            mfaRequerido:
              summary: Rol privilegiado o MFA habilitado — mfaToken emitido
              value:
                token: null
                requiereMfa: true
                mfaToken: "eyJhbGciOiJIUzI1NiIsInRpcG8iOiJNRkEifQ..."
                rol: null
                mensaje: null
    '400':
      description: Campos ausentes o formato de correo inválido
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'
    '401':
      description: Credenciales inválidas (usuario no encontrado o contraseña incorrecta)
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'
          example:
            error: "Credenciales inválidas"
    '403':
      description: Cuenta no activa (pendiente verificación, suspendida, etc.)
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'
          example:
            error: "La cuenta aun no esta activa"
    '423':
      description: Cuenta bloqueada por intentos fallidos
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'
          example:
            error: "Cuenta bloqueada temporalmente. Intente de nuevo más tarde."
    '500':
      description: Error interno del servidor
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'

components:
  schemas:
    LoginRequestDTO:
      type: object
      required: [correo, contrasenia]
      properties:
        correo:
          type: string
          format: email
          description: "@NotBlank @Email"
        contrasenia:
          type: string
          description: "@NotBlank"
    LoginResponseDTO:
      type: object
      properties:
        token:
          type: string
          nullable: true
          description: "JWT de sesión completa (HMAC-SHA256). Null si requiereMfa=true"
        requiereMfa:
          type: boolean
          description: "true si el usuario debe completar verificación MFA (HU-4)"
        mfaToken:
          type: string
          nullable: true
          description: "JWT de corto plazo (10 min, claim tipo=MFA). Null si requiereMfa=false"
        rol:
          type: string
          nullable: true
          description: "Rol del usuario (INVERSIONISTA, COMISIONISTA, etc.). Null si requiereMfa=true"
        mensaje:
          type: string
          nullable: true
```

**Tabla de validaciones server-side (`@Valid` en `LoginRequestDTO`):**

| Campo | Restricción | Mensaje de error |
|---|---|---|
| `correo` | `@NotBlank @Email` | "El correo es obligatorio" / "Formato de correo inválido" |
| `contrasenia` | `@NotBlank` | "La contraseña es obligatoria" |

---

## Modelo de datos

### Tabla `intento_fallido` (nueva en HU-3)

```sql
CREATE TABLE intento_fallido (
    id              BIGSERIAL PRIMARY KEY,
    correo          VARCHAR(255) NOT NULL UNIQUE,
    contador        INT          NOT NULL DEFAULT 0,
    ultimo_intento  TIMESTAMP,
    bloqueado_hasta TIMESTAMP
);

CREATE UNIQUE INDEX idx_intento_fallido_correo ON intento_fallido (correo);
```

**Decisiones de esquema:**
- `correo UNIQUE`: un registro por usuario; se actualiza con cada intento.
- `bloqueado_hasta`: cuando `bloqueado_hasta > now()`, el usuario está bloqueado. `NULL` significa sin bloqueo activo.
- `contador`: se reinicia a 0 tras login exitoso (`reiniciarIntentos`).
- `max-intentos` y `bloqueo-minutos` son externos al modelo, configurables en `application.properties`.

### Tabla `usuario` (referenciada, creada en HU-1)

Ver DDL completo en HU-1 SPEC. Campos relevantes para este flujo:
- `correo` — clave de búsqueda
- `contrasenia` — hash BCrypt a verificar
- `estado_cuenta` — debe ser `ACTIVA` o `OPERACIONES_RESTRINGIDAS`
- `rol` — determina si MFA es obligatorio
- `mfa_habilitado` — determina si MFA es requerido para INVERSIONISTA

---

## Módulos y arquitectura

### Módulos involucrados

| Módulo | Rol | Componentes específicos |
|---|---|---|
| `autenticacion` | Coordinador del flujo | `AuthController`, `AutenticacionService`, `MonitorIntentosService`, `MFAService`, `JwtUtil` |
| `integracion` | Notificaciones | `DespachadorNotificaciones` (impl. de `INotificacion`) |
| `trazabilidad` | Registro de eventos | `AuditLogService` (impl. de `IAuditLog`) |

### Interfaces consumidas en este flujo

| Interfaz | Módulo dueño | Métodos usados | Cuándo |
|---|---|---|---|
| `INotificacion` | `integracion` | `notificarBloqueo(correo)`, `enviarCodigoMfa(correo, nombre, codigo)` | Al bloquear cuenta; al requerir MFA |
| `IAuditLog` | `trazabilidad` | `registrar(TipoEvento, correo, detalle)` | En cada resultado del flujo |

### Interfaces nuevas o modificadas

| Interfaz | Cambio | Motivo |
|---|---|---|
| `IAutenticacion` | Expone `iniciarSesion(LoginRequestDTO)`, `verificarMfa(MFARequestDTO)`, `cerrarSesion(token)` | Contrato del módulo autenticacion hacia controllers |

### Escenarios de calidad y tácticas materializadas

| EC | Táctica | Cómo se materializa en HU-3 |
|---|---|---|
| EC-09 | Lock Computer | Bloqueo temporal tras `max-intentos` fallidos; duración configurable en `app.seguridad.bloqueo-minutos`; notificación al titular |
| EC-10 | Authenticate Actors | JWT firmado HMAC-SHA256 con `app.jwt.secret`, expiración desde `app.jwt.expiracion-ms`, claim de rol incluido |
| EC-12 | Audit Trail | `LOGIN_EXITOSO`, `LOGIN_FALLIDO`, `CUENTA_BLOQUEADA`, `MFA_ENVIADO` registrados vía `IAuditLog` |

### Desviaciones arquitectónicas

`AutenticacionService` implementa `IAutenticacion` correctamente. No se detectan cruces de frontera de módulo anómalos en este flujo.

**Observación:** `JwtUtil.generarTokenMfa` tiene el TTL del mfaToken hardcodeado (`10 * 60 * 1000L` ms). No hay propiedad `app.jwt.mfa-expiracion-ms` en `application.properties`. Deuda técnica menor — hacerlo configurable.

---

## Eventos y efectos transversales

### Eventos de auditoría emitidos

| Evento (`TipoEvento`) | Cuándo se emite | Datos en `detalle` |
|---|---|---|
| `LOGIN_EXITOSO` | Login directo completado | `"Login directo"` |
| `LOGIN_FALLIDO` | Contraseña incorrecta o usuario no encontrado | Detalle del motivo |
| `CUENTA_BLOQUEADA` | Último intento fallido que activa el bloqueo | Detalles del bloqueo |
| `MFA_ENVIADO` | Código MFA generado y enviado | `"Código MFA generado para login"` |

### Notificaciones enviadas

| Trigger | Canal | Método `INotificacion` | Contenido |
|---|---|---|---|
| Bloqueo de cuenta | Email (SMTP) | `notificarBloqueo(correo)` | Aviso de bloqueo temporal con instrucciones |
| MFA requerido | Email (SMTP) | `enviarCodigoMfa(correo, nombre, codigo)` | Código de 6 dígitos para completar el login |

### Cambios en caché u otros estados compartidos

El JWT emitido es stateless. No se persiste en BD (ver HU-5 para el punto de deuda de revocación). El contador de intentos se persiste en `intento_fallido`.

---

## Riesgos

| # | Riesgo | P | I | Mitigación | Test que lo cubre |
|---|---|:-:|:-:|---|---|
| R1 | JWT `app.jwt.secret` débil o en texto plano en properties (sin variable de entorno) hace el token predecible | Alta (dev) | Crítico | En producción, configurar como variable de entorno. En desarrollo está como placeholder en `application.properties` | Revisión manual de properties |
| R2 | SMTP falla al enviar notificación de bloqueo → usuario bloqueado no recibe aviso | Media | Bajo | El bloqueo ocurre correctamente; la notificación es informativa, no bloqueante del flujo de seguridad | Manual: desconectar SMTP y verificar que el bloqueo ocurre igual |
| R3 | Mensaje de error diferente para "usuario no encontrado" vs "contraseña incorrecta" — posible user enumeration | Baja | Medio | Ambos casos retornan el mismo mensaje `"Credenciales inválidas"` — implementación correcta | Verificar manualmente ambos casos |
| R4 | `mfaToken` TTL hardcodeado (10 min) no es configurable — si se necesita ajustar en producción se requiere recompilación | Baja | Bajo | Crear propiedad `app.jwt.mfa-expiracion-ms` (pregunta abierta #1) | No hay test |

---

## Criterios de verificación

### Escenarios de aceptación (Gherkin)

```gherkin
Funcionalidad: Inicio de sesión con credenciales y JWT

  Antecedentes:
    Dado que el backend está corriendo en http://localhost:8080
    Y existe usuario "ana@test.com" con contraseña "Segura123" y estado_cuenta="ACTIVA" y rol="INVERSIONISTA" y mfa_habilitado=false

  Escenario: Login exitoso sin MFA
    Cuando se envía POST /api/auth/login con { "correo": "ana@test.com", "contrasenia": "Segura123" }
    Entonces el sistema responde 200 OK
    Y el cuerpo contiene un campo "token" no nulo
    Y el cuerpo contiene "requiereMfa": false
    Y el cuerpo contiene "rol": "INVERSIONISTA"
    Y se emite evento LOGIN_EXITOSO en auditoría para "ana@test.com"
    Y el campo intento_fallido.contador para "ana@test.com" es 0

  Escenario: Contraseña incorrecta incrementa contador
    Dado que no hay bloqueo activo para "ana@test.com"
    Cuando se envía POST /api/auth/login con { "correo": "ana@test.com", "contrasenia": "Incorrecta" }
    Entonces el sistema responde 401 Unauthorized
    Y el cuerpo contiene { "error": "Credenciales inválidas" }
    Y se emite evento LOGIN_FALLIDO en auditoría
    Y el campo intento_fallido.contador para "ana@test.com" es 1

  Escenario: Quinto intento fallido bloquea la cuenta
    Dado que intento_fallido.contador para "ana@test.com" es 4
    Cuando se envía POST /api/auth/login con contraseña incorrecta
    Entonces el sistema responde 423 Locked
    Y el cuerpo contiene { "error": "Cuenta bloqueada temporalmente. Intente de nuevo más tarde." }
    Y intento_fallido.bloqueado_hasta para "ana@test.com" está en el futuro
    Y se emite evento CUENTA_BLOQUEADA en auditoría
    Y se envía notificación de bloqueo al correo "ana@test.com"

  Escenario: Login rechazado con cuenta bloqueada
    Dado que intento_fallido.bloqueado_hasta para "ana@test.com" está 15 minutos en el futuro
    Cuando se envía POST /api/auth/login con credenciales correctas
    Entonces el sistema responde 423 Locked
    Y el cuerpo contiene { "error": "Cuenta bloqueada temporalmente. Intente de nuevo más tarde." }

  Escenario: Usuario no registrado recibe 401
    Cuando se envía POST /api/auth/login con { "correo": "noexiste@test.com", "contrasenia": "cualquiera" }
    Entonces el sistema responde 401 Unauthorized
    Y el cuerpo contiene { "error": "Credenciales inválidas" }

  Escenario: Cuenta no activa recibe 403
    Dado que existe usuario "pendiente@test.com" con estado_cuenta="PENDIENTE_VERIFICACION"
    Cuando se envía POST /api/auth/login con credenciales correctas de "pendiente@test.com"
    Entonces el sistema responde 403 Forbidden
    Y el cuerpo contiene { "error": "La cuenta aun no esta activa" }

  Escenario: Login exitoso reinicia contador de intentos
    Dado que intento_fallido.contador para "ana@test.com" es 3
    Cuando se envía POST /api/auth/login con credenciales correctas
    Entonces el sistema responde 200 OK
    Y intento_fallido.contador para "ana@test.com" es 0

  Escenario: Rol privilegiado recibe mfaToken
    Dado que existe usuario "admin@test.com" con rol="ADMINISTRADOR" y credenciales válidas
    Cuando se envía POST /api/auth/login con credenciales correctas de "admin@test.com"
    Entonces el sistema responde 200 OK
    Y el cuerpo contiene "requiereMfa": true
    Y el cuerpo contiene un campo "mfaToken" no nulo
    Y el cuerpo contiene "token": null
    Y se emite evento MFA_ENVIADO en auditoría
    Y se envía correo con código de 6 dígitos a "admin@test.com"

  Escenario: Inversionista con MFA habilitado recibe mfaToken
    Dado que "ana@test.com" tiene mfa_habilitado=true
    Cuando se envía POST /api/auth/login con credenciales correctas
    Entonces el sistema responde 200 OK
    Y el cuerpo contiene "requiereMfa": true
    Y el cuerpo contiene un campo "mfaToken" no nulo

  Esquema del escenario: Campos obligatorios ausentes
    Cuando se envía POST /api/auth/login sin el campo <campo>
    Entonces el sistema responde 400 Bad Request

    Ejemplos:
      | campo      |
      | correo     |
      | contrasenia |
```

### Criterios no funcionales

| Criterio | Métrica | Cómo se verifica |
|---|---|---|
| Tiempo de respuesta | ≤ 2 s en condiciones normales | Medición con Postman |
| JWT no contiene contraseña | Decodificar JWT en jwt.io — claims: sub, rol, iat, exp únicamente | Decodificación manual del token |
| Bloqueo persiste tras reinicio del servidor | `bloqueado_hasta` almacenado en BD, no en memoria | Reiniciar backend con cuenta bloqueada y verificar 423 |

---

## Interfaz de usuario

### Vistas afectadas

| Ruta Angular | Componente | Cambio introducido en HU-3 |
|---|---|---|
| `/login` | `LoginComponent` | Formulario de correo y contraseña con manejo de estados de error |
| `/mfa/verificar` | `MfaComponent` | Recibe `mfaToken` de `sessionStorage`, muestra campo de código (HU-4) |

### Estados de la pantalla `/login`

| Estado | Disparador | UI resultante |
|---|---|---|
| Idle | Carga de la ruta | Formulario vacío, botón "Iniciar sesión" habilitado |
| Enviando | Usuario presiona "Iniciar sesión" | Estado de carga; botón deshabilitado |
| Éxito directo | `requiereMfa: false` | JWT guardado; navega a `/dashboard` |
| MFA requerido | `requiereMfa: true` | `mfaToken` en `sessionStorage`; navega a `/mfa/verificar` |
| Error 401 | Backend responde 401 | Mensaje "Credenciales inválidas" bajo el formulario |
| Error 403 | Backend responde 403 | Mensaje "La cuenta aún no está activa" |
| Error 423 | Backend responde 423 | Mensaje "Cuenta bloqueada temporalmente" |

---

## Fuera de alcance

- **Verificación MFA** — HU-4.
- **Cierre de sesión / revocación JWT** — HU-5.
- **Recuperación de contraseña** — SOPORTE-recuperacion-password.
- **OAuth / inicio de sesión social** — no contemplado en el MVP.
- **Rate limiting de intentos de login por IP** — no implementado.

---

## Decisiones y preguntas abiertas

| # | Pregunta / Decisión | Responsable | Fecha | Estado |
|---|---|---|---|---|
| 1 | `mfaToken` tiene TTL hardcodeado de 10 minutos en `JwtUtil`. ¿Se extrae a propiedad `app.jwt.mfa-expiracion-ms`? | Juan Diego Triana Mejia | 2026-05-24 | Abierta |
| 2 | **Decisión tomada:** El mensaje de error es idéntico para "usuario no encontrado" y "contraseña incorrecta" (`"Credenciales inválidas"`). Esto previene user enumeration. | Juan Diego Triana Mejia | 2026-05-20 | Resuelta |
| 3 | **Decisión tomada:** `max-intentos` y `bloqueo-minutos` son configurables en `application.properties`, no hardcodeados. | Juan Diego Triana Mejia | 2026-05-20 | Resuelta |

---

## Definición de terminado

- [x] `POST /api/auth/login` responde 200 con JWT para inversionistas sin MFA.
- [x] `POST /api/auth/login` responde 200 con `mfaToken` para roles privilegiados y MFA habilitado.
- [x] `POST /api/auth/login` responde 401 para credenciales inválidas (mismo mensaje en ambos casos).
- [x] `POST /api/auth/login` responde 403 para cuentas no activas.
- [x] `POST /api/auth/login` responde 423 para cuentas bloqueadas.
- [x] Bloqueo ocurre al 5.° intento fallido (configurable).
- [x] Contador de intentos se reinicia en login exitoso.
- [x] Notificación de bloqueo enviada al correo del usuario.
- [x] JWT contiene claims `sub` (correo) y `rol`.
- [x] `mfaToken` contiene claim `tipo = "MFA"` y TTL de 10 min.
- [x] Eventos `LOGIN_EXITOSO`, `LOGIN_FALLIDO`, `CUENTA_BLOQUEADA`, `MFA_ENVIADO` en auditoría.
- [x] `docs/PROGRESO.md` marcado con ✅ para HU-3.

---

## Historial de cambios

| Versión | Fecha | Descripción | Razón |
|---|---|---|---|
| 1.0 | 2026-05-24 | Refactorización a estructura SDD del proyecto. Spec narrativo reemplazado con contrato de API completo, flujos de error individualizados, DDL real de `intento_fallido`, criterios Gherkin, tabla de riesgos y decisiones documentadas. | Unificación de todos los SPEC.md bajo plantilla canónica SDD del proyecto |
