# Arquitectura del Sistema — Monolito Modular

> Fuente de verdad: Informe de Ingeniería Acciones ElBosque. Este archivo es la referencia obligatoria antes de crear cualquier archivo nuevo, tomar decisiones estructurales o modificar interfaces entre módulos.

---

## 1. Estilo arquitectónico: Monolito Modular

El sistema se organiza como un **Monolito Modular** bajo un único runtime de Spring Boot. La estructura interna se divide en **6 módulos de dominio** con responsabilidades claramente delimitadas, cada uno con su propio conjunto de clases, interfaces, repositorios y DTOs. Los módulos se comunican exclusivamente a través de interfaces Java declaradas (`I...`), no a través de llamadas HTTP entre procesos ni a través de importación directa de clases internas de otro módulo.

### Por qué Monolito Modular y no otra cosa

**Frente a microservicios:**
- El equipo es de 4 personas con un plazo académico acotado. Desplegar 6 servicios independientes implicaría configurar service discovery, API gateway, comunicación distribuida, gestión de secretos por servicio y orquestación de contenedores, lo que supera ampliamente los recursos disponibles.
- El requisito de confirmación de órdenes en menos de 5 segundos (EC-02, EC-03) es incompatible con la latencia de red entre microservicios bajo carga concurrente.
- La transaccionalidad financiera (fondos, comisiones, holdings en la misma operación) es mucho más simple y confiable dentro de un único contexto transaccional JPA que distribuida entre servicios.

**Frente a un monolito en capas (`controller → service → repository` planos):**
- Una arquitectura por capas horizontales mezcla responsabilidades de dominios distintos en el mismo paquete. Con 6 dominios funcionales y más de 50 clases, eso genera acoplamiento y dificulta la evolución independiente de cada dominio.
- El Monolito Modular organiza el código **verticalmente por dominio**, lo que permite que cada módulo sea razonado, testeado y potencialmente extraído de forma independiente.

**Relación con DDD:**
Cada módulo representa un **Bounded Context** de dominio (autenticación, órdenes, mercado, administración, integración externa, trazabilidad). Las interfaces entre módulos actúan como las **anti-corruption layers** del DDD: ningún módulo expone sus entidades internas al exterior; solo expone contratos a través de interfaces.

**Posibilidad de evolución:**
La separación por interfaces facilita extraer módulos a microservicios en el futuro sin reescribir la lógica de negocio, solo cambiando el mecanismo de comunicación.

---

## 2. Stack tecnológico y justificación de decisiones

### 2.1 Backend

| Tecnología | Versión | Justificación |
|---|---|---|
| **Java** | 17 (LTS) | Versión LTS estable con soporte hasta 2029. El equipo tiene experiencia previa en Java. Records, sealed classes y mejoras de texto facilitan la escritura de DTOs y enumeraciones del dominio financiero. |
| **Spring Boot** | 3.x | Framework productivo con autoconfiguración, amplia documentación y ecosistema maduro. El equipo ya ha trabajado con Spring Boot en proyectos anteriores, lo que reduce la curva de aprendizaje y permite enfocarse en la lógica de negocio. Incluye Spring Security, Spring Data JPA y Spring Mail de forma integrada. |
| **Maven** | Última estable | Herramienta de build estándar en el ecosistema Java. Gestión de dependencias declarativa con `pom.xml`. El equipo ya la ha utilizado. |
| **Empaquetado WAR** | — | Se eligió WAR porque permite despliegue en un servlet container externo (Tomcat) además del servidor embebido de Spring Boot, lo que da más flexibilidad para el entorno de producción académico. |
| **PostgreSQL** | Nativa en localhost:5432 | Base de datos relacional robusta, con soporte de ACID completo, esencial para datos financieros (fondos, comisiones, órdenes). El equipo tiene experiencia previa con PostgreSQL. Instalación nativa sin Docker, alineada con la restricción del proyecto. |
| **Spring Data JPA / Hibernate** | Incluido en Spring Boot 3 | ORM que reduce el código boilerplate para acceso a datos. El equipo lo ha utilizado antes. |
| **JWT (JSON Web Token)** | — | Estándar de la industria para autenticación stateless. Permite verificar rol e identidad del usuario en cada solicitud sin consultar la base de datos para cada llamada. El token incluye el rol del usuario para que el sistema verifique autorización antes de ejecutar cualquier acción. |
| **BCrypt** | Incluido en Spring Security | Algoritmo de hashing adaptativo para contraseñas. Resistente a ataques de fuerza bruta por su costo computacional configurable. Estándar recomendado para almacenamiento seguro de contraseñas. |
| **Jakarta Mail / Spring Mail** | Incluido en Spring Boot 3 | Librería estándar de Java para envío de correos electrónicos vía SMTP. El equipo la ha utilizado antes. Se usa para notificaciones, MFA y recuperación de contraseña. |

### 2.2 Frontend

| Tecnología | Versión | Justificación |
|---|---|---|
| **Angular** | 21 (standalone components) | Framework SPA basado en TypeScript con tipado fuerte, lo que reduce errores en la integración con los DTOs del backend. El equipo tiene experiencia previa en Angular. La arquitectura de componentes standalone (sin NgModules) simplifica la estructura del proyecto. |
| **TypeScript** | Incluido en Angular | Tipado estático que permite detectar errores en tiempo de compilación, especialmente valioso en la integración con los contratos REST del backend. |
| **Angular CLI** | — | Herramienta oficial para scaffolding, build (`ng build`) y desarrollo (`ng serve`). El equipo ya la ha utilizado. |
| **SCSS** | — | Preprocesador CSS que permite variables, anidación y reutilización de estilos. Ya se utiliza en el proyecto. |

### 2.3 Integraciones externas

| Integración | Propósito | Estado |
|---|---|---|
| **Alpaca Broker API** | Creación de cuentas de usuario en Alpaca, envío y seguimiento de órdenes al mercado (NYSE, NASDAQ) | Implementado en sandbox |
| **Alpaca Market Data API** | Datos de mercado en tiempo real (precios, cotizaciones, snapshots) para NYSE y NASDAQ | Implementado en sandbox |
| **Alpha Vantage** | Datos históricos y cotizaciones globales de respaldo | Implementado |
| **Stripe** | Procesamiento de pagos para suscripción premium ($12/mes o $120/año) | Implementado en modo test |
| **SMTP (Spring Mail)** | Notificaciones por correo electrónico, MFA, recuperación de contraseña | Implementado |
| **SMS / WhatsApp** | Notificaciones multicanal configurables por el usuario | En proceso, no implementado en el MVP actual |

---

## 3. Los 6 módulos de dominio

El sistema se divide en 6 módulos. Cada módulo es dueño de sus entidades, repositorios, lógica de negocio y DTOs. La comunicación entre módulos ocurre únicamente a través de las interfaces `I...` declaradas por el módulo dueño.

### 3.1 Módulo de Autenticación (`autenticacion`)

**Responsabilidad:** Gestiona el ciclo de vida de identidad de todos los usuarios del sistema: registro de inversionistas, inicio de sesión con JWT, verificación de MFA (obligatoria para Comisionista y Administrador, configurable para Inversionista), recuperación de contraseña, bloqueo por intentos fallidos (5 intentos → 15 minutos), control de acceso por rol, gestión de perfil e historial de comisionistas asignados.

**Bounded context (DDD):** Identidad y Acceso.

