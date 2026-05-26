# Tareas — HU-11: Contratar suscripción premium (Stripe)

> Estado general: **COMPLETADA**

---

## Checklist de implementación

### Dependencias y configuración

- [x] **`pom.xml`** — dependencia `com.stripe:stripe-java:24.x` añadida
- [x] **`application.properties`** — propiedades `app.stripe.secret-key`, `app.stripe.success-url`, `app.stripe.cancel-url` con placeholders documentados

### Modelo de datos

- [x] **Tabla `inversionista`** — campos de suscripción:
  - `es_premium BOOLEAN DEFAULT FALSE`
  - `plan_suscripcion VARCHAR(50) DEFAULT 'BASICO'`
  - `fecha_expiracion_premium TIMESTAMP`

- [x] **Tabla `integracion_inversionista`** — campos Stripe:
  - `stripe_customer_id VARCHAR(255)`
  - `stripe_suscripcion_id VARCHAR(255)`

- [x] **Tabla `usuario`** — campos afectados:
  - `estado_cuenta VARCHAR(50)` — PENDIENTE_VERIFICACION → ACTIVA
  - `mfa_habilitado BOOLEAN` — forzado a true al confirmar premium

### Backend — Módulo `integracion`

- [x] **`StripeAdapter.java`** en `integracion/adaptadores/stripe/`
  - Método `crearCheckoutSession(String correo, String plan, String successUrl, String cancelUrl)` → devuelve URL
  - Método `obtenerSesion(String sessionId)` → devuelve Session con paymentStatus y metadata
  - Método `cancelarSuscripcion(String stripeSuscripcionId)` → usado en HU-12
  - Maneja `StripeException` → lanza `StripeCheckoutException` (excepción propia del proyecto)

- [x] **`OrquestadorSuscripcion.java`** en `integracion/orquestadores/`
  - Método `iniciarSuscripcion(Usuario usuario)` → crea sesión Checkout, retorna URL
  - Método `confirmarPagoCheckout(String sessionId)` → verifica pago, activa cuenta, llama Alpaca, asigna comisionista si aplica
  - Inyecta `StripeAdapter`, `UsuarioRepository`, `InversionistaRepository`, `IAuditLog`, `OrquestadorRegistro`, `AsignacionComisionistaService`

### Backend — Módulo `administracion`

- [x] **`SuscripcionController.java`** en `administracion/controller/`
  - `GET /api/suscripciones/confirmar-checkout?session_id={id}` (sin autenticación)
  - Delega a `OrquestadorSuscripcion.confirmarPagoCheckout(sessionId)`
  - Retorna 200 `RespuestaDTO{mensaje}` o propaga excepciones al `GlobalExceptionHandler`

### Backend — Módulo `autenticacion`

- [x] **`RegistroService.confirmarRegistro`** — modificado para detectar `plan != BASICO`
  - Llama `OrquestadorSuscripcion.iniciarSuscripcion` si plan premium
  - Retorna `ConfirmarRegistroResponseDTO{requierePago: true, stripeCheckoutUrl: url}` si pago requerido
  - Si Stripe falla → `StripeCheckoutException` → 502 + `SUSCRIPCION_PREMIUM_FALLIDA` en auditoría

- [x] **`ConfirmarRegistroResponseDTO.java`** — campos `requierePago` (boolean) y `stripeCheckoutUrl` (String nullable) añadidos

### Auditoría

- [x] Evento `SUSCRIPCION_PREMIUM_INICIADA` emitido en `confirmarPagoCheckout` tras activación exitosa
- [x] Evento `SUSCRIPCION_PREMIUM_FALLIDA` emitido en `RegistroService` cuando Stripe lanza `StripeCheckoutException`

### Frontend (dashboard.html / Angular)

- [x] Wizard de registro fase 4: selector de plan (BASICO, PREMIUM_MENSUAL $12/mes, PREMIUM_ANUAL $120/año)
- [x] Al confirmar código de registro con plan premium: detecta `requierePago: true` en respuesta → redirige a `stripeCheckoutUrl`
- [x] Ruta `/login?session_id=...`: detecta `session_id` en URL → llama `GET /api/suscripciones/confirmar-checkout?session_id=...` → muestra mensaje de activación → navega a dashboard

### Documentación

- [x] `SPEC.md` creado/actualizado
- [x] `plan.md` creado
- [x] `tasks.md` creado (este archivo)
- [x] `docs/PROGRESO.md` marcado con ✅ para HU-11

---

## Criterios de aceptación verificados

| Criterio | Estado |
|---|:-:|
| Registro con plan premium retorna `stripeCheckoutUrl` en respuesta de confirmar registro | ✅ |
| `GET /api/suscripciones/confirmar-checkout` con `session_id` válido activa la cuenta | ✅ |
| `usuario.estado_cuenta = ACTIVA` tras confirmación | ✅ |
| `usuario.mfa_habilitado = true` tras confirmación | ✅ |
| `inversionista.es_premium = true` y `plan_suscripcion` seteado | ✅ |
| `fecha_expiracion_premium` calculada correctamente (1 mes o 1 año) | ✅ |
| `stripe_customer_id` y `stripe_suscripcion_id` almacenados | ✅ |
| Cuenta Alpaca creada tras confirmación de pago | ✅ |
| Stripe no configurado → 502 con mensaje claro | ✅ |
| Evento `SUSCRIPCION_PREMIUM_INICIADA` en audit.log | ✅ |

---

## Archivos modificados / creados

| Archivo | Tipo | Descripción |
|---|---|---|
| `integracion/adaptadores/stripe/StripeAdapter.java` | Nuevo | Wrapper Stripe SDK |
| `integracion/orquestadores/OrquestadorSuscripcion.java` | Nuevo | Orquesta flujo completo de suscripción |
| `administracion/controller/SuscripcionController.java` | Nuevo | Endpoint confirmación checkout |
| `autenticacion/service/RegistroService.java` | Modificado | Detecta plan premium y llama orquestador |
| `autenticacion/dto/ConfirmarRegistroResponseDTO.java` | Modificado | Campos `requierePago` y `stripeCheckoutUrl` añadidos |
| `autenticacion/model/Inversionista.java` | Modificado | Campos premium añadidos |
| `backend/pom.xml` | Modificado | Dependencia Stripe añadida |
| `backend/src/main/resources/application.properties` | Modificado | Variables Stripe añadidas como placeholders |

---

## Deuda técnica / pendiente MVP

| Item | Prioridad | Descripción |
|---|---|---|
| Job de expiración premium | Baja | No hay `@Scheduled` que verifique `fecha_expiracion_premium` y revierta a BASICO. La fecha se almacena pero no se valida automáticamente |
| Webhook `customer.subscription.deleted` | Fuera del MVP | Stripe puede cancelar la suscripción por fallo de pago; sin webhook el sistema no se entera |
