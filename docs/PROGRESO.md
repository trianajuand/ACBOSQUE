# Progreso del Proyecto

> **Este archivo es el "diario de bitácora" del proyecto.** Debe actualizarse cada vez que se termine una historia o se cierre un sprint. Es la primera fuente de verdad para saber dónde vamos.

---

## Estado general

- **Fecha última actualización:** 2026-05-24 (normalización 3NF completa — modelo de BD migrado)
- **Sprint actual:** Sprint 4 — Comisionista + Administración + Notificaciones
- **Sprints completados:** Sprint 1 (Autenticación + Perfil), Sprint 2+3 (Mercado + Órdenes)
- **Historias del MVP completadas:** 35 / 42 (HU-1 a HU-35 Completadas; HU-36–39, HU-41 En desarrollo)
- **Bloqueos actuales:** HU-5 (JWT revocation) pendiente como deuda técnica

## Migración 3NF (mayo 2026)

Modelo de BD completamente normalizado. Cambios estructurales principales:
- Nuevas tablas: `Activo`, `suscripcion`, `integracion_inversionista`, `propuesta_orden`, `especialidad`, `intereses`, `comisionista_especialidad`, `interes_inversionista`
- `inversionista`, `comisionista`, `administrador` → shared PK con `usuario` (Table-Per-Type)
- `holding` → PK compuesta `(inversionista_id, activo_id)`
- `cuenta_fondos` → PK = `inversionista_id` (shared 1:1)
- `orden` → `activo_id` reemplaza `simbolo`; campos de propuesta separados a `propuesta_orden`
- `parametro_comision` → `fecha_inicio`/`fecha_fin` reemplaza flag `activo`
- `feriado_mercado` → `mercado_config_id` (FK Long) reemplaza `mercado_codigo` (String)
- Campos de contacto/notificación movidos de `inversionista` a `usuario`
- `rango_ingresos` (String) → `ingresos_min`/`ingresos_max` (BigDecimal)

---

## Sprint actual: Sprint 1 — Módulo de Autenticación

**Objetivo:** Tener el Módulo de Autenticación completamente funcional, con todos sus flujos validados end-to-end (registro, login, MFA, logout, recuperación de contraseña, bloqueo).

### Historias incluidas

| ID | Historia | Estado | Notas |
|---|---|:-:|---|
| HU-1 | Registro de Inversionista (con verificación por correo) | ✅ | Incluye selección de plan (Básico/Premium + Stripe). |
| HU-3 | Inicio de sesión con credenciales (BCrypt + JWT) | ✅ | Login redirige al dashboard.html. |
| HU-4 | Autenticación multifactor (MFA) por correo | ✅ | Código 6 dígitos, TTL 10 min. |
| HU-5 | Cierre de sesión con invalidación de JWT | ⬜ | Tabla `token_revocado` pendiente (postpuesto). |
| HU-6 | Consultar información de inversionista | ✅ | GET /api/perfil — integrado en dashboard. |
| HU-7 | Actualizar datos personales | ✅ | PUT /api/perfil — integrado en dashboard. |
| HU-8 | Configurar preferencias de notificación | ✅ | PUT /api/perfil/preferencias/notificaciones. |
| HU-9 | Configurar preferencias de operación | ✅ | PUT /api/perfil/preferencias/operacion. |
| HU-10 | Activar/desactivar MFA opcional (Inversionista) | ✅ | PUT /api/perfil/mfa?activar=true/false. |
| EC-09 | Bloqueo tras 5 intentos fallidos (15 min) | ✅ | Tabla `intento_fallido` + notificación al titular. |
| — | Recuperación de contraseña (forgot + reset) | ✅ | Token 6 dígitos, TTL 30 min, single-use. |
| — | `IAuditLog` mínimo (consola + archivo) | ✅ | Todos los eventos auditados vía AuditLogService. |
| — | Dashboard simulado broker (HTML) | ✅ | dashboard.html con mercado, órdenes, portafolio, perfil. |
| — | Stripe integrado en registro (HU-11 parcial) | ✅ | StripeAdapter + OrquestadorSuscripcion + checkout URL. |

### Tareas técnicas de soporte (no son HU pero son necesarias)

- [ ] Bootstrap proyecto Spring Boot con Maven, Java 17, packaging war
- [ ] Estructura de carpetas de Monolito Modular según `ARQUITECTURA.md`
- [ ] Configuración PostgreSQL local (placeholders en `application.properties`)
- [ ] Configuración SMTP Gmail con app password (placeholders)
- [ ] Configuración JWT secret de 256 bits
- [ ] `CorsConfig` global
- [ ] `SecurityConfig` con `SecurityFilterChain`
- [ ] `GlobalExceptionHandler`
- [ ] `EmailSender` portado de Malwatcher (sin secretos hardcodeados)
- [ ] `IAuditLog` con implementación de consola + archivo
- [ ] HTML de prueba para validar todos los endpoints (ver sección 7 de la guía)
- [x] Bootstrap proyecto Angular con `ng new`
- [x] Normalización 3NF del esquema: `Orden` usa `inversionistaId`/`activoId`, `PropuestaOrden` separada, `Inversionista`/`Comisionista` con PK compartida. OrdenService, AdministracionService, NormalizacionUsuarios3FnMigration, DatosInicialesComisionistas y servicios relacionados actualizados.

### Definition of Done del Sprint 1

Todas las historias se consideran cerradas cuando:
1. ✅ Compila sin errores ni warnings críticos.
2. ✅ Pasa el plan de pruebas end-to-end en el HTML de test (los 7 escenarios de la sección 5 de la guía).
3. ✅ Cada flujo dispara los eventos de auditoría correspondientes (verificable en `logs/audit.log`).
4. ✅ El frontend Angular renderiza cada pantalla, valida formularios, llama al backend, maneja errores en español.
5. ✅ Los checkboxes de este archivo están marcados.
6. ✅ Existe un commit por historia y un tag `v0.1-sprint1` al final.

---

## Sprint 1 — Módulo de Autenticación ✅ COMPLETADO

**Resultado:** Módulo de Autenticación completamente funcional end-to-end.