**Interfaces que expone:**
- `IAutenticacion`: login, verificación MFA, logout, recuperación de contraseña, reset de contraseña, disponibilidad de correo.
- `IControlAcceso`: verificación de rol y autorización antes de ejecutar cualquier acción; consumida por todos los módulos que necesitan validar permisos.
- `IAsignacionComisionista`: consulta y gestión de la relación inversionista-comisionista; consumida por el módulo de Órdenes para verificar si un inversionista tiene comisionista asignado.
- `IGestionCuentas`: crear comisionistas, listar usuarios, cambiar estado de cuenta, eliminar usuario, asignar comisionista, validar si es administrador activo. Consumida **exclusivamente** por el módulo de Administración.
- `IConsultaInversionista`: validar que un usuario pueda operar, obtener y actualizar el Alpaca account ID, obtener la vista preferida de portafolio. Consumida por el módulo de Órdenes.

**Consume de otros módulos:**
- `IAuditLog` (Trazabilidad): registra eventos de login, logout, intentos fallidos, bloqueos, cambios de perfil.
- `INotificacion` (Integración): envía código MFA, correo de verificación de registro y correo de recuperación de contraseña.

**Entidades JPA propias:** `Usuario`, `Inversionista`, `Comisionista`, `Suscripcion`, `IntegracionInversionista`, `Especialidad`, `Intereses`, `ComisionistaEspecialidad`, `InteresInversionista`, `AsignacionComisionista`, `CodigoVerificacion`, `IntentoFallido`.

**Tablas BD propias:** `usuario`, `inversionista`, `comisionista`, `suscripcion`, `integracion_inversionista`, `especialidad`, `intereses`, `comisionista_especialidad`, `interes_inversionista`, `asignacion_comisionista`, `codigo_verificacion`, `intento_fallido`.

**Notas de diseño (normalización 3NF, mayo 2026):**
- `inversionista.id` = PK compartida con `usuario.id` (Table-Per-Type, shared PK, sin auto-generado).
- `comisionista.id` = PK compartida con `usuario.id` (idem).
- Campos de contacto/notificación (`telefono`, `notificacion_email`, etc.) viven en `usuario`.
- Datos premium/Stripe viven en `suscripcion` (1:1 con inversionista).
- Alpaca account ID y pendiente viven en `integracion_inversionista` (1:1 con inversionista).
- `rango_ingresos` (string) reemplazado por `ingresos_min`/`ingresos_max` (BigDecimal).

---

### 3.2 Módulo de Órdenes (`ordenes`)

**Responsabilidad:** Gestión completa del ciclo de vida de órdenes (Market, Limit, Stop Loss, Take Profit); verificación de fondos (saldo suficiente para compra) y holdings (al menos 1 acción para venta); cálculo de comisiones (2% parametrizable, con split 60/40 si hay comisionista); cola de órdenes fuera de horario (FIFO); flujo completo del comisionista (propuesta → aprobación del inversionista → firma → envío al mercado); portafolio del inversionista; saldo y depósito de fondos.

**Bounded context (DDD):** Trading y Finanzas.

**Interfaces que expone:**
- `IOrden`: crear orden, previsualizar comisión, cancelar orden, listar órdenes activas, historial de órdenes, crear propuesta de comisionista, aprobar/rechazar propuesta, firmar y enviar propuesta aprobada.

**Consume de otros módulos:**
- `IVerificacionMercado` (Mercado): verifica si el mercado destino está abierto antes de enviar una orden; determina si la orden debe encolarse.
- `IIntegracionAlpaca` (Integración): envía la orden al mercado a través del broker Alpaca.
- `IAsignacionComisionista` (Autenticación): consulta si el inversionista tiene comisionista asignado para aplicar el split de comisión correcto.
- `IGestorParametros` (Administración): lee el porcentaje de comisión vigente y el split plataforma/comisionista desde BD.
- `IAuditLog` (Trazabilidad): registra cada evento de orden (creación, ejecución, cancelación, firma, propuesta, encolamiento).
- `INotificacion` (Integración): envía notificación al usuario al ejecutarse, cancelarse o fallar una orden.

**Entidades JPA propias:** `Orden`, `PropuestaOrden`, `Holding`, `CuentaFondos`, `Comision`.

**Tablas BD propias:** `orden`, `propuesta_orden`, `holding`, `cuenta_fondos`, `comision`.

**Notas de diseño (normalización 3NF, mayo 2026):**
- `Orden` ya no tiene `simbolo`; usa `activo_id` (FK a `Activo` del módulo Mercado).
- `Orden` ya no tiene campos de propuesta; flujo de comisionista usa `PropuestaOrden`.
- `Holding` usa PK compuesta `(inversionista_id, activo_id)` en lugar de auto-incremental.
- `CuentaFondos.inversionista_id` es la PK (shared 1:1 con inversionista).
- `Orden.inversionista_id` reemplaza `usuario_id`.

**Reglas de negocio clave gestionadas aquí:**
- Compra: se descuenta `(Precio × Cantidad) + Comisión` del saldo.
- Venta: se acredita `(Precio × Cantidad) - Comisión` al saldo.
- Comisión = Total Transacción × % comisión (default 2%, tomado de `parametro_comision` vía `IGestorParametros`).
- Sin comisionista: comisión íntegra a la plataforma.
- Con comisionista: 60% plataforma, 40% comisionista (porcentajes parametrizables).
- Toda comisión se redondea a 2 cifras decimales.
- La comisión se muestra al usuario **antes** de confirmar la orden.

---

### 3.3 Módulo de Mercado (`mercado`)

**Responsabilidad:** Dashboard del inversionista con datos en tiempo real (precios, variaciones, volumen de acciones de interés); caché de precios actualizada cada 2-3 segundos desde Alpaca/Alpha Vantage; sincronización periódica con proveedores externos; verificación de horarios de apertura y cierre de cada mercado (NYSE, NASDAQ, LSE, TSE, ASX); consulta de detalle de una acción; gestión de feriados de mercado.

**Bounded context (DDD):** Datos de Mercado e Información Financiera.

**Interfaces que expone:**
- `IVerificacionMercado`: verificación de horarios, detección de mercado por símbolo, validación de símbolo operable, dashboard y detalle de acción. Consumida por el módulo de Órdenes antes de enviar cualquier orden.

**Consume de otros módulos:**
- Adaptadores de Integración (`IIntegracionAlpaca`, `IAlphaVantage`) para obtener datos de mercado de proveedores externos. El módulo de Mercado nunca llama a APIs externas directamente; siempre pasa por los adaptadores del módulo de Integración.
- `IAuditLog` (Trazabilidad): registra fallos de sincronización o indisponibilidad de proveedores.

**Entidades JPA propias:** `Activo`, `PrecioCache`, `MercadoConfig` (compartida lógicamente con Administración, pero el módulo de Mercado la lee; Administración la escribe).

**Tablas BD propias (lectura):** `Activo`, `precio_cache`, `mercado_config`, `feriado_mercado`.

**Notas de diseño (normalización 3NF, mayo 2026):**
- `Activo` es el catálogo de instrumentos financieros negociables (ticker, nombreEmpresa, tipo, mercadoConfigId).
- `PrecioCache.activo_id` es la PK (shared con Activo.id). `simbolo` se mantiene como columna denormalizada para consultas rápidas.
- `ActivoRepository.findByTicker(String)` es el método principal de lookup por símbolo.

**Tácticas de calidad implementadas:**
- **Caché de precios en BD** (`precio_cache`): los 1500 usuarios concurrentes consultan la caché en lugar de llamar directamente a la API externa, evitando superar límites de tasa.
- **Manage Sampling Rate**: sincronización proactiva cada 2-3 segundos con el proveedor; el desfase máximo entre precio real y precio mostrado nunca supera 3 segundos.
- **Degradation**: si el proveedor de datos no responde, el dashboard muestra los últimos datos en caché con indicador de hora de última actualización exitosa; no retorna error al usuario.

