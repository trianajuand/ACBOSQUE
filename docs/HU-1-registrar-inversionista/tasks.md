# Tareas — HU-1: Registro ampliado de inversionista

| Campo | Valor |
|---|---|
| Historia | HU-1 |
| Sprint | 1 |
| Estado | Completada |

---

## Tareas de backend

### Modelo y persistencia

- [x] Crear entidad `Usuario` (`autenticacion/model/Usuario.java`) con campos: `id`, `nombreCompleto`, `correo` (UNIQUE), `contrasenia` (BCrypt), `rol`, `estadoCuenta`, `mfaHabilitado`, `telefono`, `tipoIdentificacion`, `numeroIdentificacion`, `fechaNacimiento`, `notificacionesActivas`, `notificacionEmail`, `notificacionSms`, `notificacionWhatsapp`, `tiposNotificacion`, `fechaCreacion`, `fechaActualizacion`.
- [x] Crear entidad `Inversionista` (`autenticacion/model/Inversionista.java`) con PK compartida con `usuario.id`; campos: `nivelExperiencia`, `interesesMercado` (CSV), `direccion`, `ciudad`, `codigoPostal`, `pais`, `estiloTrading`, `ingresosMin`, `ingresosMax`, `tipoOrdenDefault`, `vistaPortafolio`, `solicitaComisionista`.
- [x] Crear entidad `CodigoVerificacion` (`autenticacion/model/CodigoVerificacion.java`) con campos: `id`, `correo`, `codigo`, `tipo`, `expiracion`, `usado`.
- [x] Crear entidad `Suscripcion` con PK compartida con `inversionista.id`; campos: `planSuscripcion`, `esPremium`, `fechaExpiracion`.
- [x] Crear entidad `IntegracionInversionista` con PK compartida; campos: `alpacaAccountId`, `pendienteCuentaAlpaca`, `stripeCustomerId`, `stripeSuscripcionId`.
- [x] Crear `UsuarioRepository` con `existsByCorreo(String correo)` y `findByCorreo(String correo)`.
- [x] Crear `InversionistaRepository` con `findById(Long id)`.
- [x] Crear `CodigoVerificacionRepository` con `findByCorreoAndTipoAndUsadoFalse(String, String)` y `deleteByCorreoAndTipo(String, String)`.
- [x] Verificar creación de tablas en PostgreSQL: `usuario`, `inversionista`, `suscripcion`, `integracion_inversionista`, `codigo_verificacion`.

### DTOs

- [x] Crear `RegistroInversionistaDTO` con anotaciones Bean Validation en los 8 campos obligatorios (`nombreCompleto`, `correo`, `contrasenia`, `tipoIdentificacion`, `numeroIdentificacion`, `direccion`, `ciudad`, `codigoPostal`).
- [x] Crear `ConfirmarRegistroDTO` con `@NotBlank @Email` en `correo` y `@NotBlank @Size(min=6,max=6)` en `codigo`.
- [x] Crear `ConfirmarRegistroResponseDTO` con `mensaje`, `requierePago`, `stripeCheckoutUrl`.
- [x] Crear `CorreoDisponibleDTO` con `correo` y `disponible`.

### Servicios

- [x] Implementar `MFAService.generarYGuardarCodigo(String correo, String tipo)`: `SecureRandom`, 6 dígitos, elimina código previo del mismo tipo, persiste con TTL de `app.seguridad.codigo-expiracion-minutos`.
- [x] Implementar `MFAService.validarCodigo(String correo, String codigo, String tipo)`: verifica no usado, no expirado, coincidencia; marca `usado = true`.
- [x] Implementar `RegistroService.correoDisponible(String correo)`: delega a `usuarioRepository.existsByCorreo`.
- [x] Implementar `RegistroService.iniciarRegistro(RegistroInversionistaDTO dto)`:
  - [x] Verifica correo único (lanza `EmailAlreadyExistsException` si duplicado).
  - [x] Normaliza intereses: mayúsculas, dedup, límite 10, default `["AAPL","MSFT","TSLA"]` si vacío.
  - [x] Aplica defaults: `pais="CO"`, `tipoOrdenDefault="MARKET"`, `vistaPortafolio="LISTA"`, `plan="BASICO"`.
  - [x] Crea y persiste `Usuario` en `PENDIENTE_VERIFICACION` con contraseña BCrypt.
  - [x] Crea y persiste `Inversionista`, `Suscripcion`, `IntegracionInversionista`.
  - [x] Llama `MFAService.generarYGuardarCodigo(correo, "REGISTRO")`.
  - [x] Llama `INotificacion.enviarCodigoRegistro(correo, nombreCompleto, codigo)`.
  - [x] Audita `REGISTRO_INICIADO` con `"Plan seleccionado: {plan}"`.
  - [x] Responde `201` con mensaje de confirmación.