| ID | Historia | Estado | Notas |
|---|---|:-:|---|
| HU-1 | Registro de Inversionista (con verificación por correo) | ✅ | Incluye selección de plan (Básico/Premium + Stripe). |
| HU-3 | Inicio de sesión con credenciales (BCrypt + JWT) | ✅ | Login redirige al dashboard.html. |
| HU-4 | Autenticación multifactor (MFA) por correo | ✅ | Código 6 dígitos, TTL 10 min. |
| HU-5 | Cierre de sesión con invalidación de JWT | ⬜ | Tabla `token_revocado` pendiente (postpuesto). |
| HU-6 | Consultar información de inversionista | ✅ | GET /api/perfil — integrado en dashboard. |
| HU-7 | Actualizar datos personales | ✅ | PUT /api/perfil — integrado en dashboard. |
| HU-8 | Configurar preferencias de notificación | ✅ | PUT /api/perfil/preferencias/notificaciones. |
| HU-9 | Configurar preferencias de operación | ✅ | PUT /api/perfil/preferencias/operacion. |
| HU-10 | Activar/desactivar MFA opcional (Inversionista) | ✅ | PUT /api/perfil/mfa?activar=true/false. |
| EC-09 | Bloqueo tras 5 intentos fallidos (15 min) | ✅ | Tabla `intento_fallido` + notificación al titular. |
| — | Recuperación de contraseña (forgot + reset) | ✅ | Token 6 dígitos, TTL 30 min, single-use. |
| — | `IAuditLog` mínimo (consola + archivo) | ✅ | Todos los eventos auditados vía AuditLogService. |
| — | Dashboard simulado broker (HTML) | ✅ | dashboard.html con mercado, órdenes, portafolio, perfil. |
| — | Stripe integrado en registro (HU-11 parcial) | ✅ | StripeAdapter + OrquestadorSuscripcion + checkout URL. |

---

## Sprint actual: Sprint 2 + 3 — Mercado y Órdenes (en validación)

**Objetivo:** Tener el Módulo de Mercado y el Módulo de Órdenes funcionales con APIs reales (Alpaca + Alpha Vantage).

### Historias implementadas (backend completo, pendiente validación end-to-end)

| ID | Historia | Estado | Notas |
|---|---|:-:|---|
| HU-2 | Integración Alpaca al registrar inversionista | ✅ | `OrquestadorRegistro` invoca `AlpacaAdapter.crearCuenta()`. Guarda `alpacaAccountId` en `Inversionista`. Maneja fallo (pendiente + notificación admin). |
| HU-13 | Dashboard de acciones de interés | ✅ | `GET /api/mercado/dashboard` — retorna cotizaciones de los `interesesMercado` del usuario. Caché de 3 min. Usa Alpaca para US, Alpha Vantage para globales. |
| HU-14 | Detalle de una acción | ✅ | `GET /api/mercado/detalle/{simbolo}` — precio, historial, métricas. |
| HU-15 | Visualización del portafolio | ✅ | `GET /api/portafolio` — holdings, precio promedio, valor total, ganancia/pérdida. |
| HU-16 | Consultar saldo y comisiones | ✅ | `GET /api/portafolio/saldo` — saldo disponible, fondos reservados, comisiones desglosadas. |
| HU-17 | Orden Market | ✅ | `POST /api/ordenes` con `tipoOrden: MARKET`. Envía a Alpaca sandbox para US. |
| HU-18 | Orden Limit | ✅ | Mismo endpoint, `tipoOrden: LIMIT` + `precioLimite`. |
| HU-19 | Orden Stop Loss | ✅ | `tipoOrden: STOP_LOSS` + `precioStop`. |
| HU-20 | Orden Take Profit | ✅ | `tipoOrden: TAKE_PROFIT` + `precioLimite`. |
| HU-21 | Cancelar orden pendiente | ✅ | `DELETE /api/ordenes/{ordenId}`. Libera fondos reservados. Cancela en Alpaca si aplica. |
| HU-22 | Consultar órdenes activas | ✅ | `GET /api/ordenes/activas` — estados PENDIENTE, ENVIADA, EN_COLA. |
| HU-23 | Encolamiento fuera de horario | ✅ | `ColaOrdenesService` con `@Scheduled` cada minuto. Procesa EN_COLA al abrir mercado. |
| HU-24 | Historial de órdenes por período | ✅ | `GET /api/ordenes/historial?desde=&hasta=` conectado a filtros Angular. |
| HU-25 | Historial por tipo y activo | ✅ | Filtros `tipoOrden` y `simbolo` en backend y frontend. |
| HU-26 | Historial por estado | ✅ | Filtro `estado` en backend y frontend. |
| EC-13 | Previsualización de comisión antes de confirmar | ✅ | `POST /api/ordenes/previsualizar` — muestra comisión (2%) y split plataforma/comisionista. |
| — | Verificador de horarios de mercado | ✅ | `GET /api/mercado/horario/{mercado}` — NYSE/NASDAQ, TSE, LSE con horarios reales. |
| — | Caché de precios (Maintain Multiple Copies) | ✅ | `PrecioCache` en BD. Refresco automático cada 3 min con `@Scheduled`. |
| — | Depósito de fondos (sandbox/pruebas) | ✅ | `POST /api/portafolio/depositar?monto=X` — solo para pruebas. |
| — | Sincronización saldo con Alpaca | ✅ | `POST /api/portafolio/sincronizar` — trae saldo real de la cuenta Alpaca. |

### Estado de las APIs externas

| API | Estado | Credenciales |
|---|:-:|---|
| **Alpaca Broker** (crear cuenta, órdenes, cancelar) | ✅ Real sandbox | `alpaca.broker.*` en `application.properties` |
| **Alpaca Market Data** (snapshots, precios US) | ✅ Real sandbox | Mismas credenciales, `alpaca.data.base-url` |
| **Alpha Vantage** (datos globales TSE, LSE) | ✅ Real | Key configurada en `application.properties` |
| **Stripe** | ⬜ Placeholder | `TU_STRIPE_SECRET_KEY_TEST` — pendiente configurar |

### Diagnóstico realizado (2026-05-06)

- Se confirmó que el backend implementa llamadas HTTP reales a Alpaca y Alpha Vantage (no simuladas).
- Se detectó que una instancia antigua del backend quedaba corriendo en el puerto 8080. **Solución:** `taskkill /PID <pid> /F` antes de `mvn spring-boot:run`.
- La clave de Alpha Vantage fue configurada por el equipo el 2026-05-06.

### Correcciones y mejoras aplicadas (2026-05-06) — segunda sesión

#### Bug 1 — `INSERT en transacción de sólo lectura` en `GET /api/portafolio/saldo`
- **Síntoma:** `org.postgresql.util.PSQLException: ERROR: no se puede ejecutar INSERT en una transacción de sólo lectura`
- **Causa raíz:** `OrdenService.obtenerSaldo` tenía `@Transactional(readOnly = true)`. Spring REQUIRED propagation hace que `SaldoService.obtenerSaldoDTO → obtenerOCrear` herede el contexto readOnly de la transacción exterior. El INSERT de creación de `CuentaFondos` fallaba porque PostgreSQL recibe la sesión marcada como readOnly.
- **Fix:** cambiar `@Transactional(readOnly = true)` a `@Transactional` en `OrdenService.obtenerSaldo` (línea 303).
- **Lección:** siempre fijar la transacción EXTERIOR, no solo la interior; la propagación REQUIRED hace que el flag readOnly se herede hacia abajo.