---

### 3.4 Módulo de Administración (`administracion`)

**Responsabilidad:** Parámetros configurables del sistema (porcentaje de comisión, split plataforma/comisionista, almacenados en BD para cambio sin redespliegue); configuración de mercados y horarios; gestión de feriados; ciclo de vida de cuentas de usuario (activar, suspender, restringir); creación de comisionistas; asignación de comisionistas a inversionistas; dashboard ejecutivo con métricas de negocio; confirmación de suscripción premium vía Stripe.

**Bounded context (DDD):** Configuración y Administración de la Plataforma.

**Interfaces que expone:**
- `IAdministracion`: lectura de configuración de mercados y verificación de feriados. Consumida por el módulo de Mercado para determinar horarios de apertura/cierre.
- `IGestorParametros`: expone los tres valores de comisión vigentes como BigDecimal independientes. Consumida por el módulo de Órdenes al calcular comisiones, para leer el porcentaje vigente desde BD sin hardcodear valores.

**Consume de otros módulos:**
- `IIntegracionStripe` (Integración): inicia y confirma el flujo de checkout de suscripción premium.
- `IAuditLog` (Trazabilidad): registra cambios de parámetros, cambios de estado de usuarios y acciones administrativas.
- `INotificacion` (Integración): notifica a usuarios afectados por cambios de estado o cambios en horarios de mercado.

**Entidades JPA propias:** `Administrador`, `MercadoConfig`, `FeriadoMercado`, `ParametroComision`.

**Tablas BD propias (escritura y lectura):** `administrador`, `mercado_config`, `feriado_mercado`, `parametro_comision`.

**Notas de diseño (normalización 3NF, mayo 2026):**
- `Administrador.id` = PK compartida con `usuario.id` (shared PK, sin auto-generado).
- `FeriadoMercado.mercado_config_id` (FK Long) reemplaza `mercado_codigo` (String).
- `ParametroComision` usa `fecha_inicio`/`fecha_fin` (LocalDate) para vigencia, en lugar del flag `activo` (boolean).
- `ParametroComisionRepository.findParametroActivo(LocalDate)` retorna el parámetro cuyo rango de fechas incluye la fecha dada.

**Tácticas de calidad implementadas:**
- **Defer Binding**: porcentaje de comisión y split guardados en `parametro_comision`; el administrador los modifica desde el panel y el efecto es inmediato para nuevas transacciones, sin recompilar ni redesplegar.
- **Defer Binding**: horarios y feriados de cada mercado guardados en `mercado_config` y `feriado_mercado`; el administrador agrega o modifica feriados desde el panel; el módulo de Mercado los consulta en tiempo real.

---

### 3.5 Módulo de Integración (`integracion`)

**Responsabilidad:** Adaptadores hacia todas las APIs externas (Alpaca, Alpha Vantage, Stripe); despachador de notificaciones multicanal (Email, SMS, WhatsApp); orquestadores de flujos que involucran múltiples sistemas (registro con creación de cuenta Alpaca, suscripción premium con Stripe); manejador de excepciones para fallos de APIs externas con reintentos (3 reintentos con backoff para Alpaca).

**Bounded context (DDD):** Anticorrupción y Adaptadores Externos. Este módulo actúa como la capa de anticorrupción que aisla al resto del sistema de los contratos específicos de cada proveedor externo. Si se cambia de Alpha Vantage a otro proveedor, solo se modifica el adaptador correspondiente sin tocar ningún otro módulo.

**Interfaces que expone:**
- `IIntegracionAlpaca`: crear cuenta Alpaca, enviar orden, consultar estado de orden, sincronizar posiciones, obtener snapshots de precios. Consumida por Órdenes (para trading) y Mercado (para datos de mercado).
- `IAlphaVantage`: cotizaciones globales, históricos y overview de acciones. Consumida por Mercado.
- `IIntegracionStripe`: crear sesión de checkout, cancelar suscripción, consultar estado de suscripción. Consumida por Administración.
- `INotificacion`: despachar notificación a los canales configurados por el usuario (Email, SMS, WhatsApp). Consumida por Autenticación y Órdenes.

**NO persiste en BD propia.** Cuando un orquestador necesita persistir un resultado (por ejemplo, marcar `pendiente_cuenta_alpaca = false` tras crear la cuenta Alpaca del usuario), lo hace invocando el módulo dueño del dato a través de su interfaz, no directamente sobre la BD.

**Tácticas de calidad implementadas:**
- **Retry**: al fallar una llamada a Alpaca, el adaptador reintenta hasta 3 veces con intervalos crecientes antes de declarar la orden como fallida.
- **Exception Handling**: captura excepciones de cada API externa, registra el error con contexto (tipo, módulo, orden involucrada) y ejecuta acción alternativa (mostrar datos de caché para el dashboard, liberar fondos reservados si la orden falla).
- **Tailor Interface**: cada adaptador traduce el formato propio del proveedor externo al formato interno unificado del sistema. El resto del sistema trabaja siempre con DTOs internos.
- **Orchestrate**: los orquestadores coordinan secuencias de pasos que involucran múltiples sistemas externos, garantizando consistencia ante fallos parciales.

---

### 3.6 Módulo de Trazabilidad (`trazabilidad`)

**Responsabilidad:** Registro inmutable de todos los eventos auditables del sistema: órdenes ejecutadas, intentos de login, bloqueos de cuenta, acciones administrativas, accesos denegados, errores del sistema. Envío asíncrono al motor de logs centralizado (Splunk o Elasticsearch; en desarrollo local: consola y archivo `logs/audit.log`). Monitoreo de salud del sistema (heartbeat).

**Bounded context (DDD):** Auditoría y Observabilidad. Módulo transversal consumido por todos los demás módulos; no consume a ninguno.

**Interfaces que expone:**
- `IAuditLog`: `void registrar(TipoEvento tipo, String correoUsuario, String detalle)` — implementado. Cualquier evento auditable va aquí; ningún módulo reimplementa lógica de logging.
- `registrarConIp(TipoEvento tipo, String correoUsuario, String detalle, String ipOrigen)` — **pendiente de implementar**: permitirá adjuntar la IP de origen al evento auditado, necesario para EC-08. Actualmente la IP se incluye como parte del texto en `detalle`.

**NO persiste directamente en la BD relacional de negocio.** Los eventos de auditoría se envían al motor de logs externo. En desarrollo se usa consola + archivo. La tabla `evento_auditoria` en PostgreSQL puede existir como respaldo temporal o para consultas del responsable legal, pero el canal primario de auditoría es el motor de logs.

**Tácticas de calidad implementadas:**
- **Maintain Audit Trail**: cada orden genera un registro inmutable que incluye timestamp, correo del usuario, IP de origen, tipo de orden, activo, cantidad, monto, comisión y resultado.
- **Auditoría asíncrona**: el registro es asíncrono (`@Async` de Spring) para que una caída del motor de logs no bloquee el flujo de negocio ni supere el límite de 5 segundos de confirmación de órdenes.
- **Heartbeat**: el módulo emite señal de salud periódica; si un módulo deja de responder durante 3 ciclos consecutivos (cada 10 segundos), se registra el fallo y se notifica al administrador.

---

## 4. Módulo compartido (`shared`)

No es un módulo de dominio. Contiene infraestructura transversal que no pertenece a ningún dominio específico:

