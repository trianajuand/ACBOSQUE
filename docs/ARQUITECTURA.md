# Arquitectura del Sistema — SOA Consolidada

> Fuente: Informe de Ingeniería Acciones ElBosque, secciones 18, 19 y 20. Este archivo es la referencia obligatoria antes de crear cualquier archivo nuevo o tomar decisiones estructurales.

---

## 1. Estilo arquitectónico: SOA (Service-Oriented Architecture)

El sistema **NO** se organiza por capas (`controller/`, `service/`, `repository/` en la raíz). Se organiza **por servicio**, con cada servicio:
- expone interfaces explícitas (`I...`),
- oculta su implementación interna,
- puede cambiar internamente sin afectar a los demás siempre que respete su interfaz.

**Topología de despliegue:** **consolidada** (Service-Based Architecture). Los 6 servicios viven en el **mismo runtime Spring Boot**, mismo contenedor lógico, misma aplicación. Se eligió esta topología porque:
- equipo de 4 personas y plazo acotado;
- microservicios separados implicarían infraestructura adicional (service discovery, gateway, comunicación distribuida) que excede los recursos;
- conserva los beneficios de SOA y permite evolucionar a microservicios después sin reescribir lógica de negocio.

---

## 2. Los 6 Servicios

### 2.1 Servicio de Autenticación
- **Responsabilidad:** registro de inversionista, login, MFA, recuperación de contraseña, monitor de intentos fallidos, control de acceso por rol.
- **Interfaces expuestas:** `IAutenticacion`, `IControlAcceso`.
- **Consume de otros servicios:** `IAuditLog` (Trazabilidad), `INotificacion` (Integración).
- **Persiste en:** BD relacional (Usuario, IntentoFallido, CodigoVerificacion, TokenRevocado).

### 2.2 Servicio de Órdenes
- **Responsabilidad:** ejecución de órdenes (Market/Limit/Stop Loss/Take Profit), verificación de fondos y holdings, cálculo de comisiones, cola de órdenes fuera de horario, flujo completo de comisionistas (propuesta/aprobación/firma).
- **Interfaces expuestas:** `IOrden`.
- **Consume:** `IVerificacionMercado` (Mercado), `IIntegracionAlpaca` (Integración), `IAuditLog` (Trazabilidad).
- **Persiste en:** BD relacional (Orden, Portafolio, Comision).

### 2.3 Servicio de Mercado
- **Responsabilidad:** dashboard, caché de precios, sincronización con APIs externas, verificación de horarios de mercados.
- **Interfaces expuestas:** `IVerificacionMercado`.
- **Consume:** adaptadores externos de Integración (Alpha Vantage, Alpaca para market data), `IAuditLog`.
- **Persiste en:** BD relacional (parámetros de mercado, caché si se persiste; en RAM si es solo memoria).

### 2.4 Servicio de Administración
- **Responsabilidad:** parámetros configurables (% comisión, split, mercados, horarios, feriados), ciclo de vida de cuentas, suscripción premium, preferencias de usuario.
- **Interfaces expuestas:** `IAdministracion`.
- **Consume:** `IIntegracionStripe`, `IAuditLog`.
- **Persiste en:** BD relacional (ParametroSistema, Mercado, Feriado, Suscripcion).

### 2.5 Servicio de Integración
- **Responsabilidad:** adaptadores Alpaca/Stripe/Alpha Vantage; despachador de notificaciones; orquestadores de Registro y Suscripción; manejador de excepciones para APIs externas.
- **Interfaces expuestas:** `IIntegracionAlpaca`, `IIntegracionStripe`, `INotificacion`.
- **Consume:** `IAuditLog`.
- **NO persiste en BD relacional.** Su rol es traducir/orquestar; cuando un orquestador necesita persistir, invoca al servicio dueño del dato vía su interfaz.

### 2.6 Servicio de Trazabilidad
- **Responsabilidad:** registro inmutable de eventos auditables; envío asíncrono al motor de logs centralizado (Splunk o Elasticsearch); monitoreo de salud de servicios (heartbeat).
- **Interfaces expuestas:** `IAuditLog`.
- **Consume:** ninguno (es transversal, lo consumen todos).
- **NO persiste en BD relacional.** Sus eventos van al motor de logs externo (en desarrollo local: console + archivo).

---

## 3. Reglas de comunicación entre servicios

1. Un servicio **nunca** importa clases internas (`*Service`, `*Repository`, entidades) de otro servicio.
2. Un servicio **siempre** consume a otro a través de su interfaz `I...` declarada en el paquete `interfaces/` del servicio dueño.
3. La inyección entre servicios es por constructor en Spring Boot.
4. **Trazabilidad es transversal:** cualquier evento auditable se registra vía `IAuditLog`; nadie reimplementa logs.
5. **Integración aísla todo lo externo:** ningún servicio llama directamente a Alpaca/Stripe/Alpha Vantage; todos pasan por los adaptadores de Integración.