#### Bug 2 — Mercado siempre mostraba "cerrado" para acciones US
- **Causa raíz:** `MercadoService.detectarMercado()` retorna `"NYSE/NASDAQ"` para símbolos US, pero el switch en `esMercadoAbierto()` solo tenía casos `"NYSE"`, `"NASDAQ"`, `"AMEX"`, `"US"`. El string compuesto caía en `default → false`.
- **Fix:** añadir `"NYSE/NASDAQ"` como caso explícito en el switch.

#### Mejora 3 — Modo sandbox para horarios de mercado
- **Motivación:** durante desarrollo/pruebas el servidor está activo fuera del horario NYSE (después de 4 PM EDT / 3 PM Colombia). Las órdenes fallaban con "mercado cerrado".
- **Fix:** property `app.mercado.sandbox-siempre-abierto=true` en `application.properties`. `MercadoService` lee `@Value("${app.mercado.sandbox-siempre-abierto:false}")` y si es `true`, `esMercadoAbierto()` retorna `true` sin importar la hora.
- **Para producción:** cambiar a `false`.

#### Bug 4 — Horario TSE incorrecto
- **Causa raíz:** código tenía `esMercadoTokioAbierto` con cierre a las 15:30 y pausa al mediodía (11:30-12:30). El spec del proyecto dice 9:00 AM a 3:00 PM sin pausa.
- **Fix:** simplificado a `totalMinutos >= 9 * 60 && totalMinutos < 15 * 60`.

#### Mejora 5 — Añadir soporte ASX (Bolsa de Sídney)
- **Spec:** 10:00 AM a 4:00 PM (Australia/Sydney), lunes a viernes.
- **Fix:** nuevo método `esMercadoSidneyAbierto()`, añadido al switch de `esMercadoAbierto`, y `.AX` en `detectarMercado()`.

#### Mejora 6 — `obtenerCotizacion` usa caché cuando mercado está cerrado
- **Motivación:** Alpaca no retorna snapshots fuera de horario → precio cero → órdenes con monto $0.
- **Fix:** si `!esMercadoAbierto(detectarMercado(simbolo))` y existe caché, retornar caché sin llamar a Alpaca.

#### Mejora 7 — `dashboard.html` completamente reescrito para llamar endpoints reales
- **Antes:** datos hardcodeados en JS (mercado, órdenes, portafolio).
- **Después:** todos los paneles consumen el backend:
  - Dashboard → `GET /api/mercado/dashboard`, `GET /api/portafolio/saldo`, `GET /api/portafolio`, `GET /api/ordenes/historial`
  - Mercado → tabla de `dashboard` + buscador `GET /api/mercado/cotizacion/{simbolo}`
  - Órdenes → `POST /api/ordenes/previsualizar` (modal) → `POST /api/ordenes`, `DELETE /api/ordenes/{id}`, `POST /api/portafolio/depositar`
  - Portafolio → `GET /api/portafolio` + `GET /api/portafolio/saldo`
  - Auto-refresco de mercado cada 3 minutos con `setInterval`.

### Pendiente para cerrar este sprint

- [ ] Reiniciar servidor (`mvn spring-boot:run`) para que todas las correcciones Java tomen efecto.
- [ ] Validar end-to-end en Postman: `GET /api/mercado/dashboard`, `GET /api/portafolio/saldo`, `POST /api/ordenes/previsualizar`, `POST /api/ordenes`.
- [ ] Confirmar que `POST /api/ordenes` con `MARKET/COMPRA/AAPL` crea la orden en Alpaca sandbox.
- [ ] Confirmar que el dashboard.html muestra datos reales tras login.

---

## Sprint 4 — Comisionista + Administración + Notificaciones

| ID | Historia | Estado | Notas |
|---|---|:-:|---|
| HU-24 | Historial de órdenes por período | ✅ | Filtros desde/hasta |
| HU-25 | Historial por tipo y activo | ✅ | Filtros tipoOrden/simbolo |
| HU-26 | Historial por estado | ✅ | Filtro estado |
| HU-27 | Reporte personal de actividad (PDF/Excel) | ✅ | GET /api/reportes/personal |
| HU-28 | Consultar portafolio de cliente asignado | ✅ | EC-18 Encapsulate |
| HU-29 | Consultar órdenes de cliente asignado | ✅ | Activas + historial |
| HU-30 | Proponer orden para cliente | ✅ | Estado PENDIENTE_APROBACION |
| HU-31 | Aprobar/rechazar propuesta del comisionista | ✅ | DecisionPropuestaDTO opcional |
| HU-32 | Firmar y enviar orden aprobada al mercado | ✅ | Recalcula precio y reserva fondos |
| HU-33 | Configurar mercados habilitados | ✅ | GET+PUT /api/admin/mercados |
| HU-34 | Configurar feriados de mercado | ✅ | GET+POST+DELETE /api/admin/mercados/{id}/feriados |
| HU-35 | Configurar parámetros de comisión | ✅ | GET+PUT /api/admin/comisiones |
| HU-36 | Crear cuenta de comisionista | 🔄 | En desarrollo — endpoint implementado |
| HU-37 | Asignar comisionista a inversionista | 🔄 | En desarrollo — endpoint implementado |
| HU-38 | Suspender/reactivar cuenta | 🔄 | En desarrollo — endpoint implementado |
| HU-39 | Eliminar cuenta (baja lógica) | 🔄 | En desarrollo — endpoint implementado |
| HU-41 | Despacho de notificaciones multicanal | 🔄 | En desarrollo — Email ✅; SMS/WhatsApp pendiente credenciales |

---

## Sprints futuros (alta granularidad — refinar al iniciar cada uno)

### Sprint 2 — Módulo de Mercado
- HU-13: Dashboard de acciones de interés.
- HU-14: Detalle de acción.
- HU-8, HU-9: Preferencias de notificación y operación (parte que toca al usuario).
- Integración con Alpha Vantage (Adapter en `integracion/adaptadores/alphavantage/`).
- Caché de precios (táctica Maintain Multiple Copies of Data).
- Verificador de horarios de mercado.

### Sprint 3 — Módulo de Órdenes (parte 1: inversionista)
- HU-15: Visualización de portafolio.
- HU-16: Saldo y comisiones.
- HU-17: Orden Market.
- HU-18 a HU-20: Orden Limit / Stop Loss / Take Profit.
- HU-21: Cancelar orden pendiente.
- HU-22: Consultar órdenes activas.
- HU-23: Encolamiento fuera de horario.
- Integración con Alpaca (Adapter en `integracion/adaptadores/alpaca/`).
- HU-2: Integración Alpaca al registrar inversionista (orquestador de registro).

