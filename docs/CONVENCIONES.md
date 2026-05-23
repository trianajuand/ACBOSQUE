# Convenciones de Programación

> Este archivo define las reglas de estilo, naming, y patrones del proyecto. Heredamos lo bueno del proyecto **Malwatcher** previo del equipo y mejoramos explícitamente lo que tenía mal. Consultar antes de escribir cualquier código.

---

## 1. Java / Spring Boot

### 1.1 Naming

- **Nombres de clases, métodos y variables en español:** `Usuario`, `obtenerPorCorreo`, `validarCredenciales`, `enviarCodigoMfa`. Mantener consistencia con el estilo del proyecto Malwatcher.
- **Excepciones:**
  - Anotaciones de Spring/JPA quedan en inglés (`@Column`, `@Entity`, `@Service`, `@RestController`, etc.).
  - Nombres de columnas en BD también en español (`@Column(name = "nombre_usuario")`), snake_case.
- **Endpoints REST:** rutas en kebab-case en inglés, payload en español.
  - ✅ `POST /api/auth/login` con body `{ "correo": "...", "contrasenia": "..." }`
  - ❌ `POST /api/autenticacion/iniciar-sesion`

### 1.2 Inyección de dependencias

- **Por constructor**, no por campo. Más testeable, evita NPEs en tests, marca dependencias inmutables con `final`.
- ✅ Correcto:
  ```java
  @Service
  public class RegistroService {
      private final UsuarioRepository usuarioRepo;
      private final IAuditLog auditLog;

      public RegistroService(UsuarioRepository usuarioRepo, IAuditLog auditLog) {
          this.usuarioRepo = usuarioRepo;
          this.auditLog = auditLog;
      }
  }
  ```
- ❌ Mal (estilo Malwatcher antiguo, **prohibido aquí**):
  ```java
  @Autowired private UsuarioService usuarioServ;  // NO
  ```

### 1.3 Transacciones

- `@Transactional` en services, **nunca en controllers**.
- Usar `@Transactional(readOnly = true)` para consultas puras, mejora performance.

### 1.4 DTOs

- **Siempre** DTOs en entrada y salida de endpoints. Nunca exponer entidades JPA con campos sensibles (contraseña hasheada, tokens, etc.).
- DTOs sufijados con propósito: `LoginRequestDTO`, `LoginResponseDTO`, `UsuarioDTO`, `RegistroInversionistaDTO`.
- Mappers manuales en services, o MapStruct si el equipo lo prefiere (decidir antes de escalar).

### 1.5 CORS

- Configurado **a nivel global** en `shared/config/CorsConfig.java`, leyendo orígenes desde `application.properties`.
- ❌ Prohibido `@CrossOrigin` por controller (el proyecto Malwatcher repetía esto en cada controller; aquí está prohibido).
- Orígenes explícitos en producción, `*` solo en development con anotación clara.

### 1.6 Manejo de errores

- `@ControllerAdvice` global en `shared/exceptions/GlobalExceptionHandler.java`.
- Excepciones de dominio extendiendo `RuntimeException`: `AccountLockedException`, `InvalidMfaException`, `EmailAlreadyExistsException`, `InvalidTokenException`, etc.
- Respuestas HTTP estandarizadas con un `ErrorResponseDTO` que incluya `timestamp`, `status`, `error`, `mensaje`, `path`.
- Códigos HTTP coherentes:
  - `400` validación de entrada
  - `401` no autenticado
  - `403` autenticado pero sin permiso
  - `404` recurso no existe
  - `409` conflicto (correo duplicado)
  - `423` cuenta bloqueada (Lock Computer EC-09)
  - `500` error interno

### 1.7 Logging

- SLF4J con `@Slf4j` de Lombok o `LoggerFactory.getLogger`.
- **Eventos auditables van por `IAuditLog`**, no por SLF4J directo. SLF4J es para logs técnicos de desarrollo (entry/exit de métodos, errores no auditables).
- Nunca loggear contraseñas, tokens, ni datos sensibles.

### 1.8 Validación

- Bean Validation (`jakarta.validation`) en DTOs: `@NotBlank`, `@Email`, `@Size`, `@Pattern`.
- En controller: `@Valid @RequestBody` + manejo de `MethodArgumentNotValidException` en `GlobalExceptionHandler`.

---

## 2. Seguridad (mejoras críticas vs. Malwatcher)

