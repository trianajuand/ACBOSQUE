# SPEC — Registro ampliado de inversionista con verificación por correo

---

## Ficha de la historia

| Campo | Valor |
|---|---|
| ID | HU-1 |
| Sprint | 1 |
| Prioridad MoSCoW | Must Have |
| Estado | Completada |
| Épica | Autenticación / Gestión de registro |
| CU asociado | CU-01 |
| Autor | Juan Diego Triana Mejia |
| Creada | 2026-05-20 |
| Última revisión | 2026-05-24 |
| Versión de esta spec | 1.0 |

**Trazabilidad:**

| Tipo | ID | Descripción |
|---|---|---|
| Requerimiento funcional | RF-01 | Registro de inversionistas con datos personales, experiencia e intereses |
| Escenario de calidad | EC-10 | Verificación de segundo factor (código en BD con TTL) |
| Historia que depende de esta | HU-2 | Necesita `alpacaAccountId` del inversionista creado |
| Historia que depende de esta | HU-3 | Necesita cuenta activa para login |
| Historia que depende de esta | HU-4 | Necesita usuario existente para MFA |
| Historia que depende de esta | HU-11 | Suscripción premium iniciada aquí si plan ≠ BASICO |

---

## Historia de usuario

**Como** usuario no registrado,
**quiero** crear una cuenta de inversionista diligenciando mis datos de acceso, identidad, perfil financiero, preferencias y plan en un wizard de 4 fases,
**para** acceder a la plataforma con un perfil completo que me permita operar en los mercados y recibir comunicaciones por mis canales preferidos.

---

## Motivación y contexto

### Por qué existe esta historia

El registro es la puerta de entrada al sistema. Sin una cuenta verificada no es posible ejercitar ningún otro flujo del MVP. La historia materializa la regla dura del proyecto: solo el rol `INVERSIONISTA` se auto-registra; los demás roles son creados por un Administrador. Separa `Usuario` (identidad y acceso) de `Inversionista` (perfil de negocio), manteniendo la cuenta en `PENDIENTE_VERIFICACION` hasta que el inversionista valide el código enviado a su correo, cumpliendo EC-10.

### Dependencias hacia atrás

| Componente | Qué provee | Sin esto... |
|---|---|---|
| SMTP configurado (`application.properties`) | Canal de envío del código de verificación | El código se genera pero el correo no llega; el inversionista no puede confirmar |
| PostgreSQL accesible | Persistencia de `usuario`, `inversionista`, `codigo_verificacion` | El registro falla antes de persistir |
| `PasswordEncoder` (BCrypt) bean activo | Hash de contraseña | Error de inyección en `RegistroService` |
| Stripe placeholder configurado | URL de checkout para planes premium | `confirmarRegistro` lanza `StripeCheckoutException` para planes no BASICO |

### Historias que dependen de esta

| Historia | Qué consume de aquí |
|---|---|
| HU-2 | Cuenta Alpaca creada con `usuario` e `inversionista` tras confirmar registro |
| HU-3 | Usuario con `estado_cuenta = ACTIVA` para autenticarse |
| HU-4 | MFA habilitado/deshabilitado según configuración del registro |
| HU-11 | Flujo Stripe iniciado aquí para `PREMIUM_MENSUAL` / `PREMIUM_ANUAL` |

---

## Actores y precondiciones

### Actores

| Actor | Rol en el sistema | Participación en este flujo |
|---|---|---|
| Usuario no autenticado | Ningún rol previo | Iniciador — completa el wizard y envía el formulario |
| `RegistroService` | Módulo `autenticacion` | Orquesta validación, creación de entidades y generación de código |
| `MFAService` | Módulo `autenticacion` | Genera y persiste el código de 6 dígitos; lo valida al confirmar |
| `DespachadorNotificaciones` | Módulo `integracion` (vía `INotificacion`) | Envía el correo con el código |
| `AuditLogService` | Módulo `trazabilidad` (vía `IAuditLog`) | Registra eventos auditables |
| `OrquestadorRegistro` | Módulo `integracion` | Crea cuenta Alpaca post-confirmación (delegado a HU-2) |
| `OrquestadorSuscripcion` | Módulo `integracion` | Inicia sesión Stripe si plan ≠ BASICO |

### Precondiciones

- El usuario no tiene sesión JWT activa (si la tiene, el frontend redirige a `/dashboard`).
- Las tablas `usuario`, `inversionista` y `codigo_verificacion` existen en PostgreSQL.
- El servidor SMTP está configurado en `application.properties`.

### Postcondiciones del flujo principal (plan BASICO)

- Existe registro en `usuario` con `estado_cuenta = ACTIVA`, `rol = INVERSIONISTA`, contraseña hasheada con BCrypt.
- Existe registro en `inversionista` con `id = usuario.id` (PK compartida).
- Existe registro en `suscripcion` con `id = inversionista.id` y `plan_suscripcion = 'BASICO'`.
- Existe registro en `integracion_inversionista` con `id = inversionista.id`.
- El código de verificación queda marcado `usado = true` en `codigo_verificacion`.
- Evento `REGISTRO_EXITOSO` registrado en auditoría.
- `OrquestadorRegistro.crearCuentaAlpaca(usuario)` fue invocado (ejecución asíncrona, HU-2).
- Si `solicitaComisionista = true`, `IAsignacionComisionista.asignarSiSolicitado(usuario)` fue invocado.

---

## Flujo principal (plan BASICO, correo nuevo)

**Fase 1 — Datos de identidad:**

1. Usuario abre `/registro` en Angular.
2. El sistema muestra el wizard en la fase 1: campos de nombre completo, correo, contraseña, teléfono, tipo de identificación, número de identificación, fecha de nacimiento, país, dirección, ciudad y código postal.
3. Al salir del campo correo, el frontend llama `GET /api/auth/register/email-disponible?correo={correo}`.
4. Si el correo está disponible (`disponible: true`), el botón "Siguiente" se habilita.

**Fase 2 — Perfil financiero:**

5. El sistema muestra intereses de mercado como chips seleccionables (AAPL, MSFT, TSLA, AMZN, GOOGL, NVDA, META, JPM, SONY, RIO.LON), estilo de trading, rango de ingresos y nivel de experiencia.

**Fase 3 — Preferencias:**

6. El sistema muestra tipo de orden por defecto, vista del portafolio (LISTA / GRAFICO), canales de notificación (Email / SMS / WhatsApp) y opción de solicitar comisionista.

**Fase 4 — Plan:**

