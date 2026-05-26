# plan.md — HU-39 Eliminar Cuenta de Inversionista (Baja Lógica)
> Derivado de `docs/HU-39-eliminar-cuenta-inversionista/SPEC.md`.
> Estado: PENDIENTE DE APROBACIÓN HUMANA.

---

## 1. Qué construye esta historia

Permite al **Administrador autenticado** realizar la **baja lógica** de un inversionista o comisionista mediante `DELETE /api/admin/usuarios/{usuarioId}`. La baja lógica consiste en cambiar `usuario.estado_cuenta = 'ELIMINADA'`; el registro no se borra físicamente de la BD, preservando el historial de operaciones para auditoría. Un usuario con estado `ELIMINADA` no puede iniciar sesión. La operación es auditada vía `IAuditLog`.

El SPEC confirma explícitamente que es **soft delete** (baja lógica), no hard delete.

---

## 2. Decisiones técnicas

| # | Decisión | Justificación |
|---|---|---|
| 1 | Soft delete (estado `ELIMINADA`) en lugar de borrado físico | El SPEC lo indica explícitamente. Preserva historial de órdenes y cumple trazabilidad (EC-12, CONVENCIONES.md §2.7). |
| 2 | El endpoint HTTP es `DELETE` aunque el efecto sea una actualización | Semántica REST: `DELETE /usuarios/{id}` representa la eliminación del recurso desde el punto de vista del API, aunque internamente sea un update. |
| 3 | Error 409 si el usuario ya tiene `estado_cuenta = INACTIVA` | Definido en el SPEC. Evita doble eliminación silenciosa. |
| 4 | La eliminación se delega a `IGestionCuentas.eliminarUsuario(usuarioId)` desde `administracion` | Respeta la frontera de módulos: `administracion` no importa repositorios de `autenticacion` (ARQUITECTURA.md §5). |
| 5 | Se registra evento `USUARIO_ADMIN_GESTIONADO` vía `IAuditLog` | Eliminación de cuenta es evento auditable obligatorio (CONVENCIONES.md §2.7). |
| 6 | El login de usuarios con `ELIMINADA` retorna 403 Forbidden | SPEC define este comportamiento; coherente con la distinción entre 401 (no autenticado) y 403 (autenticado pero sin acceso). |

---

## 3. Cambios de dependencias

Ningún cambio en `pom.xml` ni `package.json`. Se reutilizan:
- `AdminController`, `AdministracionService`, `IGestionCuentas`, `IAuditLog`, `EstadoCuenta`, `UsuarioNoEncontradoException`.

---

## 4. Deuda técnica o hallazgos previos

| Hallazgo | Acción |
|---|---|
| El SPEC menciona como condición opcional "usuario no tiene órdenes activas (EN_COLA, PENDIENTE, ENVIADA)" | No está implementado como restricción obligatoria. Exponer en §9: ¿bloquear la eliminación si hay órdenes activas, o cancelarlas automáticamente? |
| El SPEC menciona "asignación comisionista: desactivada si aplica" pero no detalla el mecanismo | Si el usuario eliminado tiene `AsignacionComisionista` activa, debe desactivarse. Requiere llamar a `IGestionCuentas` o `IAsignacionComisionista`. |
| La validación en entorno integrado aún no se completó | Ejecutar pruebas E2E antes de marcar DoD completo. |

---

## 5. Arquitectura de la solución

### 5a. Mapeo de componentes (backend)

| Capa | Componente | Módulo | Responsabilidad |
|---|---|---|---|
| Controller | `AdminController` | `administracion` | Recibe `DELETE /api/admin/usuarios/{usuarioId}`, valida JWT/rol, delega al service. |
| Service | `AdministracionService` | `administracion` | Verifica existencia, verifica que no esté ya `ELIMINADA`, llama `IGestionCuentas.eliminarUsuario()`, llama `IAuditLog`. |
| Interface | `IGestionCuentas` | `autenticacion` | `eliminarUsuario(Long usuarioId)` → cambia `estado_cuenta = INACTIVA`. |
| Interface | `IAuditLog` | `trazabilidad` | `registrar(TipoEvento.USUARIO_ADMIN_GESTIONADO, correoUsuario, detalle)`. |
| Exception | `UsuarioNoEncontradoException` | `shared/exceptions` | Lanzada si `usuarioId` no existe → 404. |
| Exception | (nueva o reutilizada) | `shared/exceptions` | Para 409 Conflict cuando ya está eliminado. Se puede usar `IllegalStateException` mapeada a 409 en `GlobalExceptionHandler`, o una excepción de dominio específica. |

### 5b. Mapeo de componentes (frontend)

| Componente | Archivo | Responsabilidad |
|---|---|---|
| `AdminDashboardComponent` | `admin/admin-dashboard.component.ts` | Muestra botón "Eliminar" con confirmación modal. |
| `ApiService` | `core/api.service.ts` | Llama `DELETE /api/admin/usuarios/{id}` con JWT. |
| `ToastService` | `core/toast.service.ts` | Muestra confirmación de baja lógica o mensaje de error. |

### 5c. Modelo de datos

Tabla afectada: `usuario` (módulo `autenticacion`).

