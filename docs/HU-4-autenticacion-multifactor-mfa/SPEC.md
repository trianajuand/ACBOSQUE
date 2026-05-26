# SPEC — Verificación de segundo factor (MFA) durante el login

---

## Ficha de la historia

| Campo | Valor |
|---|---|
| ID | HU-4 |
| Sprint | 1 |
| Prioridad MoSCoW | Must Have |
| Estado | Completada |
| Épica | Autenticación / Acceso al sistema |
| CU asociado | CU-04 |
| Autor | Juan Diego Triana Mejia |
| Creada | 2026-05-20 |
| Última revisión | 2026-05-24 |
| Versión de esta spec | 1.0 |

**Trazabilidad:**

| Tipo | ID | Descripción |
|---|---|---|
| Requerimiento funcional | RF-03 | MFA obligatorio para roles privilegiados, opcional para inversionista |
| Escenario de calidad | EC-10 | Código de 6 dígitos persistido en BD con TTL; validado antes de emitir JWT |
| Escenario de calidad | EC-12 | Trazabilidad de MFA_VERIFICADO y LOGIN_EXITOSO |
| Historia que precede a esta | HU-3 | Emite el `mfaToken` necesario para acceder a este endpoint |
| Historia que habilita esta | HU-10 | Activa/desactiva MFA opcional para inversionistas |

---

## Historia de usuario

**Como** usuario que requiere MFA (rol privilegiado o inversionista con MFA habilitado),
**quiero** ingresar el código de 6 dígitos que llegó a mi correo,
**para** completar el login y obtener el JWT de sesión completa.

---

## Motivación y contexto

### Por qué existe esta historia

HU-3 puede retornar un estado intermedio (`requiereMfa: true`, `mfaToken`) en lugar de un JWT completo. HU-4 completa ese flujo: valida el código de 6 dígitos usando el `mfaToken` como autenticación del paso intermedio, y emite el JWT de sesión completa. Materializa MFA obligatorio para COMISIONISTA, ADMINISTRADOR y RESPONSABLE_LEGAL, y MFA opcional para INVERSIONISTA (activado por HU-10).

### Dependencias hacia atrás

| Componente | Qué provee | Sin esto... |
|---|---|---|
| HU-3 (login) | `mfaToken` con claim `tipo=MFA` y `sub=correo` | No hay token con que acceder a `/mfa/verify` |
| `codigo_verificacion` en BD | Código MFA generado en HU-3 con TTL | La validación falla por no encontrar código |
| `JwtUtil` | Verificación de `mfaToken` y emisión de JWT final | No se puede validar la autenticidad del token intermedio |

### Historias que dependen de esta

| Historia | Qué consume de aquí |
|---|---|
| Todas las historias autenticadas con roles privilegiados | JWT final emitido aquí |

---

## Actores y precondiciones

### Actores

| Actor | Rol en el sistema | Participación en este flujo |
|---|---|---|
| Usuario autenticado parcialmente | Cualquier rol con MFA | Iniciador — envía código MFA y `mfaToken` |
| `AutenticacionService` | Módulo `autenticacion` | Valida `mfaToken`, verifica código, emite JWT final |
| `MFAService` | Módulo `autenticacion` | Valida el código de 6 dígitos en BD |
| `JwtUtil` | Módulo `autenticacion` (shared) | Verifica `mfaToken` y genera JWT final |
| `AuditLogService` | Módulo `trazabilidad` (vía `IAuditLog`) | Registra eventos auditables |

### Precondiciones

- El usuario completó HU-3 exitosamente con `requiereMfa: true`.
- Existe `mfaToken` en `sessionStorage` del frontend (TTL 10 min desde emisión en HU-3).
- Existe registro en `codigo_verificacion` con `tipo=MFA`, `correo` del usuario, `usado=false` y `expiracion` en el futuro.

### Postcondiciones

- JWT de sesión completa emitido (HMAC-SHA256, TTL = `app.jwt.expiracion-ms`).
- `codigo_verificacion` marcado `usado = true`.
- Eventos `MFA_VERIFICADO` y `LOGIN_EXITOSO` registrados en auditoría.

---

## Flujo principal

1. Usuario está en `/mfa/verificar` e ingresa el código de 6 dígitos recibido por correo.
2. Frontend envía `POST /api/auth/mfa/verify` con `{codigo}` y cabecera `Authorization: Bearer <mfaToken>`.

**Backend — `AutenticacionService.verificarMfa`:**