- **`shared/config/`**: configuración de CORS (`CorsConfig`) y endpoint de salud (`HealthController` → `/api/health`).
- **`shared/dto/`**: `RespuestaDTO`, envoltorio genérico de respuestas HTTP.
- **`shared/exceptions/`**: `GlobalExceptionHandler` y todas las excepciones de negocio con nombres explícitos (`FondosInsuficientesException`, `HoldingInsuficienteException`, `OrdenNoEncontradaException`, `AccountLockedException`, `InvalidMfaException`, `InvalidTokenException`, `EmailAlreadyExistsException`, `SimboloInvalidoException`, `StripeCheckoutException`, `UsuarioNoEncontradoException`).
- **`shared/util/`**: utilidades compartidas (actualmente vacío, preparado para constantes o helpers transversales).

---

## 5. Reglas de comunicación entre módulos

Estas reglas son el contrato que mantiene la modularidad del Monolito Modular. Violarlas destruye los beneficios de la arquitectura.

1. **Un módulo nunca importa clases internas (`*Service`, `*Repository`, entidades JPA) de otro módulo.** Solo importa las interfaces `I...` declaradas en el paquete `interfaces/` del módulo dueño.
2. **La inyección entre módulos se hace por constructor** en Spring Boot (inyección de la interfaz, no de la implementación).
3. **Trazabilidad es transversal:** cualquier evento auditable se registra vía `IAuditLog`. Ningún módulo reimplementa lógica de logging.
4. **Integración aísla todo lo externo:** ningún módulo llama directamente a Alpaca, Stripe, Alpha Vantage, SMTP o cualquier API externa. Todos pasan por los adaptadores del módulo de Integración a través de sus interfaces.
5. **Administración es la única escritora de parámetros:** `ParametroComision`, `MercadoConfig` y `FeriadoMercado` solo se modifican desde el módulo de Administración. El módulo de Órdenes los lee a través de `IGestorParametros` y Mercado a través de `IVerificacionMercado`.
6. **Las entidades JPA no cruzan los límites de módulo como respuesta.** La comunicación entre módulos usa DTOs o tipos primitivos/enumeraciones, nunca entidades JPA.

### Mapa de dependencias entre módulos

```
Autenticación   ──► IAuditLog, INotificacion
Órdenes         ──► IVerificacionMercado, IIntegracionAlpaca, IAsignacionComisionista,
                    IConsultaInversionista, IGestorParametros, IAuditLog
Mercado         ──► IIntegracionAlpaca, IAlphaVantage, IAdministracion, IAuditLog
Administración  ──► IGestionCuentas, IOrden, IIntegracionStripe, IAuditLog
Integración     ──► IAuditLog
Trazabilidad    ──► (ninguno; es consumido por todos)
```

---

## 6. Autorización por rol

Cada solicitud HTTP incluye el JWT del usuario. El filtro `JwtAuthenticationFilter` extrae el rol del token antes de que llegue a cualquier controller. El sistema verifica el rol antes de ejecutar cualquier acción.

| Rol | Acceso principal |
|---|---|
| **Inversionista** | Órdenes propias, portafolio propio, dashboard, perfil, gestionar suscripción premium, consultar estado de orden activa, cancelar orden, aprobar o rechazar propuestas de comisionista, solicitar comisionista |
| **Comisionista** | Portafolio y órdenes de sus clientes asignados únicamente, firmar orden cuando el inversionista la aprueba, proponer órdenes al inversionista |
| **Administrador** | Configuración de parámetros de comisión, horarios y feriados de mercado, gestión del ciclo de vida de usuarios, creación y asignación de comisionistas, dashboard ejecutivo |
| **Responsable Legal** | Módulo de auditoría, reportes de cumplimiento |

**Regla crítica de MFA:**
- Comisionista y Administrador: MFA obligatorio en todo inicio de sesión.
- Inversionista: MFA opcional; se activa durante el registro o desde Gestión de Perfil.

---

## 7. Suscripción premium

- **Precio:** USD $12/mes o USD $120/año.
- **Procesamiento:** Stripe (vía `IIntegracionStripe` → `StripeAdapter`).
- **Activación:** durante el registro o desde Gestión de Perfil.
- **Funcionalidades exclusivas de usuarios premium:**
  - Alertas de precio (por encima o por debajo de umbrales definidos).
  - Watchlist (lista de observación con alertas configurables).

---

## 8. Tipos de órdenes soportados

| Tipo | Descripción |
|---|---|
| **Market Order** | Compra o venta al mejor precio disponible en el mercado |
| **Limit Order** | Compra o venta solo si el precio alcanza un valor específico o mejor |
| **Stop Loss** | Venta automática si el precio cae a un nivel predeterminado |
| **Take Profit** | Cierre automático de posición si el precio alcanza un nivel de ganancia |

Si se genera una orden fuera del horario de operación del mercado destino, la orden puede encolarse para su procesamiento en la próxima apertura (FIFO). El encolamiento debe ser confirmado explícitamente por el usuario o comisionista. El sistema mantiene trazabilidad del encolamiento.

**Estados posibles de una orden:** Pendiente, Enviada, Firmada por Comisionista, En Ejecución, Cancelada, Rechazada, Expirada, En Revisión, Fallida, Detenida.

---

## 9. Estructura de carpetas (backend Spring Boot)

```text
backend/
├── pom.xml                         # Proyecto Maven, Spring Boot 3.x, Java 17, empaquetado WAR.
├── .env                            # Configuración local sensible: BD, JWT, SMTP, Alpaca, Stripe, Alpha Vantage, comisiones, mercado.
├── src/
│   ├── main/
│   │   ├── java/co/edu/unbosque/accioneselbosque/
│   │   │   ├── AccionesElBosqueApplication.java   # Punto de entrada Spring Boot.
│   │   │   ├── ServletInitializer.java             # Inicializador WAR para servlet container externo.
│   │   │   ├── autenticacion/                      # Módulo 1: Identidad y Acceso
│   │   │   ├── ordenes/                            # Módulo 2: Trading y Finanzas
│   │   │   ├── mercado/                            # Módulo 3: Datos de Mercado
│   │   │   ├── administracion/                     # Módulo 4: Configuración y Administración
│   │   │   ├── integracion/                        # Módulo 5: Adaptadores Externos
│   │   │   ├── trazabilidad/                       # Módulo 6: Auditoría y Observabilidad
│   │   │   └── shared/                             # Infraestructura transversal
│   │   └── resources/
│   │       ├── application.properties              # BD PostgreSQL, JPA, SMTP, JWT, Alpaca, Stripe, Alpha Vantage, parámetros de comisión, mercado, puerto 8080.
│   │       └── static/
│   │           ├── dashboard.html                  # Página estática de apoyo/prueba.
│   │           └── test-auth.html                  # Página estática de pruebas de autenticación.
│   └── test/java/                                  # Estructura de pruebas; sin clases activas actualmente.
└── target/                                         # Artefactos generados por Maven; no es código fuente.
```

---

### 9.1 Paquete raíz

```text
co.edu.unbosque.accioneselbosque/
├── AccionesElBosqueApplication.java
└── ServletInitializer.java
```

---

### 9.2 Módulo de Autenticación

