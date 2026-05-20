# Escenarios de Calidad y Tácticas Arquitectónicas

> Fuente: Informe de Ingeniería Acciones ElBosque, secciones 15 y 16. Consulta este archivo cada vez que un cambio toque rendimiento, disponibilidad, seguridad, interoperabilidad, modificabilidad, usabilidad o testeabilidad.

Cada escenario sigue el formato Fuente / Estímulo / Artefacto / Ambiente / Respuesta / Métrica. Las tácticas asociadas vienen al final de cada bloque.

---

## 1. Rendimiento (Performance)

### EC-01 — Consulta de dashboard bajo carga pico de 1500 usuarios concurrentes
- **RNF base:** RNF-01, RNF-09
- **Fuente:** 1500 inversionistas autenticados accediendo simultáneamente.
- **Estímulo:** Solicitan la carga del dashboard de acciones de interés.
- **Artefacto:** Servicio de Mercado.
- **Ambiente:** Carga pico durante horario de mercados abiertos.
- **Respuesta:** Sistema entrega datos desde almacenamiento local y atiende los 1500 en paralelo.
- **Métrica:** Dashboard se presenta en ≤ 2 segundos.
- **Tácticas:** Maintain Multiple Copies of Data (caché de precios), Introduce Concurrency.

### EC-02 — Ejecución completa de orden de compra
- **RNF base:** RNF-02
- **Fuente:** Inversionista autenticado.
- **Estímulo:** Confirma una orden de compra (Market/Limit/Stop Loss/Take Profit).
- **Artefacto:** Servicio de Órdenes.
- **Ambiente:** Horario de mercado abierto.
- **Respuesta:** Verifica fondos (monto + 2% comisión), reserva saldo, envía a Alpaca, registra, actualiza portafolio, notifica.
- **Métrica:** Ciclo completo ≤ 5 segundos.
- **Tácticas:** Reduce Overhead (componentes en mismo runtime).

### EC-03 — Ejecución completa de orden de venta
- **RNF base:** RNF-02
- **Similar a EC-02** pero con verificación de holdings y acreditación neta.
- **Métrica:** ≤ 5 segundos.
- **Tácticas:** Reduce Overhead.

### EC-04 — Procesamiento de cola de órdenes al abrir mercado
- **RNF base:** RNF-02, RNF-09
- **Fuente:** Apertura de mercado, 200 órdenes encoladas.
- **Artefacto:** Servicio de Órdenes.
- **Respuesta:** Procesa en grupos paralelos, valida usuario activo y fondos, envía las válidas, actualiza estados, notifica.
- **Métrica:** 200 órdenes ≤ 120 segundos.
- **Tácticas:** Introduce Concurrency, Schedule Resources.

### EC-05 — Actualización en tiempo real de datos de mercado
- **RNF base:** RNF-03
- **Fuente:** Inversionista autenticado consultando dashboard.
- **Estímulo:** Proveedor emite actualización de precio de acción configurada.
- **Respuesta:** Sistema poll periódicamente al proveedor, refresca caché y dashboard sin recarga.
- **Métrica:** Cambio reflejado ≤ 3 segundos.
- **Tácticas:** Increase Resources (polling worker dedicado), Maintain Multiple Copies of Data.

---

## 2. Disponibilidad (Availability)

### EC-06 — Detección automática de fallo en uno de los servicios
- **RNF base:** RNF-04
- **Fuente:** Componente de monitoreo interno.
- **Estímulo:** Uno de los 6 servicios deja de responder o lanza errores sostenidos.
- **Artefacto:** Monitor de salud (en Servicio de Trazabilidad).
- **Respuesta:** Detecta tras 30s, registra evento, notifica a admin por correo.
- **Métrica:** Detección ≤ 30 segundos.
- **Tácticas:** Ping/Echo, Heartbeat.

### EC-07 — Fallo de conexión con Alpaca durante horario de mercado
- **RNF base:** RNF-04, RNF-10
- **Fuente:** Alpaca API.
- **Estímulo:** Pérdida de conexión.
- **Artefacto:** Servicio de Órdenes (vía Servicio de Integración).
- **Respuesta:** Detecta, registra, notifica admin, ejecuta recuperación con reintentos automáticos.
- **Métrica:** Restablecimiento ≤ 30 segundos.
- **Tácticas:** Exception Handling, Retry, Circuit Breaker.

### EC-08 — Inversionista consulta dashboard cuando proveedor de datos no responde
- **RNF base:** RNF-03, RNF-04
- **Estímulo:** Alpha Vantage no responde a solicitudes de precios.
- **Artefacto:** Servicio de Mercado.
- **Respuesta:** Sistema reintenta, muestra últimos datos en caché con aviso visible de la hora de la última actualización exitosa.
- **Métrica:** Reconexión ≤ 60 segundos o alerta a admin.
- **Tácticas:** Degradation (caché de precios), Retry.