### Sprint 4 — Módulo de Órdenes (parte 2: comisionista) + Trazabilidad real
- HU-28 a HU-32: flujo completo del comisionista (consulta, propuesta, aprobación, firma).
- Integración real con Splunk o Elasticsearch para `IAuditLog`.
- HU-40: Logs auditables de todas las operaciones críticas.

### Sprint 5 — Módulo de Administración
- HU-33: Configurar mercados.
- HU-34: Configurar feriados.
- HU-35: Configurar comisiones (% y split).
- HU-36 a HU-39: gestión de usuarios (crear comisionista, asignar, suspender, eliminar).
- HU-11, HU-12: Suscripción premium con Stripe.
- Integración con Stripe (Adapter en `integracion/adaptadores/stripe/`).

### Sprint 6 — Notificaciones multicanal + reportes + pulido final
- HU-41: Despacho de notificaciones por Email/SMS/WhatsApp. ✅ Backend completo. Pendiente: UI en perfil para configurar canal desde el dashboard Angular, y activar Twilio real con credenciales.
- HU-42: Detección de fallo de servicio + alerta. ⬜ Pendiente.
- HU-27: Reporte personal de actividad. ✅ Completado en sesión anterior.
- Pruebas de carga con JMeter (1500 usuarios concurrentes — RNF-09). ⬜ Pendiente.
- Documentación final y entrega. ⬜ Pendiente.

---

## Bitácora de cambios y decisiones

> Anotar aquí las decisiones técnicas relevantes que se tomen durante el desarrollo, con fecha y motivo. Esto ayuda a recordar el porqué de elecciones que después parezcan obvias o cuestionables.

### [Fecha] — Decisión inicial: estructura Monolito Modular por módulo
Se rechazó la estructura por capas tradicional para alinear con la arquitectura del informe de ingeniería. Cada módulo replica internamente `controller/service/repository/model/dto/interfaces` y se comunica con otros módulos únicamente a través de interfaces `I...`.

### [2026-05-06] — Alpha Vantage key configurada en application.properties
Clave gratuita obtenida de alphavantage.co y colocada en `alphavantage.api-key`. Reemplaza el placeholder anterior `TU_ALPHA_VANTAGE_API_KEY`.


### [2026-05-06] — Diagnóstico: proceso viejo bloqueaba puerto 8080
Al hacer cambios en el código (agregar mercado/órdenes) y relanzar con `mvn spring-boot:run`, el proceso anterior no se detenía automáticamente. La nueva instancia fallaba silenciosamente y la vieja (sin los controllers nuevos) seguía respondiendo. Solución: siempre detener el proceso anterior (`Ctrl+C` o `taskkill`) antes de relanzar.

### [2026-05-06] — Dashboard HTML reescrito para conectar endpoints reales
El `dashboard.html` fue completamente reescrito para consumir los endpoints reales del backend (`/api/mercado/*`, `/api/ordenes/*`, `/api/portafolio/*`). Ya no usa datos hardcodeados. Requiere reinicio del servidor para servir el archivo actualizado desde `target/classes/static/`.

### [2026-05-06] — Correcciones en MercadoService: sandbox mode, TSE horario, ASX añadido
- `esMercadoAbierto` corregido para manejar `"NYSE/NASDAQ"` como string combinado.
- Property `app.mercado.sandbox-siempre-abierto=true` añadida para testing fuera de horario.
- TSE corregido a 9:00-15:00 (sin pausa mediodía) per spec.
- ASX añadido (10:00-16:00 Australia/Sydney).
- `obtenerCotizacion` retorna caché cuando mercado cerrado (evita precio $0 de Alpaca).

### [2026-05-06] — Fix @Transactional readOnly en OrdenService.obtenerSaldo
Spring REQUIRED propagation hace que el flag `readOnly` de la transacción exterior se herede hacia abajo. `SaldoService.obtenerSaldoDTO → obtenerOCrear` intentaba hacer INSERT con sesión readOnly. Fix: cambiar `@Transactional(readOnly = true)` a `@Transactional` en el método exterior `OrdenService.obtenerSaldo`.

### [2026-05-18] — Frontend Angular inicial
Se creo el proyecto Angular en `frontend/` con rutas separadas para login, registro, verificacion de registro, recuperacion, reset de contrasenia y dashboard. El dashboard consume los endpoints existentes de perfil, mercado, ordenes y portafolio, mantiene la paleta oscura minimalista de los HTML de prueba y deja `dashboard.html` / `test-auth.html` como referencia legacy.

### [2026-05-20] — Registro de inversionista ampliado por fases
Se amplio HU-1 en backend y frontend: el registro Angular ahora funciona como wizard de 4 fases (datos e identidad, perfil financiero, preferencias, plan). El backend persiste identidad, direccion, codigo postal, estilo de trading, rango de ingresos, preferencias iniciales y solicitud de comisionista; mantiene el flujo existente de codigo de verificacion y redireccion a Stripe para planes premium.

### [2026-05-20] — Correccion flujo Stripe y validacion temprana de correo
Se corrigieron las URLs de retorno de Stripe para volver al frontend Angular (`/login`) en lugar del HTML legacy `test-auth.html`. Tambien se agrego `GET /api/auth/register/email-disponible` y el wizard valida el correo al salir de la primera fase para evitar avanzar con correos ya registrados.

### [2026-05-20] — Especificaciones retrospectivas por HU implementada
Se analizaron backend, frontend, endpoints, servicios, DTOs, modelos, validaciones y flujos UI existentes para generar 28 carpetas `docs/HU-*/spec.md` con especificaciones funcionales y tecnicas retrospectivas de las funcionalidades implementadas.

### [2026-05-20] — Normalizacion 3FN de usuarios y sesiones por pestaña
Se separo `usuario` como tabla general de identidad/acceso/rol, dejando datos propios del inversionista en `inversionista` (Alpaca, Stripe, perfil financiero, preferencias y notificaciones) y especialidades en `comisionista`. Se agrego migracion defensiva para mover columnas legacy y se cambio el JWT Angular a `sessionStorage` para evitar que sesiones de comisionista e inversionista se pisen al refrescar.

### [2026-05-20] — Refuerzo migracion Postgres usuario/inversionista/comisionista
Se reforzo `NormalizacionUsuarios3FnMigration` para recrear/asegurar el esquema esperado en PostgreSQL: `usuario` queda solo con identidad, acceso, rol, estado y MFA; `inversionista` concentra Alpaca, Stripe, premium, perfil financiero, preferencias y notificaciones; `comisionista` conserva especialidades y vinculo a usuario. La migracion copia columnas legacy por tipo, agrega restricciones y elimina sobrantes en `usuario`.