- [x] Implementar `RegistroService.confirmarRegistro(ConfirmarRegistroDTO dto)`:
  - [x] Llama `MFAService.validarCodigo(correo, codigo, "REGISTRO")`.
  - [x] Carga `Usuario` (lanza `UsuarioNoEncontradoException` si no existe).
  - [x] Para plan BASICO: actualiza `estadoCuenta = ACTIVA`, guarda.
  - [x] Para plan premium: llama `OrquestadorSuscripcion.iniciarSuscripcion(usuario)`, responde con `stripeCheckoutUrl`.
  - [x] Llama `IAsignacionComisionista.asignarSiSolicitado(usuario)` si `solicita_comisionista = true`.
  - [x] Audita `REGISTRO_EXITOSO`.
  - [x] Llama `OrquestadorRegistro.crearCuentaAlpaca(usuario)`.

### Controladores

- [x] Crear `RegistroController` con:
  - [x] `GET /api/auth/register/email-disponible?correo=` → `200 OK` con `CorreoDisponibleDTO`.
  - [x] `POST /api/auth/register/investor` → delega a `RegistroService.iniciarRegistro`, responde `201`.
  - [x] `POST /api/auth/register/confirm` → delega a `RegistroService.confirmarRegistro`, responde `200`.
- [x] Configurar endpoint `POST /api/auth/register/investor` y `POST /api/auth/register/confirm` como públicos en `SecurityConfig`.

### Manejo de errores

- [x] `EmailAlreadyExistsException` mapeada a `409 Conflict` en `GlobalExceptionHandler`.
- [x] `InvalidMfaException` mapeada a `401 Unauthorized`.
- [x] `UsuarioNoEncontradoException` mapeada a `404 Not Found`.
- [x] `StripeCheckoutException` mapeada a `502 Bad Gateway`.
- [x] `MethodArgumentNotValidException` mapeada a `400 Bad Request`.

---

## Tareas de frontend

- [x] Crear `RegistroComponent` (`frontend/src/app/auth/registro.component.ts/.html`):
  - [x] Fase 1: campos de identidad con `FormGroup`; validación de correo disponible al blur (`GET /api/auth/register/email-disponible`); botón "Siguiente" deshabilitado si correo no disponible o campos inválidos.
  - [x] Fase 2: intereses de mercado como chips seleccionables, estilo de trading, rango de ingresos, nivel de experiencia.
  - [x] Fase 3: tipo de orden por defecto, vista del portafolio, canales de notificación, solicitud de comisionista.
  - [x] Fase 4: selección de plan (BASICO, PREMIUM_MENSUAL, PREMIUM_ANUAL).
  - [x] Botón "Crear cuenta" en fase 4: llama `POST /api/auth/register/investor`, guarda `reg_correo` en `localStorage`, navega a `/verificar-registro`.
  - [x] Manejo de errores: `409` muestra mensaje en campo correo; `400/500` muestra toast en rojo.
- [x] Crear `VerificarRegistroComponent` (`frontend/src/app/auth/verificar-registro.component.ts/.html`):
  - [x] Lee `reg_correo` de `localStorage`.
  - [x] Campo de código de 6 dígitos; botón "Confirmar".
  - [x] `POST /api/auth/register/confirm` → si `requierePago: false`, navega a `/login`; si `requierePago: true`, redirige a `stripeCheckoutUrl`.
  - [x] Manejo de errores `401`: mensaje en el campo código.
- [x] Registrar rutas `/registro` y `/verificar-registro` en `app.routes.ts` como rutas públicas.

---

## Tareas de calidad y verificación

- [x] Verificar en BD que `contrasenia` empieza con `$2a$` (BCrypt): `SELECT contrasenia FROM usuario LIMIT 1;`.
- [x] Verificar que `codigo_verificacion` se crea con `usado=false` tras `POST /register/investor`.
- [x] Verificar que `codigo_verificacion.usado = true` tras `POST /register/confirm` exitoso.
- [x] Verificar evento `REGISTRO_INICIADO` en `logs/audit.log`.
- [x] Verificar evento `REGISTRO_EXITOSO` en `logs/audit.log`.
- [x] Probar flujo completo end-to-end: registro → correo → confirmar → login.
- [ ] Agregar handler `DataIntegrityViolationException` → `409` en `GlobalExceptionHandler` (race condition de correo duplicado — pregunta abierta #3).
- [ ] Implementar endpoint de reenvío de código `POST /api/auth/register/reenviar-codigo` (pregunta abierta #1).

---

## Actualización de documentación

- [x] Marcar HU-1 como `✅` en `docs/PROGRESO.md`.
- [x] Registrar decisión de PK compartida y CSV de intereses en `docs/CONVENCIONES.md` o `docs/ARQUITECTURA.md`.
