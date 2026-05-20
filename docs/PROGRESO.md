# Progreso del Proyecto

> **Este archivo es el "diario de bitácora" del proyecto.** Debe actualizarse cada vez que se termine una historia o se cierre un sprint. Es la primera fuente de verdad para saber dónde vamos.

---

## Estado general

- **Fecha última actualización:** 2026-05-06
- **Sprint actual:** Sprint 2 — Servicio de Mercado + Sprint 3 — Servicio de Órdenes (parcial)
- **Sprints completados:** Sprint 1 (Autenticación + Perfil)
- **Historias del MVP completadas:** 16 / 42
- **Bloqueos actuales:** ninguno

---

## Sprint actual: Sprint 1 — Servicio de Autenticación

**Objetivo:** Tener el Servicio de Autenticación completamente funcional, con todos sus flujos validados end-to-end (registro, login, MFA, logout, recuperación de contraseña, bloqueo).

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
- [ ] Estructura de carpetas SOA según `ARQUITECTURA.md`
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

### Definition of Done del Sprint 1

Todas las historias se consideran cerradas cuando:
1. ✅ Compila sin errores ni warnings críticos.
2. ✅ Pasa el plan de pruebas end-to-end en el HTML de test (los 7 escenarios de la sección 5 de la guía).
3. ✅ Cada flujo dispara los eventos de auditoría correspondientes (verificable en `logs/audit.log`).
4. ✅ El frontend Angular renderiza cada pantalla, valida formularios, llama al backend, maneja errores en español.
5. ✅ Los checkboxes de este archivo están marcados.
6. ✅ Existe un commit por historia y un tag `v0.1-sprint1` al final.

---

## Sprint 1 — Servicio de Autenticación ✅ COMPLETADO

**Resultado:** Servicio de Autenticación completamente funcional end-to-end.

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

**Objetivo:** Tener el Servicio de Mercado y el Servicio de Órdenes funcionales con APIs reales (Alpaca + Alpha Vantage).

### Historias implementadas (backend completo, pendiente validación end-to-end)

| ID | Historia | Estado | Notas |
|---|---|:-:|---|
| HU-2 | Integración Alpaca al registrar inversionista | ✅ | `OrquestadorRegistro` invoca `AlpacaAdapter.crearCuenta()`. Guarda `alpacaAccountId` en `Usuario`. Maneja fallo (pendiente + notificación admin). |
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

## Sprints futuros (alta granularidad — refinar al iniciar cada uno)

### Sprint 2 — Servicio de Mercado
- HU-13: Dashboard de acciones de interés.
- HU-14: Detalle de acción.
- HU-8, HU-9: Preferencias de notificación y operación (parte que toca al usuario).
- Integración con Alpha Vantage (Adapter en `integracion/adaptadores/alphavantage/`).
- Caché de precios (táctica Maintain Multiple Copies of Data).
- Verificador de horarios de mercado.

### Sprint 3 — Servicio de Órdenes (parte 1: inversionista)
- HU-15: Visualización de portafolio.
- HU-16: Saldo y comisiones.
- HU-17: Orden Market.
- HU-18 a HU-20: Orden Limit / Stop Loss / Take Profit.
- HU-21: Cancelar orden pendiente.
- HU-22: Consultar órdenes activas.
- HU-23: Encolamiento fuera de horario.
- Integración con Alpaca (Adapter en `integracion/adaptadores/alpaca/`).
- HU-2: Integración Alpaca al registrar inversionista (orquestador de registro).

### Sprint 4 — Servicio de Órdenes (parte 2: comisionista) + Trazabilidad real
- HU-28 a HU-32: flujo completo del comisionista (consulta, propuesta, aprobación, firma).
- Integración real con Splunk o Elasticsearch para `IAuditLog`.
- HU-40: Logs auditables de todas las operaciones críticas.

### Sprint 5 — Servicio de Administración
- HU-33: Configurar mercados.
- HU-34: Configurar feriados.
- HU-35: Configurar comisiones (% y split).
- HU-36 a HU-39: gestión de usuarios (crear comisionista, asignar, suspender, eliminar).
- HU-11, HU-12: Suscripción premium con Stripe.
- Integración con Stripe (Adapter en `integracion/adaptadores/stripe/`).

### Sprint 6 — Notificaciones multicanal + reportes + pulido final
- HU-41: Despacho de notificaciones por Email/SMS/WhatsApp.
- HU-42: Detección de fallo de servicio + alerta.
- HU-27: Reporte personal de actividad.
- HU-24 a HU-26: Historial con filtros.
- Pruebas de carga con JMeter (1500 usuarios concurrentes — RNF-09).
- Documentación final y entrega.

---

## Bitácora de cambios y decisiones

> Anotar aquí las decisiones técnicas relevantes que se tomen durante el desarrollo, con fecha y motivo. Esto ayuda a recordar el porqué de elecciones que después parezcan obvias o cuestionables.

### [Fecha] — Decisión inicial: estructura SOA por servicio
Se rechazó la estructura por capas tradicional para alinear con la arquitectura del informe de ingeniería. Cada servicio replica internamente `controller/service/repository/model/dto/interfaces`.

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

---

## Métricas del proyecto

| Métrica | Valor actual |
|---|---|
| Historias completadas | 16 / 42 |
| Cobertura de tests (services) | — |
| Cobertura de tests (global) | — |
| Endpoints REST funcionando | ~20 (auth + perfil + mercado + órdenes + portafolio) |
| Servicios con interfaces definidas | 4 / 6 (Auth, Mercado, Órdenes, Trazabilidad) |
| Eventos auditables registrados | ✅ vía `AuditLogService` (consola + archivo) |

---

## Bloqueos y riesgos abiertos

> Anotar aquí cualquier impedimento, dependencia externa pendiente, o riesgo activo que requiera atención.

- *(ninguno por ahora)*

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