7. El sistema muestra opciones BASICO, PREMIUM_MENSUAL ($12/mes), PREMIUM_ANUAL ($120/año). Usuario selecciona BASICO.
8. Usuario presiona "Crear cuenta".
9. El frontend envía `POST /api/auth/register/investor` con el cuerpo completo.

**Backend — `iniciarRegistro`:**

10. `RegistroService` verifica que el correo no exista en `usuario` (`existsByCorreo`). Si existe, lanza `EmailAlreadyExistsException`.
11. Normaliza intereses: convierte a mayúsculas, deduplica, limita a 10. Si la lista queda vacía, usa `["AAPL", "MSFT", "TSLA"]`. Los almacena como CSV en `inversionista.intereses_mercado`.
12. Aplica defaults: `pais = "CO"` si ausente; `tipoOrdenDefault = "MARKET"` si ausente; `vistaPortafolio = "LISTA"` si ausente; `plan = "BASICO"` si ausente o nulo.
13. Crea `Usuario` con `estado_cuenta = PENDIENTE_VERIFICACION`, `rol = INVERSIONISTA`, `mfa_habilitado = false`, contraseña hasheada con BCrypt.
14. Crea `Inversionista` con los datos de perfil del DTO (campos de identidad y notificación ahora en `Usuario`). Crea `Suscripcion` con `plan_suscripcion = 'BASICO'`, `es_premium = false`. Crea `IntegracionInversionista` con `alpaca_account_id = null`, `pendiente_cuenta_alpaca = false`. Escribe `tipos_notificacion = "ORDENES,MERCADO,SEGURIDAD"` en `usuario`.
15. `MFAService.generarYGuardarCodigo(correo, REGISTRO)`: elimina cualquier código previo del mismo tipo y correo, genera código de 6 dígitos numéricos con `SecureRandom`, persiste en `codigo_verificacion` con `expiracion = now() + ${app.seguridad.codigo-expiracion-minutos}` y `usado = false`.
16. `INotificacion.enviarCodigoRegistro(correo, nombreCompleto, codigo)` → email con el código.
17. `IAuditLog.registrar(REGISTRO_INICIADO, correo, "Plan seleccionado: BASICO")`.
18. Responde `201 Created` con `RespuestaDTO{mensaje: "Código de verificación enviado a {correo}"}`.

**Frontend — redirección:**

19. El frontend guarda `reg_correo` en `localStorage` y navega a `/verificar-registro`.

**Confirmación del código:**

20. Usuario ingresa el código de 6 dígitos en `/verificar-registro` y presiona "Confirmar".
21. El frontend envía `POST /api/auth/register/confirm` con `{correo, codigo}`.
22. `MFAService.validarCodigo(correo, codigo, REGISTRO)`: busca código no usado, verifica no expirado, verifica coincidencia, marca `usado = true`.
23. `RegistroService` carga `Usuario` y `Inversionista`. Como el plan es BASICO, actualiza `usuario.estado_cuenta = ACTIVA` y guarda.
24. `IAsignacionComisionista.asignarSiSolicitado(usuario)` si `solicita_comisionista = true`.
25. `IAuditLog.registrar(REGISTRO_EXITOSO, correo, "Cuenta activada exitosamente")`.
26. `OrquestadorRegistro.crearCuentaAlpaca(usuario)` (inicio del flujo HU-2).
27. Responde `200 OK` con `ConfirmarRegistroResponseDTO{mensaje: "Cuenta creada exitosamente", requierePago: false}`.

---

## Flujos alternativos

### Alternativo A — Plan premium (PREMIUM_MENSUAL o PREMIUM_ANUAL)

**Condición:** Usuario selecciona plan distinto a BASICO en fase 4.

Pasos 1–19 idénticos, con diferencia en el audit: `REGISTRO_INICIADO` con `"Plan seleccionado: PREMIUM_MENSUAL"` (o `PREMIUM_ANUAL`).

En la confirmación (paso 22–23):
- `RegistroService` detecta `plan ≠ BASICO`.
- Llama a `OrquestadorSuscripcion.iniciarSuscripcion(usuario)` → genera URL de Stripe Checkout.
- **No** activa la cuenta en este punto (el `estado_cuenta` no cambia a `ACTIVA`; Stripe confirma la activación via webhook, HU-11).
- Responde `200 OK` con `ConfirmarRegistroResponseDTO{mensaje: "Correo verificado. Completa el pago para activar tu cuenta premium.", requierePago: true, stripeCheckoutUrl: "https://checkout.stripe.com/..."}`.
- El frontend debe redirigir al usuario a `stripeCheckoutUrl`.

### Alternativo B — Sin intereses seleccionados

**Condición:** `interesesMercado` ausente, nulo o vacío.

Backend asigna `["AAPL", "MSFT", "TSLA"]` como lista por defecto antes de persistir. El flujo continúa normalmente.

---

## Flujos de error

### Error 1 — Correo ya registrado (verificación previa en GET)

| Campo | Valor |
|---|---|
| Condición | `GET /api/auth/register/email-disponible` retorna `disponible: false` |
| Comportamiento frontend | Muestra mensaje "Este correo ya está registrado" en el campo correo; botón "Siguiente" permanece deshabilitado |
| HTTP | 200 (el GET siempre responde 200; el error es semántico: `disponible: false`) |
| Evento de auditoría | Ninguno (consulta de disponibilidad, no es evento auditable) |

### Error 2 — Correo ya registrado (POST directo)

| Campo | Valor |
|---|---|
| Condición | `POST /api/auth/register/investor` con correo existente en `usuario` |
| Excepción Java | `EmailAlreadyExistsException` |
| HTTP | 409 Conflict |
| Cuerpo | `RespuestaDTO{error: "El correo {correo} ya está registrado"}` |
| Estado final | Sin cambios persistidos (la excepción se lanza antes de `save`) |
| Evento de auditoría | Ninguno |

### Error 3 — Campos obligatorios inválidos o ausentes

| Campo | Valor |
|---|---|
| Condición | Falla Bean Validation en `RegistroInversionistaDTO` (campos marcados `@NotBlank`, `@Email`, `@Size`) |
| Excepción Java | `MethodArgumentNotValidException` |
| HTTP | 400 Bad Request |
| Cuerpo | `RespuestaDTO{error: "{campo}: {mensaje de validación}"}` (primer error encontrado) |
| Estado final | Sin cambios persistidos |
| Evento de auditoría | Ninguno |

### Error 4 — Código de verificación no encontrado o ya usado