```text
autenticacion/
├── controller/
│   ├── AuthController.java             # Login, verificación MFA, logout: /api/auth
│   ├── RegistroController.java         # Registro, confirmación y disponibilidad de correo
│   ├── RecuperacionController.java     # Recuperación y reset de contraseña
│   └── PerfilController.java           # Perfil del inversionista, preferencias, MFA y solicitud de comisionista
├── service/
│   ├── AutenticacionService.java
│   ├── RegistroService.java
│   ├── MFAService.java
│   ├── MonitorIntentosService.java
│   ├── RecuperacionPasswordService.java
│   ├── PerfilService.java
│   ├── AsignacionComisionistaService.java
│   ├── DatosInicialesComisionistas.java
│   ├── DatosInicialesPerfilesRol.java
│   └── NormalizacionUsuarios3FnMigration.java
├── repository/
│   ├── UsuarioRepository.java
│   ├── InversionistaRepository.java
│   ├── ComisionistaRepository.java
│   ├── AsignacionComisionistaRepository.java
│   ├── CodigoVerificacionRepository.java
│   └── IntentoFallidoRepository.java
├── model/
│   ├── Usuario.java
│   ├── Inversionista.java
│   ├── Comisionista.java
│   ├── AsignacionComisionista.java
│   ├── CodigoVerificacion.java
│   ├── IntentoFallido.java
│   ├── Rol.java
│   └── EstadoCuenta.java
├── dto/
│   ├── LoginRequestDTO.java
│   ├── LoginResponseDTO.java
│   ├── MFARequestDTO.java
│   ├── RegistroInversionistaDTO.java
│   ├── ConfirmarRegistroDTO.java
│   ├── ConfirmarRegistroResponseDTO.java
│   ├── CorreoDisponibleDTO.java
│   ├── RecuperarPasswordDTO.java
│   ├── ResetPasswordDTO.java
│   ├── ActualizarPerfilDTO.java
│   ├── PerfilInversionistaDTO.java
│   ├── PreferenciasNotificacionDTO.java
│   ├── PreferenciasOperacionDTO.java
│   ├── ClienteAsignadoDTO.java
│   └── ComisionistaAsignadoDTO.java
├── interfaces/
│   ├── IAutenticacion.java             # Expuesta al exterior del módulo
│   ├── IControlAcceso.java             # Expuesta: consumida por todos los módulos para verificar permisos
│   └── IAsignacionComisionista.java    # Expuesta: consumida por Órdenes
└── security/
    ├── SecurityConfig.java
    ├── JwtUtil.java
    ├── JwtAuthenticationFilter.java
    └── BCryptConfig.java
```

**Interfaces expuestas y sus contratos:**

`IAutenticacion`:
- `LoginResponseDTO login(LoginRequestDTO request)`
- `void verificarMFA(MFARequestDTO request)`
- `void logout(String token)`
- `void solicitarRecuperacion(RecuperarPasswordDTO request)`
- `void resetPassword(ResetPasswordDTO request)`
- `boolean correoDisponible(String correo)`

`IControlAcceso`:
- `boolean tieneRol(String token, Rol rolRequerido)`
- `Long extraerUsuarioId(String token)`

`IAsignacionComisionista`:
- `Optional<AsignacionComisionista> obtenerAsignacionActiva(Long inversionistaId)`
- `boolean tieneComisionistaAsignado(Long inversionistaId)`

---

### 9.3 Módulo de Órdenes

```text
ordenes/
├── controller/
│   ├── OrdenController.java            # Crear, previsualizar, cancelar, listar activas e historial: /api/ordenes
│   ├── PortafolioController.java       # Portafolio, saldo, depósito y sincronización: /api/portafolio
│   ├── PropuestaController.java        # Aprobación/rechazo de propuestas del comisionista: /api/propuestas
│   └── ComisionistaController.java     # Vista del comisionista: clientes, propuestas y firma/envío: /api/comisionista
├── service/
│   ├── OrdenService.java
│   ├── PortafolioService.java
│   ├── SaldoService.java
│   └── ColaOrdenesService.java
├── repository/
│   ├── OrdenRepository.java
│   ├── HoldingRepository.java
│   ├── CuentaFondosRepository.java
│   └── ComisionRepository.java
├── model/
│   ├── Orden.java
│   ├── Holding.java
│   ├── CuentaFondos.java
│   ├── Comision.java
│   ├── EstadoOrden.java
│   ├── TipoOrden.java
│   └── TipoLado.java
├── dto/
│   ├── CrearOrdenRequestDTO.java
│   ├── CrearPropuestaOrdenDTO.java
│   ├── DecisionPropuestaDTO.java
│   ├── OrdenDTO.java
│   ├── PortafolioDTO.java
│   ├── HoldingDTO.java
│   ├── SaldoDTO.java
│   ├── ResumenComisionDTO.java
│   └── ComisionDetalleDTO.java
└── interfaces/
    └── IOrden.java                     # Expuesta al exterior del módulo
```

**Interfaces expuestas y sus contratos:**

`IOrden`:
- `OrdenDTO previsualizar(CrearOrdenRequestDTO request, Long usuarioId)`
- `OrdenDTO crear(CrearOrdenRequestDTO request, Long usuarioId, String ipOrigen)`
- `void cancelar(Long ordenId, Long usuarioId)`
- `List<OrdenDTO> listarActivas(Long usuarioId)`
- `List<OrdenDTO> historial(Long usuarioId)`
- `OrdenDTO crearPropuesta(CrearPropuestaOrdenDTO request, Long comisionistaId)`
- `void decidirPropuesta(Long ordenId, DecisionPropuestaDTO decision, Long inversionistaId)`
- `void firmarYEnviar(Long ordenId, Long comisionistaId)`

---

### 9.4 Módulo de Mercado

```text
mercado/
├── controller/
│   └── MercadoController.java          # Dashboard, cotización, detalle, símbolos y horario: /api/mercado
├── service/
│   └── MercadoService.java             # Caché de precios, horarios y consulta a Alpaca/Alpha Vantage
├── repository/
│   └── PrecioCacheRepository.java
├── model/
│   └── PrecioCache.java
├── dto/
│   ├── CotizacionDTO.java
│   └── DetalleAccionDTO.java
└── interfaces/
    └── IVerificacionMercado.java       # Expuesta al exterior del módulo
```

**Interfaces expuestas y sus contratos:**

`IVerificacionMercado`:
- `boolean esMercadoAbierto(String mercado)`
- `String detectarMercado(String simbolo)`
- `CotizacionDTO validarSimboloOperable(String simbolo)`
- `List<CotizacionDTO> obtenerDashboard(String interesesMercado)`
- `DetalleAccionDTO obtenerDetalle(String simbolo)`

---

### 9.5 Módulo de Administración

```text
administracion/
├── controller/
│   ├── AdminController.java            # Dashboard ejecutivo, mercados, feriados, comisiones y usuarios: /api/admin
│   └── SuscripcionController.java      # Confirmación de checkout Stripe
├── service/
│   ├── AdministracionService.java
│   └── DatosInicialesAdministracion.java
├── repository/
│   ├── AdministradorRepository.java
│   ├── MercadoConfigRepository.java
│   ├── FeriadoMercadoRepository.java
│   └── ParametroComisionRepository.java
├── model/
│   ├── Administrador.java
│   ├── MercadoConfig.java
│   ├── FeriadoMercado.java
│   └── ParametroComision.java
├── dto/
│   ├── DashboardEjecutivoDTO.java
│   ├── TendenciaMercadoDTO.java
│   ├── UsuarioAdminDTO.java
│   ├── CrearComisionistaDTO.java
│   ├── CambiarEstadoCuentaDTO.java
│   ├── MercadoConfigDTO.java
│   ├── FeriadoMercadoDTO.java
│   └── ParametroComisionDTO.java
└── interfaces/
    ├── IAdministracion.java            # Expuesta: consumida por controllers de Administrador
    └── IGestorParametros.java          # Expuesta: consumida por Órdenes para leer parámetros de comisión
```

**Interfaces expuestas y sus contratos:**

