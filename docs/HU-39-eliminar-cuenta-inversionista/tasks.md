# tasks.md — HU-39 Eliminar Cuenta de Inversionista (Baja Lógica)
> Descomposición del plan.md aprobado (SDD Paso 3).
> Rama: `feat/HU-39-eliminar-cuenta-inversionista`.
Leyenda: ☐ pendiente · ◐ en progreso · ☑ hecho · ✗ cancelada

---

## Lote 1 — Verificación de contrato y manejo de errores

- ☐ **T1.1** Verificar que `IGestionCuentas` en `autenticacion/interfaces/` declara `eliminarUsuario(Long usuarioId)` y que su implementación setea `usuario.estado_cuenta = INACTIVA` y `actualizado_en = now()` dentro de `@Transactional`.
  - Artefactos: `autenticacion/interfaces/IGestionCuentas.java`, servicio implementador.
  - Verificación: `mvn compile -pl backend` sin errores. Inspección visual del método.

- ☐ **T1.2** Verificar que `GlobalExceptionHandler` en `shared/exceptions/` mapea el caso de "usuario ya eliminado" a HTTP 409. Si no existe una excepción de dominio específica, decidir si se usa `IllegalStateException` mapeada a 409 o se crea `CuentaYaEliminadaException`.
  - Artefactos: `shared/exceptions/GlobalExceptionHandler.java`
  - Verificación: el handler retorna 409 con `ErrorResponseDTO` correctamente formado.

- ☐ **T1.3** Resolver pregunta abierta §9 #1: documentar en comentario en `AdministracionService` la política decidida respecto a órdenes activas al momento de la eliminación (bloquear o cancelar automáticamente).
  - Artefactos: comentario en `AdministracionService.java` o nota en SPEC.md.
  - Verificación: comentario presente y aprobado por el equipo.

**← HITO 1 — Contratos y política de eliminación definidos (validación humana)**

---

## Lote 2 — Lógica del service y auditoría

- ☐ **T2.1** Verificar / completar `AdministracionService.eliminarUsuario(Long usuarioId, String adminCorreo)`:
  - Consulta usuario; si no existe lanza `UsuarioNoEncontradoException` → 404.
  - Si `estado_cuenta == ELIMINADA`, lanza excepción 409.
  - Llama `IGestionCuentas.eliminarUsuario(usuarioId)`.
  - (Opcional según T1.3) Desactiva `AsignacionComisionista` activa.
  - Llama `IAuditLog.registrar(TipoEvento.USUARIO_ADMIN_GESTIONADO, correoUsuario, "Baja lógica por {adminCorreo}")`.
  - Anotado con `@Transactional`.
  - Artefactos: `administracion/service/AdministracionService.java`
  - Verificación: `mvn compile -pl backend` sin errores.

- ☐ **T2.2** Escribir test unitario `AdministracionServiceEliminarTest`:
  - `eliminarUsuario_exitoso_invocaGestionCuentasYAuditLog`.
  - `eliminarUsuario_yaEliminado_lanzaExcepcion409`.
  - `eliminarUsuario_noEncontrado_lanzaUsuarioNoEncontradoException`.
  - Artefactos: `backend/src/test/java/.../administracion/service/AdministracionServiceEliminarTest.java`
  - Verificación: `mvn test -pl backend -Dtest=AdministracionServiceEliminarTest` — todos en verde.

**← HITO 2 — Lógica del service con tests unitarios en verde (validación humana)**

---

## Lote 3 — Controller, login bloqueado y tests de integración

- ☐ **T3.1** Verificar / completar `AdminController`:
  - Método `DELETE /api/admin/usuarios/{usuarioId}` con autorización `ADMINISTRADOR`.
  - Extrae `adminCorreo` del JWT.
  - Delega a `AdministracionService.eliminarUsuario(...)`.
  - Retorna `ResponseEntity<RespuestaDTO>` 200 OK.
  - Artefactos: `administracion/controller/AdminController.java`
  - Verificación: `mvn compile -pl backend` sin errores.

- ☐ **T3.2** Verificar que `AutenticacionService.login()` rechaza con 403 si `usuario.estado_cuenta == ELIMINADA`.
  - Artefactos: `autenticacion/service/AutenticacionService.java`
  - Verificación: test unitario `login_usuarioEliminado_retorna403` en verde.

- ☐ **T3.3** Escribir test de integración `AdminControllerEliminarIntegrationTest` con `MockMvc`:
  - JWT admin + usuario activo → 200; registro aún existe en BD con `estado_cuenta = INACTIVA`.
  - JWT admin + usuario ya eliminado → 409.
  - JWT admin + id inexistente → 404.
  - Sin JWT → 401; rol INVERSIONISTA → 403.
  - Artefactos: `backend/src/test/java/.../administracion/controller/AdminControllerEliminarIntegrationTest.java`
  - Verificación: `mvn test -pl backend -Dtest=AdminControllerEliminarIntegrationTest` — todos en verde.

**← HITO 3 — Endpoint con tests de integración en verde (validación humana)**

---

## Lote 4 — Validación frontend y cierre

- ☐ **T4.1** Verificar que `AdminDashboardComponent` muestra modal de confirmación antes de llamar `DELETE /api/admin/usuarios/{id}` y que actualiza la lista de usuarios tras la respuesta exitosa.
  - Artefactos: `frontend/src/app/admin/admin-dashboard.component.ts`
  - Verificación: `ng serve` + prueba manual: clic en "Eliminar" muestra modal; confirmar ejecuta la petición; usuario desaparece de la lista o aparece con estado ELIMINADA.

- ☐ **T4.2** Verificar E2E el flujo completo:
  - Admin elimina usuario → usuario intenta login → recibe 403 con mensaje adecuado.
  - Historial de órdenes del usuario eliminado sigue consultable desde panel admin.
  - Artefactos: entorno local con backend + frontend corriendo.
  - Verificación: prueba manual documentada.

- ☐ **T4.3** Verificación DoD end-to-end: revisar todos los ítems del §10 del plan.md.
  - Artefactos: `docs/HU-39-eliminar-cuenta-inversionista/plan.md`
  - Verificación: todos los checks del DoD marcados.

- ☐ **T4.4** Actualizar `docs/PROGRESO.md` marcando HU-39 como ✅.
  - Artefactos: `docs/PROGRESO.md`
  - Verificación: `git diff docs/PROGRESO.md` muestra el cambio.

- ☐ **T4.5** PR abierto con checklist DoD, pipeline verde.
  - Artefactos: Pull Request en repositorio.
  - Verificación: CI/build sin errores, al menos 1 revisor asignado.

**← HITO FINAL — Entrega HU-39**