| Campo | Valor |
|---|---|
| Condición | `POST /api/auth/register/confirm` con código que no existe en BD o ya tiene `usado = true` |
| Excepción Java | `InvalidMfaException("Código no encontrado o ya utilizado.")` |
| HTTP | 401 Unauthorized |
| Cuerpo | `RespuestaDTO{error: "Código no encontrado o ya utilizado."}` |
| Estado final | Sin cambios persistidos |
| Evento de auditoría | Ninguno |

### Error 5 — Código expirado

| Campo | Valor |
|---|---|
| Condición | `now()` es posterior a `codigo_verificacion.expiracion` |
| Excepción Java | `InvalidMfaException("El código ha expirado.")` |
| HTTP | 401 Unauthorized |
| Cuerpo | `RespuestaDTO{error: "El código ha expirado."}` |
| Estado final | Sin cambios persistidos; el código sigue en BD con `usado = false` |
| Evento de auditoría | Ninguno |

### Error 6 — Código incorrecto

| Campo | Valor |
|---|---|
| Condición | El código enviado no coincide con el almacenado en BD |
| Excepción Java | `InvalidMfaException("Código incorrecto.")` |
| HTTP | 401 Unauthorized |
| Cuerpo | `RespuestaDTO{error: "Código incorrecto."}` |
| Estado final | Sin cambios persistidos |
| Evento de auditoría | Ninguno |

### Error 7 — Usuario no encontrado al confirmar

| Campo | Valor |
|---|---|
| Condición | El código es válido pero no existe `Usuario` con ese correo (caso extremo: registro parcial) |
| Excepción Java | `UsuarioNoEncontradoException` |
| HTTP | 404 Not Found |
| Cuerpo | `RespuestaDTO{error: "Usuario no encontrado: {correo}"}` |
| Estado final | Sin cambios en estado de cuenta |
| Evento de auditoría | Ninguno |

### Error 8 — Fallo al iniciar pago Stripe (plan premium)

| Campo | Valor |
|---|---|
| Condición | `OrquestadorSuscripcion.iniciarSuscripcion` lanza excepción (Stripe no configurado, timeout, etc.) |
| Excepción Java | `StripeCheckoutException` |
| HTTP | 502 Bad Gateway |
| Cuerpo | `RespuestaDTO{error: "No se pudo iniciar el pago premium. Revisa la configuracion de Stripe e intenta de nuevo."}` |
| Estado final | Código marcado `usado = true`; cuenta permanece en `PENDIENTE_VERIFICACION` |
| Evento de auditoría | `TipoEvento.SUSCRIPCION_PREMIUM_FALLIDA` con detalle `"Error al crear sesion Stripe: {mensaje}"` |

### Error 9 — Error técnico genérico

| Campo | Valor |
|---|---|
| Condición | Cualquier excepción no manejada (falla BD, NPE, etc.) |
| Excepción Java | `Exception` (manejador catch-all) |
| HTTP | 500 Internal Server Error |
| Cuerpo | `RespuestaDTO{error: "Error interno del servidor"}` |
| Estado final | Rollback automático por `@Transactional` |
| Evento de auditoría | Ninguno (stack trace en consola de aplicación) |

---

## Contrato de API

### Endpoint 1 — `GET /api/auth/register/email-disponible`

```yaml
GET /api/auth/register/email-disponible:
  summary: Verifica si un correo está disponible para registro
  security: []  # Endpoint público
  parameters:
    - name: correo
      in: query
      required: true
      schema:
        type: string
        format: email
      example: "usuario@ejemplo.com"
  responses:
    '200':
      description: Resultado de disponibilidad
      content:
        application/json:
          schema:
            type: object
            properties:
              correo:
                type: string
                format: email
              disponible:
                type: boolean
          examples:
            disponible:
              value: { "correo": "nuevo@ejemplo.com", "disponible": true }
            ocupado:
              value: { "correo": "existente@ejemplo.com", "disponible": false }
    '400':
      description: Correo ausente o formato inválido (@NotBlank @Email en @RequestParam)
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'
```

### Endpoint 2 — `POST /api/auth/register/investor`

```yaml
POST /api/auth/register/investor:
  summary: Inicia el registro de un nuevo inversionista
  security: []  # Endpoint público
  requestBody:
    required: true
    content:
      application/json:
        schema:
          $ref: '#/components/schemas/RegistroInversionistaDTO'
        example:
          nombreCompleto: "Ana Gómez Torres"
          correo: "ana.gomez@correo.com"
          contrasenia: "Segura123"
          tipoIdentificacion: "CC"
          numeroIdentificacion: "1020304050"
          fechaNacimiento: "1992-04-15"
          direccion: "Cra 7 #45-12"
          ciudad: "Bogotá"
          codigoPostal: "110111"
          pais: "CO"
          telefono: "+573101234567"
          nivelExperiencia: "INTERMEDIO"
          interesesMercado: ["AAPL", "TSLA", "NVDA"]
          estiloTrading: "SWING"
          rangoIngresos: "5000000-10000000"
          tipoOrdenDefault: "MARKET"
          vistaPortafolio: "LISTA"
          notificacionEmail: true
          notificacionSms: false
          notificacionWhatsapp: false
          solicitaComisionista: false
          planSuscripcion: "BASICO"
  responses:
    '201':
      description: Registro iniciado; código de verificación enviado por correo
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'
          example:
            mensaje: "Código de verificación enviado a ana.gomez@correo.com"
    '400':
      description: Campos obligatorios ausentes o inválidos (Bean Validation)
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'
          example:
            error: "contrasenia: La contrasenia debe tener al menos 8 caracteres"
    '409':
      description: Correo ya registrado en el sistema
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'
          example:
            error: "El correo ana.gomez@correo.com ya está registrado"
    '500':
      description: Error interno del servidor
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'
          example:
            error: "Error interno del servidor"
```

### Endpoint 3 — `POST /api/auth/register/confirm`