### 2.1 Contraseñas

- **BCrypt obligatorio** vía `shared/util/PasswordEncoder.java` (wrapper de `BCryptPasswordEncoder` de Spring Security).
- ❌ Prohibido contraseñas en texto plano en BD (Malwatcher las guardaba así; aquí está prohibido).
- Hash al persistir, comparación con `matches()` al validar.

### 2.2 JWT

- Firmado con HMAC-SHA256, secret de 256 bits desde `application.properties` (variable `jwt.secret`, en producción desde variable de entorno).
- Expiración 1h por defecto, configurable.
- Claims mínimos: `sub` (id usuario), `rol`, `iat`, `exp`.
- Validación en cada request via `JwtAuthenticationFilter` (extends `OncePerRequestFilter`).
- **Tokens revocados en tabla `token_revocado`**: el filtro JWT consulta antes de aceptar.

### 2.3 Códigos de verificación (registro, MFA, recuperación)

- 6 dígitos numéricos para registro/MFA, 64 chars hex para recuperación de contraseña.
- TTL 10 min para registro/MFA, 30 min para recuperación.
- Un solo uso (campo `usado` boolean en entidad `CodigoVerificacion`).
- **Persistidos en BD**, no en `ConcurrentHashMap` en memoria (prohibido — el proyecto Malwatcher hacía esto y no escala con múltiples instancias).

### 2.4 Bloqueo de cuenta (Lock Computer EC-09)

- 5 intentos fallidos en ventana móvil de 15 min → cuenta bloqueada 15 min.
- Tabla `intento_fallido` con (correo, ip, timestamp, exitoso boolean).
- Al bloquear: `IAuditLog.registrar(CUENTA_BLOQUEADA, ...)` + `INotificacion.notificarBloqueo(usuario)`.

### 2.5 Secretos

- **Nunca hardcodeados.** Todos en `application.properties` (placeholders en repo) y en variables de entorno en producción.
- ❌ Prohibido pegar API keys, contraseñas SMTP, secrets de JWT, etc., en el código fuente (Malwatcher tenía la API key de VirusTotal y la contraseña SMTP literales en el código; aquí está prohibido).

### 2.6 Validación de entrada y sanitización

- Bean Validation en DTOs (ver 1.8).
- Para queries dinámicas (cuando aplique), usar siempre Prepared Statements / parámetros en JPQL/Criteria API. Nunca concatenación de strings.

### 2.7 Auditoría obligatoria

Eventos que SIEMPRE deben pasar por `IAuditLog`:
- Registro de usuario
- Login exitoso
- Login fallido
- Bloqueo de cuenta
- Verificación MFA exitosa/fallida
- Logout
- Cambio de contraseña
- Recuperación de contraseña iniciada/completada
- Ejecución de orden (compra/venta)
- Cancelación de orden
- Cambio de parámetro administrativo (comisión, horario, feriado)
- Acceso denegado a recurso protegido
- Suspensión/reactivación/eliminación de cuenta

---

## 3. Frontend (Angular + TypeScript)

### 3.1 Estructura

- Por **feature** (alineada con módulos del backend): `features/autenticacion/`, `features/mercado/`, etc.
- Componentes **standalone** (Angular 15+ moderno).
- Servicios HTTP en `core/services/`.

### 3.2 Estado y autenticación

- `AuthService` con BehaviorSubject del usuario actual.
- JWT guardado en `localStorage` por simplicidad académica; documentar trade-off con `httpOnly cookie` para producción.
- `JwtInterceptor` adjunta `Authorization: Bearer <token>` a cada request.
- `ErrorInterceptor` maneja 401 globalmente: cierra sesión y redirige a login.

### 3.3 Guards

- `AuthGuard` bloquea rutas no autenticadas.
- `RoleGuard` bloquea rutas según rol (`['INVERSIONISTA', 'COMISIONISTA', 'ADMINISTRADOR']`).

### 3.4 Formularios

- Reactive Forms, no Template-driven.
- Validadores síncronos y asíncronos (ej. `correoUnicoValidator` para registro).
- Mensajes de error claros y en español (`"El correo es obligatorio"`, `"La contraseña debe tener al menos 8 caracteres"`).

### 3.5 Estilo visual

- TailwindCSS o SCSS modular según preferencia del equipo (decidir antes del Sprint 6).
- Componentes UI compartidos en `shared/components/` (botones, inputs, modales, alerts).