### [2026-05-20] — Mercado con refresco por API y filtros de historial
`MercadoService.obtenerCotizacion` ahora intenta refrescar desde Alpaca Market Data o Alpha Vantage antes de responder y usa `precio_cache` solo como respaldo si el proveedor falla. Tambien se implementaron filtros de historial de ordenes por periodo, tipo, activo y estado en `/api/ordenes/historial` y en el dashboard Angular.

### [2026-05-20] — Modulo administrador con MFA obligatorio y configuracion en BD
Se implemento el modulo administrador separado del inversionista y comisionista: tabla `administrador` para perfil manual, endpoints `/api/admin/*`, vista Angular `/admin`, gestion de mercados/horarios/feriados, parametros de comision y split desde BD, creacion de comisionistas con MFA obligatorio, asignacion de comisionistas, estados de cuenta, baja logica y estado `OPERACIONES_RESTRINGIDAS` para impedir nuevas ordenes. Tambien se agrego dashboard ejecutivo con filtros por periodo y tendencias por mercado.

### [2026-05-21] — Ajuste visual y documentacion del modulo administrador
Se separo visualmente `/admin` del dashboard de inversionista, se reforzo la redireccion por rol para evitar que un administrador cargue `/api/perfil`, se valido login admin con MFA y endpoint `/api/admin/dashboard`, y se actualizaron los SPEC de HU-33 a HU-39. Se agrego `docs/HU-48-dashboard-ejecutivo-metricas-admin/SPEC.md` para documentar el dashboard ejecutivo implementado.

### [2026-05-22] — Notificaciones multicanal (HU-41): Email, SMS y WhatsApp

**Alcance:** Sistema completo de notificaciones multicanal por eventos de negocio (órdenes, mercado, resumen diario) con canal configurable por usuario y opción de desactivar todas las notificaciones.

**Archivos creados (5):**
| Archivo | Propósito |
|---|---|
| `integracion/notificaciones/canales/SmsSender.java` | Adaptador Twilio SMS. Si `app.notificaciones.sms.habilitado=false` simula en log (modo dev). |
| `integracion/notificaciones/canales/WhatsAppSender.java` | Adaptador Twilio WhatsApp Sandbox. Mismo flag para dev. |
| `integracion/notificaciones/ContextoNotificacion.java` | DTO interno con datos de contacto y preferencias; factory `desde(NotificacionPreferenciasDTO)`. |
| `autenticacion/dto/NotificacionPreferenciasDTO.java` | DTO neutro que cruza frontera autenticacion → ordenes sin exponer entidades. |
| (canales existentes actualizados) | `EmailSender` + `construirHtmlEvento` + `enviarMensajeLibre` |

**Archivos modificados (10):**
- `INotificacion` → nuevos métodos: `notificarOrdenCreada`, `notificarOrdenCancelada`, `notificarOrdenEjecutada`, `notificarOrdenFallida`, `notificarOrdenEncolada`, `notificarAperturaMercado`, `notificarCierreMercado`, `notificarResumenDiario`.
- `DespachadorNotificaciones` → enruta cada evento al/los canal(es) activos del usuario; Twilio SMS/WhatsApp con modo simulado en dev.
- `IConsultaInversionista` → nuevo método `obtenerPreferenciasNotificacion(Long usuarioId)`.
- `ConsultaInversionistaService` → implementa el nuevo método.
- `Inversionista` (modelo) → nuevo campo `notificaciones_activas` (interruptor maestro).
- `PreferenciasNotificacionDTO` → campo `notificacionesActivas`.
- `PerfilInversionistaDTO` → campo `notificacionesActivas`.
- `PerfilService` → persiste y mapea el nuevo campo; default `true`.
- `OrdenService` → inyecta `INotificacion`; notifica en crear, cancelar, ejecutar.
- `ColaOrdenesService` → inyecta `INotificacion`; notifica apertura de mercado y ejecución de órdenes encoladas.
- `pom.xml` → dependencia `com.twilio.sdk:twilio:10.6.4`.
- `application.properties` → variables `twilio.*` y `app.notificaciones.*.habilitado`.

**Variables de entorno necesarias (configurar en `.env`):**
```
TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_AUTH_TOKEN=your_auth_token
TWILIO_SMS_FROM=+15551234567
TWILIO_WHATSAPP_FROM=whatsapp:+14155238886
NOTIF_SMS_HABILITADO=false   # true para activar Twilio real
NOTIF_WHATSAPP_HABILITADO=false
```

**Modo desarrollo:** `NOTIF_SMS_HABILITADO=false` y `NOTIF_WHATSAPP_HABILITADO=false` simulan el envío en el log (`[SMS-SIMULADO]`, `[WHATSAPP-SIMULADO]`). Email sigue usando Gmail SMTP y es siempre real.

**Canal preferido en el registro:** el wizard de registro ya tiene la fase de preferencias que guarda `notificacionEmail/Sms/Whatsapp` en BD. El usuario puede cambiarlo desde `PUT /api/perfil/preferencias/notificaciones` con el nuevo campo `notificacionesActivas` (interruptor maestro para deshabilitar todo).

### [2026-05-25] — Auditoría SDD completa: coherencia código ↔ documentación

Se realizó auditoría exhaustiva de 14 puntos críticos comparando código Java real con todos los documentos SDD. Resultados:
- **14 archivos .md corregidos** (ver tabla en sección "Auditoría SDD" de este archivo).
- **8 deudas técnicas documentadas** (DT-01 a DT-08).
- **3 preguntas abiertas** que requieren decisión de negocio (PA-1 a PA-3).
- El código Java **no fue modificado**: las correcciones son exclusivamente documentales (los docs reflejan ahora el comportamiento real del código).

### [2026-05-24] — Refactorización SDD completa de todos los SPEC.md (HU-1 a HU-41)

Se reescribieron todos los archivos `docs/HU-*/SPEC.md` bajo la plantilla canónica SDD del proyecto. Cada spec ahora incluye: ficha de trazabilidad, historia de usuario, motivación y contexto, actores y precondiciones, flujo principal, flujos de error individuales con código HTTP, contrato de API en YAML, modelo de datos DDL, módulos y arquitectura con interfaces nombradas, eventos y efectos transversales, riesgos con tabla P×I, criterios de verificación en Gherkin, interfaz de usuario, fuera de alcance, decisiones abiertas, definición de terminado y historial de cambios.

