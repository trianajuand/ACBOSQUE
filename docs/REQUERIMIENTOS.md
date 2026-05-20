# Requerimientos Funcionales y No Funcionales

> Fuente: Informe de Ingeniería Acciones ElBosque, sección 9 y matriz de requerimientos. Este archivo es el catálogo consolidado para consulta rápida desde Claude Code.

---

## 1. Requerimientos Funcionales (RF)

### Servicio de Autenticación

| ID | Descripción | Servicio | Prioridad |
|---|---|---|---|
| RF-01 | El sistema debe permitir el registro de inversionistas con datos personales, nivel de experiencia e intereses de mercado. | Autenticación | Must |
| RF-02 | El sistema debe permitir autenticación de usuarios mediante correo y contraseña. | Autenticación | Must |
| RF-03 | El sistema debe permitir cierre de sesión seguro invalidando el token JWT activo. | Autenticación | Must |
| RF-04 | El sistema debe soportar autenticación multifactor (MFA) obligatoria para Comisionista, Administrador, Responsable Legal e Inversionista Premium, y opcional para Inversionista regular. | Autenticación | Must |
| RF-21 | El sistema debe bloquear temporalmente el acceso tras 5 intentos fallidos de inicio de sesión. | Autenticación | Must |

### Servicio de Órdenes

| ID | Descripción | Servicio | Prioridad |
|---|---|---|---|
| RF-09 | El sistema debe permitir colocar órdenes de tipo Market. | Órdenes | Must |
| RF-10 | El sistema debe permitir colocar órdenes de tipo Limit. | Órdenes | Must |
| RF-11 | El sistema debe permitir colocar órdenes de tipo Stop Loss. | Órdenes | Must |
| RF-12 | El sistema debe permitir colocar órdenes de tipo Take Profit. | Órdenes | Must |
| RF-13 | El sistema debe verificar fondos disponibles antes de ejecutar una orden de compra. | Órdenes | Must |
| RF-14 | El sistema debe verificar holdings disponibles antes de ejecutar una orden de venta. | Órdenes | Must |
| RF-15 | El sistema debe permitir cancelar órdenes pendientes. | Órdenes | Must |
| RF-16 | El sistema debe calcular y mostrar el desglose de comisiones (2% del monto, split 60/40 plataforma/comisionista cuando aplique). | Órdenes | Must |
| RF-17 | El sistema debe permitir consultar el estado de órdenes activas. | Órdenes | Must |
| RF-18 | El sistema debe permitir consultar el historial de órdenes con filtros. | Órdenes | Must |
| RF-19 | El sistema debe mostrar el portafolio con holdings y ganancia/pérdida por posición. | Órdenes | Must |
| RF-20 | El sistema debe permitir consultar saldo disponible, fondos reservados e historial de comisiones. | Órdenes | Must |
| RF-22 | El sistema debe permitir al comisionista consultar el portafolio y órdenes de clientes asignados. | Órdenes | Must |
| RF-23 | El sistema debe permitir al comisionista proponer órdenes en nombre de un cliente. | Órdenes | Should |
| RF-24 | El sistema debe permitir al inversionista aprobar o rechazar propuestas del comisionista. | Órdenes | Should |
| RF-25 | El sistema debe permitir al comisionista firmar y enviar órdenes aprobadas al mercado. | Órdenes | Should |
| RF-33 | El sistema debe encolar órdenes recibidas fuera del horario del mercado y procesarlas en la apertura. | Órdenes | Must |
| RF-35 | El sistema debe generar reportes personales de actividad del inversionista. | Órdenes | Could |

### Servicio de Mercado

| ID | Descripción | Servicio | Prioridad |
|---|---|---|---|
| RF-06 | El sistema debe mostrar un dashboard con precios y variaciones de las acciones de interés del inversionista. | Mercado | Must |
| RF-07 | El sistema debe permitir consultar el detalle completo de una acción (precio, historial, capitalización, volumen). | Mercado | Must |
| RF-08 | El sistema debe verificar el horario del mercado antes de procesar una orden. | Mercado | Must |

### Servicio de Administración

| ID | Descripción | Servicio | Prioridad |
|---|---|---|---|
| RF-05 | El sistema debe permitir al inversionista configurar preferencias (canales notificación, tipo orden por defecto, vista portafolio). | Administración | Must |
| RF-17a | El sistema debe permitir al administrador configurar el porcentaje de comisión y el split plataforma/comisionista. | Administración | Must |
| RF-17b | El sistema debe permitir al administrador configurar mercados habilitados y sus horarios. | Administración | Must |
| RF-26 | El sistema debe permitir al administrador gestionar el ciclo de vida de las cuentas (activar, suspender, eliminar). | Administración | Must |
| RF-27 | El sistema debe permitir al administrador asignar comisionistas a inversionistas. | Administración | Must |
| RF-31 | El sistema debe permitir al inversionista contratar suscripción premium vía Stripe. | Administración | Should |
| RF-32 | El sistema debe permitir al inversionista cancelar la suscripción premium. | Administración | Should |

### Servicio de Trazabilidad

