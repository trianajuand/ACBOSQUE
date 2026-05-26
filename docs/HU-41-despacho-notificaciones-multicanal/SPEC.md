# SPEC — Despacho de notificaciones transaccionales multicanal

---

## Ficha de la historia

| Campo | Valor |
|---|---|
| ID | HU-41 |
| Sprint | 4 |
| Prioridad MoSCoW | Should Have |
| Estado | En desarrollo |
| Épica | Integración / Notificaciones |
| CU asociado | CU-41 |
| Autor | Juan Diego Triana Mejia |
| Creada | 2026-05-20 |
| Última revisión | 2026-05-24 |
| Versión de esta spec | 1.0 |

> ⚠️ **Estado En desarrollo:** La arquitectura de notificaciones está implementada (EmailSender, SmsSender, WhatsAppSender, DespachadorNotificaciones). Los canales Email están operativos. SMS y WhatsApp requieren credenciales de proveedor externo para validación completa.

**Trazabilidad:**

| Tipo | ID | Descripción |
|---|---|---|
| Requerimiento funcional | RF-40 | Notificaciones transaccionales por email, SMS y WhatsApp |
| Historia relacionada | HU-3 | Notificación de código MFA y bloqueo de cuenta |
| Historia relacionada | HU-1 | Notificación de código de verificación de registro |
| Historia relacionada | HU-17..21 | Notificaciones de eventos de órdenes |

---

## Historia de usuario

**Como** inversionista autenticado,
**quiero** recibir notificaciones de mis transacciones y eventos de cuenta por mis canales preferidos (email, SMS, WhatsApp),
**para** estar informado en tiempo real de cualquier actividad en mi cuenta sin tener que revisar la plataforma constantemente.

---

## Motivación y contexto

### Por qué existe esta historia

El sistema necesita notificar a los usuarios sobre eventos críticos (órdenes ejecutadas, bloqueos de cuenta, códigos MFA) de forma confiable y a través del canal que el usuario prefiera. La arquitectura separa el despacho de los canales mediante `INotificacion` y `DespachadorNotificaciones`, permitiendo agregar canales sin modificar el código de negocio.

### Flujo de despacho

```
Evento de negocio (login, orden, bloqueo)
    ↓
INotificacion.notificar*(ContextoNotificacion ctx, ...)
    ↓
DespachadorNotificaciones.despachar(ctx, ...)
    ├─ ctx.tieneEmail() && preferencias.notificacionEmail → EmailSender
    ├─ ctx.tieneSms() && preferencias.notificacionSms → SmsSender
    └─ ctx.tieneWhatsapp() && preferencias.notificacionWhatsapp → WhatsAppSender
```

---

## Actores y precondiciones

### Actores

| Actor | Rol en el sistema | Participación |
|---|---|---|
| Cualquier usuario autenticado | `INVERSIONISTA` / `COMISIONISTA` | Destinatario de notificaciones |
| `DespachadorNotificaciones` | Módulo `integracion` | Orquesta el envío por canal |
| `EmailSender` | Módulo `integracion` | Canal email vía SMTP |
| `SmsSender` | Módulo `integracion` | Canal SMS (proveedor externo) |
| `WhatsAppSender` | Módulo `integracion` | Canal WhatsApp (proveedor externo) |

### Precondiciones

- El usuario tiene preferencias de notificación configuradas (HU-8).
- El canal está habilitado en las preferencias del usuario.
- Para SMS/WhatsApp: el usuario tiene teléfono registrado.

---

## Catálogo de notificaciones implementadas

### Notificaciones de autenticación (solo email)

| Evento | Cuándo se envía | Canal |
|---|---|---|
| Código de verificación de registro | Al completar registro (HU-1) | Email |
| Código MFA | Al iniciar sesión con MFA requerido (HU-3) | Email |
| Token de recuperación de contraseña | Al solicitar recuperación | Email |
| Notificación de bloqueo de cuenta | Al alcanzar 5 intentos fallidos (HU-3) | Email |

### Notificaciones de órdenes (multicanal según preferencias)

