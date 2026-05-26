# tasks.md — SOPORTE Recuperación y Restablecimiento de Contraseña
> Descomposición del plan.md aprobado (SDD Paso 3).
> Rama: `feat/SOPORTE-recuperacion-password`.
Leyenda: ☐ pendiente · ◐ en progreso · ☑ hecho · ✗ cancelada

---

## Lote 1 — Modelo de datos y configuración

- ☐ **T1.1** Verificar que la entidad `CodigoVerificacion` en `autenticacion/model/` tiene los campos: `correo` (String), `codigo` (String), `tipo` (enum/String con valor `RECUPERACION_PASSWORD`), `expiracion` (LocalDateTime), `usado` (boolean).
  - Artefactos: `autenticacion/model/CodigoVerificacion.java`
  - Verificación: `mvn compile -pl backend` sin errores.

- ☐ **T1.2** Verificar que `CodigoVerificacionRepository` tiene métodos: `findByCodigoAndTipoAndUsadoFalse(String codigo, String tipo)` y método para eliminar tokens previos del mismo correo y tipo (ej. `deleteByCorreoAndTipo(String correo, String tipo)` o similar).
  - Artefactos: `autenticacion/repository/CodigoVerificacionRepository.java`
  - Verificación: métodos presentes y compilables.

- ☐ **T1.3** Verificar que `application.properties` contiene `app.seguridad.recuperacion-expiracion-minutos` con valor por defecto (recomendado: 30). Si no existe, agregar.
  - Artefactos: `backend/src/main/resources/application.properties`
  - Verificación: `grep "recuperacion-expiracion-minutos" application.properties` retorna la línea.

- ☐ **T1.4** Resolver y documentar la discrepancia del token (§9 pregunta 1): 6 dígitos numéricos (código actual) vs. 64 chars hex (CONVENCIONES.md). Agregar nota en `docs/CONVENCIONES.md §2.3` con la decisión del equipo.
  - Artefactos: `docs/CONVENCIONES.md`
  - Verificación: nota presente en el archivo.

**← HITO 1 — Modelo, repositorio y configuración verificados (validación humana)**

---

## Lote 2 — Implementación del service

- ☐ **T2.1** Implementar / verificar `RecuperacionPasswordService.solicitarRecuperacion(RecuperarPasswordDTO dto)` con `@Transactional`:
  - Busca usuario por `dto.correo`.
  - Si no existe: registra `IAuditLog.registrar(LOGIN_FALLIDO, dto.correo, "Recuperación solicitada para correo inexistente")` y retorna respuesta genérica (no revela inexistencia).
  - Si existe: elimina tokens previos de tipo `RECUPERACION_PASSWORD` para ese correo; genera token de 6 dígitos; crea y persiste `CodigoVerificacion` con `expiracion = now() + TTL`; llama `INotificacion.enviarTokenRecuperacion(...)`; registra `IAuditLog.registrar(RECUPERACION_INICIADA, correo, ...)`.
  - Artefactos: `autenticacion/service/RecuperacionPasswordService.java`
  - Verificación: `mvn compile -pl backend` sin errores.

- ☐ **T2.2** Implementar / verificar `RecuperacionPasswordService.resetPassword(ResetPasswordDTO dto)` con `@Transactional`:
  - Busca `CodigoVerificacion` por `codigo` y tipo `RECUPERACION_PASSWORD` donde `usado = false`.
  - Si no existe: lanza `InvalidTokenException` → 400.
  - Si `expiracion < now()`: lanza `InvalidTokenException` → 400 (token expirado).
  - Marca `usado = true`.
  - Hashea `dto.nuevaContrasenia` con BCrypt y actualiza `usuario.contrasenia`.
  - Registra `IAuditLog.registrar(CONTRASENIA_CAMBIADA, correo, "Recuperación completada")`.
  - Artefactos: `autenticacion/service/RecuperacionPasswordService.java`
  - Verificación: `mvn compile -pl backend` sin errores.