```yaml
POST /api/auth/register/confirm:
  summary: Confirma el registro con el código recibido por correo
  security: []  # Endpoint público
  requestBody:
    required: true
    content:
      application/json:
        schema:
          type: object
          required: [correo, codigo]
          properties:
            correo:
              type: string
              format: email
              description: "@NotBlank @Email"
            codigo:
              type: string
              minLength: 6
              maxLength: 6
              description: "@NotBlank @Size(min=6, max=6) — 6 dígitos numéricos"
        example:
          correo: "ana.gomez@correo.com"
          codigo: "482931"
  responses:
    '200':
      description: Cuenta confirmada (BASICO) o URL de pago generada (premium)
      content:
        application/json:
          schema:
            type: object
            properties:
              mensaje:
                type: string
              requierePago:
                type: boolean
              stripeCheckoutUrl:
                type: string
                nullable: true
          examples:
            basicoExitoso:
              value:
                mensaje: "Cuenta creada exitosamente"
                requierePago: false
                stripeCheckoutUrl: null
            premiumPendientePago:
              value:
                mensaje: "Correo verificado. Completa el pago para activar tu cuenta premium."
                requierePago: true
                stripeCheckoutUrl: "https://checkout.stripe.com/pay/cs_test_..."
    '400':
      description: Campos inválidos en la solicitud (Bean Validation)
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'
    '401':
      description: Código no encontrado, expirado, ya usado o incorrecto (InvalidMfaException)
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
    '404':
      description: Usuario no encontrado (caso extremo de registro parcial)
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'
    '502':
      description: Fallo al crear sesión Stripe (plan premium)
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'
          example:
            error: "No se pudo iniciar el pago premium. Revisa la configuracion de Stripe e intenta de nuevo."
    '500':
      description: Error interno del servidor
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'

components:
  schemas:
    RespuestaDTO:
      type: object
      properties:
        mensaje:
          type: string
          nullable: true
        error:
          type: string
          nullable: true
        data:
          nullable: true
    RegistroInversionistaDTO:
      type: object
      required: [nombreCompleto, correo, contrasenia, tipoIdentificacion, numeroIdentificacion, direccion, ciudad, codigoPostal]
      properties:
        nombreCompleto:
          type: string
          description: "@NotBlank"
        correo:
          type: string
          format: email
          description: "@NotBlank @Email"
        contrasenia:
          type: string
          minLength: 8
          description: "@NotBlank @Size(min=8)"
        tipoIdentificacion:
          type: string
          description: "@NotBlank — CC, CE, PASAPORTE (validación semántica solo en frontend)"
        numeroIdentificacion:
          type: string
          description: "@NotBlank"
        fechaNacimiento:
          type: string
          description: "Opcional. Formato libre (String en backend)"
        direccion:
          type: string
          description: "@NotBlank"
        ciudad:
          type: string
          description: "@NotBlank"
        codigoPostal:
          type: string
          description: "@NotBlank"
        pais:
          type: string
          description: "Opcional. Default: 'CO'"
        nivelExperiencia:
          type: string
          description: "Opcional. PRINCIPIANTE, INTERMEDIO, AVANZADO"
        interesesMercado:
          type: array
          items:
            type: string
          description: "Opcional. Se normalizan a mayúsculas, deduplicados, máx 10. Default: [AAPL, MSFT, TSLA]"
        telefono:
          type: string
          description: "Opcional"
        estiloTrading:
          type: string
          description: "Opcional. DAY_TRADING, SWING, LARGO_PLAZO"
        rangoIngresos:
          type: string
          description: "Opcional. Rango de ingresos mensuales como String (ej. '5000000-10000000'). No se persiste actualmente (DT-12)."
        tipoOrdenDefault:
          type: string
          description: "Opcional. Default: 'MARKET'"
        vistaPortafolio:
          type: string
          description: "Opcional. LISTA o GRAFICO. Default: 'LISTA'"
        notificacionEmail:
          type: boolean
          description: "Default: false si ausente"
        notificacionSms:
          type: boolean
          description: "Default: false si ausente"
        notificacionWhatsapp:
          type: boolean
          description: "Default: false si ausente"
        solicitaComisionista:
          type: boolean
          description: "Si true, se asigna comisionista automáticamente al confirmar registro"
        planSuscripcion:
          type: string
          description: "BASICO, PREMIUM_MENSUAL, PREMIUM_ANUAL. Default: 'BASICO'"
```

**Tabla de validaciones server-side (`@Valid` en `RegistroInversionistaDTO`):**

| Campo | Restricción | Mensaje de error |
|---|---|---|
| `nombreCompleto` | `@NotBlank` | "El nombre completo es obligatorio" |
| `correo` | `@NotBlank @Email` | "El correo es obligatorio" / "Formato de correo invalido" |
| `contrasenia` | `@NotBlank @Size(min=8)` | "La contrasenia es obligatoria" / "La contrasenia debe tener al menos 8 caracteres" |
| `tipoIdentificacion` | `@NotBlank` | "El tipo de identificacion es obligatorio" |
| `numeroIdentificacion` | `@NotBlank` | "El numero de identificacion es obligatorio" |
| `direccion` | `@NotBlank` | "La direccion es obligatoria" |
| `ciudad` | `@NotBlank` | "La ciudad es obligatoria" |
| `codigoPostal` | `@NotBlank` | "El codigo postal es obligatorio" |

**Validaciones en `ConfirmarRegistroDTO`:**

| Campo | Restricción | Mensaje de error |
|---|---|---|
| `correo` | `@NotBlank @Email` | "El correo es obligatorio" / "Formato de correo inválido" |
| `codigo` | `@NotBlank @Size(min=6, max=6)` | "El código de verificación es obligatorio" / "El código debe tener 6 dígitos" |

---

## Modelo de datos

### Tabla `usuario` (nueva en Sprint 1)

```sql
CREATE TABLE usuario (
    id                        BIGSERIAL PRIMARY KEY,
    nombre_completo           VARCHAR(255) NOT NULL,
    correo                    VARCHAR(255) NOT NULL UNIQUE,
    contrasenia               VARCHAR(255) NOT NULL,  -- hash BCrypt, ~60 chars
    rol                       VARCHAR(50)  NOT NULL,  -- 'INVERSIONISTA'
    estado_cuenta             VARCHAR(50)  NOT NULL,  -- PENDIENTE_VERIFICACION → ACTIVA
    mfa_habilitado            BOOLEAN      NOT NULL DEFAULT FALSE,
    telefono                  VARCHAR(50),
    tipo_identificacion       VARCHAR(50),
    numero_identificacion     VARCHAR(100),
    fecha_nacimiento          VARCHAR(50),
    notificaciones_activas    BOOLEAN      NOT NULL DEFAULT TRUE,
    notificacion_email        BOOLEAN      NOT NULL DEFAULT TRUE,
    notificacion_sms          BOOLEAN      NOT NULL DEFAULT FALSE,
    notificacion_whatsapp     BOOLEAN      NOT NULL DEFAULT FALSE,
    tipos_notificacion        VARCHAR(500) DEFAULT 'ORDENES,MERCADO,SEGURIDAD',
    fecha_creacion            TIMESTAMP    NOT NULL,
    fecha_actualizacion       TIMESTAMP
);

CREATE UNIQUE INDEX idx_usuario_correo ON usuario (correo);
```

