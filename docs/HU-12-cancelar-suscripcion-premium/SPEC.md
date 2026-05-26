# SPEC — Cancelación de suscripción premium

---

## Ficha de la historia

| Campo | Valor |
|---|---|
| ID | HU-12 |
| Sprint | 2 |
| Prioridad MoSCoW | Should Have |
| Estado | Completada |
| Épica | Administración / Suscripciones |
| CU asociado | CU-12 |
| Autor | Juan Diego Triana Mejia |
| Creada | 2026-05-20 |
| Última revisión | 2026-05-24 |
| Versión de esta spec | 1.0 |

**Trazabilidad:**

| Tipo | ID | Descripción |
|---|---|---|
| Requerimiento funcional | RF-11 | Cancelación de suscripción premium desde el perfil |
| Escenario de calidad | EC-07 | Degradation — el sistema tolera fallos de Stripe en la cancelación |
| Escenario de calidad | EC-12 | Trazabilidad de SUSCRIPCION_PREMIUM_CANCELADA |
| Historia que precede a esta | HU-11 | La suscripción premium debe existir antes de cancelarla |

---

## Historia de usuario

**Como** inversionista premium,
**quiero** cancelar mi suscripción premium desde mi perfil,
**para** dejar de ser cobrado y volver al plan básico sin perder el acceso hasta el fin del período pagado.

---

## Motivación y contexto

### Por qué existe esta historia

Los inversionistas premium deben poder cancelar su suscripción sin asistencia del administrador. La cancelación en Stripe detiene la renovación automática, pero el estado premium en el sistema se revierte inmediatamente (downgrade a BASICO). La implementación es tolerante a fallos de Stripe: si la API de Stripe falla, el sistema registra el error pero continúa con el downgrade local.

---

## Actores y precondiciones

### Actores

| Actor | Rol en el sistema | Participación en este flujo |
|---|---|---|
| Inversionista premium autenticado | `INVERSIONISTA` con `es_premium = true` | Iniciador — solicita cancelación |
| `PerfilService` | Módulo `autenticacion` | Valida premium, cancela en Stripe, hace downgrade local |
| `AuditLogService` | Módulo `trazabilidad` (vía `IAuditLog`) | Registra SUSCRIPCION_PREMIUM_CANCELADA |

### Precondiciones

- JWT válido con rol `INVERSIONISTA`.
- `suscripcion.es_premium = true` y `integracion_inversionista.stripe_suscripcion_id != null`.

### Postcondiciones

- `suscripcion.es_premium = false`.
- `suscripcion.plan_suscripcion = "BASICO"`.
- `integracion_inversionista.stripe_customer_id` = null, `integracion_inversionista.stripe_suscripcion_id` = null. `suscripcion.fecha_expiracion` = null.
- Suscripción en Stripe cancelada (si Stripe está disponible).
- Evento `SUSCRIPCION_PREMIUM_CANCELADA` registrado en auditoría.

---

## Flujo principal

1. Usuario presiona "Cancelar suscripción" en `/perfil`.
2. Frontend envía `DELETE /api/perfil/suscripcion` con JWT.

**Backend — `PerfilService.cancelarSuscripcion(correo)`:**

3. Spring Security extrae `correo` del JWT.
4. Carga `usuario`, `inversionista`, `suscripcion` e `integracionInversionista`.
5. Valida que `suscripcion.esPremium == true`; si no, lanza excepción → 400.
6. **Tolerante a fallos Stripe:** Intenta cancelar la suscripción en Stripe via `StripeAdapter`. Si falla (Stripe no disponible, `stripe_suscripcion_id` inválido), registra el error en logs pero **continúa**.
7. Actualiza `suscripcion`: `esPremium = false`, `planSuscripcion = "BASICO"`, `fechaExpiracion = null`.
   Actualiza `integracionInversionista`: `stripeCustomerId = null`, `stripeSuscripcionId = null`.
8. Persiste `suscripcionRepository.save(suscripcion)` e `integracionInversionistaRepository.save(...)`.
9. `IAuditLog.registrar(SUSCRIPCION_PREMIUM_CANCELADA, correo, "Suscripción premium cancelada")`.
10. Responde `200 OK` con `RespuestaDTO{mensaje: "Suscripción cancelada exitosamente"}`.

---

## Flujos de error

### Error 1 — No autenticado

| Campo | Valor |
|---|---|
| Condición | JWT ausente, inválido o expirado |
| HTTP | 401 Unauthorized |
| Evento de auditoría | Ninguno |

### Error 2 — Usuario no tiene suscripción premium activa

| Campo | Valor |
|---|---|
| Condición | `suscripcion.esPremium == false` |
| HTTP | 400 Bad Request |
| Cuerpo | `RespuestaDTO{error: "No tienes una suscripción premium activa"}` |
| Evento de auditoría | Ninguno |

### Error 3 — Fallo de Stripe (tolerante)

| Campo | Valor |
|---|---|
| Condición | La API de Stripe retorna error al cancelar |
| HTTP | 200 OK (se continúa con downgrade local) |
| Cuerpo | `RespuestaDTO{mensaje: "Suscripción cancelada exitosamente"}` |
| Estado final | Downgrade local completado; Stripe puede seguir facturando hasta que expire |
| Evento de auditoría | `SUSCRIPCION_PREMIUM_CANCELADA` + log de error de Stripe |