- ☐ **T2.3** Verificar que los endpoints `POST /api/auth/forgot-password` y `POST /api/auth/reset-password` en `RecuperacionController` son públicos (no requieren JWT) y están incluidos en la lista `permitAll()` de `SecurityConfig`.
  - Artefactos: `autenticacion/controller/RecuperacionController.java`, `autenticacion/security/SecurityConfig.java`
  - Verificación: `curl -X POST http://localhost:8080/api/auth/forgot-password -H "Content-Type: application/json" -d '{"correo":"test@test.com"}'` sin JWT retorna 200 (no 401).

**← HITO 2 — Service implementado (validación humana)**

---

## Lote 3 — Tests unitarios

- ☐ **T3.1** Escribir tests unitarios `RecuperacionPasswordServiceTest`:
  - `solicitarRecuperacion_correoExistente_generaTokenYEnviaCorreo`.
  - `solicitarRecuperacion_correoInexistente_retornaRespuestaGenericaSinRevelar`.
  - `resetPassword_tokenCorrecto_actualizaContraseniaBcrypt`: verificar que la nueva contraseña en BD pasa `BCryptPasswordEncoder.matches(nueva, hash)`.
  - `resetPassword_tokenExpirado_lanzaInvalidTokenException` (test obligatorio CONVENCIONES.md §8).
  - `resetPassword_tokenUsado_rechazaOperacion` (test obligatorio CONVENCIONES.md §8).
  - `resetPassword_tokenInexistente_lanzaInvalidTokenException`.
  - Artefactos: `backend/src/test/java/.../autenticacion/service/RecuperacionPasswordServiceTest.java`
  - Verificación: `mvn test -pl backend -Dtest=RecuperacionPasswordServiceTest` — todos en verde.

**← HITO 3 — Tests unitarios en verde (validación humana)**

---

## Lote 4 — Tests de integración y validación frontend

- ☐ **T4.1** Escribir test de integración `RecuperacionPasswordIntegrationTest` con `MockMvc`:
  - `POST /api/auth/forgot-password` con correo existente → 200 (token generado en BD).
  - `POST /api/auth/forgot-password` con correo inexistente → 200 (respuesta genérica idéntica).
  - `POST /api/auth/reset-password` con token correcto y contraseña válida → 200.
  - `POST /api/auth/reset-password` con token expirado → 400.
  - `POST /api/auth/reset-password` con contraseña < 8 chars → 400.
  - Artefactos: `backend/src/test/java/.../autenticacion/controller/RecuperacionPasswordIntegrationTest.java`
  - Verificación: `mvn test -pl backend -Dtest=RecuperacionPasswordIntegrationTest` — todos en verde.

- ☐ **T4.2** Verificar flujo E2E en frontend Angular:
  - `/recuperar` → ingresa correo existente → navega a `/reset-password`.
  - `/reset-password` → ingresa token recibido por correo y nueva contraseña → 200.
  - Login con nueva contraseña → exitoso.
  - Artefactos: `frontend/src/app/auth/recuperar.component.ts`, `reset-password.component.ts`
  - Verificación: prueba manual documentada con entorno local (SMTP configurado o log del token en consola).

**← HITO 4 — Tests de integración y flujo E2E verificados (validación humana)**

---

## Lote 5 — Cierre

- ☐ **T5.1** Verificación DoD end-to-end: revisar todos los ítems del §10 del plan.md.
  - Artefactos: `docs/SOPORTE-recuperacion-password/plan.md`
  - Verificación: todos los checks del DoD marcados.

- ☐ **T5.2** Actualizar `docs/PROGRESO.md` con nota de recuperación de contraseña validada.
  - Artefactos: `docs/PROGRESO.md`
  - Verificación: `git diff docs/PROGRESO.md` muestra el cambio.

- ☐ **T5.3** PR abierto con checklist DoD, pipeline verde.
  - Artefactos: Pull Request en repositorio.
  - Verificación: CI/build sin errores, al menos 1 revisor asignado.

**← HITO FINAL — Entrega SOPORTE-recuperacion-password**