`IAdministracion`:
- `DashboardEjecutivoDTO obtenerDashboard()`
- `List<MercadoConfigDTO> listarMercados()`
- `MercadoConfigDTO actualizarMercado(String codigo, MercadoConfigDTO dto)`
- `FeriadoMercadoDTO agregarFeriado(FeriadoMercadoDTO dto)`
- `void eliminarFeriado(Long feriadoId)`
- `ParametroComisionDTO actualizarParametroComision(ParametroComisionDTO dto)`
- `List<UsuarioAdminDTO> listarUsuarios()`
- `void cambiarEstadoCuenta(Long usuarioId, CambiarEstadoCuentaDTO dto)`
- `void crearComisionista(CrearComisionistaDTO dto)`
- `void asignarComisionista(Long comisionistaId, Long inversionistaId)`

`IGestorParametros`:
- `BigDecimal obtenerPorcentajeComision()`
- `BigDecimal obtenerSplitPlataforma()`
- `BigDecimal obtenerSplitComisionista()`

---

### 9.6 Módulo de Integración

```text
integracion/
├── adaptadores/
│   ├── alpaca/
│   │   ├── IIntegracionAlpaca.java     # Expuesta: consumida por Órdenes y Mercado
│   │   └── AlpacaAdapter.java          # Broker API, market data, cuentas, órdenes, posiciones y snapshots
│   ├── alphavantage/
│   │   ├── IAlphaVantage.java          # Expuesta: consumida por Mercado
│   │   └── AlphaVantageAdapter.java    # Cotizaciones globales, históricos y overview
│   ├── stripe/
│   │   ├── IIntegracionStripe.java     # Expuesta: consumida por Administración
│   │   └── StripeAdapter.java          # Checkout, sesiones y cancelación de suscripciones
│   └── polygon/                        # Carpeta preparada; sin implementación actual
├── notificaciones/
│   ├── DespachadorNotificaciones.java  # Implementa INotificacion; enruta al canal correcto
│   └── canales/
│       └── EmailSender.java            # Envío SMTP vía Jakarta Mail / Spring Mail
├── orquestadores/
│   ├── OrquestadorRegistro.java        # Creación de cuenta Alpaca al completar registro
│   └── OrquestadorSuscripcion.java     # Flujo de suscripción premium con Stripe
└── excepciones/                        # Carpeta preparada para manejadores de excepciones de APIs externas
```

> **Nota:** Las interfaces de integración están ubicadas dentro de cada paquete de adaptador (`adaptadores/alpaca/`, `adaptadores/alphavantage/`, `adaptadores/stripe/`). `INotificacion` se declara en `notificaciones/` y la implementa `DespachadorNotificaciones`.
>
> **Excepción arquitectónica documentada:** `IIntegracionAlpaca.crearCuenta` recibe `Usuario` e `Inversionista` (entidades de autenticación) porque Alpaca requiere el perfil completo del usuario en el momento de crear la cuenta broker. Esta dependencia está acotada exclusivamente a la firma de ese método y no representa acoplamiento adicional entre módulos.

**Interfaces expuestas y sus contratos:**

`IIntegracionAlpaca`:
- `String crearCuenta(RegistroInversionistaDTO dto)`
- `OrdenDTO enviarOrden(CrearOrdenRequestDTO request, String alpacaAccountId)`
- `String consultarEstadoOrden(String alpacaOrderId)`
- `List<HoldingDTO> sincronizarPosiciones(String alpacaAccountId)`
- `List<CotizacionDTO> obtenerSnapshots(List<String> simbolos)`

`IAlphaVantage`:
- `CotizacionDTO obtenerCotizacion(String simbolo)`
- `DetalleAccionDTO obtenerDetalle(String simbolo)`
- `List<CotizacionDTO> obtenerHistorico(String simbolo, String intervalo)`

`IIntegracionStripe`:
- `String crearSesionCheckout(Long usuarioId, String planSuscripcion)`
- `void cancelarSuscripcion(String stripeSubscriptionId)`
- `boolean verificarSuscripcionActiva(String stripeCustomerId)`

`INotificacion`:
- `void enviarCodigoRegistro(String correo, String nombreCompleto, String codigo)`
- `void enviarCodigoMfa(String correo, String nombreCompleto, String codigo)`
- `void enviarTokenRecuperacion(String correo, String nombreCompleto, String token)`
- `void notificarBloqueo(String correo, String nombreCompleto)`
- `void notificarAdmin(String asunto, String mensaje)`

---

### 9.7 Módulo de Trazabilidad

```text
trazabilidad/
├── service/
│   └── AuditLogService.java            # Implementa IAuditLog; envío asíncrono al motor de logs
├── repository/
│   └── AuditLogRepository.java         # Respaldo temporal en BD (tabla evento_auditoria)
├── model/
│   ├── EventoAuditoria.java
│   └── TipoEvento.java
└── interfaces/
    └── IAuditLog.java                  # Expuesta: consumida por todos los módulos
```

**Interfaces expuestas y sus contratos:**

`IAuditLog`:
- `void registrar(TipoEvento tipo, String correoUsuario, String detalle)` — implementado.
- `void registrarConIp(TipoEvento tipo, String correoUsuario, String detalle, String ipOrigen)` — **pendiente**: permite adjuntar la IP de origen al evento; necesario para EC-08. Actualmente la IP se incluye como parte del texto en `detalle`.

**Tipos de evento auditados (`TipoEvento`) — nombres canónicos del enum real:**
- Autenticación: `REGISTRO_INICIADO`, `REGISTRO_EXITOSO`, `REGISTRO_FALLO_ALPACA`, `LOGIN_EXITOSO`, `LOGIN_FALLIDO`, `CUENTA_BLOQUEADA`, `MFA_ENVIADO`, `MFA_VERIFICADO`, `MFA_FALLIDO`, `MFA_ACTIVADO`, `MFA_DESACTIVADO`, `LOGOUT`, `RECUPERACION_PASSWORD_SOLICITADA`, `PASSWORD_RESETEADA`, `CAMBIO_ESTADO_CUENTA`
- Perfil: `PERFIL_CONSULTADO`, `PERFIL_ACTUALIZADO`, `PREFERENCIAS_NOTIFICACION_ACTUALIZADAS`, `PREFERENCIAS_OPERACION_ACTUALIZADAS`
- Suscripción: `SUSCRIPCION_PREMIUM_INICIADA`, `SUSCRIPCION_PREMIUM_FALLIDA`, `SUSCRIPCION_PREMIUM_CANCELADA`
- Órdenes: `ORDEN_CREADA`, `ORDEN_ENVIADA_ALPACA`, `ORDEN_EJECUTADA`, `ORDEN_CANCELADA`, `ORDEN_ENCOLADA`, `ORDEN_RECHAZADA_FONDOS`, `ORDEN_RECHAZADA_HOLDINGS`, `ORDEN_FALLO_ALPACA`
- Propuestas: `PROPUESTA_ORDEN_CREADA`, `PROPUESTA_ORDEN_APROBADA`, `PROPUESTA_ORDEN_RECHAZADA`, `PROPUESTA_ORDEN_FIRMADA`
- Comisionista: `COMISIONISTA_ASIGNADO`, `COMISIONISTA_ASIGNACION_FALLIDA`, `ACCESO_DENEGADO_CLIENTE_NO_ASIGNADO`
- Portafolio/Saldo: `FONDOS_SINCRONIZADOS`, `HOLDING_ACTUALIZADO`
- Mercado: `PRECIO_CONSULTADO`, `CACHE_REFRESCADO`
- Administración: `PARAMETRO_ADMIN_ACTUALIZADO`, `USUARIO_ADMIN_GESTIONADO`, `ACCESO_DENEGADO_ADMIN`, `OPERACION_RESTRINGIDA_BLOQUEADA`