3. `JwtUtil.esValido(mfaToken)`: si el token es inválido o expirado, lanza excepción → 400 o 401.
4. Extrae `correo` del claim `sub` del `mfaToken`.
5. `MFAService.validarCodigo(correo, codigo, MFA)`: verifica que el código no esté usado, no esté expirado y coincida → marca `usado = true`. Si falla, lanza `InvalidMfaException`.
6. `usuarioRepository.findByCorreo(correo)`: carga el usuario.
7. `JwtUtil.generarToken(correo, rol)`: JWT HMAC-SHA256, TTL = `app.jwt.expiracion-ms`.
8. `IAuditLog.registrar(MFA_VERIFICADO, correo, "MFA validado")`.
9. `IAuditLog.registrar(LOGIN_EXITOSO, correo, "Login via MFA")`.
10. Responde `200 OK` con `LoginResponseDTO{token: "<jwt>", requiereMfa: false, mfaToken: null, rol: "<rol>"}`.

**Frontend:**

11. Limpia `mfaToken` de `sessionStorage`, guarda JWT en `localStorage`, navega a `/dashboard`.

---

## Flujos de error

### Error 1 — `mfaToken` inválido o expirado

| Campo | Valor |
|---|---|
| Condición | `JwtUtil.esValido(mfaToken)` retorna false (token malformado, firma inválida o expirado tras 10 min) |
| Excepción Java | `InvalidTokenException` |
| HTTP | 400 Bad Request |
| Cuerpo | `RespuestaDTO{error: "Token inválido o expirado"}` |
| Estado final | Sin cambios |
| Evento de auditoría | Ninguno |

### Error 2 — Código MFA no encontrado o ya usado

| Campo | Valor |
|---|---|
| Condición | No existe código con `tipo=MFA, correo=X, usado=false` en BD |
| Excepción Java | `InvalidMfaException("Código no encontrado o ya utilizado.")` |
| HTTP | 401 Unauthorized |
| Cuerpo | `RespuestaDTO{error: "Código no encontrado o ya utilizado."}` |
| Estado final | Sin cambios |
| Evento de auditoría | Ninguno |

### Error 3 — Código MFA expirado

| Campo | Valor |
|---|---|
| Condición | `now() > codigo_verificacion.expiracion` |
| Excepción Java | `InvalidMfaException("El código ha expirado.")` |
| HTTP | 401 Unauthorized |
| Cuerpo | `RespuestaDTO{error: "El código ha expirado."}` |
| Estado final | Código sigue en BD con `usado=false`; usuario debe reiniciar login |
| Evento de auditoría | Ninguno |

### Error 4 — Código MFA incorrecto

| Campo | Valor |
|---|---|
| Condición | El código enviado no coincide con el almacenado |
| Excepción Java | `InvalidMfaException("Código incorrecto.")` |
| HTTP | 401 Unauthorized |
| Cuerpo | `RespuestaDTO{error: "Código incorrecto."}` |
| Estado final | Sin cambios |
| Evento de auditoría | Ninguno |

### Error 5 — Cabecera Authorization ausente

| Campo | Valor |
|---|---|
| Condición | Request sin cabecera `Authorization: Bearer ...` |
| HTTP | 401 Unauthorized |
| Cuerpo | Respuesta estándar de Spring Security |
| Estado final | Sin cambios |
| Evento de auditoría | Ninguno |

### Error 6 — Campos obligatorios ausentes

| Campo | Valor |
|---|---|
| Condición | `codigo` ausente en el body |
| Excepción Java | `MethodArgumentNotValidException` |
| HTTP | 400 Bad Request |
| Cuerpo | `RespuestaDTO{error: "codigo: El código es obligatorio"}` |
| Estado final | Sin cambios |
| Evento de auditoría | Ninguno |

---

## Contrato de API

### Endpoint — `POST /api/auth/mfa/verify`

```yaml
POST /api/auth/mfa/verify:
  summary: Completa el login verificando el código MFA
  security:
    - bearerAuth: []  # Requiere mfaToken (JWT con claim tipo="MFA")
  requestBody:
    required: true
    content:
      application/json:
        schema:
          $ref: '#/components/schemas/MFARequestDTO'
        example:
          codigo: "482931"
  responses:
    '200':
      description: MFA verificado — JWT de sesión completa emitido
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/LoginResponseDTO'
          example:
            token: "eyJhbGciOiJIUzI1NiJ9..."
            requiereMfa: false
            mfaToken: null
            rol: "ADMINISTRADOR"
            mensaje: "Login exitoso"
    '400':
      description: mfaToken inválido/expirado o campo codigo ausente
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'
          examples:
            tokenInvalido:
              value: { "error": "Token inválido o expirado" }
            codigoAusente:
              value: { "error": "codigo: El código es obligatorio" }
    '401':
      description: Código MFA no encontrado, expirado o incorrecto
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'
          examples:
            noEncontrado:
              value: { "error": "Código no encontrado o ya utilizado." }
            expirado:
              value: { "error": "El código ha expirado." }
            incorrecto:
              value: { "error": "Código incorrecto." }
    '500':
      description: Error interno del servidor
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'

components:
  schemas:
    MFARequestDTO:
      type: object
      required: [codigo]
      properties:
        codigo:
          type: string
          minLength: 6
          maxLength: 6
          description: "@NotBlank @Size(min=6, max=6) — 6 dígitos numéricos"
```