```
usuario
  id               BIGINT PK
  estado_cuenta    VARCHAR  -- cambia a 'ELIMINADA'
  actualizado_en   TIMESTAMPTZ  -- se actualiza al momento de la baja

NO se ejecuta DELETE FROM usuario.
El historial en tablas: orden, holding, cuenta_fondos, comision, evento_auditoria
queda intacto y referenciable.
```

Tabla secundaria afectada: `asignacion_comisionista`.
```
asignacion_comisionista
  activa   BOOLEAN  -- debe ponerse en false si el usuario eliminado tiene asignación activa
```

### 5d. Contratos de API

```
DELETE /api/admin/usuarios/{usuarioId}
Authorization: Bearer <JWT_ADMIN>

Responses:
200 OK   → { "mensaje": "Cuenta eliminada exitosamente" }
401/403  → { "error": "No autorizado" }
404      → { "error": "Usuario no encontrado" }
409      → { "error": "La cuenta ya fue eliminada" }
```

---

## 6. Grafo de dependencias entre tareas

```
T1.1 (verificar IGestionCuentas.eliminarUsuario)
    └─► T1.2 (verificar manejo 409 en GlobalExceptionHandler)
            └─► T2.1 (implementar/verificar AdministracionService.eliminarUsuario)
                    └─► T2.2 (test unitario service)
                            └─► T3.1 (verificar AdminController DELETE)
                                    └─► T3.2 (test integración endpoint)
                                            └─► T4.1 (validación frontend)
                                                    └─► T4.2 (DoD + PROGRESO.md)
```

---

## 7. Estrategia de tests

- **Unitario `AdministracionService`:** mockear `IGestionCuentas` e `IAuditLog`; casos: eliminación exitosa, ya eliminado (409), no encontrado (404).
- **Unitario `AutenticacionService`:** verificar que login retorna 403 si `estado_cuenta = INACTIVA`.
- **Integración `MockMvc`:** `DELETE /api/admin/usuarios/{id}` con JWT admin → 200; sin JWT → 401; rol INVERSIONISTA → 403; id inexistente → 404; usuario ya eliminado → 409.
- **Naming:** `eliminarUsuario_exitoso_retorna200`, `eliminarUsuario_yaEliminado_retorna409`, `eliminarUsuario_noEncontrado_retorna404`, `login_usuarioEliminado_retorna403`.

---

## 8. Trazabilidad criterios de aceptación → artefacto

| Criterio (SPEC) | Test o mecanismo |
|---|---|
| `DELETE /api/admin/.../` cambia `estado_cuenta = INACTIVA` (baja lógica) | Test integración MockMvc + verificación en BD. |
| Registro permanece en tabla `usuario` | Test de integración consulta `SELECT` post-DELETE y verifica registro existente. |
| Usuario no encontrado → 404 | `eliminarUsuario_noEncontrado_retorna404` |
| Usuario ya eliminado → 409 | `eliminarUsuario_yaEliminado_retorna409` |
| Sin JWT o rol incorrecto → 401/403 | Tests de seguridad en `AdminControllerIntegrationTest`. |
| Usuario con estado ELIMINADA no puede iniciar sesión | `login_usuarioEliminado_retorna403` |
| Evento `USUARIO_ADMIN_GESTIONADO` en auditoría | Verificar invocación de `IAuditLog` en test unitario del service. |

---

## 9. Preguntas abiertas

| # | Pregunta | Propuesta |
|---|---|---|
| 1 | ¿Qué sucede con las órdenes activas (EN_COLA, PENDIENTE, ENVIADA) de un usuario que se elimina lógicamente? | Propuesta: bloquear la eliminación si hay órdenes activas (retornar 409 con mensaje específico), o cancelarlas automáticamente antes de eliminar. Requiere decisión de negocio. |
| 2 | ¿Se debe desactivar automáticamente la `AsignacionComisionista` activa al eliminar el usuario? | El SPEC lo menciona como consecuencia pero no detalla el mecanismo. Propuesta: llamar `IAsignacionComisionista` para desactivar la asignación como parte del flujo de `AdministracionService.eliminarUsuario`. |
| 3 | ¿El administrador debe recibir confirmación adicional (doble clic / modal) antes de que el frontend envíe la petición? | Recomendado para una operación destructiva. Propuesta: modal de confirmación en `AdminDashboardComponent`. |
| 4 | ¿El SPEC cubre solo inversionistas y comisionistas, o también administradores? | El SPEC dice "inversionista o comisionista". Aclarar si un administrador puede eliminar lógicamente a otro administrador. |

---

## 10. Definition of Done

- [ ] `DELETE /api/admin/usuarios/{id}` cambia `estado_cuenta = 'ELIMINADA'` sin borrar el registro de la BD.
- [ ] Usuario con `ELIMINADA` no puede iniciar sesión (retorna 403).
- [ ] Usuario no encontrado retorna 404.
- [ ] Usuario ya eliminado retorna 409.
- [ ] Sin JWT o rol incorrecto retorna 401/403.
- [ ] Evento `USUARIO_ADMIN_GESTIONADO` registrado vía `IAuditLog`.
- [ ] Tests unitarios del service con cobertura ≥ 80%.
- [ ] Test de integración MockMvc verde.
- [ ] Validación E2E en panel de administración del frontend.
- [ ] Preguntas §9 sobre órdenes activas y asignación comisionista respondidas o documentadas como decisión diferida.
- [ ] `docs/PROGRESO.md` marcado con ✅ para HU-39.