**Decisiones de esquema:**
- `correo UNIQUE`: garantiza a nivel BD que dos usuarios no compartan correo, como segunda línea de defensa tras la verificación en `RegistroService`.
- `contrasenia VARCHAR(255)`: BCrypt produce hashes de ~60 chars; 255 da margen para cambio de algoritmo futuro.
- `estado_cuenta` almacenado como `VARCHAR` mapeado al enum `EstadoCuenta` por JPA. Valores posibles: `PENDIENTE_VERIFICACION`, `ACTIVA`, `SUSPENDIDA`, `RESTRINGIDA`, `ELIMINADA`.
- `telefono`, `tipo_identificacion`, `numero_identificacion`, `fecha_nacimiento`: campos de identidad movidos a `usuario` en la normalización 3NF para evitar duplicación entre roles.
- Las preferencias de notificación (`notificaciones_activas`, `notificacion_email`, `notificacion_sms`, `notificacion_whatsapp`, `tipos_notificacion`) residen en `usuario` para ser accesibles por todos los módulos sin cruzar fronteras hacia `inversionista`.

### Tabla `inversionista` (nueva en Sprint 1 — PK compartida con `usuario`)

```sql
CREATE TABLE inversionista (
    id                      BIGINT PRIMARY KEY REFERENCES usuario(id),  -- PK compartida, no auto-generada
    nivel_experiencia       VARCHAR(50),
    intereses_mercado       VARCHAR(500),  -- CSV: "AAPL,MSFT,TSLA"
    direccion               VARCHAR(255),
    ciudad                  VARCHAR(100),
    codigo_postal           VARCHAR(20),
    pais                    VARCHAR(10)  DEFAULT 'CO',
    estilo_trading          VARCHAR(50),
    tipo_orden_default      VARCHAR(50)  DEFAULT 'MARKET',
    vista_portafolio        VARCHAR(50)  DEFAULT 'LISTA',
    solicita_comisionista   BOOLEAN      DEFAULT FALSE,
    fecha_creacion          TIMESTAMP    NOT NULL,
    fecha_actualizacion     TIMESTAMP
);
```

**Decisiones de esquema:**
- `id` es PK compartida con `usuario.id` (patrón shared PK). No hay columna `usuario_id` separada; el `id` del inversionista ES el mismo `id` del usuario.
- `intereses_mercado VARCHAR(500)`: almacenado como CSV por decisión de implementación. No es un catálogo relacional. Máx 10 símbolos de hasta ~10 chars cada uno → cabe bien en 500. Nota: esto facilita el desarrollo pero limita consultas por interés individual.
- `rango_ingresos` no existe como columna en la entidad `Inversionista` actual; el DTO acepta el campo `rangoIngresos` (String) pero no se persiste. Deuda técnica DT-12.
- Campos de identidad personal (`telefono`, `tipo_identificacion`, etc.) y preferencias de notificación han sido movidos a `usuario` (normalización 3NF).

### Tabla `suscripcion` (nueva — datos premium del inversionista)

```sql
CREATE TABLE suscripcion (
    id                    BIGINT PRIMARY KEY REFERENCES inversionista(id),  -- PK compartida
    plan_suscripcion      VARCHAR(50)  NOT NULL DEFAULT 'BASICO',
    es_premium            BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha_expiracion      TIMESTAMP,
    fecha_creacion        TIMESTAMP    NOT NULL,
    fecha_actualizacion   TIMESTAMP
);
```

### Tabla `integracion_inversionista` (nueva — IDs de sistemas externos)

```sql
CREATE TABLE integracion_inversionista (
    id                      BIGINT PRIMARY KEY REFERENCES inversionista(id),  -- PK compartida
    alpaca_account_id       VARCHAR(255),
    pendiente_cuenta_alpaca BOOLEAN      NOT NULL DEFAULT FALSE,
    stripe_customer_id      VARCHAR(255),
    stripe_suscripcion_id   VARCHAR(255),
    fecha_creacion          TIMESTAMP    NOT NULL,
    fecha_actualizacion     TIMESTAMP
);
```

**Decisiones de esquema:**
- `integracion_inversionista` separa los IDs de sistemas externos (Alpaca, Stripe) del perfil de negocio. Facilita agregar nuevas integraciones sin modificar `inversionista`.

### Tabla `codigo_verificacion` (nueva en Sprint 1)

```sql
CREATE TABLE codigo_verificacion (
    id          BIGSERIAL PRIMARY KEY,
    correo      VARCHAR(255) NOT NULL,
    codigo      VARCHAR(10)  NOT NULL,
    tipo        VARCHAR(50)  NOT NULL,  -- REGISTRO, MFA, RECUPERACION_PASSWORD
    expiracion  TIMESTAMP    NOT NULL,
    usado       BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_codigo_verificacion_correo_tipo ON codigo_verificacion (correo, tipo);
```

**Decisiones de esquema:**
- Códigos persistidos en BD (no en `ConcurrentHashMap` en memoria). Razón: un mapa en memoria no escala si se añaden réplicas del backend; BD garantiza consistencia (decisión arquitectónica del proyecto, mejora sobre Malwatcher).
- TTL: `expiracion = now() + ${app.seguridad.codigo-expiracion-minutos}`. El valor por defecto es 10 minutos para códigos de registro y MFA.
- Al generar un nuevo código se elimina el anterior del mismo tipo y correo (`deleteByCorreoAndTipo`) para evitar acumulación.

---

## Módulos y arquitectura

### Módulos involucrados

| Módulo | Rol | Componentes específicos |
|---|---|---|
| `autenticacion` | Coordinador del flujo | `RegistroController`, `RegistroService`, `MFAService`, `IAsignacionComisionista` |
| `integracion` | Notificaciones y orquestación externa | `DespachadorNotificaciones` (impl. de `INotificacion`), `OrquestadorRegistro`, `OrquestadorSuscripcion` |
| `trazabilidad` | Registro de eventos | `AuditLogService` (impl. de `IAuditLog`) |

### Interfaces consumidas en este flujo

