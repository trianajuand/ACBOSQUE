# SPEC — Suscripción premium mediante Stripe Checkout

---

## Ficha de la historia

| Campo | Valor |
|---|---|
| ID | HU-11 |
| Sprint | 2 |
| Prioridad MoSCoW | Should Have |
| Estado | Completada |
| Épica | Administración / Suscripciones |
| CU asociado | CU-11 |
| Autor | Juan Diego Triana Mejia |
| Creada | 2026-05-20 |
| Última revisión | 2026-05-24 |
| Versión de esta spec | 1.0 |

**Trazabilidad:**

| Tipo | ID | Descripción |
|---|---|---|
| Requerimiento funcional | RF-10 | Suscripción premium con pago integrado via Stripe |
| Escenario de calidad | EC-14 | Orchestrate — integración con Stripe para activación de cuenta |
| Historia que precede a esta | HU-1 | Flujo premium inicia al confirmar registro con plan ≠ BASICO |
| Historia relacionada | HU-12 | Cancelación de la suscripción creada aquí |

---

## Historia de usuario

**Como** inversionista que desea beneficios premium,
**quiero** pagar un plan mensual o anual via Stripe,
**para** activar mi cuenta premium con MFA obligatorio y beneficios adicionales.

---

## Motivación y contexto

### Por qué existe esta historia

La suscripción premium es el modelo de monetización del sistema. El flujo cubre dos partes: (1) la creación de la sesión de Stripe Checkout iniciada al confirmar el registro (HU-1), y (2) la activación de la cuenta premium al retornar de Stripe exitosamente. La cuenta no se activa hasta que el pago sea confirmado.

### Dependencias hacia atrás

| Componente | Qué provee | Sin esto... |
|---|---|---|
| HU-1 (registro con plan premium) | Sesión Stripe iniciada en `confirmarRegistro` | No hay URL de checkout disponible |
| Stripe API configurado (`app.stripe.*`) | Creación de sesión de checkout | `StripeCheckoutException` → 502 en HU-1 |
| Tabla `suscripcion` | Almacena `es_premium`, `plan_suscripcion`, `fecha_expiracion` | No se puede persistir el estado premium |
| Tabla `integracion_inversionista` | Almacena `stripe_customer_id`, `stripe_suscripcion_id` | No se pueden guardar los IDs de Stripe |

---

## Actores y precondiciones

### Actores

| Actor | Rol en el sistema | Participación en este flujo |
|---|---|---|
| Inversionista no activado | Sin rol activo aún | Completa el pago en Stripe |
| `OrquestadorSuscripcion` | Módulo `integracion` | Crea sesión Checkout; confirma pago y activa cuenta |
| `SuscripcionController` | Módulo `administracion` | Endpoint de retorno de Stripe |
| `AuditLogService` | Módulo `trazabilidad` (vía `IAuditLog`) | Registra eventos de suscripción |

### Precondiciones

- Usuario en `PENDIENTE_VERIFICACION` con `plan_suscripcion != BASICO` tras confirmar registro.
- `app.stripe.secret-key` configurado con clave válida.
- URL de retorno configurada (`app.stripe.success-url`).

### Postcondiciones (pago exitoso)

- `usuario.estado_cuenta = ACTIVA`.
- `usuario.mfa_habilitado = true` (MFA obligatorio para premium).
- `suscripcion.es_premium = true`.
- `suscripcion.plan_suscripcion` = plan seleccionado.
- `suscripcion.fecha_expiracion` = ahora + 1 mes (mensual) o 1 año (anual).
- `integracion_inversionista.stripe_customer_id` y `integracion_inversionista.stripe_suscripcion_id` almacenados.
- Cuenta Alpaca creada.
- Comisionista asignado si `solicita_comisionista = true`.
- Evento `SUSCRIPCION_PREMIUM_INICIADA` en auditoría.

---

## Flujo principal

**Fase 1 — Inicio del checkout (en HU-1 `confirmarRegistro`):**

1. Usuario confirmó código de registro con plan `PREMIUM_MENSUAL` o `PREMIUM_ANUAL`.
2. `RegistroService.confirmarRegistro` detecta `plan ≠ BASICO`.
3. Llama `OrquestadorSuscripcion.iniciarSuscripcion(usuario)`.
4. `OrquestadorSuscripcion` busca el `Inversionista`, construye sesión Stripe Checkout.
5. Retorna URL de checkout. `RegistroService` responde 200 con `ConfirmarRegistroResponseDTO{requierePago: true, stripeCheckoutUrl: "https://checkout.stripe.com/pay/..."}`.
6. Frontend redirige al usuario a `stripeCheckoutUrl`.

**Fase 2 — Retorno de Stripe:**

7. Stripe procesa el pago y redirige a `{success-url}?session_id={id}`.
8. Frontend (en `/login` o ruta dedicada) detecta `session_id` en la URL.
9. Frontend llama `GET /api/suscripciones/confirmar-checkout?session_id={id}`.

**Backend — `OrquestadorSuscripcion.confirmarPagoCheckout(sessionId)`:**