- **HU-1 a HU-35:** Estado `Completada`. Specs completos con todos los artefactos SDD.
- **HU-36 a HU-39, HU-41:** Estado `En desarrollo`. Funcionalidad implementada, pendiente validación en entorno integrado. Checkboxes de DoD parcialmente marcados.
- **HU-40, HU-42–47:** No aplica en este ciclo (omitidas según instrucciones del proyecto).

### [2026-05-22] — Mejoras funcionales: cancelación premium, reporte PDF, correcciones de cola

**Cambios implementados:**

1. **Comisionista por intereses** — Se auditó `AsignacionComisionistaService`; la lógica es correcta (ordena por coincidencias de intereses, desempate por menor carga). Si siempre se asigna el mismo comisionista es porque hay solo uno activo en el sistema o sus especialidades coinciden con todos los perfiles de prueba.

2. **Portafolio completo** — Verificado: el frontend ya muestra los 6 campos requeridos (holdings, cantidad, precio promedio, precio mercado, valor total, ganancia/pérdida con %).

3. **Cancelar suscripción premium** (HU-11 parcial):
   - `PerfilService.cancelarSuscripcion(correo)` — cancela en Stripe (tolerante a placeholder) y pone `esPremium=false`, `planSuscripcion=BASICO`, borra `stripeSuscripcionId`.
   - `DELETE /api/perfil/suscripcion` en `PerfilController`.
   - Frontend: botón rojo "Cancelar suscripcion premium" visible solo si `perfil.esPremium`, con `window.confirm()` como confirmación.

4. **Reporte PDF** (HU-27):
   - Dependencia OpenPDF 1.3.43 añadida a `pom.xml`.
   - `ReporteService` en módulo ordenes: genera PDF con resumen financiero, tabla de órdenes ejecutadas y top activos más operados.
   - `GET /api/ordenes/reporte?desde=&hasta=` en `ReporteController`.
   - Frontend: nuevo panel "Reporte" en el dashboard con selector de fechas y botón "Descargar PDF". `ApiService.getBlob()` para descarga binaria.

5. **Cola + Alpaca** (sesión anterior) — Corrección: cuando el mercado está cerrado, las órdenes US se envían inmediatamente a Alpaca (estado `accepted`) y quedan EN_COLA localmente. Al abrir el mercado, `ColaOrdenesService` las promueve a ENVIADA sin reenviar.

### [2026-05-22] — Refactoring arquitectónico: Monolito Modular completo y sin violaciones de módulo

**Contexto:** El código venía de una implementación SOA. Se auditó todo el backend para garantizar que ningún módulo importe clases internas de otro. Se encontraron y corrigieron violaciones críticas de frontera.

**Nuevos archivos creados (7):**

| Archivo | Propósito |
|---|---|
| `autenticacion/dto/UsuarioGestionDTO.java` | DTO neutro que cruza la frontera autenticación → administración sin exponer entidades JPA. |
| `autenticacion/interfaces/IGestionCuentas.java` | Expone gestión de ciclo de vida de usuarios (crear comisionista, asignar, suspender, eliminar) al módulo Administración. |
| `autenticacion/interfaces/IConsultaInversionista.java` | Expone consultas de inversionista (validar, alpacaAccountId, portafolio) al módulo Órdenes. |
| `autenticacion/service/GestionCuentasService.java` | Implementa `IGestionCuentas`; toda la lógica de usuario sigue dentro del módulo autenticación. |
| `autenticacion/service/ConsultaInversionistaService.java` | Implementa `IConsultaInversionista`; encapsula acceso a `UsuarioRepository` e `InversionistaRepository`. |
| `integracion/notificaciones/INotificacion.java` | Interfaz que `DespachadorNotificaciones` implementa; los servicios de autenticación la inyectan en lugar de la clase concreta. |
| `ordenes/dto/ResumenNegocioDTO.java` + `ResumenMercadoDTO.java` | DTOs para que `IOrden.obtenerResumenNegocio()` entregue métricas al dashboard ejecutivo de Administración. |

**Archivos modificados (8):**

- `DespachadorNotificaciones` → añadido `implements INotificacion`.
- `AutenticacionService`, `RegistroService`, `RecuperacionPasswordService` → inyectan `INotificacion` en lugar del tipo concreto.
- `AdministracionService` → reescrito completamente; eliminados todos los imports de `autenticacion.*` y `ordenes.*`; inyecta `IGestionCuentas` e `IOrden`; constructor reducido de 12 a 7 parámetros.
- `OrdenService` → eliminado import de `EstadoCuenta` (autenticación); `validarUsuarioPuedeOperar` delega a `IConsultaInversionista`; acceso a `alpacaAccountId` vía interfaz.
- `IAdministracion` → limpiada de 3 métodos de comisión duplicados (ya presentes en `IGestorParametros`); queda solo como interfaz de lectura para el módulo Mercado.
- `IOrden` → nuevo método `obtenerResumenNegocio(LocalDateTime, LocalDateTime, String)`.

**Excepción pragmática documentada:** `IIntegracionAlpaca.crearCuenta(Usuario, Inversionista)` mantiene la firma original con tipos del módulo autenticación. El cambio de lógica de negocio estaba fuera del alcance y la creación on-demand de cuenta Alpaca ya funcionaba correctamente. Esto está anotado en `ARQUITECTURA.md` sección de interfaces.

**Resultado:** todos los módulos se comunican exclusivamente vía interfaces `I...`. No quedan imports directos entre módulos.

---

## Métricas del proyecto

| Métrica | Valor actual |
|---|---|
| Historias completadas | 35 / 42 (HU-36–39, HU-41 en desarrollo) |
| Cobertura de tests (services) | — |
| Cobertura de tests (global) | — |
| Endpoints REST funcionando | ~35 (auth + perfil + mercado + órdenes + portafolio + comisionista + admin) |
| Módulos con interfaces definidas | 6 / 6 (Auth, Mercado, Órdenes, Administración, Integración, Trazabilidad) |
| Eventos auditables registrados | ✅ vía `AuditLogService` (consola + archivo) |

---

## Auditoría SDD — Coherencia código ↔ documentación

**Fecha:** 2026-05-25

### Documentos corregidos