### Quién consume qué (resumen)

```
Autenticación   ──► IAuditLog, INotificacion
Órdenes         ──► IVerificacionMercado, IIntegracionAlpaca, IAuditLog, INotificacion
Mercado         ──► (adaptadores externos vía Integración), IAuditLog
Administración  ──► IIntegracionStripe, IAuditLog, INotificacion
Integración     ──► IAuditLog
Trazabilidad    ──► (ninguno; es consumido por todos)
```

---

## 4. Persistencia y datos

| Servicio | Consume BD relacional | Almacenamiento |
|---|:-:|---|
| Autenticación | ✅ | PostgreSQL |
| Órdenes | ✅ | PostgreSQL |
| Mercado | ✅ | PostgreSQL (parámetros) + caché en memoria (precios) |
| Administración | ✅ | PostgreSQL |
| Integración | ❌ | sin estado propio |
| Trazabilidad | ❌ | motor de logs externo (Splunk / Elasticsearch); en desarrollo: console + archivo |

Los 4 servicios que consumen BD lo hacen vía interfaces internas `ILecturaBD` / `IEscrituraBD` (en práctica, vía Spring Data Repositories — la interfaz queda implícita en el repositorio).

---

## 5. Estructura de carpetas (backend Spring Boot)

```
co.edu.unbosque.accioneselbosque/
├── AccionesElBosqueApplication.java
├── ServletInitializer.java
│
├── autenticacion/
│   ├── controller/        AuthController, RegistroController, RecuperacionController
│   ├── service/           AutenticacionService, RegistroService, MFAService,
│   │                      MonitorIntentosService, RecuperacionPasswordService
│   ├── repository/        UsuarioRepository, CodigoVerificacionRepository,
│   │                      IntentoFallidoRepository, TokenRevocadoRepository
│   ├── model/             Usuario, Rol(enum), EstadoCuenta(enum),
│   │                      CodigoVerificacion, IntentoFallido, TokenRevocado
│   ├── dto/               LoginRequestDTO, LoginResponseDTO, RegistroInversionistaDTO,
│   │                      VerificarCodigoDTO, MFARequestDTO, RecuperarPasswordDTO,
│   │                      ResetPasswordDTO
│   ├── interfaces/        IAutenticacion, IControlAcceso
│   └── security/          JwtUtil, JwtAuthenticationFilter, SecurityConfig
│
├── ordenes/               (Sprint 3+)
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── model/
│   ├── dto/
│   └── interfaces/        IOrden
│
├── mercado/               (Sprint 2)
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── model/
│   ├── dto/
│   └── interfaces/        IVerificacionMercado
│
├── administracion/        (Sprint 4+)
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── model/
│   ├── dto/
│   └── interfaces/        IAdministracion
│
├── integracion/
│   ├── adaptadores/
│   │   ├── alpaca/        AlpacaAdapter, AlpacaConfig (implementa IIntegracionAlpaca)
│   │   ├── stripe/        StripeAdapter, StripeConfig (implementa IIntegracionStripe)
│   │   └── alphavantage/  AlphaVantageAdapter, AlphaVantageConfig
│   ├── orquestadores/
│   │   ├── OrquestadorRegistro.java
│   │   └── OrquestadorSuscripcion.java
│   ├── notificaciones/
│   │   ├── DespachadorNotificaciones.java (implementa INotificacion)
│   │   └── canales/
│   │       ├── EmailSender.java        (Jakarta Mail; basado en MailSender de Malwatcher
│   │       │                            pero SIN secretos hardcodeados)
│   │       ├── SmsSender.java
│   │       └── WhatsAppSender.java
│   ├── excepciones/
│   │   └── ManejadorExcepcionesAPI.java
│   └── interfaces/        IIntegracionAlpaca, IIntegracionStripe, INotificacion
│
├── trazabilidad/
│   ├── service/           AuditLogService (implementa IAuditLog),
│   │                      MonitorSaludService (heartbeat)
│   ├── interfaces/        IAuditLog
│   ├── model/             EventoAuditoria, TipoEvento(enum), NivelSeveridad(enum)
│   └── repository/        cliente Splunk/Elasticsearch (async); en dev: archivo + consola
│
└── shared/
    ├── config/            CorsConfig, OpenApiConfig (Swagger), AsyncConfig
    ├── util/              PasswordEncoder (wrapper BCrypt), AESUtil (heredado)
    └── exceptions/        GlobalExceptionHandler, AccountLockedException,
                          InvalidMfaException, EmailAlreadyExistsException, etc.
```

**Cada servicio replica internamente** `controller/service/repository/model/dto/interfaces`. Es válido en SOA con topología consolidada — el agrupamiento principal sigue siendo por **servicio**, no por capa.