| Interfaz | Módulo dueño | Métodos usados | Cuándo |
|---|---|---|---|
| `INotificacion` | `integracion` | `enviarCodigoRegistro(correo, nombreCompleto, codigo)` | Tras generar código en `iniciarRegistro` |
| `IAuditLog` | `trazabilidad` | `registrar(TipoEvento, correo, detalle)` | Al iniciar registro, al confirmar y al fallar Stripe |
| `IAsignacionComisionista` | `autenticacion` | `asignarSiSolicitado(usuario)` | Al confirmar registro si `solicita_comisionista = true` |

### Interfaces nuevas o modificadas

Ninguna. Esta historia no expone interfaces nuevas hacia otros módulos. `RegistroController` es un endpoint público del módulo `autenticacion`.

### Escenarios de calidad y tácticas materializadas

| EC | Táctica | Cómo se materializa en HU-1 |
|---|---|---|
| EC-10 | Authenticate Actors | Código de 6 dígitos generado con `SecureRandom`, persistido en BD con TTL; el inversionista solo activa su cuenta si lo valida en `/verificar-registro` |
| (implícito) | BCrypt / Protect Data | Contraseña nunca almacenada en texto plano; `PasswordEncoder.encode()` en `iniciarRegistro` |
| (implícito) | Secrets en `application.properties` | TTL de código en `app.seguridad.codigo-expiracion-minutos`, no hardcodeado |

### Desviaciones arquitectónicas

`OrquestadorRegistro` y `OrquestadorSuscripcion` son clases concretas dentro de `integracion/orquestadores/`, no interfaces. Son invocados directamente desde `RegistroService` (módulo `autenticacion`). Esto representa un cruce de frontera de módulo por referencia directa a clase de implementación, no por interfaz `I...`.

**Justificación:** Los orquestadores actúan como servicios de infraestructura sin estado y su uso está restringido a este flujo específico. Extraerlos a interfaces implicaría boilerplate sin beneficio inmediato dado el scope académico.

**Plan de resolución:** En una evolución futura, crear `IOrquestadorRegistro` e `IOrquestadorSuscripcion` en `integracion/interfaces/` para que `autenticacion` no dependa de implementaciones de otro módulo.

---

## Eventos y efectos transversales

### Eventos de auditoría emitidos

| Evento (`TipoEvento`) | Cuándo se emite | Datos en `detalle` |
|---|---|---|
| `REGISTRO_INICIADO` | `iniciarRegistro` completado sin errores | `"Plan seleccionado: {plan}"` |
| `REGISTRO_EXITOSO` | `confirmarRegistro` activa la cuenta (plan BASICO) | `"Cuenta activada exitosamente"` |
| `SUSCRIPCION_PREMIUM_FALLIDA` | `OrquestadorSuscripcion` lanza excepción | `"Error al crear sesion Stripe: {mensaje}"` |

### Notificaciones enviadas

| Trigger | Canal | Método `INotificacion` | Contenido |
|---|---|---|---|
| `iniciarRegistro` exitoso | Email (SMTP) | `enviarCodigoRegistro(correo, nombreCompleto, codigo)` | Código de 6 dígitos numéricos con instrucciones para confirmar registro |

### Llamadas a sistemas externos

| Sistema | Componente | Cuándo | Manejo de fallo |
|---|---|---|---|
| SMTP / Gmail | `EmailSender` vía `DespachadorNotificaciones` | Al enviar código en `iniciarRegistro` | Si falla, el email no llega; la cuenta queda en `PENDIENTE_VERIFICACION` indefinidamente (riesgo R1) |
| Stripe | `OrquestadorSuscripcion` | `confirmarRegistro` con plan premium | Lanza `StripeCheckoutException` → HTTP 502; código marcado `usado = true` |
| Alpaca | `OrquestadorRegistro` | Tras `confirmarRegistro` exitoso (BASICO) | Delegado a HU-2; fallo manejado en ese flujo |

### Cambios en caché u otros estados compartidos

No aplica. Este flujo no modifica `precio_cache` ni ningún otro estado compartido.

---

## Riesgos