> Nota de auditoría (2026-05-25): Estos son los nombres reales del enum `TipoEvento.java`. Todos los SPECs deben usar estos nombres exactos. Los nombres anteriores (`ORDEN_FALLIDA`, `PROPUESTA_CREADA`, `FERIADO_MODIFICADO`, `MERCADO_MODIFICADO`, `SUSCRIPCION_ACTIVADA`, `FALLO_API_EXTERNA`, `ESTADO_CUENTA_CAMBIADO`, `USUARIO_ELIMINADO`) **no existen** en el enum.

---

### 9.8 Módulo compartido (`shared`)

```text
shared/
├── config/
│   ├── CorsConfig.java                 # Permite solicitudes desde http://localhost:4200
│   └── HealthController.java           # GET /api/health → estado del sistema
├── dto/
│   └── RespuestaDTO.java              # Envoltorio genérico de respuestas HTTP
├── exceptions/
│   ├── GlobalExceptionHandler.java
│   ├── AccountLockedException.java
│   ├── EmailAlreadyExistsException.java
│   ├── FondosInsuficientesException.java
│   ├── HoldingInsuficienteException.java
│   ├── InvalidMfaException.java
│   ├── InvalidTokenException.java
│   ├── OrdenNoEncontradaException.java
│   ├── SimboloInvalidoException.java
│   ├── StripeCheckoutException.java
│   └── UsuarioNoEncontradoException.java
└── util/                               # Preparado para helpers y constantes transversales
```

---

### 9.9 Recursos y configuración del backend

```text
src/main/resources/
├── application.properties              # BD PostgreSQL, JPA/Hibernate, SMTP, JWT, seguridad, Alpaca, Stripe, Alpha Vantage, parámetros de comisión, mercado y puerto 8080.
└── static/
    ├── dashboard.html                  # Página estática de apoyo/prueba.
    └── test-auth.html                  # Página estática de pruebas de autenticación.
```

---

## 10. Estructura de carpetas (frontend Angular)

El frontend vive bajo `frontend/` y usa Angular 21 con componentes standalone, rutas declaradas en `app.routes.ts` y un `ApiService` central basado en `fetch`. La estructura actual no usa `features/`, `shared/`, `interceptors/` ni `environments/`; esos paquetes no existen en el código fuente actual.

```text
frontend/
├── package.json                        # Scripts npm y dependencias Angular/RxJS/Vitest/Prettier
├── package-lock.json
├── angular.json                        # Configuración del workspace Angular
├── tsconfig.json
├── tsconfig.app.json
├── tsconfig.spec.json
├── .editorconfig
├── .prettierrc
├── .gitignore
├── README.md
├── public/
│   └── favicon.ico
├── src/
│   ├── index.html
│   ├── main.ts
│   ├── styles.scss                     # Estilos globales
│   └── app/
│       ├── app.ts                      # Componente raíz
│       ├── app.html
│       ├── app.scss
│       ├── app.config.ts               # Providers globales: router y manejo global de errores
│       ├── app.routes.ts               # Rutas principales y protección por authGuard
│       ├── app.spec.ts
│       ├── core/
│       ├── auth/
│       ├── dashboard/
│       ├── comisionista/
│       └── admin/
├── .vscode/
├── node_modules/                       # Dependencias; no es código fuente
├── .angular/                           # Caché de Angular; generado
└── dist/                               # Build generado por Angular
```

### 10.1 Rutas declaradas

```text
/                       → AuthShellComponent; redirige a /login
/login                  → LoginComponent
/registro               → RegistroComponent
/verificar-registro     → VerificarRegistroComponent
/recuperar              → RecuperarComponent
/reset-password         → ResetPasswordComponent
/dashboard              → DashboardComponent          protegido por authGuard (INVERSIONISTA)
/comisionista           → ComisionistaDashboardComponent  protegido por authGuard (COMISIONISTA)
/admin                  → AdminDashboardComponent     protegido por authGuard (ADMINISTRADOR)
**                      → redirige a /login
```

### 10.2 Core frontend

```text
core/
├── api.service.ts          # Cliente HTTP central hacia http://localhost:8080; maneja JWT en sessionStorage/localStorage
├── auth.guard.ts           # Protege rutas y redirige según rol: INVERSIONISTA, COMISIONISTA o ADMINISTRADOR
├── models.ts               # Interfaces TypeScript compartidas: LoginResponse, Perfil, Cotizacion, Orden, Portafolio, Admin, etc.
└── toast.service.ts        # Servicio transversal de mensajes visuales
```

### 10.3 Módulo de autenticación (frontend)

```text
auth/
├── auth-shell.component.ts/html/scss
├── auth-card.scss                      # Estilos compartidos de pantallas auth
├── login.component.ts/html
├── registro.component.ts/html
├── verificar-registro.component.ts/html
├── recuperar.component.ts/html
└── reset-password.component.ts/html
```

Consume endpoints de `/api/auth` y `/api/suscripciones`: login, MFA, registro, verificación de correo, confirmación de registro, recuperación de contraseña y confirmación de checkout Stripe.

### 10.4 Dashboard del inversionista (frontend)

```text
dashboard/
├── dashboard.component.ts
├── dashboard.component.html
└── dashboard.component.scss
```

Agrupa la experiencia principal del inversionista: perfil, preferencias, MFA, mercado, detalle de acción, portafolio, saldo, depósito, creación/previsualización/cancelación de órdenes, historial y aprobación/rechazo de propuestas de comisionista. Consume endpoints de `/api/perfil`, `/api/mercado`, `/api/portafolio`, `/api/ordenes` y `/api/propuestas`.

### 10.5 Dashboard del comisionista (frontend)

```text
comisionista/
├── comisionista-dashboard.component.ts
├── comisionista-dashboard.component.html
└── comisionista-dashboard.component.scss
```

Vista del comisionista para consultar clientes asignados, ver portafolios e historiales, crear propuestas de orden y firmar/enviar propuestas aprobadas. Consume endpoints de `/api/comisionista` y reutiliza datos de `/api/mercado`.

### 10.6 Dashboard administrativo (frontend)

```text
admin/
├── admin-dashboard.component.ts
├── admin-dashboard.component.html
└── admin-dashboard.component.scss
```

Panel administrativo para dashboard ejecutivo, configuración de mercados, feriados, parámetros de comisión, gestión de usuarios, creación de comisionistas, asignación de comisionistas a inversionistas, cambios de estado y eliminación lógica. Consume endpoints de `/api/admin` y `/api/auth/logout`.

---

## 11. Topología de despliegue

