# Tareas — HU-12: Cancelar suscripción premium

> Estado general: **COMPLETADA**

---

## Checklist de implementación

### Backend — Módulo `integracion`

- [x] **`StripeAdapter.cancelarSuscripcion(String stripeSuscripcionId)`**
  - Llama `Subscription.retrieve(id).cancel()` del Stripe SDK
  - Lanza `StripeException` en caso de error (no capturado aquí — se captura en el service)

### Backend — Módulo `autenticacion`

- [x] **Service** — `PerfilService.cancelarSuscripcion(String correo)`
  - Carga `Usuario` por correo
  - Carga `Inversionista` (PK compartida con `usuario.id`)
  - Carga `Suscripcion` asociada al inversionista
  - Carga `IntegracionInversionista` para obtener `stripeSuscripcionId`
  - Valida `suscripcion.esPremium == true`; si no → `SinSuscripcionPremiumException` → 400
  - Bloque tolerante a fallos:
    ```java
    try {
        stripeAdapter.cancelarSuscripcion(integracion.getStripeSuscripcionId());
    } catch (Exception e) {
        log.warn("Error al cancelar en Stripe: {}", e.getMessage());
    }
    ```
  - Aplica downgrade local: `esPremium = false`, `planSuscripcion = "BASICO"`, `fechaExpiracion = null`
  - Limpia IDs Stripe: `stripeCustomerId = null`, `stripeSuscripcionId = null`
  - Persiste: `suscripcionRepository.save`, `integracionRepository.save`
  - Llama `IAuditLog.registrar(SUSCRIPCION_PREMIUM_CANCELADA, correo, "Suscripción premium cancelada")`
  - Retorna `RespuestaDTO{mensaje: "Suscripción cancelada exitosamente"}`

- [x] **Controller** — `PerfilController.cancelarSuscripcion()`
  - Verbo: `DELETE /api/perfil/suscripcion`
  - Extrae correo de `Authentication`
  - Delega a `PerfilService.cancelarSuscripcion(correo)`
  - Retorna 200 OK con `RespuestaDTO`

- [x] **Manejo de errores** — `GlobalExceptionHandler` captura:
  - `SinSuscripcionPremiumException` → 400 con mensaje "No tienes una suscripción premium activa"

- [x] **Auditoría** — evento `SUSCRIPCION_PREMIUM_CANCELADA` emitido en `PerfilService` tras downgrade exitoso

### Frontend (dashboard.html / Angular)

- [x] Botón "Cancelar suscripción premium" visible solo si `perfil.esPremium == true`
- [x] Confirmación con `window.confirm("¿Estás seguro de que quieres cancelar tu suscripción premium?")` antes de llamar
- [x] Llamada `DELETE /api/perfil/suscripcion` con JWT
- [x] Tras 200 OK: recarga perfil desde `GET /api/perfil` para mostrar plan BASICO actualizado
- [x] Mensaje de confirmación visible al usuario

### Documentación

- [x] `SPEC.md` creado/actualizado
- [x] `plan.md` creado
- [x] `tasks.md` creado (este archivo)
- [x] `docs/PROGRESO.md` marcado con ✅ para HU-12

---

## Criterios de aceptación verificados

| Criterio | Estado |
|---|:-:|
| `DELETE /api/perfil/suscripcion` con premium activo devuelve 200 y hace downgrade | ✅ |
| `suscripcion.es_premium = false` tras cancelación | ✅ |
| `suscripcion.plan_suscripcion = 'BASICO'` tras cancelación | ✅ |
| `stripe_suscripcion_id = null` e `stripe_customer_id = null` tras cancelación | ✅ |
| Fallo de Stripe tolerado — downgrade local procede igualmente | ✅ |
| Usuario sin premium activo recibe 400 con mensaje | ✅ |
| Sin JWT devuelve 401 | ✅ |
| Evento `SUSCRIPCION_PREMIUM_CANCELADA` en audit.log | ✅ |

---

## Archivos modificados / creados

| Archivo | Tipo | Descripción |
|---|---|---|
| `integracion/adaptadores/stripe/StripeAdapter.java` | Modificado | Método `cancelarSuscripcion` añadido |
| `autenticacion/service/PerfilService.java` | Modificado | Método `cancelarSuscripcion` añadido |
| `autenticacion/controller/PerfilController.java` | Modificado | `DELETE /api/perfil/suscripcion` añadido |

---

## Deuda técnica / limitaciones conocidas

| Item | Descripción |
|---|---|
| Downgrade inmediato | El inversionista pierde acceso premium inmediatamente, aunque haya pagado hasta fin de mes. No hay lógica de "mantener hasta `fechaExpiracion`" en MVP |
| `mfa_habilitado` no se resetea | Tras cancelar, si el inversionista quiere desactivar MFA, debe hacerlo explícitamente con HU-10 |
| Stripe puede seguir facturando | Si Stripe falló al cancelar (modo tolerante), la suscripción en Stripe sigue activa y puede generar un cobro automático. Solución real: webhook + job de reconciliación |