**Nota sobre autenticación del endpoint:** El `mfaToken` se valida en `AutenticacionService.verificarMfa` mediante `JwtUtil.esValido`. El endpoint está configurado como público en `SecurityConfig` pero la validación del token se hace manualmente en el servicio.

---

## Modelo de datos

### Tabla `codigo_verificacion` (creada en HU-1)

Ver DDL completo en HU-1 SPEC. En HU-4 se consume el código con `tipo = 'MFA'`.

Flujo de datos:
1. HU-3 crea: `INSERT INTO codigo_verificacion (correo, codigo, tipo, expiracion, usado) VALUES (?, ?, 'MFA', now() + interval, false)`.
2. HU-4 valida y marca: `UPDATE codigo_verificacion SET usado = true WHERE correo = ? AND tipo = 'MFA' AND usado = false AND expiracion > now()`.

---

## Módulos y arquitectura

### Módulos involucrados

| Módulo | Rol | Componentes específicos |
|---|---|---|
| `autenticacion` | Coordinador del flujo | `AuthController`, `AutenticacionService`, `MFAService`, `JwtUtil` |
| `trazabilidad` | Registro de eventos | `AuditLogService` (impl. de `IAuditLog`) |

### Interfaces consumidas en este flujo

| Interfaz | Módulo dueño | Métodos usados | Cuándo |
|---|---|---|---|
| `IAuditLog` | `trazabilidad` | `registrar(TipoEvento, correo, detalle)` | Al completar MFA y al emitir JWT |

### Escenarios de calidad y tácticas materializadas

| EC | Táctica | Cómo se materializa en HU-4 |
|---|---|---|
| EC-10 | Authenticate Actors | Dos factores requeridos: contraseña (HU-3) + código de 6 dígitos (HU-4). JWT solo emitido tras ambas validaciones |
| EC-12 | Audit Trail | `MFA_VERIFICADO` y `LOGIN_EXITOSO` registrados en auditoría |

### Desviaciones arquitectónicas

El endpoint `/api/auth/mfa/verify` acepta el `mfaToken` como Bearer token pero Spring Security no lo valida automáticamente. La validación se hace manualmente en `AutenticacionService.verificarMfa`. El endpoint está excluido del filtro JWT normal en `SecurityConfig`.

---

## Eventos y efectos transversales

### Eventos de auditoría emitidos

| Evento (`TipoEvento`) | Cuándo se emite | Datos en `detalle` |
|---|---|---|
| `MFA_VERIFICADO` | Código validado correctamente | `"MFA validado"` |
| `LOGIN_EXITOSO` | JWT de sesión completa emitido | `"Login via MFA"` |

---

## Riesgos

| # | Riesgo | P | I | Mitigación | Test que lo cubre |
|---|---|:-:|:-:|---|---|
| R1 | Código MFA expirado antes de ingresarlo — el usuario debe reiniciar el login completo | Media | Medio | TTL configurable en `app.seguridad.codigo-expiracion-minutos`; mostrar indicador de tiempo en UI | Manual: esperar expiración y verificar 401 |
| R2 | Sin límite de intentos en `/mfa/verify` — posible fuerza bruta al código de 6 dígitos con `mfaToken` válido | Media | Alto | `mfaToken` expira en 10 min limitando la ventana de ataque. Implementar límite en iteración futura | No hay test de fuerza bruta |

---

## Criterios de verificación

### Escenarios de aceptación (Gherkin)