```
┌────────────────────────┐
│ Dispositivo del Usuario│   Browser (Chrome / Edge / Opera GX)
└──────────┬─────────────┘
           │ HTTP
┌──────────▼─────────────┐
│   Frontend Angular     │   ng serve → http://localhost:4200
└──────────┬─────────────┘
           │ HTTP REST API
┌──────────▼─────────────────────────────────────────┐
│         Servidor de Aplicación (Spring Boot)       │
│   http://localhost:8080                            │
│                                                    │
│   ┌─────────────────────────────────────────────┐  │
│   │     6 módulos de dominio en el mismo JVM:   │  │
│   │                                             │  │
│   │  autenticacion  │  ordenes  │  mercado      │  │
│   │  administracion │ integracion│ trazabilidad │  │
│   │                                             │  │
│   │  shared/ (CORS, excepciones, DTOs comunes)  │  │
│   └─────────────────────────────────────────────┘  │
└──┬──────────────────┬──────────────────┬────────────┘
   │ JDBC             │ HTTPS REST       │ Async HTTP
   ▼                  ▼                  ▼
┌──────────┐  ┌───────────────────┐  ┌──────────────────┐
│PostgreSQL│  │ APIs Externas:    │  │ Servidor de Logs │
│localhost │  │ Alpaca (broker +  │  │ Splunk /         │
│ :5432    │  │ market data),     │  │ Elasticsearch    │
│          │  │ Stripe,           │  │ (en desarrollo:  │
│          │  │ Alpha Vantage,    │  │  consola +       │
└──────────┘  │ SMTP              │  │  audit.log)      │
              └───────────────────┘  └──────────────────┘
```

**En desarrollo local:**
- Frontend: `ng serve` en `http://localhost:4200`.
- Backend: Spring Boot en `http://localhost:8080`.
- PostgreSQL: instalación nativa en `localhost:5432`.
- APIs externas: sandbox/test mode (Alpaca Paper Trading, Stripe test mode).
- Logs: consola y archivo `logs/audit.log`.

---

## 12. Base de datos (PostgreSQL)

La base de datos tiene **16 tablas**, todas ya creadas y funcionales. Se listan a continuación con el módulo dueño de cada tabla:

| Tabla | Módulo dueño | Descripción |
|---|---|---|
| `usuario` | Autenticación | Credenciales, rol, estado de cuenta, MFA |
| `inversionista` | Autenticación | Datos del perfil del inversionista, preferencias, suscripción premium, Alpaca account ID |
| `comisionista` | Autenticación | Datos del comisionista |
| `asignacion_comisionista` | Autenticación | Relación inversionista-comisionista activa |
| `codigo_verificacion` | Autenticación | Códigos MFA y verificación de registro |
| `intento_fallido` | Autenticación | Control de bloqueo por intentos fallidos |
| `orden` | Órdenes | Ciclo de vida completo de cada orden |
| `holding` | Órdenes | Acciones en posesión del inversionista |
| `cuenta_fondos` | Órdenes | Saldo disponible y fondos reservados |
| `comision` | Órdenes | Desglose de comisiones por orden |
| `mercado_config` | Administración (escribe) / Mercado (lee) | Horarios de apertura/cierre por mercado |
| `feriado_mercado` | Administración (escribe) / Mercado (lee) | Feriados por mercado |
| `parametro_comision` | Administración (escribe) / Órdenes (lee) | Porcentaje de comisión y split vigentes |
| `administrador` | Administración | Datos del perfil de administradores |
| `precio_cache` | Mercado | Caché local de precios actualizada cada 2-3 segundos |
| `evento_auditoria` | Trazabilidad | Respaldo de eventos auditables |

---

## 13. Decisiones arquitectónicas registradas

### 13.1 Por qué Monolito Modular

El sistema corre en un único Spring Boot. Organizar el código verticalmente por dominio con interfaces como frontera entre módulos describe con exactitud lo que existe: un único proceso con módulos de dominio claramente delimitados que se comunican a través de interfaces Java en el mismo JVM. Los beneficios de modularidad (cohesión por dominio, bajo acoplamiento entre módulos, interfaces explícitas) se conservan completamente y la transaccionalidad financiera es simple y confiable dentro de un único contexto JPA.

### 13.2 Por qué DDD para organizar los módulos

Los 6 módulos corresponden a contextos acotados (Bounded Contexts) con sus propias entidades, reglas de negocio y lenguaje ubícuo. Esto facilita que cada integrante del equipo trabaje en un dominio sin necesidad de conocer los detalles internos de los demás.

### 13.3 Por qué tabla `parametro_comision` en BD y no constantes en código

El porcentaje de comisión y el split plataforma/comisionista son parametrizables por el administrador sin intervención técnica. Hardcodearlos como constantes requeriría recompilar y redesplegar ante cualquier cambio. Almacenados en BD, el efecto es inmediato (táctica Defer Binding).

### 13.4 Por qué los códigos de verificación (MFA) están en BD y no en RAM

Un `ConcurrentHashMap` estático funcionaría en un proceso único, pero si en el futuro se añaden réplicas detrás de un balanceador, cada réplica tendría su propio mapa y el código enviado desde una réplica no sería reconocido por otra. BD garantiza consistencia ahora y facilita escalar después.

### 13.5 Por qué `EmailSender` como canal separado y no un método único

EC-17 exige enviar notificaciones por múltiples canales (Email, SMS, WhatsApp). Un único método monolítico mezclaría APIs y protocolos distintos. El patrón Adaptador por canal (`EmailSender`, y en el futuro `SmsSender`, `WhatsAppSender`) aísla cada integración y cumple la táctica Tailor Interface.

### 13.6 Por qué auditoría asíncrona

Si el registro en el motor de logs fuera síncrono y bloqueara el flujo, una caída de Splunk o Elasticsearch derribaría el procesamiento de órdenes. La auditoría asíncrona (`@Async`) desacopla la observabilidad del flujo de negocio y mantiene el límite de 5 segundos para confirmación de órdenes.

### 13.7 Por qué IGestorParametros es una interfaz separada de IAdministracion

`IAdministracion` expone operaciones de escritura y gestión que solo deben ser accesibles desde controllers del rol Administrador. `IGestorParametros` expone únicamente los tres valores de comisión como BigDecimal, que el módulo de Órdenes necesita leer para calcular comisiones. Separar las interfaces evita que el módulo de Órdenes dependa de una interfaz con más permisos de los necesarios (principio de mínimo privilegio entre módulos).

### 13.8 Por qué Java 17 + Spring Boot 3 + Maven WAR + PostgreSQL

El equipo tiene experiencia previa con este stack. Usar tecnologías ya conocidas reduce errores por curva de aprendizaje en un proyecto con plazo acotado. Son tecnologías de uso real en la industria financiera. La combinación es la más documentada para aplicaciones empresariales en Java y el soporte de Spring Boot 3 con Jakarta EE 10 es el estándar actual.

### 13.9 Por qué Angular 21 con componentes standalone

El equipo tiene experiencia previa en Angular. Los componentes standalone (sin NgModules) simplifican la estructura del proyecto y reducen el boilerplate. TypeScript proporciona tipado que facilita la integración con los DTOs del backend.

---

## 14. Reglas operativas (referencia para desarrollo)

- **Antes de crear un archivo nuevo**, identifica a qué módulo pertenece y colócalo bajo el paquete correcto del módulo.
- **Nunca importes una clase de implementación (`*Service`, `*Repository`, entidad JPA) de otro módulo.** Solo importa las interfaces `I...` declaradas en el paquete `interfaces/` del módulo dueño.
- **Si un endpoint requiere datos de varios módulos**, encadénalos a través de sus interfaces en el service del módulo coordinador; no pongas la lógica de coordinación directamente en un controller.
- **Para cualquier llamada a una API externa** (Alpaca, Stripe, Alpha Vantage, SMTP), crea o reusa un adaptador en `integracion/adaptadores/`. Ningún módulo de negocio llama a una API externa directamente.
- **Para cualquier evento auditable**, llama a `IAuditLog` desde el módulo donde se origina el evento. No dupliques la lógica de logging.
- **Los parámetros de comisión se leen siempre desde `IGestorParametros`**, nunca como constantes en el código.
- **La verificación de horarios de mercado se hace siempre a través de `IVerificacionMercado`**, nunca con lógica de horarios hardcodeada en el módulo de Órdenes.