---

## 3. Seguridad (Security)

### EC-09 — Usuario externo supera 5 intentos consecutivos de login fallidos
- **RNF base:** RNF-06
- **Fuente:** Usuario externo no identificado.
- **Estímulo:** 5 intentos fallidos sobre la misma cuenta.
- **Artefacto:** Servicio de Autenticación.
- **Respuesta:** Bloquea cuenta 15 minutos tras 5to intento, notifica al titular por correo.
- **Métrica:** Bloqueo ≤ 1 segundo tras 5to intento.
- **Tácticas:** **Lock Computer** (componente Monitor de Intentos).

### EC-10 — Verificación de segundo factor (MFA) durante login
- **RNF base:** RNF-05
- **Fuente:** Usuario con credenciales válidas.
- **Estímulo:** Inicia proceso de autenticación.
- **Artefacto:** Servicio de Autenticación.
- **Respuesta:** Genera código, lo envía por correo y/o celular según configuración, exige ingresarlo antes de dar acceso.
- **Métrica:** Código generado y enviado ≤ 5 segundos.
- **Tácticas:** **Authenticate Actors** (componente Verificador MFA).

### EC-11 — Comisionista intenta acceder a inversionista no asignado
- **RNF base:** RNF-07
- **Fuente:** Comisionista autenticado.
- **Estímulo:** Intenta consultar portafolio de inversionista que no es su cliente.
- **Artefacto:** Servicio de Autenticación (control de acceso).
- **Respuesta:** Verifica relación, bloquea acceso, mensaje genérico sin filtrar datos del inversionista.
- **Métrica:** 99% intentos bloqueados.
- **Tácticas:** **Authorize Actors** (control de acceso por relación).

### EC-12 — Trazabilidad ante ejecución de orden
- **RNF base:** RNF-14
- **Fuente:** Inversionista autenticado.
- **Estímulo:** Confirma orden de cualquier tipo.
- **Artefacto:** Servicio de Trazabilidad.
- **Respuesta:** Genera registro con fecha/hora, módulo, ID inversionista, IP, tipo de orden, activo, cantidad, monto, comisión, resultado.
- **Métrica:** Registro generado ≤ 2 segundos tras la operación.
- **Tácticas:** **Audit Trail**.

### EC-13 — Presentación transparente de comisión antes de confirmar orden
- **RNF base:** RNF-15
- **Fuente:** Inversionista autenticado.
- **Estímulo:** Ingresa parámetros de orden.
- **Artefacto:** Servicio de Órdenes.
- **Respuesta:** Calcula 2%, muestra desglose (total, comisión, total a debitar/acreditar, split 60/40 si hay comisionista). No permite confirmar sin mostrar.
- **Métrica:** 99% de órdenes muestran info completa antes.
- **Tácticas:** **Verify Message Integrity** + diseño del flujo (validación previa).

---

## 4. Interoperabilidad (Interoperability)

### EC-14 — Creación de cuenta en Alpaca durante registro de inversionista
- **RNF base:** RNF-10
- **Fuente:** Inversionista que termina registro.
- **Artefacto:** Servicio de Integración.
- **Respuesta:** Envía datos a Alpaca, recibe confirmación, asocia identificador, redirige a panel.
- **Métrica:** 99.9% solicitudes exitosas.
- **Tácticas:** **Orchestrate** (Orquestador de Registro), **Tailor Interface** (Adaptador Alpaca).

### EC-15 — Procesamiento de pago de suscripción premium con Stripe
- **RNF base:** RNF-12
- **Fuente:** Inversionista autenticado.
- **Estímulo:** Elige plan mensual ($12) o anual ($120) y completa pago.
- **Artefacto:** Servicio de Integración.
- **Respuesta:** Redirige a Stripe Checkout, recibe confirmación/rechazo, actualiza estado, habilita features premium, notifica.
- **Métrica:** 99% respuestas procesadas.
- **Tácticas:** **Orchestrate** (Orquestador de Suscripción), **Tailor Interface** (Adaptador Stripe).

### EC-16 — Consulta de detalle de acción desde proveedor de datos
- **RNF base:** RNF-11
- **Artefacto:** Servicio de Integración.
- **Métrica:** Datos obtenidos y presentados ≤ 2 segundos.
- **Tácticas:** **Tailor Interface**, caché.

### EC-17 — Envío de notificación por múltiples canales tras ejecución de orden
- **RNF base:** RNF-13
- **Fuente:** Inversionista autenticado tras orden ejecutada.
- **Artefacto:** Servicio de Integración (Despachador de Notificaciones).
- **Respuesta:** Consulta canales configurados, envía por Email + WhatsApp + SMS según corresponda.
- **Métrica:** Notificación enviada por todos los canales ≤ 10 segundos.
- **Tácticas:** **Orchestrate** (Despachador), **Tailor Interface** por canal.