| Documento | Corrección aplicada |
|---|---|
| `docs/HU-32-firmar-enviar-orden-aprobada-mercado/SPEC.md` | P1: `IAlpacaAdapter.enviarOrden(OrdenAlpacaDTO)` → `IIntegracionAlpaca.crearOrden(accountId, simbolo, ...)` |
| `docs/HU-34-configurar-feriados-mercado/SPEC.md` | P2: Eliminada validación de fecha pasada (código no la implementa); P3: `mercado_feriado` → `feriado_mercado` |
| `docs/HU-34-configurar-feriados-mercado/plan.md` | P3: `mercado_feriado` → `feriado_mercado` (hallazgo marcado como resuelto) |
| `docs/HU-23-encolamiento-ordenes-fuera-horario/SPEC.md` | P3: DDL corregido: `mercado_feriado` → `feriado_mercado`, columna `mercado_config_id` |
| `docs/HU-35-configurar-parametros-comision/SPEC.md` | P4: `IParametroComision.obtenerVigente()` → `IGestorParametros.obtenerPorcentajeComision()/obtenerSplitPlataforma()/obtenerSplitComisionista()`; columnas `split_plataforma`/`split_comisionista` corregidas; P8: deuda técnica R3 de `ColaOrdenesService` documentada |
| `docs/HU-37-asignar-comisionista-inversionista/SPEC.md` | P5: Política de reasignación: el código siempre reemplaza (desactiva la anterior), nunca retorna 409 |
| `docs/EC-09-bloqueo-intentos-fallidos/SPEC.md` | P6: Schema real de `intento_fallido` documentado (`correo, contador, bloqueado_hasta, ultimo_intento`; sin columnas `ip` ni `exitoso`) |
| `docs/ARQUITECTURA.md` | P7: Valores canónicos de `TipoEvento` actualizados con los nombres reales del enum |
| `docs/HU-38-suspender-reactivar-cuenta-inversionista/SPEC.md` | P7: `ESTADO_CUENTA_CAMBIADO` → `CAMBIO_ESTADO_CUENTA`; P9: comportamiento sobre órdenes activas documentado; enum `EstadoCuenta` real corregido (INACTIVA, OPERACIONES_RESTRINGIDAS, no SUSPENDIDA/RESTRINGIDA/ELIMINADA) |
| `docs/HU-39-eliminar-cuenta-inversionista/SPEC.md` | P7: `USUARIO_ELIMINADO` → `USUARIO_ADMIN_GESTIONADO`; P10: `ELIMINADA` → `INACTIVA` (estado real en código); nota sobre 409 no implementado |
| `docs/CONVENCIONES.md` | P6: Schema de `intento_fallido` corregido; P11: código OTP recuperación = 6 dígitos (no 64 hex) |
| `docs/RF-08-verificar-horario-mercado/SPEC.md` | P12: Deuda técnica ampliada (horarios hardcodeados, `ColaOrdenesService` no verifica feriados de BD) |
| `docs/SOPORTE-deposito-fondos-sandbox/SPEC.md` | P14: Deuda técnica ALTA: sin `@PreAuthorize`, cualquier usuario autenticado puede depositar |

### Hallazgos de código como deuda técnica documentada

| ID | Descripción | Severidad | Archivo de código |
|---|---|---|---|
| DT-01 | `ColaOrdenesService` usa `@Value("${app.comision.porcentaje:2.0}")` en lugar de `IGestorParametros` para calcular comisiones de órdenes encoladas | ALTA | `ordenes/service/ColaOrdenesService.java` línea 48 |
| DT-02 | `OrdenService`, `ColaOrdenesService` y varios controllers del módulo `ordenes` importan directamente `autenticacion.model.Usuario`, `autenticacion.repository.UsuarioRepository`, `autenticacion.model.Inversionista` — violación de frontera modular (deben usar interfaces `I...`) | MEDIA | `ordenes/service/OrdenService.java`, `ordenes/controller/OrdenController.java`, `ordenes/controller/ComisionistaController.java`, `ordenes/service/ColaOrdenesService.java` |
| DT-03 | `MercadoController` importa `autenticacion.model.Usuario` y `autenticacion.repository.UsuarioRepository` — violación de frontera modular | MEDIA | `mercado/controller/MercadoController.java` |
| DT-04 | `POST /api/portafolio/depositar` sin restricción de rol; cualquier usuario autenticado puede depositar fondos simulados | ALTA | `ordenes/controller/PortafolioController.java` |
| DT-05 | Horarios de mercado hardcodeados en `MercadoService`; no se leen de `mercado_config` en BD | MEDIA | `mercado/service/MercadoService.java` |
| DT-06 | `ColaOrdenesService` no consulta `feriado_mercado` antes de procesar; ignorará feriados configurados por el admin | MEDIA | `ordenes/service/ColaOrdenesService.java` |
| DT-07 | JWT revocation table (`token_revocado`) no implementada — HU-5 pendiente | MEDIA | Toda la cadena de autenticación |
| DT-08 | `GestionCuentasService.eliminarUsuario` no verifica si el usuario ya está INACTIVA antes de aplicar baja (no retorna 409) | BAJA | `autenticacion/service/GestionCuentasService.java` |

### Preguntas abiertas que necesitan decisión humana

| ID | Pregunta | HU/EC afectada |
|---|---|---|
| PA-1 | ¿La suspensión de cuenta debe cancelar inmediatamente las órdenes `PENDIENTE` y `ENVIADA` del usuario, o solo las `EN_COLA` al procesarlas? | HU-38 |
| PA-2 | ¿Se debe validar que la fecha de un feriado sea futura al registrarlo? El código actual acepta fechas pasadas. | HU-34 |
| PA-3 | ¿El plan de escalabilidad requiere que `ColaOrdenesService` lea la comisión de BD (`IGestorParametros`) en lugar de la property? Actualmente órdenes encoladas pueden ejecutarse con la tasa incorrecta si se cambió el parámetro. | HU-35, HU-23 |

---

## Auditoría SDD — HU-1 a HU-39 (cobertura completa)

**Fecha:** 2026-05-26

### HUs revisadas y estado