```gherkin
Funcionalidad: Verificación MFA durante login

  Antecedentes:
    Dado que el backend está corriendo en http://localhost:8080
    Y existe usuario "admin@test.com" con rol="ADMINISTRADOR" y credenciales válidas
    Y el usuario completó POST /api/auth/login y recibió un mfaToken válido
    Y existe codigo_verificacion tipo=MFA para "admin@test.com" con código "482931" vigente

  Escenario: Verificación MFA exitosa
    Cuando se envía POST /api/auth/mfa/verify con { "codigo": "482931" } y Authorization: Bearer <mfaToken>
    Entonces el sistema responde 200 OK
    Y el cuerpo contiene un campo "token" no nulo
    Y el cuerpo contiene "requiereMfa": false
    Y el cuerpo contiene "rol": "ADMINISTRADOR"
    Y codigo_verificacion para "admin@test.com" tiene usado=true
    Y se emite evento MFA_VERIFICADO en auditoría
    Y se emite evento LOGIN_EXITOSO en auditoría

  Escenario: Código MFA incorrecto
    Cuando se envía POST /api/auth/mfa/verify con { "codigo": "999999" } y mfaToken válido
    Entonces el sistema responde 401 Unauthorized
    Y el cuerpo contiene { "error": "Código incorrecto." }

  Escenario: Código MFA expirado
    Dado que codigo_verificacion para "admin@test.com" tiene expiracion en el pasado
    Cuando se envía POST /api/auth/mfa/verify con el código correcto
    Entonces el sistema responde 401 Unauthorized
    Y el cuerpo contiene { "error": "El código ha expirado." }

  Escenario: mfaToken expirado (pasaron más de 10 min desde el login)
    Dado que el mfaToken fue emitido hace más de 10 minutos
    Cuando se envía POST /api/auth/mfa/verify con código correcto
    Entonces el sistema responde 400 Bad Request
    Y el cuerpo contiene { "error": "Token inválido o expirado" }

  Escenario: Código ya utilizado
    Dado que codigo_verificacion para "admin@test.com" tiene usado=true
    Cuando se envía POST /api/auth/mfa/verify con el código correcto
    Entonces el sistema responde 401 Unauthorized
    Y el cuerpo contiene { "error": "Código no encontrado o ya utilizado." }

  Escenario: Sin cabecera Authorization
    Cuando se envía POST /api/auth/mfa/verify sin cabecera Authorization
    Entonces el sistema responde 401 Unauthorized
```

---

## Interfaz de usuario

### Vistas afectadas

| Ruta Angular | Componente | Cambio introducido en HU-4 |
|---|---|---|
| `/mfa/verificar` | `MfaComponent` | Campo de código de 6 dígitos; recupera `mfaToken` de `sessionStorage`; navega a `/dashboard` al completar |

### Estados de la pantalla `/mfa/verificar`

| Estado | Disparador | UI resultante |
|---|---|---|
| Idle | Ruta cargada con `mfaToken` en `sessionStorage` | Campo de código vacío; indicación de que el código llegó al correo |
| Enviando | Usuario ingresa código y presiona "Verificar" | Estado de carga |
| Éxito | 200 OK con JWT | JWT en `localStorage`; elimina `mfaToken`; navega a `/dashboard` |
| Error código | 401 | Mensaje de error bajo el campo |
| Token expirado | 400 con mfaToken expirado | Mensaje "Tu sesión temporal expiró. Inicia sesión de nuevo"; navega a `/login` |

---

## Fuera de alcance

- **Activar/desactivar MFA** — HU-10.
- **Canales MFA alternativos (SMS, WhatsApp)** — No implementado en MVP; solo correo electrónico.
- **Límite de intentos en verificación MFA** — No implementado (riesgo R2).

---

## Decisiones y preguntas abiertas

| # | Pregunta / Decisión | Responsable | Fecha | Estado |
|---|---|---|---|---|
| 1 | No hay límite de intentos para código MFA. ¿Se implementa bloqueo tras N intentos fallidos en `/mfa/verify`? | Juan Diego Triana Mejia | 2026-05-24 | Abierta |
| 2 | **Decisión tomada:** El `mfaToken` no pasa por el filtro JWT estándar — se valida manualmente en `AutenticacionService`. El endpoint está en la lista pública de `SecurityConfig`. | Juan Diego Triana Mejia | 2026-05-20 | Resuelta |

---

## Definición de terminado

- [x] `POST /api/auth/mfa/verify` con código válido responde 200 con JWT de sesión completa.
- [x] `POST /api/auth/mfa/verify` con código incorrecto responde 401.
- [x] `POST /api/auth/mfa/verify` con código expirado responde 401.
- [x] `POST /api/auth/mfa/verify` con `mfaToken` expirado responde 400.
- [x] `codigo_verificacion` queda con `usado=true` tras verificación exitosa.
- [x] Eventos `MFA_VERIFICADO` y `LOGIN_EXITOSO` en auditoría.
- [x] Frontend navega a `/dashboard` tras completar MFA.
- [x] Frontend navega a `/login` si `mfaToken` expiró.
- [x] `docs/PROGRESO.md` marcado con ✅ para HU-4.

---

## Historial de cambios

| Versión | Fecha | Descripción | Razón |
|---|---|---|---|
| 1.0 | 2026-05-24 | Refactorización a estructura SDD del proyecto. Spec narrativo reemplazado con contrato de API, flujos de error individualizados, criterios Gherkin y decisiones documentadas. | Unificación de todos los SPEC.md bajo plantilla canónica SDD del proyecto |
