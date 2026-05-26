# Plan de implementación — HU-11: Contratar suscripción premium (Stripe)

## Contexto

La suscripción premium es el modelo de monetización del sistema. El flujo tiene dos fases: (1) durante el registro, si el inversionista elige un plan de pago, se crea una sesión de Stripe Checkout y se devuelve la URL al frontend; (2) cuando Stripe redirige al usuario de vuelta, el frontend llama al backend para confirmar el pago y activar la cuenta premium. La cuenta NO se activa hasta confirmar el pago.

Esta historia es la que da sentido al campo `planSuscripcion` del wizard de registro (HU-1) y precede lógicamente a la cancelación (HU-12).

---

## Estado

**Completada** — implementación en `OrquestadorSuscripcion` + `StripeAdapter` + `SuscripcionController`. Stripe con clave test en `application.properties`.

---

## Decisiones de diseño

| Decisión | Justificación |
|---|---|
| Flujo en dos fases (checkout → confirmar) | Patrón Stripe Checkout: la URL de pago solo se genera una vez; la confirmación se hace al retornar. No hay webhook en MVP (complejidad adicional sin beneficio académico claro) |
| `OrquestadorSuscripcion` en módulo `integracion` | Orquesta Stripe + activación de usuario + creación de Alpaca + asignación de comisionista. Es cross-módulo por naturaleza → pertenece al módulo de integración (táctica EC-14 Orchestrate) |
| `SuscripcionController` en módulo `administracion` | El endpoint de confirmación es de negocio (no de perfil personal). La suscripción es administrable en el futuro por el admin |
| `confirmarCheckout` sin JWT | El usuario aún no tiene JWT al retornar de Stripe (su cuenta estaba en `PENDIENTE_VERIFICACION`). El `session_id` actúa como token de un solo uso |
| `mfa_habilitado = true` obligatorio al activar premium | EC-10 + regla dura del proyecto: inversionista premium tiene MFA obligatorio |
| Tolerante a Stripe no configurado | En desarrollo, la clave es un placeholder. El sistema devuelve 502 con mensaje claro, registra `SUSCRIPCION_PREMIUM_FALLIDA` y no deja la cuenta en un estado inconsistente |

---

## Módulos involucrados

| Módulo | Componente | Rol |
|---|---|---|
| `autenticacion` | `RegistroService` | Fase 1: llama `OrquestadorSuscripcion` cuando `plan != BASICO` |
| `integracion` | `OrquestadorSuscripcion` | Crea sesión Checkout (fase 1); confirma pago y activa cuenta (fase 2) |
| `integracion` | `StripeAdapter` | Wraps Stripe Java SDK: `crearCheckoutSession`, `obtenerSesion`, `cancelarSuscripcion` |
| `administracion` | `SuscripcionController` | Endpoint `GET /api/suscripciones/confirmar-checkout` |
| `autenticacion` | `UsuarioRepository`, `InversionistaRepository` | Actualiza `estadoCuenta`, `mfaHabilitado`, `esPremium`, etc. |
| `trazabilidad` | `AuditLogService` (vía `IAuditLog`) | Registra `SUSCRIPCION_PREMIUM_INICIADA` y `SUSCRIPCION_PREMIUM_FALLIDA` |

---

## Flujo de implementación

### Fase 1 — Inicio del checkout (en `RegistroService.confirmarRegistro`)

```
POST /api/auth/register/confirmar
  → RegistroService.confirmarRegistro(ConfirmarRegistroDTO)
    → valida código de verificación
    → if plan != BASICO
        → OrquestadorSuscripcion.iniciarSuscripcion(usuario)
          → busca Inversionista
          → StripeAdapter.crearCheckoutSession(correo, plan, successUrl, cancelUrl)
            → stripe.checkout.Session.create({...}) → retorna Session con url
          → if error Stripe → throw StripeCheckoutException → 502 + audit SUSCRIPCION_PREMIUM_FALLIDA
          → return stripeCheckoutUrl
        → return ConfirmarRegistroResponseDTO{requierePago: true, stripeCheckoutUrl: url}
    → else (plan BASICO)
        → activar cuenta directamente
        → return ConfirmarRegistroResponseDTO{requierePago: false}
```

### Fase 2 — Confirmación del pago (endpoint dedicado)