| HU | Resultado |
|---|---|
| HU-1 | Corrección: `ingresosMin`/`ingresosMax` → `rangoIngresos` (String) en DTO example/schema; DDL elimina columnas `ingresos_min`/`ingresos_max` (no existen en entidad). DT-12 añadida. |
| HU-2 | Sin cambios — SPEC ya correcto con código real. Verificado 2026-05-26. |
| HU-3 | Sin cambios — SPEC ya correcto con código real. Verificado 2026-05-26. |
| HU-4 | Sin cambios — SPEC ya correcto con código real. Verificado 2026-05-26. |
| HU-5 | Sin cambios — SPEC ya documenta estado Pendiente y deuda JWT revocation. Verificado 2026-05-26. |
| HU-6 | Corrección: referencia a `inversionista.comisionistaAsignadoId` → acceso via tabla `asignacion_comisionista` (campo no existe en entidad `Inversionista`). |
| HU-7 | Sin cambios — SPEC ya correcto. Verificado 2026-05-26. |
| HU-8 | Sin cambios — SPEC ya correcto (campos de notificación en `usuario`, endpoint `PUT /api/perfil/preferencias/notificaciones`). Verificado 2026-05-26. |
| HU-9 | Sin cambios — SPEC ya correcto (endpoint `PUT /api/perfil/preferencias/operacion`). Verificado 2026-05-26. |
| HU-10 | Sin cambios — SPEC ya correcto (endpoint `PUT /api/perfil/mfa?activar=`). Verificado 2026-05-26. |
| HU-13 | Corrección: nota de deuda técnica DT-10 añadida — dashboard pasa `""` en lugar de los intereses del usuario; siempre muestra los 8 símbolos por defecto. |
| HU-14 | Sin cambios — SPEC ya correcto con código real. Verificado 2026-05-26. |
| HU-15 | Sin cambios — SPEC ya correcto (endpoint `GET /api/portafolio`, `PortafolioDTO` con `HoldingDTO`). Verificado 2026-05-26. |
| HU-16 | Corrección: `orden.monto_comision` → `orden.comision` (nombre real del campo en entidad `Orden`). |
| HU-17 | Corrección mayor: `activoId` (Long) → `simbolo` (String) en `CrearOrdenRequestDTO`, ejemplos YAML y Gherkin. `ResumenComisionDTO.ticker` → `simbolo`. `OrdenDTO.ticker` → `simbolo`. HTTP de `POST /api/ordenes` 201→200. |
| HU-18 | Corrección: `activoId` → `simbolo` en ejemplos. HTTP 201→200. |
| HU-19 | Corrección: `activoId` → `simbolo` en ejemplos. HTTP 201→200. |
| HU-20 | Corrección: `activoId` → `simbolo` en ejemplos. HTTP 201→200. |
| HU-21 | Sin cambios — SPEC ya correcto. Verificado 2026-05-26. |
| HU-22 | Sin cambios — SPEC ya correcto. Verificado 2026-05-26. |
| HU-24 | Corrección: parámetro `ticker` → `simbolo` en la spec del endpoint `GET /api/ordenes/historial`. |
| HU-25 | Corrección: parámetro `ticker` → `simbolo` en flujo principal, tabla de parámetros, Gherkin y definición de terminado. |
| HU-26 | Sin cambios — SPEC ya correcto. Verificado 2026-05-26. |
| HU-27 | Corrección mayor: endpoint `GET /api/reportes/personal?formato=PDF` → `GET /api/ordenes/reporte`. Parámetro `formato` eliminado (no existe). `desde`/`hasta` ahora opcionales. Excel no implementado (DT-11). |
| HU-28 | Sin cambios — SPEC ya correcto. Verificado 2026-05-26. |
| HU-29 | Sin cambios — SPEC ya correcto. Verificado 2026-05-26. |
| HU-30 | Corrección: `activoId` → `simbolo` en `CrearPropuestaOrdenDTO` (schema y ejemplo). HTTP 201→200. |
| HU-31 | Sin cambios — SPEC ya correcto (endpoints `POST /api/propuestas/{id}/aprobar` y `/rechazar`). Verificado 2026-05-26. |
| HU-33 | Sin cambios — SPEC ya correcto (endpoints `GET` y `PUT /api/admin/mercados`). Verificado 2026-05-26. |
| HU-36 | Sin cambios — SPEC ya correcto (endpoint `POST /api/admin/comisionistas`). Verificado 2026-05-26. |
| HU-41 | Sin cambios — SPEC ya documenta estado En desarrollo y arquitectura de notificaciones. Verificado 2026-05-26. |
| EC-13 | Corregido como parte de HU-17 (`activoId` → `simbolo`). |

### Nuevas deudas técnicas encontradas

| ID | Descripción | Severidad | Archivo de código |
|---|---|---|---|
| DT-09 | `tipoIdentificacion` existe tanto en `Usuario` como en `Inversionista` — campo duplicado en ambas entidades. Colisión de datos potencial. | BAJA | `autenticacion/model/Usuario.java` y `autenticacion/model/Inversionista.java` |
| DT-10 | `MercadoController.dashboard()` pasa `""` a `MercadoService.obtenerDashboard("")`, ignorando los intereses personalizados del inversionista. El dashboard siempre muestra los 8 símbolos por defecto en lugar de los intereses de usuario. | MEDIA | `mercado/controller/MercadoController.java` línea 37 |
| DT-11 | Reporte Excel (XLSX) no implementado. `ReporteService` solo genera PDF. HU-27 documentaba soporte a Excel en el MVP. | BAJA | `ordenes/service/ReporteService.java` |
| DT-12 | Campo `rangoIngresos` recibido en `RegistroInversionistaDTO` no se persiste en ninguna columna de la entidad `Inversionista` ni `Usuario`. El dato de rango de ingresos se pierde silenciosamente al registrar. | MEDIA | `autenticacion/dto/RegistroInversionistaDTO.java`, `autenticacion/service/RegistroService.java` |

### Nuevas preguntas abiertas

| ID | Pregunta | HU/EC afectada |
|---|---|---|
| PA-4 | ¿El campo `tipoIdentificacion` debe existir solo en `usuario` o solo en `inversionista`? Actualmente está en ambas entidades (DT-09). | HU-1, HU-6, HU-7 |
| PA-5 | ¿Se debe implementar el filtro de intereses personalizados en el dashboard (DT-10), o es aceptable siempre mostrar los 8 símbolos por defecto en el MVP? | HU-13 |
| PA-6 | ¿El soporte de reporte Excel (HU-27, DT-11) queda fuera del MVP o se prioriza antes de la entrega? | HU-27 |

---

## Bloqueos y riesgos abiertos

> Anotar aquí cualquier impedimento, dependencia externa pendiente, o riesgo activo que requiera atención.

- *(ninguno bloqueante por ahora; ver deudas técnicas DT-01 a DT-12 en la sección de auditoría)*

---

## Cómo actualizar este archivo

1. **Al terminar una historia:**
   - Marca el checkbox correspondiente con `✅`.
   - Cambia su estado en la tabla a "✅ Hecho".
   - Suma 1 a "Historias completadas".
   - Si introdujiste alguna decisión técnica nueva, añádela en "Bitácora de cambios".
2. **Al cerrar un sprint:**
   - Mueve la sección "Sprint actual" a "Sprints completados" con su resumen.
   - Crea la nueva sección "Sprint actual" con el siguiente sprint.
   - Genera el tag git correspondiente (`v0.X-sprintX`).
3. **Al detectar un bloqueo:**
   - Añádelo en "Bloqueos y riesgos abiertos" con fecha y descripción clara.
4. **Al resolver un bloqueo:**
   - Quítalo de la lista activa y agrégalo en "Bitácora de cambios" con la solución aplicada.