10. Consulta la sesión de Stripe por `sessionId`.
11. Verifica `paymentStatus == "paid"`.
12. Extrae correo del usuario del metadata de la sesión.
13. Actualiza `usuario.estadoCuenta = ACTIVA`, `usuario.mfaHabilitado = true`.
14. Actualiza `suscripcion.esPremium = true`, `suscripcion.planSuscripcion`, `suscripcion.fechaExpiracion`. Actualiza `integracionInversionista.stripeCustomerId`, `integracionInversionista.stripeSuscripcionId`.
15. Crea cuenta Alpaca (`OrquestadorRegistro.crearCuentaAlpaca`).
16. Si `solicita_comisionista = true`: asigna comisionista.
17. `IAuditLog.registrar(SUSCRIPCION_PREMIUM_INICIADA, correo, "Suscripción premium confirmada")`.
18. Responde `200 OK` con `RespuestaDTO{mensaje: "Cuenta premium activada para {correo}"}`.

**Frontend:**

19. Muestra mensaje de éxito, navega a `/login`.

---

## Flujos de error

### Error 1 — Stripe no configurado (desarrollo)

| Campo | Valor |
|---|---|
| Condición | `app.stripe.secret-key` con placeholder o inválido al llamar la API de Stripe |
| Excepción Java | `StripeCheckoutException` |
| HTTP | 502 Bad Gateway (en respuesta de `confirmarRegistro`) |
| Cuerpo | `RespuestaDTO{error: "No se pudo iniciar el pago premium. Revisa la configuracion de Stripe e intenta de nuevo."}` |
| Estado final | Código de verificación marcado `usado=true`; cuenta permanece en `PENDIENTE_VERIFICACION` |
| Evento de auditoría | `SUSCRIPCION_PREMIUM_FALLIDA` |

### Error 2 — Pago no completado (`paymentStatus != "paid"`)

| Campo | Valor |
|---|---|
| Condición | `confirmarCheckout` llamado pero el pago no fue completado |
| Excepción Java | `IllegalStateException` |
| HTTP | 400 Bad Request |
| Cuerpo | `RespuestaDTO{error: "El pago no fue completado"}` |
| Estado final | Cuenta permanece en `PENDIENTE_VERIFICACION` |
| Evento de auditoría | Ninguno |

### Error 3 — Inversionista no encontrado al confirmar

| Campo | Valor |
|---|---|
| Condición | `sessionId` sin `correo` en metadata, o correo sin inversionista asociado |
| Excepción Java | `IllegalStateException` |
| HTTP | 400 Bad Request |
| Cuerpo | `RespuestaDTO{error: "..."}` |
| Evento de auditoría | Ninguno |

### Error 4 — `session_id` ausente en query param

| Campo | Valor |
|---|---|
| Condición | `GET /api/suscripciones/confirmar-checkout` sin `session_id` |
| HTTP | 400 Bad Request |
| Cuerpo | Error de Spring MVC (parámetro requerido) |
| Evento de auditoría | Ninguno |

---

## Contrato de API

### Endpoint — `GET /api/suscripciones/confirmar-checkout`