---

## 6. Estructura de carpetas (frontend Angular)

```
src/app/
├── core/
│   ├── services/          AuthService, ApiService, NotificationService
│   ├── interceptors/      JwtInterceptor, ErrorInterceptor
│   ├── guards/            AuthGuard, RoleGuard
│   └── models/            User, Rol, etc.
│
├── features/
│   ├── autenticacion/     login, register, verify-code, mfa,
│   │                      forgot-password, reset-password (componentes standalone)
│   ├── perfil/            (Sprint posterior)
│   ├── mercado/           dashboard, detalle-accion (Sprint 2)
│   ├── ordenes/           crear-orden, historial, portafolio (Sprint 3)
│   ├── comisionista/      (Sprint posterior)
│   └── admin/             (Sprint posterior)
│
├── shared/
│   ├── components/        botones, inputs, modales reutilizables
│   ├── pipes/
│   └── directives/
│
└── environments/
    ├── environment.ts
    └── environment.prod.ts
```

Cada feature está alineada con su servicio del backend.

---

## 7. Topología de despliegue

```
┌────────────────────────┐
│ Dispositivo del Usuario│   Browser (Chrome/Edge/OperaGX)
└──────────┬─────────────┘
           │ HTTPS
┌──────────▼─────────────┐
│   Frontend Server      │   App Angular (HTML/CSS/JS)
└──────────┬─────────────┘
           │ HTTPS REST API
┌──────────▼─────────────────────────────────────┐
│      Servidor de Aplicación (Spring Boot)      │
│  ┌─────────────────────────────────────────┐   │
│  │  6 servicios SOA en mismo runtime:      │   │
│  │  Auth · Órdenes · Mercado ·             │   │
│  │  Admin · Integración · Trazabilidad     │   │
│  └─────────────────────────────────────────┘   │
└──┬──────────────────┬──────────────────┬───────┘
   │ JDBC             │ HTTPS REST       │ HTTP async
   ▼                  ▼                  ▼
┌──────────┐  ┌──────────────────┐  ┌──────────────────┐
│PostgreSQL│  │ APIs Externas:   │  │ Servidor de Logs │
│   (BD)   │  │ Alpaca, Stripe,  │  │ Splunk / ES      │
└──────────┘  │ Alpha Vantage,   │  └──────────────────┘
              │ SMTP/SMS/WhatsApp│
              └──────────────────┘
```

En desarrollo local:
- **Frontend:** `ng serve` en `http://localhost:4200`.
- **Backend:** Spring Boot en `http://localhost:8080`.
- **PostgreSQL:** instalación nativa en `localhost:5432`.
- **APIs externas:** sandbox/test mode.
- **Logs:** consola y archivo `logs/audit.log`.

---

## 8. Decisiones arquitectónicas relevantes

### 8.1 Por qué SOA y no microservicios
Equipo pequeño, plazos académicos, y la mayoría de operaciones financieras requieren <5s de latencia (RNF-02). Microservicios añadirían latencia de red y complejidad de despliegue. SOA consolidada da los beneficios de modularidad sin el overhead operacional.

### 8.2 Por qué tabla `TokenRevocado`
JWT puro es stateless, pero HU-5 exige invalidar el token en logout. La alternativa estándar es una lista de tokens revocados consultada por el filtro JWT. Pequeño costo de performance compensado por seguridad real.

### 8.3 Por qué códigos de verificación en BD y no en RAM
El proyecto Malwatcher previo usaba `ConcurrentHashMap` estático. En SOA consolidada con réplicas detrás de un balanceador, ese enfoque rompe (cada réplica tendría su propio mapa). BD garantiza consistencia entre instancias.

### 8.4 Por qué `Sender` separados por canal
EC-17 exige enviar por múltiples canales (Email, SMS, WhatsApp). Un único `Sender` monolítico mezclaría APIs distintas. El patrón Adapter por canal aísla cada integración y cumple la táctica Tailor Interface.

### 8.5 Por qué auditoría asíncrona
RNF-14 exige registrar trazabilidad de cada orden en ≤2s. Si la escritura al motor de logs fuera síncrona y bloqueara el flujo, una caída de Splunk derribaría todo. Async desacopla y mantiene disponibilidad.

---

## 9. Reglas operativas para Claude Code

- **Antes de crear un archivo nuevo**, identifica a qué servicio pertenece y colócalo bajo el paquete correcto del servicio.
- **Nunca importes una clase de otro servicio que no sea su interfaz `I...`.**
- **Si un endpoint requiere datos de varios servicios**, encadénalos vía interfaces; no metas la lógica en un solo controller.
- **Para cualquier llamada a API externa**, crea o reusa un adaptador en `integracion/adaptadores/`.
- **Para cualquier evento auditable**, llama a `IAuditLog` desde el servicio donde se origina; no dupliques la lógica de logs.