### 3.6 Idioma

- Toda la UI en español.
- Considerar `@angular/localize` si se planea i18n a futuro (no en MVP).

---

## 4. PostgreSQL

- Nombres de tablas y columnas en **snake_case** y español.
- Claves primarias `id` (BIGINT, IDENTITY).
- Foreign keys con `_id` sufijo (`usuario_id`).
- Timestamps con TIMESTAMPTZ y nombres `creado_en`, `actualizado_en`.
- Índices en columnas de búsqueda frecuente (correo, sha256, etc.).
- Constraints explícitos en BD (`UNIQUE`, `NOT NULL`, `CHECK`).

---

## 5. Lo que heredamos del proyecto Malwatcher (estilo a imitar)

✅ **Imitar:**
- Naming en español para clases y métodos.
- Estilo de controllers REST con `@RestController`, `@RequestMapping`, `ResponseEntity`.
- Repositories Spring Data extendiendo `CrudRepository<Entidad, Integer>` con métodos `findBy...`.
- Estructura de service con métodos `create`, `getById`, `getAll`, `updateById`, etc.
- Uso de Jakarta Mail con MimeMessage y MimeMultipart para correos HTML.
- Paquete `package-info.java` en cada subpaquete (estilo organizativo de Malwatcher).

## 6. Lo que NO heredamos (malas prácticas explícitamente prohibidas)

❌ **Prohibido:**
- Contraseñas en texto plano en la BD (Malwatcher: campo `contrasenia` sin hash).
- Secretos hardcodeados en el código (Malwatcher: API key de VirusTotal y contraseña Gmail literales).
- `ConcurrentHashMap` estático para almacenar códigos de verificación (Malwatcher: `CodigoVerificacionStorage`).
- `@Autowired` en campo (Malwatcher: `@Autowired private UsuarioService usuarioServ`).
- `@CrossOrigin` repetido en cada controller (Malwatcher lo hace en `UsuarioController` y `VirusTotalController`).
- Mezclar lógica de negocio en controllers (Malwatcher tiene el método `generarCodigoVerificacion` dentro del controller; debería estar en el service).
- Usar `@Transactional` a nivel de clase de controller (Malwatcher hace esto en `UsuarioController`).
- Devolver entidades JPA directamente en endpoints (Malwatcher devuelve `Usuario` con la contraseña en el JSON).
- Acceso directo a `Optional.get()` sin validar `isPresent()` (Malwatcher hace esto en `validateCredentials`).

---

## 7. Estilo de commits y branches

- **Branches:** `feature/sprint1-auth`, `feature/hu1-registro-inversionista`, `fix/bloqueo-tras-5-intentos`.
- **Commits descriptivos en presente:**
  - ✅ `feat(auth): agrega endpoint POST /api/auth/login`
  - ✅ `fix(auth): corrige cálculo de ventana de bloqueo`
  - ❌ `cosas`, `arreglos`, `wip`
- Tags al cierre de cada sprint: `git tag v0.1-sprint1`, `git tag v0.2-sprint2`.

---

## 8. Tests

- JUnit 5 + Mockito para tests unitarios.
- `@SpringBootTest` + `MockMvc` para tests de integración.
- Cobertura mínima objetivo: **80%** en services, **70%** global.
- Naming: `metodoBajoPrueba_condicion_resultadoEsperado`.
  - Ej.: `login_credencialesValidas_retornaJWT`, `registro_correoDuplicado_lanzaEmailAlreadyExistsException`.
- Tests de seguridad obligatorios:
  - `MonitorIntentosService.login_5IntentosFallidos_bloqueaCuenta15Min`
  - `JwtUtil.validarToken_tokenExpirado_lanzaExcepcion`
  - `RecuperacionService.resetPassword_tokenUsado_rechazaOperacion`

---

## 9. Reglas operativas para Claude Code

- Antes de escribir código, **lee la sección relevante de este archivo y de `ARQUITECTURA.md`**.
- Si tienes que decidir entre dos enfoques y ambos cumplen las reglas, **prefiere el más cercano al estilo Malwatcher** (sección 5).
- Si estás por hacer algo de la sección 6 (prohibido), **detente y haz la versión correcta**.
- Si introduces una nueva convención que no está aquí, **agrégala** a este archivo.