| Evento | Cuándo se envía | Canal |
|---|---|---|
| Orden creada | Al crear orden exitosamente (HU-17) | Email, SMS, WhatsApp |
| Orden cancelada | Al cancelar orden (HU-21) | Email, SMS, WhatsApp |
| Orden ejecutada | Al ejecutar orden (HU-17, HU-23) | Email, SMS, WhatsApp |
| Orden fallida | Si Alpaca rechaza la orden | Email, SMS, WhatsApp |
| Orden encolada | Si mercado cerrado (HU-23) | Email, SMS, WhatsApp |

### Notificaciones de mercado

| Evento | Cuándo se envía | Canal |
|---|---|---|
| Apertura de mercado | Al iniciar procesamiento de cola | Email, SMS, WhatsApp |
| Cierre de mercado | Al detectar cierre | Email, SMS, WhatsApp |

### Notificaciones de resumen

| Evento | Cuándo se envía | Canal |
|---|---|---|
| Resumen diario | Tarea programada al final del día | Email, SMS, WhatsApp |

---

## Flujo de error y tolerancia a fallos

### Error — Canal no disponible

| Campo | Valor |
|---|---|
| Condición | EmailSender / SmsSender / WhatsAppSender lanza excepción |
| Comportamiento | El fallo de un canal NO interrumpe el envío por otros canales |
| Log | Error registrado vía SLF4J; operación de negocio no afectada |
| HTTP | No aplica — notificaciones son asíncronas respecto al request principal |

---

## Contrato interno — `INotificacion`

No hay endpoint REST expuesto. La interfaz es consumida internamente:

```java
// Métodos de autenticación (email only)
void enviarCodigoRegistro(String correo, String nombreCompleto, String codigo);
void enviarCodigoMfa(String correo, String nombreCompleto, String codigo);
void enviarTokenRecuperacion(String correo, String nombreCompleto, String token);
void notificarBloqueo(String correo, String nombreCompleto);
void notificarAdmin(String asunto, String mensaje);

// Métodos de negocio (multicanal via ContextoNotificacion)
void notificarOrdenCreada(ContextoNotificacion ctx, String simbolo, String tipoOrden,
    String lado, BigDecimal monto, BigDecimal comision);
void notificarOrdenCancelada(ContextoNotificacion ctx, String simbolo,
    String tipoOrden, BigDecimal montoLiberado);
void notificarOrdenEjecutada(ContextoNotificacion ctx, String simbolo, String tipoOrden,
    String lado, BigDecimal precioEjecucion, BigDecimal cantidad, BigDecimal comision);
void notificarOrdenFallida(ContextoNotificacion ctx, String simbolo, String razon);
void notificarOrdenEncolada(ContextoNotificacion ctx, String simbolo,
    String tipoOrden, String lado);
void notificarAperturaMercado(ContextoNotificacion ctx, String mercado);
void notificarCierreMercado(ContextoNotificacion ctx, String mercado);
void notificarResumenDiario(ContextoNotificacion ctx, int ordenesEjecutadas,
    int ordenesCanceladas, BigDecimal gananciaNetaDia);
```

### ContextoNotificacion

```java
// Contiene información del usuario para el despacho multicanal
class ContextoNotificacion {
    String correo;
    String nombreCompleto;
    String telefono;           // para SMS y WhatsApp
    boolean notificacionEmail;
    boolean notificacionSms;
    boolean notificacionWhatsapp;

    boolean tieneCanal(String canal);  // verifica si el canal está habilitado
}
```

---

## Modelo de datos

Las preferencias de notificación se leen de la tabla `usuario`:

```sql
-- Campos relevantes para notificaciones (en tabla usuario)
notificaciones_activas     BOOLEAN NOT NULL DEFAULT TRUE,  -- master switch
notificacion_email         BOOLEAN NOT NULL DEFAULT TRUE,
notificacion_sms           BOOLEAN NOT NULL DEFAULT FALSE,
notificacion_whatsapp      BOOLEAN NOT NULL DEFAULT FALSE,
tipos_notificacion         VARCHAR(500),  -- CSV: ORDEN_CREADA,ORDEN_EJECUTADA,...
telefono                   VARCHAR(20),   -- requerido para SMS/WhatsApp
```

---

## Módulos y arquitectura

| Módulo | Rol | Componentes |
|---|---|---|
| `integracion` | Despacho | `DespachadorNotificaciones`, `EmailSender`, `SmsSender`, `WhatsAppSender` |
| `autenticacion` | Consumidor | Llama `INotificacion` para auth events |
| `ordenes` | Consumidor | Llama `INotificacion` para order events |

