# tasks.md — EC-09 Bloqueo por Intentos Fallidos de Login
> Descomposición del plan.md aprobado (SDD Paso 3).
> Rama: `feat/EC-09-bloqueo-intentos-fallidos`.
Leyenda: ☐ pendiente · ◐ en progreso · ☑ hecho · ✗ cancelada

---

## Lote 1 — Modelo de datos y configuración

- ☐ **T1.1** Verificar / ajustar la entidad `IntentoFallido` en `autenticacion/model/` para que tenga los campos: `correo` (String, indexed), `contador` (int), `bloqueadoHasta` (LocalDateTime, nullable), `ultimoIntento` (LocalDateTime). Resolver discrepancia con esquema de ARQUITECTURA.md (ver §9 del plan).
  - Artefactos: `autenticacion/model/IntentoFallido.java`
  - Verificación: `mvn compile -pl backend` sin errores; tabla `intento_fallido` generada por Hibernate con las columnas correctas.

- ☐ **T1.2** Verificar / ajustar `IntentoFallidoRepository` en `autenticacion/repository/` para que tenga `findByCorreo(String correo)`.
  - Artefactos: `autenticacion/repository/IntentoFallidoRepository.java`
  - Verificación: método present y compilable.

- ☐ **T1.3** Verificar que `application.properties` declara `app.seguridad.max-intentos` y `app.seguridad.bloqueo-minutos` con valores por defecto (5 y 15 respectivamente). Si no existen, agregarlos.
  - Artefactos: `backend/src/main/resources/application.properties`
  - Verificación: `grep "app.seguridad.max-intentos" application.properties` retorna la línea con valor.

**← HITO 1 — Modelo de datos y configuración listos (validación humana)**

---

## Lote 2 — Implementación de MonitorIntentosService

- ☐ **T2.1** Implementar / verificar `MonitorIntentosService` en `autenticacion/service/`:
  - `verificarBloqueoPorCorreo(String correo)`: consulta `IntentoFallidoRepository`; si `bloqueadoHasta > now()`, lanza `AccountLockedException`.
  - `registrarIntentoFallido(String correo)`: incrementa `contador`; si `contador >= max-intentos`, setea `bloqueadoHasta = now() + bloqueo-minutos`.
  - `reiniciarContador(String correo)`: limpia `contador` y `bloqueadoHasta`.
  - Inyección por constructor con `@Value` para los parámetros configurables.
  - Anotado con `@Transactional` en los métodos que escriben.
  - NO usa `ConcurrentHashMap` ni estado estático.
  - Artefactos: `autenticacion/service/MonitorIntentosService.java`
  - Verificación: `mvn compile -pl backend` sin errores.

- ☐ **T2.2** Verificar / ajustar `AutenticacionService.login()` para que:
  1. Llame `MonitorIntentosService.verificarBloqueoPorCorreo(correo)` al inicio (antes de buscar usuario).
  2. Si credenciales inválidas: llame `MonitorIntentosService.registrarIntentoFallido(correo)`, registre `IAuditLog.registrar(LOGIN_FALLIDO, ...)`.
  3. Si se alcanza el límite: llame `IAuditLog.registrar(CUENTA_BLOQUEADA, ...)` y `INotificacion.notificarBloqueo(...)` (solo si usuario existe).
  4. Si credenciales válidas: llame `MonitorIntentosService.reiniciarContador(correo)`, registre `IAuditLog.registrar(LOGIN_EXITOSO, ...)`.
  - Artefactos: `autenticacion/service/AutenticacionService.java`
  - Verificación: `mvn compile -pl backend` sin errores.

- ☐ **T2.3** Verificar que `GlobalExceptionHandler` mapea `AccountLockedException` a HTTP 423 con `ErrorResponseDTO` descriptivo.
  - Artefactos: `shared/exceptions/GlobalExceptionHandler.java`, `shared/exceptions/AccountLockedException.java`
  - Verificación: handler retorna `{ "status": 423, "error": "Cuenta bloqueada" }`.

**← HITO 2 — MonitorIntentosService implementado y conectado al flujo de login (validación humana)**

---

## Lote 3 — Tests unitarios y de integración

- ☐ **T3.1** Escribir / verificar test unitario `MonitorIntentosServiceTest`:
  - `login_5IntentosFallidos_bloqueaCuenta15Min` (test obligatorio de CONVENCIONES.md §8).
  - `verificarBloqueo_cuentaBloqueada_lanzaAccountLockedException`.
  - `registrarIntento_exitoso_reiniciaContador`.
  - `registrarIntento_correoInexistente_creaRegistroEnBD`.
  - Artefactos: `backend/src/test/java/.../autenticacion/service/MonitorIntentosServiceTest.java`
  - Verificación: `mvn test -pl backend -Dtest=MonitorIntentosServiceTest` — todos en verde.

- ☐ **T3.2** Escribir test de integración `LoginBloqueoCuentaIntegrationTest` con `MockMvc`:
  - Enviar 5 peticiones `POST /api/auth/login` con contraseña errónea → la 5.ª retorna 423.
  - Enviar 6.ª petición (cualquier contraseña) → 423 (aún bloqueada).
  - Simular `bloqueadoHasta = now() - 1min` y enviar login correcto → 200 (desbloqueo automático).
  - Artefactos: `backend/src/test/java/.../autenticacion/controller/LoginBloqueoCuentaIntegrationTest.java`
  - Verificación: `mvn test -pl backend -Dtest=LoginBloqueoCuentaIntegrationTest` — todos en verde.

**← HITO 3 — Tests en verde (validación humana)**

---

## Lote 4 — Frontend y cierre

- ☐ **T4.1** Verificar que `LoginComponent` en Angular muestra un mensaje diferenciado cuando recibe HTTP 423 (ej. "Tu cuenta está bloqueada temporalmente. Intenta de nuevo en 15 minutos.") usando `ToastService`.
  - Artefactos: `frontend/src/app/auth/login.component.ts`
  - Verificación: `ng serve` + simular respuesta 423 desde el backend o mockeando `ApiService`.

- ☐ **T4.2** Verificación DoD end-to-end: revisar todos los ítems del §10 del plan.md.
  - Artefactos: `docs/EC-09-bloqueo-intentos-fallidos/plan.md`
  - Verificación: todos los checks del DoD marcados.

- ☐ **T4.3** Actualizar `docs/PROGRESO.md` con nota sobre EC-09 implementado.
  - Artefactos: `docs/PROGRESO.md`
  - Verificación: `git diff docs/PROGRESO.md` muestra el cambio.

- ☐ **T4.4** PR abierto con checklist DoD, pipeline verde.
  - Artefactos: Pull Request en repositorio.
  - Verificación: CI/build sin errores, al menos 1 revisor asignado.

**← HITO FINAL — Entrega EC-09**