### Error 4 — Error técnico genérico

| Campo | Valor |
|---|---|
| Condición | Falla BD u otro error no tolerado |
| HTTP | 500 Internal Server Error |
| Cuerpo | `RespuestaDTO{error: "Error interno del servidor"}` |

---

## Contrato de API

### Endpoint — `DELETE /api/perfil/suscripcion`

```yaml
DELETE /api/perfil/suscripcion:
  summary: Cancela la suscripción premium del inversionista autenticado
  security:
    - bearerAuth: []
  responses:
    '200':
      description: Suscripción cancelada exitosamente (incluso si Stripe tuvo error tolerado)
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'
          example:
            mensaje: "Suscripción cancelada exitosamente"
    '400':
      description: Usuario no tiene suscripción premium activa
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'
          example:
            error: "No tienes una suscripción premium activa"
    '401':
      description: No autenticado
    '500':
      description: Error interno del servidor
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'
```

---

## Módulos y arquitectura

### Módulos involucrados

| Módulo | Rol | Componentes específicos |
|---|---|---|
| `autenticacion` | Coordinador del flujo | `PerfilController`, `PerfilService` |
| `integracion` | Comunicación con Stripe | `StripeAdapter` |
| `trazabilidad` | Registro de eventos | `AuditLogService` (impl. de `IAuditLog`) |

### Escenarios de calidad y tácticas materializadas

| EC | Táctica | Cómo se materializa en HU-12 |
|---|---|---|
| EC-07 | Degradation | Fallo de Stripe no bloquea el downgrade local; la operación continúa con estado degradado |
| EC-12 | Audit Trail | `SUSCRIPCION_PREMIUM_CANCELADA` registrado en auditoría |

---

## Eventos y efectos transversales

### Eventos de auditoría emitidos

| Evento (`TipoEvento`) | Cuándo se emite | Datos en `detalle` |
|---|---|---|
| `SUSCRIPCION_PREMIUM_CANCELADA` | Downgrade completado exitosamente | `"Suscripción premium cancelada"` |

---

## Riesgos

| # | Riesgo | P | I | Mitigación | Test que lo cubre |
|---|---|:-:|:-:|---|---|
| R1 | Stripe no cancelado pero sistema muestra downgrade → Stripe sigue facturando al usuario | Media | Alto | Diseño tolerante documentado como limitación. En producción, monitorear Stripe webhooks | Manual: simular fallo de Stripe y verificar logs |
| R2 | `mfa_habilitado` permanece `true` en el usuario tras cancelar premium — el login seguirá requiriendo MFA | Media | Bajo | Comportamiento actual: `mfa_habilitado` no se resetea en cancelación. El usuario puede desactivarlo con HU-10 si es inversionista regular. Nota: si el inversionista era premium, HU-10 puede no aplicar directamente. | Manual: cancelar y verificar login |

---

## Criterios de verificación

### Escenarios de aceptación (Gherkin)

```gherkin
Funcionalidad: Cancelación de suscripción premium

  Antecedentes:
    Dado que "ana@test.com" tiene JWT válido, es_premium=true y stripe_suscripcion_id="sub_123"

  Escenario: Cancelación exitosa
    Cuando se envía DELETE /api/perfil/suscripcion con JWT de "ana@test.com"
    Entonces el sistema responde 200 OK
    Y el cuerpo contiene { "mensaje": "Suscripción cancelada exitosamente" }
    Y suscripcion.es_premium para "ana@test.com" es false
    Y suscripcion.plan_suscripcion es "BASICO"
    Y integracion_inversionista.stripe_suscripcion_id es null
    Y se emite evento SUSCRIPCION_PREMIUM_CANCELADA en auditoría

  Escenario: Cancelación tolerante a fallo de Stripe
    Dado que la API de Stripe retorna error al cancelar la suscripción
    Cuando se envía DELETE /api/perfil/suscripcion
    Entonces el sistema responde 200 OK
    Y suscripcion.es_premium es false (downgrade completado)
    Y el error de Stripe está registrado en los logs del sistema

  Escenario: Usuario sin suscripción premium recibe 400
    Dado que "ana@test.com" tiene es_premium=false
    Cuando se envía DELETE /api/perfil/suscripcion
    Entonces el sistema responde 400 Bad Request
    Y el cuerpo contiene error sobre no tener suscripción activa

  Escenario: Sin JWT — 401
    Cuando se envía DELETE /api/perfil/suscripcion sin Authorization
    Entonces el sistema responde 401 Unauthorized
```

---

## Definición de terminado

- [x] `DELETE /api/perfil/suscripcion` hace downgrade a BASICO y responde 200.
- [x] Sin suscripción premium activa responde 400.
- [x] Fallo de Stripe tolerado — el sistema hace downgrade local de todas formas.
- [x] Evento `SUSCRIPCION_PREMIUM_CANCELADA` en auditoría.
- [x] Sin JWT responde 401.
- [x] `docs/PROGRESO.md` marcado con ✅ para HU-12.

---

## Historial de cambios

| Versión | Fecha | Descripción | Razón |
|---|---|---|---|
| 1.0 | 2026-05-24 | Refactorización a estructura SDD del proyecto. | Unificación de todos los SPEC.md bajo plantilla canónica SDD del proyecto |
