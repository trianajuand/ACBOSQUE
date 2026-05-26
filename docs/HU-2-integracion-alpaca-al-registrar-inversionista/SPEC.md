# SPEC — Creación de cuenta Alpaca al activar inversionista

---

## Ficha de la historia

| Campo | Valor |
|---|---|
| ID | HU-2 |
| Sprint | 2 |
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
| Requerimiento funcional | RF-01 | Registro de inversionistas — integración con plataforma de trading |
| Escenario de calidad | EC-14 | Creación de cuenta Alpaca durante registro (Orchestrate + Tailor Interface) |
| Escenario de calidad | EC-07 | Manejo de fallo de conexión con Alpaca (degradación a estado pendiente) |
| Historia de la que depende | HU-1 | Necesita usuario e inversionista persistidos con cuenta confirmada |
| Historia de la que depende | HU-11 | Cuenta Alpaca también se crea al confirmar pago Stripe premium |
| Historia que depende de esta | HU-17 | Órdenes Market requieren `alpacaAccountId` para enviar a Alpaca |
| Historia que depende de esta | HU-18 | Ídem Limit |
| Historia que depende de esta | HU-19 | Ídem Stop Loss |
| Historia que depende de esta | HU-20 | Ídem Take Profit |

---

## Historia de usuario

**Como** inversionista recién registrado,
**quiero** que el sistema cree automáticamente una cuenta de trading en Alpaca asociada a mi perfil,
**para** poder enviar órdenes reales o sandbox a los mercados internacionales desde la plataforma.

---

## Motivación y contexto

### Por qué existe esta historia

Sin un `alpacaAccountId` asociado al perfil del inversionista, no es posible enviar ni cancelar órdenes en Alpaca. La creación de la cuenta se hace automáticamente al activar el inversionista para no interrumpir su experiencia de onboarding. El módulo de Integración encapsula la complejidad del API de Alpaca Broker, de modo que el módulo de Autenticación solo delega sin conocer los contratos externos.

### Dependencias hacia atrás

| Componente | Qué provee | Sin esto... |
|---|---|---|
| HU-1 (`confirmarRegistro`) | Usuario activo + Inversionista persistido con `id` válido | `OrquestadorRegistro` lanza `IllegalStateException` al no encontrar el inversionista |
| HU-11 (`confirmarPagoCheckout`) | Activación premium + inversionista persistido | Misma consecuencia para flujo premium |
| `alpaca.broker.*` en `application.properties` | Credenciales Alpaca Broker API (sandbox) | `AlpacaAdapter` falla con error de autenticación; `alpacaId` retorna null y cuenta queda pendiente |

### Historias que dependen de esta

| Historia | Qué consume de aquí |
|---|---|
| HU-17 a HU-20 (órdenes) | `integracion_inversionista.alpaca_account_id` para enviar órdenes vía `IIntegracionAlpaca.crearOrden` |
| HU-15 (portafolio) | `integracion_inversionista.alpaca_account_id` para sincronizar posiciones |

---

## Actores y precondiciones

### Actores

| Actor | Rol en el sistema | Participación en este flujo |
|---|---|---|
| Sistema (`RegistroService`) | Módulo `autenticacion` | Disparador tras confirmar registro BASICO |
| Sistema (`OrquestadorSuscripcion`) | Módulo `integracion` | Disparador tras confirmar pago Stripe premium |
| `OrquestadorRegistro` | Módulo `integracion` | Orquestador central de este flujo |
| `AlpacaAdapter` | Módulo `integracion` | Adaptador hacia Alpaca Broker API |
| `AuditLogService` | Módulo `trazabilidad` (vía `IAuditLog`) | Registra éxito o fallo de la creación |

### Precondiciones

- Existe registro en `usuario` con `estado_cuenta = ACTIVA` (plan BASICO) o recién activado por Stripe (plan premium).
- Existe registro en `inversionista` con `id = usuario.id` (PK compartida).
- Existe registro en `integracion_inversionista` con `id = inversionista.id`.
- Las propiedades `alpaca.broker.base-url`, `alpaca.broker.api-key` y `alpaca.broker.api-secret` están configuradas en `application.properties`.

### Postcondiciones del flujo principal (Alpaca OK)

- `integracion_inversionista.alpaca_account_id` contiene el UUID retornado por Alpaca.
- `integracion_inversionista.pendiente_cuenta_alpaca = false`.
- Evento `REGISTRO_EXITOSO` registrado en auditoría con el Alpaca ID.