```
GET /api/suscripciones/confirmar-checkout?session_id={id}
  → SuscripcionController.confirmarCheckout(@RequestParam String sessionId)
    → OrquestadorSuscripcion.confirmarPagoCheckout(sessionId)
      → StripeAdapter.obtenerSesion(sessionId) → Session
      → if session.paymentStatus != "paid" → throw IllegalStateException → 400
      → extrae correo del metadata de la sesión
      → carga usuario + inversionista
      → usuario.estadoCuenta = ACTIVA
      → usuario.mfaHabilitado = true
      → inversionista.esPremium = true
      → inversionista.planSuscripcion = plan de la sesión
      → inversionista.fechaExpiracionPremium = now + (plan == MENSUAL ? 1 mes : 1 año)
      → integracionInversionista.stripeCustomerId = session.customer
      → integracionInversionista.stripeSuscripcionId = session.subscription
      → save(usuario), save(inversionista), save(integracionInversionista)
      → OrquestadorRegistro.crearCuentaAlpaca(usuario, inversionista)
      → if inversionista.solicitaComisionista → asignarComisionista(inversionista)
      → IAuditLog.registrar(SUSCRIPCION_PREMIUM_INICIADA, correo, "Suscripción premium confirmada")
      → return RespuestaDTO{mensaje: "Cuenta premium activada para {correo}"}
    → 200 OK con RespuestaDTO
```

---

## Modelo de datos

### Tabla `inversionista` (campos actualizados en HU-11)

| Columna | Tipo SQL | Descripción |
|---|---|---|
| `es_premium` | `BOOLEAN DEFAULT FALSE` | Marcador de cuenta premium activa |
| `plan_suscripcion` | `VARCHAR(50) DEFAULT 'BASICO'` | BASICO, PREMIUM_MENSUAL, PREMIUM_ANUAL |
| `fecha_expiracion_premium` | `TIMESTAMP` | Fecha hasta la que dura el premium pagado |

### Tabla `integracion_inversionista` (campos actualizados en HU-11)

| Columna | Tipo SQL | Descripción |
|---|---|---|
| `stripe_customer_id` | `VARCHAR(255)` | ID del customer en Stripe |
| `stripe_suscripcion_id` | `VARCHAR(255)` | ID de la suscripción activa en Stripe |

### Tabla `usuario` (campos actualizados en HU-11)

| Columna | Tipo SQL | Descripción |
|---|---|---|
| `estado_cuenta` | `VARCHAR(50)` | PENDIENTE_VERIFICACION → ACTIVA tras confirmación |
| `mfa_habilitado` | `BOOLEAN` | Forzado a `true` al activar premium |

---

## Contrato resumido

| Verbo | URL | Auth | Parámetro | Respuesta exitosa |
|---|---|---|---|---|
| GET | `/api/suscripciones/confirmar-checkout` | Ninguna | `?session_id=cs_test_...` | 200 `RespuestaDTO{mensaje}` |

**Códigos de error:**
- `400` — pago no completado o metadata inválida
- `502` — Stripe no configurado (solo en fase 1, al crear sesión)
- `500` — error técnico genérico

---

## Escenarios de calidad cubiertos

| EC | Táctica | Materialización |
|---|---|---|
| EC-14 | Orchestrate | `OrquestadorSuscripcion` coordina Stripe + activación + Alpaca + comisionista en un único flujo controlado |
| EC-12 | Audit Trail | `SUSCRIPCION_PREMIUM_INICIADA` y `SUSCRIPCION_PREMIUM_FALLIDA` registrados |

---

## Configuración requerida en `application.properties`

```properties
# Stripe — reemplazar con clave test real de dashboard.stripe.com
app.stripe.secret-key=TU_STRIPE_SECRET_KEY_TEST
app.stripe.success-url=http://localhost:4200/login?session_id={CHECKOUT_SESSION_ID}
app.stripe.cancel-url=http://localhost:4200/registro
```

---

## Notas para el desarrollador

- **Precio en Stripe:** los Price IDs (`price_mensual`, `price_anual`) deben existir en el panel de Stripe. En desarrollo, crear Products/Prices en modo test y poner los IDs en `application.properties`.
- **Metadata de la sesión:** al crear la sesión Checkout, incluir `{correo: usuario.correo}` en `metadata` para recuperarlo en la confirmación.
- **Idempotencia:** si `confirmarCheckout` se llama dos veces con el mismo `session_id`, la segunda llamada debe ser inocua (el usuario ya está ACTIVA y esPremium=true). Verificar con un `if (usuario.estadoCuenta == ACTIVA)` antes de aplicar cambios.
- **No hay webhook:** en MVP, no se procesa el webhook `customer.subscription.deleted` de Stripe. La cancelación es iniciada desde el frontend (HU-12) o por el admin.
