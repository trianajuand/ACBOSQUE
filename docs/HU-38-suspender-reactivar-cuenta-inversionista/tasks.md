# tasks.md — HU-38 Suspender / Reactivar Cuenta de Inversionista
> Descomposición del plan.md aprobado (SDD Paso 3).
> Rama: `feat/HU-38-suspender-reactivar-cuenta`.
Leyenda: ☐ pendiente · ◐ en progreso · ☑ hecho · ✗ cancelada

---

## Lote 1 — Verificación y ajuste de modelos y contratos

- ☑ **T1.1** ~~Verificar que el enum `EstadoCuenta` incluye `SUSPENDIDA`, `RESTRINGIDA`, `ELIMINADA`.~~ **Auditado 2026-05-25:** El enum real tiene `ACTIVA`, `INACTIVA`, `BLOQUEADA`, `OPERACIONES_RESTRINGIDAS`, `PENDIENTE_VERIFICACION`. No existen `SUSPENDIDA`, `RESTRINGIDA` ni `ELIMINADA`. El SPEC y el código usan `INACTIVA` para la baja/suspensión y `OPERACIONES_RESTRINGIDAS` para restricción.
  - Artefactos: `backend/src/main/java/co/edu/unbosque/accioneselbosque/autenticacion/model/EstadoCuenta.java`

- ☐ **T1.2** Verificar que `CambiarEstadoCuentaDTO` en `administracion/dto/` tiene campos `nuevoEstado` (enum `EstadoCuenta`) y `motivo` (String, nullable), con `@NotNull` en `nuevoEstado` y validación Bean Validation.
  - Artefactos: `backend/src/main/java/co/edu/unbosque/accioneselbosque/administracion/dto/CambiarEstadoCuentaDTO.java`
  - Verificación: compilar con `mvn compile -pl backend` sin errores.

- ☐ **T1.3** Verificar que `IGestionCuentas` en `autenticacion/interfaces/` declara el método `cambiarEstadoUsuario(Long usuarioId, EstadoCuenta nuevoEstado)` y que la implementación en `AutenticacionService` (o el service dueño de `Usuario`) actualiza `usuario.estado_cuenta` y `actualizado_en` con `@Transactional`.
  - Artefactos: `autenticacion/interfaces/IGestionCuentas.java`, servicio implementador.
  - Verificación: revisar firma del método y anotación `@Transactional` en la implementación.

**← HITO 1 — Contratos y modelos verificados (validación humana)**

---

## Lote 2 — Lógica del service y auditoría

- ☐ **T2.1** Verificar / completar `AdministracionService.cambiarEstadoUsuario(Long usuarioId, CambiarEstadoCuentaDTO dto, String adminCorreo)`:
  - Llama `IGestionCuentas.cambiarEstadoUsuario(usuarioId, dto.getNuevoEstado())`.
  - Si el usuario no existe, lanza `UsuarioNoEncontradoException`.
  - Llama `IAuditLog.registrar(TipoEvento.USUARIO_ADMIN_GESTIONADO, correoUsuario, "Estado cambiado a {nuevoEstado} por {adminCorreo}")`.
  - Anotado con `@Transactional`.
  - Artefactos: `administracion/service/AdministracionService.java`
  - Verificación: `mvn compile -pl backend` sin errores.

- ☐ **T2.2** Escribir test unitario `AdministracionServiceTest`:
  - `cambiarEstado_suspender_usuarioActivo_retorna200`: verifica invocación de `IGestionCuentas` e `IAuditLog`.
  - `cambiarEstado_usuarioInexistente_lanzaUsuarioNoEncontradoException`.
  - `cambiarEstado_rolInversionista_retorna403`: validar restricción de rol.
  - Artefactos: `backend/src/test/java/.../administracion/service/AdministracionServiceTest.java`
  - Verificación: `mvn test -pl backend -Dtest=AdministracionServiceTest` — todos en verde.

**← HITO 2 — Lógica del service con tests unitarios en verde (validación humana)**

---

## Lote 3 — Controller y tests de integración

- ☐ **T3.1** Verificar / completar `AdminController`:
  - Método `PUT /api/admin/usuarios/{usuarioId}/estado` con `@PreAuthorize("hasRole('ADMINISTRADOR')")` o equivalente en `SecurityConfig`.
  - Recibe `@Valid @RequestBody CambiarEstadoCuentaDTO`.
  - Extrae `adminCorreo` del JWT via `IControlAcceso`.
  - Delega a `AdministracionService.cambiarEstadoUsuario(...)`.
  - Retorna `ResponseEntity<RespuestaDTO>` 200 OK.
  - Artefactos: `administracion/controller/AdminController.java`
  - Verificación: `mvn compile -pl backend` sin errores.

- ☐ **T3.2** Escribir test de integración `AdminControllerIntegrationTest` con `MockMvc`:
  - Con JWT admin válido + usuario existente → 200.
  - Sin JWT → 401.
  - JWT con rol INVERSIONISTA → 403.
  - `usuarioId` inexistente → 404.
  - `nuevoEstado` inválido → 400.
  - Artefactos: `backend/src/test/java/.../administracion/controller/AdminControllerIntegrationTest.java`
  - Verificación: `mvn test -pl backend -Dtest=AdminControllerIntegrationTest` — todos en verde.

**← HITO 3 — Endpoint con tests de integración en verde (validación humana)**

---

## Lote 4 — Validación frontend y cierre

- ☐ **T4.1** Verificar que `AdminDashboardComponent` en Angular llama `PUT /api/admin/usuarios/{id}/estado` mediante `ApiService` al hacer clic en botón de suspender/reactivar, y muestra el resultado mediante `ToastService`.
  - Artefactos: `frontend/src/app/admin/admin-dashboard.component.ts`, `core/api.service.ts`
  - Verificación: `ng serve` + prueba manual en `http://localhost:4200/admin` con usuario de prueba.

- ☐ **T4.2** Verificar E2E el flujo completo:
  - Admin suspende usuario → usuario intenta login → recibe mensaje de cuenta inactiva.
  - Admin reactiva usuario → usuario puede iniciar sesión nuevamente.
  - Artefactos: entorno local con backend + frontend corriendo.
  - Verificación: prueba manual documentada con capturas.

- ☐ **T4.3** Verificación DoD end-to-end: revisar todos los ítems del §10 del plan.md y marcar los completados.
  - Artefactos: `docs/HU-38-suspender-reactivar-cuenta-inversionista/plan.md`
  - Verificación: todos los checks del DoD marcados.

- ☐ **T4.4** Actualizar `docs/PROGRESO.md` marcando HU-38 como ✅ con nota de fecha y validaciones completadas.
  - Artefactos: `docs/PROGRESO.md`
  - Verificación: `git diff docs/PROGRESO.md` muestra el cambio.

- ☐ **T4.5** PR abierto con checklist DoD, pipeline verde.
  - Artefactos: Pull Request en repositorio.
  - Verificación: CI/build sin errores, al menos 1 revisor asignado.

**← HITO FINAL — Entrega HU-38**