### Interfaces utilizadas

| Interfaz | Módulo proveedor | Consumidores |
|---|---|---|
| `INotificacion` | `integracion` | `autenticacion`, `ordenes`, `administracion` |

### Escenarios de calidad y tácticas materializadas

| EC | Táctica | Cómo se materializa en HU-41 |
|---|---|---|
| EC-05 | Degradation | Fallo de un canal no interrumpe los otros ni la operación de negocio |
| EC-14 | Orchestrate | `DespachadorNotificaciones` centraliza el routing de canales |

---

## Configuración requerida

```properties
# SMTP (email) — operativo
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
app.mail.remitente.nombre=Acciones ElBosque

# SMS — requiere credenciales de proveedor (Twilio u otro)
app.sms.api.key=${SMS_API_KEY}
app.sms.api.url=${SMS_API_URL}

# WhatsApp — requiere credenciales de proveedor
app.whatsapp.api.key=${WHATSAPP_API_KEY}
app.whatsapp.api.url=${WHATSAPP_API_URL}
```

---

## Riesgos

| # | Riesgo | P | I | Mitigación |
|---|---|:-:|:-:|---|
| R1 | SMTP no disponible → notificaciones de MFA no llegan | Media | Alto | Reintentos automáticos; log de fallos; canal SMS como alternativa |
| R2 | SMS/WhatsApp sin credenciales en entorno de desarrollo | Alta | Bajo | Solo email activo en dev; otros canales en modo degradado silencioso |
| R3 | Spam si múltiples eventos generan muchas notificaciones | Baja | Bajo | Rate limiting por canal (fuera de alcance actual) |

---

## Criterios de verificación

### Escenarios de aceptación (Gherkin)

```gherkin
Funcionalidad: Despacho de notificaciones multicanal

  Antecedentes:
    Dado que "ana@test.com" tiene notificacionEmail=true, notificacionSms=true

  Escenario: Notificación de orden ejecutada por email
    Dado que "ana@test.com" tiene orden ejecutada (simbolo=AAPL, cantidad=5, precio=185.00)
    Entonces se envía email a "ana@test.com" con detalles de la orden

  Escenario: Master switch desactivado suprime todas las notificaciones
    Dado que "ana@test.com" tiene notificacionesActivas=false
    Cuando se ejecuta su orden
    Entonces NO se envía ninguna notificación por ningún canal

  Escenario: Fallo de email no interrumpe la operación
    Dado que el servidor SMTP no está disponible
    Cuando se ejecuta una orden de "ana@test.com"
    Entonces la orden queda en estado EJECUTADA
    Y se registra un error de notificación en los logs
    Y la operación retorna 200 OK al usuario

  Escenario: Código MFA enviado por email
    Cuando "ana@test.com" inicia sesión y requiere MFA
    Entonces se envía email con código de 6 dígitos a "ana@test.com"
    Y el código tiene TTL de 5 minutos
```

---

## Fuera de alcance

- Notificaciones push (navegador o app móvil).
- Rate limiting de notificaciones.
- Plantillas de notificación configurables por el administrador.
- Canal SMS y WhatsApp en entorno de desarrollo (requieren credenciales de proveedor de producción).

---

## Definición de terminado

- [x] `INotificacion` implementada por `DespachadorNotificaciones`.
- [x] `EmailSender` operativo con SMTP para todos los eventos de autenticación.
- [x] `DespachadorNotificaciones.despachar()` enruta por canal según `ContextoNotificacion`.
- [x] Fallo de un canal no interrumpe los otros ni la operación de negocio.
- [x] Master switch (`notificacionesActivas = false`) suprime todas las notificaciones.
- [ ] `SmsSender` validado con credenciales de proveedor real.
- [ ] `WhatsAppSender` validado con credenciales de proveedor real.
- [ ] `docs/PROGRESO.md` marcado con ✅ para HU-41.

---

## Historial de cambios

| Versión | Fecha | Descripción | Razón |
|---|---|---|---|
| 1.0 | 2026-05-24 | Refactorización a estructura SDD del proyecto. | Unificación de todos los SPEC.md bajo plantilla canónica SDD del proyecto |