| # | Riesgo | P | I | Mitigación | Test que lo cubre |
|---|---|:-:|:-:|---|---|
| R1 | SMTP falla al enviar código → usuario queda atrapado en `PENDIENTE_VERIFICACION` sin poder confirmar | Media | Alto | El registro en BD sí ocurre; el usuario puede solicitar reenvío (flujo no implementado actualmente — pregunta abierta #1) | Manual: configurar SMTP inválido y verificar que el registro se crea pero el correo no llega |
| R2 | Race condition de correo duplicado: dos requests simultáneos con el mismo correo pasan la verificación `existsByCorreo` antes de que el primero persista | Baja | Bajo | La constraint `UNIQUE` en `usuario.correo` lanza `DataIntegrityViolationException`, no manejada explícitamente → cae en handler genérico 500. No mapeada a 409. | Pendiente — no hay test de concurrencia |
| R3 | Stripe no configurado (`TU_STRIPE_SECRET_KEY_TEST` placeholder) → `confirmarRegistro` con plan premium falla siempre con 502 | Alta (dev) | Medio | Solo afecta planes premium. Configurar credenciales Stripe test para Sprint 5/6. Marcado en PROGRESO.md | Manual: registrar con plan PREMIUM_MENSUAL y verificar 502 |
| R4 | `intereses_mercado` almacenado como CSV. Si un símbolo contiene coma, se corrompería el parsing posterior | Baja | Bajo | Los símbolos provienen de una lista controlada en el frontend (no hay entrada libre en producción normal) | No hay test; mitigación es la lista cerrada del frontend |
| R5 | El código de verificación expira antes de que el usuario lo ingrese (si el TTL es muy corto o hay retraso del correo) | Baja | Medio | TTL configurable en `app.seguridad.codigo-expiracion-minutos` (default 10 min). En caso de expiración, el usuario debe solicitar nuevo registro (no hay reenvío de código — pregunta abierta #1) | Manual: reducir TTL a 0 en properties, intentar confirmar |

---

## Criterios de verificación

### Escenarios de aceptación (Gherkin)

```gherkin
Funcionalidad: Registro ampliado de inversionista

  Antecedentes:
    Dado que el backend está corriendo en http://localhost:8080
    Y la base de datos PostgreSQL está accesible y vacía de usuarios de prueba
    Y el servidor SMTP está configurado y accesible

  Escenario: Verificar disponibilidad de correo nuevo
    Dado que el correo "nuevo@test.com" no existe en la tabla usuario
    Cuando se consulta GET /api/auth/register/email-disponible?correo=nuevo@test.com
    Entonces el sistema responde 200 OK con { "correo": "nuevo@test.com", "disponible": true }

  Escenario: Verificar correo ya registrado
    Dado que existe un usuario con correo "existente@test.com" en la tabla usuario
    Cuando se consulta GET /api/auth/register/email-disponible?correo=existente@test.com
    Entonces el sistema responde 200 OK con { "correo": "existente@test.com", "disponible": false }

  Escenario: Registro exitoso con plan BASICO
    Dado que no existe usuario con correo "ana@test.com"
    Cuando se envía POST /api/auth/register/investor con datos válidos y planSuscripcion="BASICO"
    Entonces el sistema responde 201 Created
    Y el cuerpo contiene { "mensaje": "Código de verificación enviado a ana@test.com" }
    Y existe un registro en usuario con correo="ana@test.com" y estado_cuenta="PENDIENTE_VERIFICACION"
    Y existe un registro en inversionista vinculado al usuario creado
    Y existe un registro en codigo_verificacion con tipo="REGISTRO", usado=false para "ana@test.com"
    Y la contrasenia en usuario NO es texto plano (empieza con "$2a$")
    Y se emite evento REGISTRO_INICIADO en auditoría

  Escenario: Confirmación exitosa del código — plan BASICO
    Dado que existe usuario "ana@test.com" en PENDIENTE_VERIFICACION con código de registro "123456" vigente
    Cuando se envía POST /api/auth/register/confirm con { "correo": "ana@test.com", "codigo": "123456" }
    Entonces el sistema responde 200 OK con { "mensaje": "Cuenta creada exitosamente", "requierePago": false }
    Y el usuario "ana@test.com" tiene estado_cuenta="ACTIVA"
    Y el registro en codigo_verificacion tiene usado=true
    Y se emite evento REGISTRO_EXITOSO en auditoría

  Escenario: Correo duplicado bloquea registro en POST
    Dado que existe un usuario con correo "existente@test.com"
    Cuando se envía POST /api/auth/register/investor con correo="existente@test.com" y demás campos válidos
    Entonces el sistema responde 409 Conflict
    Y el cuerpo contiene { "error": "El correo existente@test.com ya está registrado" }
    Y no se crea ningún registro nuevo en usuario ni inversionista

  Escenario: Código expirado rechaza confirmación
    Dado que existe código "654321" para "ana@test.com" con expiracion en el pasado
    Cuando se envía POST /api/auth/register/confirm con { "correo": "ana@test.com", "codigo": "654321" }
    Entonces el sistema responde 401 Unauthorized
    Y el cuerpo contiene { "error": "El código ha expirado." }
    Y el usuario sigue en PENDIENTE_VERIFICACION

  Escenario: Código incorrecto rechaza confirmación
    Dado que existe código "123456" válido para "ana@test.com"
    Cuando se envía POST /api/auth/register/confirm con { "correo": "ana@test.com", "codigo": "999999" }
    Entonces el sistema responde 401 Unauthorized
    Y el cuerpo contiene { "error": "Código incorrecto." }

  Escenario: Intereses vacíos usan defaults
    Cuando se envía POST /api/auth/register/investor con interesesMercado=[] y demás campos válidos
    Entonces el sistema responde 201 Created
    Y el registro en inversionista tiene intereses_mercado="AAPL,MSFT,TSLA"

  Esquema del escenario: Validación de campos obligatorios ausentes
    Cuando se envía POST /api/auth/register/investor sin el campo <campo>
    Entonces el sistema responde 400 Bad Request
    Y el cuerpo contiene { "error": "<campo>: <mensaje>" }

    Ejemplos:
      | campo                | mensaje                                         |
      | nombreCompleto       | El nombre completo es obligatorio               |
      | correo               | El correo es obligatorio                        |
      | contrasenia          | La contrasenia es obligatoria                   |
      | tipoIdentificacion   | El tipo de identificacion es obligatorio        |
      | numeroIdentificacion | El numero de identificacion es obligatorio      |
      | direccion            | La direccion es obligatoria                     |
      | ciudad               | La ciudad es obligatoria                        |
      | codigoPostal         | El codigo postal es obligatorio                 |

  Escenario: Contraseña menor a 8 caracteres es rechazada
    Cuando se envía POST /api/auth/register/investor con contrasenia="Corta1" y demás campos válidos
    Entonces el sistema responde 400 Bad Request
    Y el cuerpo contiene { "error": "contrasenia: La contrasenia debe tener al menos 8 caracteres" }

  Escenario: Frontend valida correo antes de avanzar de fase 1
    Dado que el usuario está en la fase 1 del wizard en /registro
    Cuando ingresa un correo ya registrado y pasa al siguiente campo
    Entonces el frontend muestra "Este correo ya está registrado" en el campo correo
    Y el botón "Siguiente" permanece deshabilitado
    Y no se envía el formulario al backend
```

### Criterios no funcionales

| Criterio | Métrica | Cómo se verifica |
|---|---|---|
| Tiempo de respuesta `POST /register/investor` | ≤ 5 s en condiciones normales (EC-02 no aplica directo, pero registro incluye BCrypt + email) | Medición manual con Postman (tiempo de respuesta visible en panel) |
| Contraseña nunca en texto plano en BD | Campo `contrasenia` empieza con `$2a$` (BCrypt) | `SELECT contrasenia FROM usuario LIMIT 1;` en PostgreSQL |
| Código de verificación en BD (no en RAM) | Registro visible en `codigo_verificacion` tras `POST /register/investor` | `SELECT * FROM codigo_verificacion WHERE tipo='REGISTRO';` |

---

## Interfaz de usuario

### Vistas afectadas

| Ruta Angular | Componente | Cambio introducido en HU-1 |
|---|---|---|
| `/registro` | `RegistroComponent` | Wizard de 4 fases completo con ReactiveFormsModule |
| `/verificar-registro` | `VerificarRegistroComponent` | Formulario de código de confirmación (consume `POST /api/auth/register/confirm`) |

### Estados de la pantalla `/registro`

| Estado | Disparador | UI resultante |
|---|---|---|
| Idle fase 1 | Carga de la ruta | Formulario vacío, botón "Siguiente" deshabilitado |
| Correo disponible | `GET email-disponible` retorna `disponible: true` | Campo correo con indicador verde; botón "Siguiente" habilitado |
| Correo ocupado | `GET email-disponible` retorna `disponible: false` | Mensaje de error en el campo correo; botón "Siguiente" deshabilitado |
| Avanzando fases | Usuario completa cada fase | Wizard muestra fase 2, 3, 4 secuencialmente |
| Enviando | Usuario presiona "Crear cuenta" en fase 4 | Estado de carga; botón deshabilitado |
| Éxito | Backend responde 201 | Toast de confirmación; `localStorage.setItem("reg_correo", correo)`; navega a `/verificar-registro` |
| Error 409 | Backend responde 409 | Mensaje de correo duplicado en el campo correspondiente |
| Error 400/500 | Backend responde 4xx/5xx | Toast de error en rojo con el mensaje del backend |

### Estados de la pantalla `/verificar-registro`

| Estado | Disparador | UI resultante |
|---|---|---|
| Idle | Carga de la ruta | Campo de código vacío; correo recuperado de `localStorage` |
| Confirmando | Usuario ingresa código y presiona "Confirmar" | Estado de carga |
| Éxito BASICO | `requierePago: false` | Navega a `/login` con mensaje de éxito |
| Éxito premium | `requierePago: true` | Redirige a `stripeCheckoutUrl` |
| Error código | 401 | Mensaje de error en el campo código |

### Componentes Angular involucrados

- `RegistroComponent` — `frontend/src/app/auth/registro.component.ts` / `.html`
- `VerificarRegistroComponent` — `frontend/src/app/auth/verificar-registro.component.ts` / `.html`
- `ApiService` — `frontend/src/app/core/api.service.ts` — métodos HTTP usados:
  - `get('/api/auth/register/email-disponible', { params: { correo } })`
  - `post('/api/auth/register/investor', payload)`
  - `post('/api/auth/register/confirm', { correo, codigo })`

---

## Fuera de alcance

Esta spec NO cubre:

- **Creación de cuenta Alpaca** — delegado a HU-2 (`OrquestadorRegistro.crearCuentaAlpaca` se llama aquí, pero el flujo completo con reintentos y notificaciones de fallo está especificado en HU-2).
- **Suscripción premium completa con Stripe** — delegado a HU-11 y HU-12. Esta historia inicia la sesión de checkout pero no procesa el webhook ni activa la cuenta premium.
- **Reenvío de código de verificación** — no implementado; el usuario debe repetir el registro (pregunta abierta #1).
- **Roles Comisionista, Administrador y Responsable Legal** — solo se auto-registra `INVERSIONISTA`. Los demás roles son creados por Administrador (HU-36).
- **Login y MFA durante login** — HU-3 y HU-4.
- **Recuperación de contraseña** — SOPORTE-recuperacion-password.
- **Configuración de MFA opcional post-registro** — HU-10.
- **Captcha o rate limiting de registros** — no implementado en MVP.

---

## Decisiones y preguntas abiertas

| # | Pregunta / Decisión | Responsable | Fecha | Estado |
|---|---|---|---|---|
| 1 | No existe endpoint de reenvío de código de verificación. Si el código expira o el correo no llega, el usuario debe repetir el registro completo. ¿Se implementará un `POST /api/auth/register/reenviar-codigo`? | Juan Diego Triana Mejia | 2026-05-24 | Abierta |
| 2 | `intereses_mercado` almacenado como CSV. ¿Se normalizará a tabla relacional en el futuro para soportar filtros por interés? | Juan Diego Triana Mejia | 2026-05-24 | Abierta |
| 3 | Race condition de correo duplicado (R2) cae en 500 en lugar de 409 porque `DataIntegrityViolationException` no está mapeada en `GlobalExceptionHandler`. ¿Se agrega el handler? | Juan Diego Triana Mejia | 2026-05-24 | Abierta |
| 4 | **Decisión tomada:** Códigos de verificación persistidos en BD (no en `ConcurrentHashMap`). Razón: escala con múltiples instancias y sobrevive reinicios del servidor. Mejora explícita sobre proyecto Malwatcher. | Juan Diego Triana Mejia | 2026-05-20 | Resuelta |
| 5 | **Decisión tomada:** `OrquestadorRegistro` y `OrquestadorSuscripcion` invocados directamente (sin interfaz) desde `RegistroService`. Deuda técnica documentada: crear `IOrquestadorRegistro` e `IOrquestadorSuscripcion` post-MVP. | Juan Diego Triana Mejia | 2026-05-22 | Resuelta — deuda pendiente |

---

## Definición de terminado

- [x] `POST /api/auth/register/investor` responde 201 con correo de código enviado.
- [x] `GET /api/auth/register/email-disponible` responde correctamente para correos nuevos y existentes.
- [x] `POST /api/auth/register/confirm` activa la cuenta en `ACTIVA` para plan BASICO.
- [x] `POST /api/auth/register/confirm` devuelve `stripeCheckoutUrl` para planes premium.
- [x] La contraseña se almacena hasheada (BCrypt) en `usuario.contrasenia`.
- [x] El código de verificación se persiste en `codigo_verificacion` con TTL.
- [x] Intereses vacíos usan defaults AAPL, MSFT, TSLA.
- [x] El wizard de 4 fases funciona en Angular con validación por fase.
- [x] El frontend valida disponibilidad del correo antes de avanzar de fase 1.
- [x] El frontend navega a `/verificar-registro` tras POST exitoso.
- [x] Eventos `REGISTRO_INICIADO` y `REGISTRO_EXITOSO` visibles en `logs/audit.log`.
- [ ] Race condition de correo duplicado (R2) devuelve 409 en lugar de 500 (pregunta abierta #3 pendiente).
- [ ] Reenvío de código no implementado (pregunta abierta #1 pendiente).
- [x] `docs/PROGRESO.md` marcado con ✅ para HU-1.

---

## Historial de cambios

| Versión | Fecha | Descripción | Razón |
|---|---|---|---|
| 1.0 | 2026-05-24 | Refactorización a estructura SDD del proyecto. Spec original reemplazado con contratos de API completos, flujos de error individualizados, DDL real, criterios Gherkin, tabla de riesgos y decisiones técnicas documentadas. | Unificación de todos los SPEC.md bajo plantilla canónica SDD del proyecto |
| 1.1 | 2026-05-26 | Auditoría SDD: campo DTO `ingresosMin`/`ingresosMax` → `rangoIngresos` (String, refleja `RegistroInversionistaDTO` real). Columnas `ingresos_min`/`ingresos_max` eliminadas del DDL (no existen en entidad `Inversionista`). Nota de deuda técnica DT-12 agregada. | Código real no tiene estos campos como numéricos en entidad ni DTO. |