---

## Flujo principal (Alpaca responde con ID)

Este flujo no es iniciado directamente por el usuario. Se activa automáticamente al final de `RegistroService.confirmarRegistro` (plan BASICO) o de `OrquestadorSuscripcion.confirmarPagoCheckout` (plan premium).

1. El disparador llama `OrquestadorRegistro.crearCuentaAlpaca(usuario)`.
2. `OrquestadorRegistro` carga el `Inversionista` desde BD usando `InversionistaRepository.findById(usuario.getId())` (PK compartida — `inversionista.id == usuario.id`).
3. `OrquestadorRegistro` invoca `IIntegracionAlpaca.crearCuenta(usuario, inversionista)` → delegado a `AlpacaAdapter`.
4. `AlpacaAdapter` construye el payload para Alpaca Broker API:
   - Divide `nombreCompleto` en `nombre` (primera palabra) y `apellido` (resto).
   - Normaliza teléfono: sin valor → `+573001234567`; sin `+` → antepone `+57`; con prefijo `57` sin `+` → `+57{resto}`.
   - Normaliza país: `CO` → `COL` (ISO 3 letras); código 2 letras → mayúsculas.
   - Genera `tax_id` sandbox: `hash(usuario.id, usuario.correo)` módulo 9 dígitos; si todos los dígitos son iguales → fallback `"123456789"`.
   - Aplica valores de respaldo para campos opcionales vacíos: `direccion → "123 Main St"`, `ciudad → "Bogota"`, `codigoPostal → "110111"`, `fechaNacimiento → "1990-01-01"`.
   - Construye payload con secciones `contact`, `identity`, `disclosures` y `agreements` (margin, account, customer — firmados en timestamp sandbox `2024-01-01T00:00:00Z`).
5. `AlpacaAdapter` llama `POST {alpaca.broker.base-url}/v1/accounts` con `Authorization: Basic Base64(apiKey:apiSecret)`.
6. Alpaca responde con JSON que incluye campo `"id"`.
7. `AlpacaAdapter` retorna el `id` como `String`.
8. `OrquestadorRegistro` actualiza `integracionInversionista.alpacaAccountId = id`, `integracionInversionista.pendienteCuentaAlpaca = false`, guarda con `integracionInversionistaRepository.save(...)`.
9. `IAuditLog.registrar(REGISTRO_EXITOSO, correo, "Cuenta Alpaca creada: {alpacaId}")`.

---

## Flujos alternativos

### Alternativo A — Activación tras pago Stripe (plan premium)

**Condición:** El inversionista seleccionó plan `PREMIUM_MENSUAL` o `PREMIUM_ANUAL` en el registro y completó el pago en Stripe.

El flujo es idéntico al principal. El disparador es `OrquestadorSuscripcion.confirmarPagoCheckout(sessionId)` en lugar de `RegistroService.confirmarRegistro`. `OrquestadorSuscripcion` activa la cuenta, persiste datos Stripe y luego llama `orquestadorRegistro.crearCuentaAlpaca(usuario)`. Los pasos 2–9 son idénticos.

### Alternativo B — Creación on-demand al colocar primera orden

**Condición:** El inversionista intenta colocar una orden (HU-17 a HU-20) y `inversionista.alpacaAccountId` es null (la creación inicial falló o quedó pendiente).

En ese flujo, `OrdenService` detecta `alpacaAccountId == null`, llama `OrquestadorRegistro.crearCuentaAlpaca(usuario)` antes de enviar la orden. Si la creación on-demand también falla, la orden se rechaza. Este comportamiento está documentado en HU-17.

---

## Flujos de error

### Error 1 — Alpaca no responde o retorna error HTTP

| Campo | Valor |
|---|---|
| Condición | `AlpacaAdapter` lanza cualquier excepción (timeout, 4xx, 5xx de Alpaca, red caída) |
| Manejo en `AlpacaAdapter` | Capturado en bloque `catch(Exception e)` → log de error → retorna `null` |
| Manejo en `OrquestadorRegistro` | `alpacaId == null` → `inversionista.pendienteCuentaAlpaca = true` → guarda → audita |
| HTTP expuesto al cliente | Ninguno — el fallo es silente para el usuario final; la cuenta local queda activa |
| Estado final | `integracion_inversionista.pendiente_cuenta_alpaca = true`; `integracion_inversionista.alpaca_account_id = null` |
| Evento de auditoría | `TipoEvento.REGISTRO_FALLO_ALPACA` con detalle `"Fallo al crear cuenta Alpaca"` |

