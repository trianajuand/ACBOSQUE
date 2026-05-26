# Plan de implementación — HU-12: Cancelar suscripción premium

## Contexto

El inversionista premium puede cancelar su suscripción desde el perfil. La cancelación tiene dos dimensiones: (1) cancelar en Stripe para detener la facturación futura, y (2) hacer downgrade local a plan BASICO. El diseño es **tolerante a fallos de Stripe**: si la API de Stripe falla (o la clave es un placeholder), el sistema igualmente hace el downgrade local y responde 200. Esto implementa la táctica de **Degradation** (EC-07).

---

## Estado

**Completada** — implementación en `PerfilController.cancelarSuscripcion` + `PerfilService.cancelarSuscripcion` + `StripeAdapter.cancelarSuscripcion`.

---

## Decisiones de diseño

| Decisión | Justificación |
|---|---|
| Tolerancia a fallos de Stripe | En desarrollo la clave es un placeholder. En producción, la API puede estar temporalmente caída. El downgrade local siempre procede; el error de Stripe se loguea pero no bloquea la operación |
| Downgrade inmediato (no al vencimiento del período) | Simplificación del MVP: el inversionista pierde beneficios premium inmediatamente. El período ya pagado no se reembolsa automáticamente. Esto se documenta como limitación |
| `mfa_habilitado` NO se resetea al cancelar | Comportamiento actual documentado como deuda técnica. El inversionista puede desactivar MFA manualmente con HU-10 si lo desea después del downgrade |
| Endpoint `DELETE /api/perfil/suscripcion` | Semántica HTTP correcta: se "elimina" la suscripción premium del perfil. Protegido por JWT |
| Verificación previa de `esPremium` | Si el usuario no tiene suscripción activa y llama al endpoint, recibe 400 con mensaje claro. Evita operaciones Stripe innecesarias |

---

## Módulos involucrados

| Módulo | Componente | Rol |
|---|---|---|
| `autenticacion` | `PerfilController` | Recibe `DELETE /api/perfil/suscripcion` |
| `autenticacion` | `PerfilService` | Valida premium, cancela en Stripe (tolerante), hace downgrade local |
| `integracion` | `StripeAdapter` | Wraps `Subscription.cancel(stripeSuscripcionId)` de Stripe SDK |
| `trazabilidad` | `AuditLogService` (vía `IAuditLog`) | Registra `SUSCRIPCION_PREMIUM_CANCELADA` |

---

## Flujo de implementación

```
DELETE /api/perfil/suscripcion
  → JwtFilter valida token, SecurityContext contiene correo
  → PerfilController.cancelarSuscripcion()
    → extrae correo de Authentication
    → delega a PerfilService.cancelarSuscripcion(correo)
      → carga usuario, inversionista, suscripcion, integracionInversionista
      → if !suscripcion.esPremium → throw SinSuscripcionPremiumException → 400
      → [TOLERANTE A FALLOS]
        try {
          StripeAdapter.cancelarSuscripcion(integracionInversionista.stripeSuscripcionId)
        } catch (Exception e) {
          log.warn("Stripe no pudo cancelar la suscripción: {}", e.getMessage())
          // continúa con el downgrade local
        }
      → suscripcion.esPremium = false
      → suscripcion.planSuscripcion = "BASICO"
      → suscripcion.fechaExpiracion = null
      → integracionInversionista.stripeCustomerId = null
      → integracionInversionista.stripeSuscripcionId = null
      → suscripcionRepository.save(suscripcion)
      → integracionInversionistaRepository.save(integracionInversionista)
      → IAuditLog.registrar(SUSCRIPCION_PREMIUM_CANCELADA, correo, "Suscripción premium cancelada")
      → return RespuestaDTO{mensaje: "Suscripción cancelada exitosamente"}
    → 200 OK con RespuestaDTO
```

---

## Manejo de errores

| Condición | HTTP | Cuerpo |
|---|---|---|
| JWT ausente/inválido | 401 | — |
| `suscripcion.esPremium == false` | 400 | `RespuestaDTO{error: "No tienes una suscripción premium activa"}` |
| Stripe falla (tolerado) | 200 | `RespuestaDTO{mensaje: "Suscripción cancelada exitosamente"}` (downgrade local igual procede) |
| Error BD u otro técnico | 500 | `RespuestaDTO{error: "Error interno del servidor"}` |

---

## Contrato resumido

| Verbo | URL | Auth | Cuerpo | Respuesta exitosa |
|---|---|---|---|---|
| DELETE | `/api/perfil/suscripcion` | Bearer JWT (INVERSIONISTA) | Ninguno | 200 `RespuestaDTO{mensaje}` |

---

## Escenarios de calidad cubiertos

| EC | Táctica | Materialización |
|---|---|---|
| EC-07 | Degradation | Fallo de Stripe no bloquea el downgrade local; el sistema degrada graciosamente la funcionalidad externa |
| EC-12 | Audit Trail | `SUSCRIPCION_PREMIUM_CANCELADA` registrado tras downgrade exitoso |

---

## Dependencias

| Tipo | ID | Descripción |
|---|---|---|
| Historia previa | HU-11 | La suscripción debe existir y estar activa para poder cancelarla |
| Adaptador | `StripeAdapter` | Reutilizado de HU-11; agrega el método `cancelarSuscripcion` |

---

## Notas para el desarrollador

- **`stripeSuscripcionId` puede ser null** si la suscripción se activó con Stripe en modo placeholder (el ID nunca se guardó porque `confirmarCheckout` falló pero el admin activó la cuenta manualmente). En ese caso, el bloque try-catch captura el intento de cancelar con id `null` y continúa silenciosamente.
- **El inversionista conserva `mfa_habilitado = true`** después de cancelar si fue activado como premium. Esto es intencional por simplicidad del MVP — MFA activo no es perjudicial para el usuario.
- El frontend debe ocultar el botón "Cancelar suscripción" si `esPremium == false` (lo sabe por `GET /api/perfil`).
- Tras la cancelación, el frontend debe refrescar el perfil para mostrar el plan `BASICO` actualizado.