| ID | Descripción | Servicio | Prioridad |
|---|---|---|---|
| RF-28 | El sistema debe registrar logs de todas las operaciones críticas (autenticación, órdenes, cambios admin, intentos denegados). | Trazabilidad | Must |
| RF-29 | El sistema debe enviar notificaciones por los canales configurados (Email, SMS, WhatsApp) ante eventos relevantes. | Trazabilidad | Should |

> Esta tabla refleja los RF principales del MVP. La matriz completa con código de impacto, complejidad y origen se mantiene en el Excel del proyecto académico (`Matriz_de_Requerimientos_AccionesElBosque.xlsx`).

---

## 2. Requerimientos No Funcionales (RNF)

### Rendimiento (Performance)

| ID | Descripción | Métrica |
|---|---|---|
| RNF-01 | El dashboard debe cargar bajo carga pico de 1500 usuarios concurrentes. | ≤ 2 segundos |
| RNF-02 | El ciclo completo de ejecución de una orden (validación + envío + registro + notificación) debe completarse rápido. | ≤ 5 segundos |
| RNF-03 | Las actualizaciones de precios en el dashboard deben reflejarse en tiempo casi real. | ≤ 3 segundos desde emisión del proveedor |
| RNF-09 | El sistema debe soportar 1500 usuarios concurrentes sin degradar el rendimiento. | 95% transacciones ≤ 2s |

### Disponibilidad (Availability)

| ID | Descripción | Métrica |
|---|---|---|
| RNF-04 | El sistema debe detectar fallos en sus servicios y notificar al administrador. | Detección ≤ 30s |
| RNF-10 | El sistema debe restablecer la conexión con APIs externas (Alpaca, Alpha Vantage) tras un fallo. | Reconexión ≤ 30-60s |

### Seguridad (Security)

| ID | Descripción | Métrica |
|---|---|---|
| RNF-05 | El sistema debe aplicar MFA en el inicio de sesión según rol. | Código MFA enviado ≤ 5s |
| RNF-06 | El sistema debe bloquear cuentas tras 5 intentos fallidos de login. | Bloqueo ≤ 1s tras 5to intento, duración 15 min |
| RNF-07 | El sistema debe garantizar el control de acceso por rol y por relación (un comisionista solo accede a sus clientes asignados). | 99% intentos no autorizados bloqueados |
| RNF-08 | Las contraseñas deben almacenarse cifradas (BCrypt). | N/A — verificación por inspección |
| RNF-14 | El sistema debe registrar trazabilidad de toda ejecución de orden (fecha, IP, módulo, usuario, datos de la orden). | Registro ≤ 2s tras la operación |
| RNF-15 | El sistema debe presentar comisiones de forma transparente antes de confirmar una orden. | 99% órdenes muestran info completa antes |

### Interoperabilidad (Interoperability)

| ID | Descripción | Métrica |
|---|---|---|
| RNF-10 | El sistema debe integrarse con Alpaca API para crear cuentas y ejecutar órdenes. | 99.9% solicitudes exitosas |
| RNF-11 | El sistema debe integrarse con Alpha Vantage para obtener datos de mercado. | Detalle obtenido ≤ 2s |
| RNF-12 | El sistema debe integrarse con Stripe para procesar pagos premium. | 99% respuestas procesadas |
| RNF-13 | El sistema debe enviar notificaciones por múltiples canales (Email, SMS, WhatsApp). | ≤ 10s tras evento |

### Modificabilidad (Modifiability)

| ID | Descripción | Métrica |
|---|---|---|
| RNF-17 | El administrador debe poder modificar parámetros (comisiones, horarios, feriados) sin redespliegue. | Cambio aplicado ≤ 1-2 minutos sin intervención técnica |

### Usabilidad (Usability)

| ID | Descripción | Métrica |
|---|---|---|
| RNF-16 | La interfaz debe ser usable por inversionistas principiantes y permitir personalización (vista portafolio). | Primera orden completada ≤ 5 minutos |

### Testeabilidad (Testability)

| ID | Descripción | Métrica |
|---|---|---|
| RNF-18 | El sistema debe ser probable en entornos aislados (sandbox de Alpaca, modo test de Stripe). | Cobertura de caminos en flujo de orden ≥ 85% |

---

## 3. Mapa RF/RNF → Servicio (resumen)

| Servicio | RF cubiertos | RNF más relevantes |
|---|---|---|
| Autenticación | RF-01, RF-02, RF-03, RF-04, RF-21 | RNF-05, RNF-06, RNF-07, RNF-08 |
| Órdenes | RF-09 a RF-20, RF-22 a RF-25, RF-33, RF-35 | RNF-02, RNF-14, RNF-15 |
| Mercado | RF-06, RF-07, RF-08 | RNF-01, RNF-03, RNF-09 |
| Administración | RF-05, RF-17a, RF-17b, RF-26, RF-27, RF-31, RF-32 | RNF-17 |
| Integración | (transversal a Alpaca, Stripe, Alpha Vantage, notificaciones) | RNF-10, RNF-11, RNF-12, RNF-13 |
| Trazabilidad | RF-28, RF-29 | RNF-04, RNF-14 |