### Error 2 — Inversionista no encontrado en BD

| Campo | Valor |
|---|---|
| Condición | `InversionistaRepository.findByUsuarioId` retorna vacío (registro parcial inconsistente) |
| Excepción Java | `IllegalStateException("Inversionista no encontrado para usuario {id}")` |
| HTTP expuesto al cliente | 500 (manejador genérico de `GlobalExceptionHandler`) |
| Estado final | No se modifica ningún registro de inversionista |
| Evento de auditoría | Ninguno (stack trace en consola) |

---

## Contrato de API

Esta historia no expone endpoints REST propios. El flujo se activa internamente desde dos puntos de disparo:

| Disparador | Endpoint que lo activa | Condición |
|---|---|---|
| `RegistroService.confirmarRegistro` | `POST /api/auth/register/confirm` | Plan `BASICO` — tras activar cuenta |
| `OrquestadorSuscripcion.confirmarPagoCheckout` | `GET /api/suscripciones/confirmar-checkout?session_id={id}` | Plan premium — tras pago Stripe confirmado (HU-11) |

**Payload hacia Alpaca Broker API (interno, no expuesto al cliente):**

```yaml
POST {alpaca.broker.base-url}/v1/accounts:
  headers:
    Authorization: "Basic Base64(apiKey:apiSecret)"
    Content-Type: application/json
  body:
    account_type: "trading"
    contact:
      email_address: "{usuario.correo}"
      phone_number: "{telefono normalizado}"
      street_address: ["{direccion o '123 Main St'}"]
      city: "{ciudad o 'Bogota'}"
      state: "CO"
      postal_code: "{codigoPostal o '110111'}"
      country: "{pais normalizado ISO-3}"
    identity:
      given_name: "{primera palabra de nombreCompleto}"
      family_name: "{resto de nombreCompleto}"
      date_of_birth: "{fechaNacimiento o '1990-01-01'}"
      tax_id: "{hash 9 dígitos derivado de usuario.id + correo}"
      tax_id_type: "OTHER"
      country_of_citizenship: "{pais normalizado ISO-3}"
      country_of_birth: "{pais normalizado ISO-3}"
      country_of_tax_residence: "{pais normalizado ISO-3}"
      funding_source: ["employment_income"]
    disclosures:
      is_control_person: false
      is_affiliated_exchange_or_finra: false
      is_politically_exposed: false
      immediate_family_exposed: false
    agreements:
      - agreement: "margin_agreement"
        signed_at: "2024-01-01T00:00:00Z"
        ip_address: "127.0.0.1"
      - agreement: "account_agreement"
        signed_at: "2024-01-01T00:00:00Z"
        ip_address: "127.0.0.1"
      - agreement: "customer_agreement"
        signed_at: "2024-01-01T00:00:00Z"
        ip_address: "127.0.0.1"
  responses:
    '200' / '201':
      body: { "id": "alpaca-uuid-...", ... }  # Solo el campo "id" es usado
    '4xx' / '5xx' / timeout:
      Capturado en catch → retorna null → cuenta queda pendiente
```

---

## Modelo de datos

Esta historia no crea tablas nuevas. Solo modifica columnas en `integracion_inversionista`:

| Columna | Tipo | Cambio | Cuándo |
|---|---|---|---|
| `alpaca_account_id` | `VARCHAR(255)` | Actualiza de `null` al UUID retornado por Alpaca | Flujo exitoso |
| `pendiente_cuenta_alpaca` | `BOOLEAN` | `false` (exitoso) o `true` (fallo) | Siempre tras intentar la creación |

**Justificación:**
- Los IDs de sistemas externos (Alpaca, Stripe) están en `integracion_inversionista`, separados del perfil de negocio en `inversionista` (normalización 3NF).
- `alpaca_account_id` nullable: la cuenta puede quedar sin ID si Alpaca falla; la plataforma sigue operando localmente.
- `pendiente_cuenta_alpaca`: flag que el sistema puede consultar para reintentar la creación al colocar la primera orden (flujo on-demand en HU-17).

---

## Módulos y arquitectura

### Módulos involucrados

| Módulo | Rol | Componentes específicos |
|---|---|---|
| `autenticacion` | Disparador | `RegistroService` (llama `crearCuentaAlpaca` tras confirmar BASICO) |
| `integracion` | Orquestador + Adaptador | `OrquestadorRegistro`, `OrquestadorSuscripcion`, `AlpacaAdapter` |
| `trazabilidad` | Auditoría | `AuditLogService` (vía `IAuditLog`) |

