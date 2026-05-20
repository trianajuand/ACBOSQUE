# Historia de Usuario

## Título
Suscripción premium mediante Stripe Checkout.

## Descripción
Como inversionista
Quiero seleccionar plan premium mensual o anual y pagar con Stripe
Para activar beneficios premium y MFA obligatorio.

## Contexto
La integración cubre HU-11 parcialmente: el alta premium se inicia al confirmar correo y se confirma con retorno de Stripe. No existe flujo de cancelación desde frontend.

## Flujo funcional
1. Usuario selecciona plan premium en registro.
2. Tras verificar correo, `RegistroService.confirmarRegistro` detecta plan no básico.
3. `OrquestadorSuscripcion.iniciarSuscripcion` crea sesión Checkout.
4. Frontend muestra botón para ir a Stripe.
5. Stripe redirige a `/login?stripe_success=true&session_id=...`.
6. `LoginComponent` llama `GET /api/suscripciones/confirmar-checkout`.
7. Backend consulta sesión Stripe, verifica `paymentStatus=paid`, activa cuenta, marca premium, habilita MFA y define expiración.
8. Se intenta crear cuenta Alpaca.

## Reglas de negocio
- Planes soportados: `PREMIUM_MENSUAL`, `PREMIUM_ANUAL`.
- Mensual expira en un mes; anual en un año.
- Premium habilita `esPremium=true` y `mfaHabilitado=true`.
- La cuenta no queda activa hasta pago confirmado.

## Componentes involucrados
- `frontend/src/app/auth/verificar-registro.component.ts`
- `frontend/src/app/auth/login.component.ts`
- `backend/.../integracion/orquestadores/OrquestadorSuscripcion.java`
- `backend/.../integracion/adaptadores/stripe/StripeAdapter.java`
- `backend/.../administracion/controller/SuscripcionController.java`
- `backend/src/main/resources/application.properties`

## Backend
`StripeAdapter` crea sesiones en modo `SUBSCRIPTION` con price/product id configurado y metadata `usuarioId`, `plan`. `OrquestadorSuscripcion.confirmarPagoCheckout` actualiza usuario y audita.

## Frontend
`VerificarRegistroComponent` recibe `stripeCheckoutUrl` y redirige. `LoginComponent.ngOnInit` procesa retorno `stripe_success` o `stripe_cancel`.

## Base de datos
Tabla `usuario`: `estado_cuenta`, `mfa_habilitado`.
Tabla `inversionista`: `plan_suscripcion`, `es_premium`, `stripe_customer_id`, `stripe_suscripcion_id`, `fecha_expiracion_premium`.

## API / Endpoints
- `POST /api/auth/register/confirm`
- `GET /api/suscripciones/confirmar-checkout?session_id={id}`

## Validaciones
- Stripe debe estar configurado con secret key y price ids.
- La sesión debe estar pagada.
- Metadata debe incluir `usuarioId`.

## Seguridad
Las claves Stripe no se exponen. El retorno de Stripe se confirma server-side consultando la sesión.

## Consideraciones técnicas
Las URLs de retorno usan `app.frontend.base-url`, por defecto `http://localhost:4200`.

## Dependencias
Depende de registro, verificación de correo, Stripe y creación Alpaca.

## Criterios de aceptación
- [ ] Plan premium crea sesión Checkout.
- [ ] Pago confirmado activa cuenta.
- [ ] Premium habilita MFA.
- [ ] Retorno de Stripe va al frontend Angular.

## Notas
`cancelarSuscripcion` existe en el adaptador, pero no hay endpoint funcional de cancelación premium.