---

## 5. Modificabilidad (Modifiability)

### EC-18 — Cambio del porcentaje de comisión y split de distribución
- **RNF base:** RNF-17
- **Fuente:** Administrador autenticado.
- **Estímulo:** Modifica comisión 2% → 3% y split 60/40 → 70/30.
- **Artefacto:** Servicio de Administración.
- **Respuesta:** Aplica a transacciones futuras, sin afectar las ya ejecutadas o pendientes con comisión calculada.
- **Métrica:** Cambio aplicado ≤ 1 minuto sin redespliegue.
- **Tácticas:** **Encapsulate** (Calculador de Comisión consulta parámetros desde BD en cada operación).

### EC-19 — Configuración de nuevo feriado en mercado existente
- **RNF base:** RNF-17
- **Fuente:** Administrador autenticado.
- **Estímulo:** Bolsa de Tokio anuncia feriado, admin lo agrega al calendario.
- **Artefacto:** Servicio de Administración.
- **Respuesta:** Guarda config, notifica usuarios con operaciones en TSE, cambia órdenes pendientes a "En Revisión".
- **Métrica:** Configuración ≤ 2 minutos sin intervención técnica.
- **Tácticas:** **Encapsulate** (Verificador de Horarios consulta parámetros).

---

## 6. Usabilidad (Usability)

### EC-20 — Inversionista principiante completa su primera orden de compra
- **RNF base:** RNF-16
- **Fuente:** Inversionista principiante.
- **Estímulo:** Quiere comprar 5 acciones de AAPL como Market Order.
- **Artefacto:** Servicio de Órdenes (UI).
- **Respuesta:** UI guía paso a paso (seleccionar activo → tipo orden → cantidad → resumen con comisión → confirmar), valida y muestra mensajes claros en cada paso.
- **Métrica:** Primera orden completada ≤ 5 minutos.
- **Tácticas:** **Support User Initiative** (asistente paso a paso).

### EC-21 — Inversionista alterna visualización del portafolio
- **RNF base:** RNF-16
- **Estímulo:** Cambia preferencia de vista (lista, gráfico de barras, vista detallada).
- **Artefacto:** Servicio de Administración.
- **Métrica:** Cambio de vista ≤ 1 segundo sin recargar.
- **Tácticas:** **Maintain User Model** (Gestor de Preferencias).

---

## 7. Testeabilidad (Testability)

### EC-22 — Prueba de integración del flujo de orden con sandbox
- **RNF base:** RNF-18
- **Estímulo:** Suite de pruebas que cubre el flujo completo.
- **Artefacto:** Servicio de Órdenes.
- **Ambiente:** Staging conectado a sandbox Alpaca y Stripe test.
- **Métrica:** Cobertura de caminos ≥ 85%.
- **Tácticas:** **Sandbox** (entornos aislados con sandbox Alpaca y modo test Stripe).

### EC-23 — Prueba de carga simulando 1500 usuarios concurrentes
- **RNF base:** RNF-18, RNF-09
- **Estímulo:** JMeter ejecuta operaciones mixtas durante 15 min.
- **Métrica:** 95% transacciones ≤ 2 segundos.
- **Tácticas:** **Sandbox** + **Recording** (capturar tiempos).

---

## 8. Mapa de tácticas por servicio

| Servicio | Tácticas que implementa |
|---|---|
| Autenticación | Lock Computer, Authenticate Actors, Authorize Actors, Audit Trail (consume IAuditLog) |
| Órdenes | Reduce Overhead, Introduce Concurrency, Verify Message Integrity, Audit Trail |
| Mercado | Maintain Multiple Copies of Data (caché), Increase Resources, Degradation, Retry |
| Administración | Encapsulate (parámetros configurables), Maintain User Model |
| Integración | Orchestrate (registro/suscripción/notificaciones), Tailor Interface (adaptadores), Exception Handling, Circuit Breaker |
| Trazabilidad | Audit Trail (núcleo), Ping/Echo, Heartbeat (monitoreo de salud de servicios) |

---

## 9. Reglas operativas derivadas para Claude Code

- Cada feature de **autenticación** debe disparar evento auditable vía `IAuditLog`.
- Cada feature que toque **dinero o portafolio** debe presentar comisiones explícitas antes de confirmar (EC-13).
- Cada llamada a **API externa** debe tener manejo de excepción + reintento (EC-07).
- Cada **parámetro configurable** (comisión, horario, feriado) debe leerse de BD en cada operación, no estar hardcodeado (EC-18, EC-19).
- Las pruebas de integración deben usar **sandbox**, nunca producción (EC-22).