```yaml
GET /api/suscripciones/confirmar-checkout:
  summary: Confirma el pago de Stripe y activa la cuenta premium
  security: []  # Llamado desde frontend tras retorno de Stripe — no requiere JWT
  parameters:
    - name: session_id
      in: query
      required: true
      schema:
        type: string
      description: "ID de sesión de Stripe Checkout"
      example: "cs_test_a1B2c3..."
  responses:
    '200':
      description: Cuenta premium activada exitosamente
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'
          example:
            mensaje: "Cuenta premium activada para ana.gomez@correo.com"
    '400':
      description: Pago no completado o datos de sesión inválidos
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

### Campos en `inversionista` (actualizados en HU-11)

```sql
-- Campos de suscripción premium (ya en tabla desde HU-1):
stripe_customer_id       VARCHAR(255),
stripe_suscripcion_id    VARCHAR(255),
fecha_expiracion_premium TIMESTAMP,
plan_suscripcion         VARCHAR(50) DEFAULT 'BASICO',
es_premium               BOOLEAN DEFAULT FALSE
```

### Campos en `usuario` (actualizados en HU-11)

```sql
estado_cuenta    VARCHAR(50)  -- PENDIENTE_VERIFICACION → ACTIVA
mfa_habilitado   BOOLEAN      -- false → true (premium obliga MFA)
```

---

## Módulos y arquitectura

### Módulos involucrados

| Módulo | Rol | Componentes específicos |
|---|---|---|
| `administracion` | Endpoint de retorno Stripe | `SuscripcionController` |
| `integracion` | Orquestación Stripe | `OrquestadorSuscripcion`, `StripeAdapter` |
| `autenticacion` | Actualización de usuario | `UsuarioRepository`, `InversionistaRepository` |
| `trazabilidad` | Registro de eventos | `AuditLogService` (impl. de `IAuditLog`) |

### Escenarios de calidad y tácticas materializadas

| EC | Táctica | Cómo se materializa en HU-11 |
|---|---|---|
| EC-14 | Orchestrate | `OrquestadorSuscripcion` coordina Stripe + activación + Alpaca + comisionista |
| EC-12 | Audit Trail | `SUSCRIPCION_PREMIUM_INICIADA` y `SUSCRIPCION_PREMIUM_FALLIDA` registrados |

---

## Eventos y efectos transversales

### Eventos de auditoría emitidos

| Evento (`TipoEvento`) | Cuándo se emite | Datos en `detalle` |
|---|---|---|
| `SUSCRIPCION_PREMIUM_INICIADA` | Sesión Checkout creada; también al confirmar pago | `"Suscripción premium iniciada/confirmada"` |
| `SUSCRIPCION_PREMIUM_FALLIDA` | Falla al crear sesión Checkout (en HU-1) | `"Error al crear sesion Stripe: {mensaje}"` |

---

## Riesgos

| # | Riesgo | P | I | Mitigación | Test que lo cubre |
|---|---|:-:|:-:|---|---|
| R1 | `app.stripe.secret-key` es placeholder en desarrollo → todas las operaciones premium fallan con 502 | Alta (dev) | Medio | Usar clave test de Stripe para desarrollo. Documentado en `application.properties` | Manual: verificar 502 con placeholder |
| R2 | Stripe redirige a la URL de éxito pero el pago puede haber fallado — siempre verificar `paymentStatus` en backend | Baja | Alto | `confirmarPagoCheckout` verifica explícitamente `paymentStatus == "paid"` | Manual: simular sesión sin pago completado |
| R3 | Cuenta premium sin fecha de expiración verificada automáticamente — no hay job que desactive premium expirado | Media | Medio | `fecha_expiracion_premium` almacenada; job de verificación no implementado en MVP | No hay test |

---

## Criterios de verificación

### Escenarios de aceptación (Gherkin)

```gherkin
Funcionalidad: Suscripción premium con Stripe

  Antecedentes:
    Dado que Stripe está configurado con clave test válida

  Escenario: Registro con plan premium inicia checkout
    Dado que usuario completa registro con planSuscripcion="PREMIUM_MENSUAL"
    Cuando confirma el código de registro
    Entonces la respuesta contiene "requierePago": true
    Y la respuesta contiene "stripeCheckoutUrl" no nulo

  Escenario: Confirmación de pago activa la cuenta
    Dado que existe sesión Stripe con session_id="cs_test_123" y paymentStatus="paid"
    Y la sesión tiene metadata con correo="ana@test.com"
    Cuando se consulta GET /api/suscripciones/confirmar-checkout?session_id=cs_test_123
    Entonces el sistema responde 200 OK
    Y usuario.estado_cuenta para "ana@test.com" es "ACTIVA"
    Y usuario.mfa_habilitado para "ana@test.com" es true
    Y inversionista.es_premium para "ana@test.com" es true
    Y se emite evento SUSCRIPCION_PREMIUM_INICIADA en auditoría

  Escenario: Stripe no configurado retorna 502 al confirmar registro premium
    Dado que app.stripe.secret-key es un placeholder inválido
    Cuando usuario confirma registro con plan premium
    Entonces la respuesta es 502 Bad Gateway
    Y se emite evento SUSCRIPCION_PREMIUM_FALLIDA en auditoría
```

---

## Fuera de alcance

- **Renovación automática de suscripción** — manejada por Stripe; no hay webhook procesado en MVP.
- **Cancelación** — HU-12.
- **Vencimiento automático de premium** — job no implementado en MVP.

---

## Decisiones y preguntas abiertas

| # | Pregunta / Decisión | Responsable | Fecha | Estado |
|---|---|---|---|---|
| 1 | No hay job periódico que verifique `fecha_expiracion_premium` y desactive premium vencido. ¿Se implementa en sprint posterior? | Juan Diego Triana Mejia | 2026-05-24 | Abierta |
| 2 | **Decisión tomada:** MFA se activa obligatoriamente al confirmarse el pago premium (`usuario.mfaHabilitado = true`). No se puede desactivar con HU-10 ya que HU-10 solo aplica a INVERSIONISTA no premium. | Juan Diego Triana Mejia | 2026-05-22 | Resuelta |

---

## Definición de terminado

- [x] Registro con plan premium retorna `stripeCheckoutUrl` en respuesta de confirmar registro.
- [x] `GET /api/suscripciones/confirmar-checkout` activa cuenta, habilita MFA y marca premium.
- [x] `fecha_expiracion_premium` calculada correctamente (1 mes o 1 año).
- [x] `stripe_customer_id` y `stripe_suscripcion_id` almacenados.
- [x] Cuenta Alpaca creada tras confirmación de pago.
- [x] Evento `SUSCRIPCION_PREMIUM_INICIADA` en auditoría.
- [x] `docs/PROGRESO.md` marcado con ✅ para HU-11.

---

## Historial de cambios

| Versión | Fecha | Descripción | Razón |
|---|---|---|---|
| 1.0 | 2026-05-24 | Refactorización a estructura SDD del proyecto. | Unificación de todos los SPEC.md bajo plantilla canónica SDD del proyecto |