### Interfaces consumidas en este flujo

| Interfaz | Módulo dueño | Métodos usados | Cuándo |
|---|---|---|---|
| `IIntegracionAlpaca` | `integracion` | `crearCuenta(Usuario usuario, Inversionista inversionista)` | En `OrquestadorRegistro.crearCuentaAlpaca` |
| `IAuditLog` | `trazabilidad` | `registrar(TipoEvento, correo, detalle)` | Al final de `crearCuentaAlpaca`, éxito o fallo |

### Interfaces nuevas o modificadas

Ninguna. `IIntegracionAlpaca` ya existía. Este flujo no introduce interfaces nuevas.

### Escenarios de calidad y tácticas materializadas

| EC | Táctica | Cómo se materializa en HU-2 |
|---|---|---|
| EC-14 | **Orchestrate** | `OrquestadorRegistro` coordina la secuencia: cargar inversionista → llamar adaptador → actualizar BD → auditar |
| EC-14 | **Tailor Interface** | `AlpacaAdapter` traduce el modelo interno (`Usuario`, `Inversionista`) al payload específico de Alpaca Broker API; el resto del sistema no conoce esa estructura |
| EC-07 | **Exception Handling / Degradation** | Cualquier excepción en `AlpacaAdapter` es capturada → retorna `null` → el sistema degrada a estado `pendiente_cuenta_alpaca = true` sin interrumpir la activación de la cuenta local |

### Desviaciones arquitectónicas

**Desviación 1:** `OrquestadorRegistro` inyecta `InversionistaRepository` (repositorio del módulo `autenticacion`) directamente en lugar de acceder al dato a través de una interfaz `I...` del módulo `autenticacion`.

**Justificación:** La operación requiere cargar el `Inversionista` con todos sus campos para construir el payload de Alpaca, incluidos datos de identidad que no están expuestos en ninguna interfaz existente. Crear una interfaz solo para esta consulta puntual implicaría boilerplate desproporcionado al scope académico.

**Plan de resolución:** Añadir método `obtenerPerfilCompleto(Long usuarioId)` en `IConsultaInversionista` que retorne un DTO con los campos necesarios para Alpaca. `OrquestadorRegistro` pasaría a consumir la interfaz. Deuda técnica registrada.

**Desviación 2:** La firma de `IIntegracionAlpaca.crearCuenta(Usuario usuario, Inversionista inversionista)` acepta entidades JPA del módulo `autenticacion`, no DTOs. Esto viola la regla de que la comunicación entre módulos use DTOs, no entidades.

**Justificación:** Documentada en `ARQUITECTURA.md` sección 9.6. Alpaca requiere el perfil completo; crear un DTO de transferencia específico para este caso no fue prioridad en el sprint. La dependencia está acotada a este único método.

**Plan de resolución:** Crear `PerfilAlpacaDTO` en `autenticacion/dto/` y cambiar la firma a `crearCuenta(PerfilAlpacaDTO dto)`. Post-MVP.

---

## Eventos y efectos transversales

### Eventos de auditoría emitidos

| Evento (`TipoEvento`) | Cuándo se emite | Datos en `detalle` |
|---|---|---|
| `REGISTRO_EXITOSO` | Alpaca retornó un ID válido y fue guardado | `"Cuenta Alpaca creada: {alpacaId}"` |
| `REGISTRO_FALLO_ALPACA` | `AlpacaAdapter` retornó `null` (cualquier error externo) | `"Fallo al crear cuenta Alpaca"` |

### Notificaciones enviadas

Ninguna directa en este flujo. Si Alpaca falla (`REGISTRO_FALLO_ALPACA`), no se notifica al administrador desde este flujo. La notificación al admin por fallo de API externa está pendiente de implementación (HU-42).

### Llamadas a sistemas externos

| Sistema | Endpoint | Adaptador | Cuándo | Manejo de fallo |
|---|---|---|---|---|
| Alpaca Broker API | `POST {alpaca.broker.base-url}/v1/accounts` | `AlpacaAdapter` | Al activar cuenta | Catch-all → `null` → estado pendiente |

### Cambios en caché u otros estados compartidos

No aplica. Este flujo no modifica `precio_cache` ni ningún otro estado compartido.

---

## Riesgos

| # | Riesgo | P | I | Mitigación | Test que lo cubre |
|---|---|:-:|:-:|---|---|
| R1 | Alpaca Broker API en sandbox rechaza el payload (campos de identidad inválidos, email duplicado en Alpaca, etc.) → `alpacaId = null` → cuenta queda pendiente indefinidamente | Media | Alto | El flag `pendiente_cuenta_alpaca = true` permite reintento on-demand al colocar primera orden (HU-17). Revisión manual de errores en log de aplicación. | Manual: desactivar credenciales Alpaca y confirmar registro; verificar `pendiente_cuenta_alpaca = true` en BD |
| R2 | `tax_id` generado con `hash(id, correo)` puede colisionar con otro usuario en Alpaca sandbox (aunque es improbable en entorno académico) | Baja | Bajo | Fallback `"123456789"` si todos los dígitos son iguales. Sandbox de Alpaca acepta tax IDs repetidos sin error en la mayoría de los casos. | No hay test automatizado |
| R3 | `nombreCompleto` con un solo token (sin espacio) → `apellido = nombre` (repetido). Alpaca acepta esto en sandbox pero podría rechazarlo en producción | Baja | Bajo | Validar en `RegistroInversionistaDTO` que `nombreCompleto` contenga al menos un espacio (pregunta abierta #1) | No hay test automatizado |
| R4 | `OrquestadorRegistro` accede directamente a `InversionistaRepository` (desviación arquitectónica D1). Si el repositorio cambia de nombre o de módulo, este componente rompe sin error de compilación claro | Baja | Medio | Documentada como deuda técnica. Mitigación: crear `obtenerPerfilCompleto` en `IConsultaInversionista`. | No hay test automatizado |

---

## Criterios de verificación

### Escenarios de aceptación (Gherkin)

```gherkin
Funcionalidad: Creación de cuenta Alpaca al activar inversionista

  Antecedentes:
    Dado que el backend está corriendo en http://localhost:8080
    Y las credenciales Alpaca sandbox están configuradas en application.properties
    Y existe un inversionista en PENDIENTE_VERIFICACION con todos los datos requeridos

  Escenario: Cuenta Alpaca creada exitosamente tras confirmar registro BASICO
    Dado que el inversionista confirmó su código de registro exitosamente (plan BASICO)
    Cuando se ejecuta OrquestadorRegistro.crearCuentaAlpaca(usuario)
    Entonces integracion_inversionista.alpaca_account_id contiene un UUID válido retornado por Alpaca
    Y integracion_inversionista.pendiente_cuenta_alpaca = false
    Y se emite evento REGISTRO_EXITOSO en auditoría con el alpacaId

  Escenario: Creación fallida degrada a estado pendiente
    Dado que las credenciales Alpaca son inválidas o el servidor no responde
    Cuando se ejecuta OrquestadorRegistro.crearCuentaAlpaca(usuario)
    Entonces integracion_inversionista.alpaca_account_id permanece null
    Y integracion_inversionista.pendiente_cuenta_alpaca = true
    Y se emite evento REGISTRO_FALLO_ALPACA en auditoría
    Y la cuenta local del usuario permanece ACTIVA (el fallo de Alpaca no revierte la activación)

  Escenario: Cuenta Alpaca creada tras confirmar pago Stripe premium
    Dado que el inversionista completó el pago Stripe para plan PREMIUM_MENSUAL
    Y OrquestadorSuscripcion.confirmarPagoCheckout activó la cuenta con suscripcion.es_premium=true
    Cuando OrquestadorRegistro.crearCuentaAlpaca es invocado por el orquestador de suscripción
    Entonces integracion_inversionista.alpaca_account_id contiene el UUID de Alpaca
    Y integracion_inversionista.pendiente_cuenta_alpaca = false

  Escenario: Teléfono vacío usa valor sandbox
    Dado que el inversionista registró sin teléfono (telefono = null)
    Cuando AlpacaAdapter construye el payload de contacto
    Entonces el campo phone_number del payload es "+573001234567"

  Escenario: País CO se normaliza a COL en el payload
    Dado que el inversionista tiene pais = "CO"
    Cuando AlpacaAdapter construye el payload de identidad
    Entonces los campos country_of_citizenship, country_of_birth y country_of_tax_residence son "COL"
```

### Criterios no funcionales

| Criterio | Métrica | Cómo se verifica |
|---|---|---|
| Integración Alpaca exitosa | 99.9% solicitudes en sandbox (EC-14: RNF-10) | Inspección de BD: `SELECT alpaca_account_id FROM integracion_inversionista` tras registro en sandbox |
| Restablecimiento ante fallo | Inversionista queda operativo localmente incluso si Alpaca falla | Manual: deshabilitar Alpaca, confirmar registro, verificar `estado_cuenta = ACTIVA` y `pendiente_cuenta_alpaca = true` |

---

## Interfaz de usuario

Esta historia no tiene pantalla específica en Angular. El efecto se refleja de forma indirecta:

- El inversionista accede normalmente al dashboard tras el registro exitoso, sin saber si la cuenta Alpaca fue creada o no.
- Si la cuenta Alpaca falló y el inversionista intenta colocar una orden, el flujo on-demand (HU-17) intentará crearla nuevamente.
- No hay indicador visual de estado de cuenta Alpaca en el MVP.

---

## Fuera de alcance

Esta spec NO cubre:

- **Reintento automático programado de creación de cuenta pendiente** — no hay scheduler que reintente periódicamente las cuentas con `pendiente_cuenta_alpaca = true`. Solo se reintenta on-demand al colocar orden (HU-17).
- **Notificación al administrador cuando Alpaca falla** — diferido a HU-42 (detección de fallo de servicio).
- **Consulta del estado de la cuenta en Alpaca** — `IIntegracionAlpaca.obtenerCuenta(accountId)` existe pero no es llamado en este flujo.
- **Envío de órdenes** — `IIntegracionAlpaca.crearOrden(...)` está implementado en `AlpacaAdapter` pero pertenece a HU-17 a HU-20.
- **Sincronización de posiciones** — `IIntegracionAlpaca.obtenerPosiciones(...)` pertenece a HU-15.
- **Datos reales de identidad para producción** — el sandbox usa tax_id derivado y timestamps fijos en agreements; un entorno de producción requeriría datos legales reales.

---

## Decisiones y preguntas abiertas

| # | Pregunta / Decisión | Responsable | Fecha | Estado |
|---|---|---|---|---|
| 1 | Si `nombreCompleto` tiene un solo token (sin espacio), `apellido = nombre`. ¿Debe validarse que `nombreCompleto` tenga al menos un espacio en `RegistroInversionistaDTO`? | Juan Diego Triana Mejia | 2026-05-24 | Abierta |
| 2 | **Decisión tomada:** Fallo de Alpaca degrada a estado `pendiente_cuenta_alpaca = true` sin interrumpir la activación de la cuenta local. Razón: la experiencia del usuario no debe bloquearse por un fallo externo. El reintento ocurre on-demand. | Juan Diego Triana Mejia | 2026-05-20 | Resuelta |
| 3 | **Desviación documentada:** `OrquestadorRegistro` accede directamente a `InversionistaRepository`. Deuda: crear `obtenerPerfilCompleto` en `IConsultaInversionista`. | Juan Diego Triana Mejia | 2026-05-22 | Resuelta — deuda pendiente |
| 4 | **Desviación documentada:** `IIntegracionAlpaca.crearCuenta` recibe entidades JPA en lugar de DTO. Deuda: crear `PerfilAlpacaDTO`. Documentada en `ARQUITECTURA.md` §9.6. | Juan Diego Triana Mejia | 2026-05-22 | Resuelta — deuda pendiente |

---

## Definición de terminado

- [x] Al confirmar registro BASICO, `integracion_inversionista.alpaca_account_id` contiene UUID válido de Alpaca sandbox.
- [x] Al confirmar pago Stripe premium, ídem.
- [x] Si Alpaca falla, `inversionista.pendiente_cuenta_alpaca = true` y la cuenta local queda `ACTIVA`.
- [x] Eventos `REGISTRO_EXITOSO` y `REGISTRO_FALLO_ALPACA` visibles en `logs/audit.log`.
- [x] Credenciales Alpaca leídas de `application.properties`, nunca hardcodeadas.
- [x] `docs/PROGRESO.md` marcado con ✅ para HU-2.
- [ ] Deuda técnica D3 (interfaz para repositorio) y D4 (PerfilAlpacaDTO) registradas para resolución post-MVP.

---

## Historial de cambios

| Versión | Fecha | Descripción | Razón |
|---|---|---|---|
| 1.0 | 2026-05-24 | Refactorización a estructura SDD del proyecto. Spec original reemplazado con flujo técnico detallado, payload Alpaca real, desviaciones arquitectónicas documentadas, criterios Gherkin y tabla de riesgos. | Unificación de todos los SPEC.md bajo plantilla canónica SDD del proyecto |
