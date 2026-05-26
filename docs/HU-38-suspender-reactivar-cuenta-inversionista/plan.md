# plan.md — HU-38 Suspender / Reactivar Cuenta de Inversionista
> Derivado de `docs/HU-38-suspender-reactivar-cuenta-inversionista/SPEC.md`.
> Estado: PENDIENTE DE APROBACIÓN HUMANA.

---

## 1. Qué construye esta historia

Permite al **Administrador autenticado** cambiar el `estado_cuenta` de cualquier usuario (inversionista o comisionista) entre los valores `ACTIVA`, `SUSPENDIDA`, `RESTRINGIDA` y `BLOQUEADA` mediante el endpoint `PUT /api/admin/usuarios/{usuarioId}/estado`. El estado `ELIMINADA` queda reservado para HU-39. La operación es auditada y el usuario con estado `SUSPENDIDA` o `RESTRINGIDA` no puede iniciar sesión ni operar.

El SPEC indica que la funcionalidad ya está implementada en backend pero pendiente de validación integrada con frontend.

---

## 2. Decisiones técnicas

| # | Decisión | Justificación |
|---|---|---|
| 1 | Solo el rol `ADMINISTRADOR` puede invocar el endpoint | Regla dura del proyecto (CLAUDE.md §4). La verificación se hace en `SecurityConfig` + claim JWT. |
| 2 | El cambio de estado se delega a `IGestionCuentas.cambiarEstadoUsuario()` desde `administracion` | Módulo `administracion` no importa repositorios de `autenticacion`; solo interfaces I... (ARQUITECTURA.md §5). |
| 3 | Se registra evento `CAMBIO_ESTADO_CUENTA` vía `IAuditLog` | Cambio de estado de cuenta es evento auditable obligatorio (CONVENCIONES.md §2.7). |
| 4 | Respuesta HTTP 423 cuando la cuenta está bloqueada por intentos fallidos | Estándar del proyecto para cuenta bloqueada (CONVENCIONES.md §1.6). |
| 5 | El `motivo` en `CambiarEstadoCuentaDTO` es opcional | SPEC indica campo `nullable: true`; no rompe contratos existentes. |
| 6 | Inyección por constructor en `AdministracionService` | Convención obligatoria del proyecto (CONVENCIONES.md §1.2). |

---

## 3. Cambios de dependencias

Ningún cambio en `pom.xml` ni `package.json` es necesario. La funcionalidad usa componentes ya existentes:
- `AdminController`, `AdministracionService`, `IGestionCuentas`, `IAuditLog`, `CambiarEstadoCuentaDTO`, `EstadoCuenta` (enum).

---

## 4. Deuda técnica o hallazgos previos

| Hallazgo | Acción |
|---|---|
| El SPEC indica que la validación en entorno integrado (frontend + backend) aún no se completó | Ejecutar pruebas E2E con el panel de administración antes de marcar DoD completo. |
| El SPEC no menciona notificación al usuario afectado cuando su cuenta es suspendida o reactivada | Exponer en §9. Según CONVENCIONES.md §2.7, cambios de estado son auditables; si hay notificación también debería ir por `INotificacion`. |
| `BLOQUEADA` está listado como valor posible en el DTO, pero el SPEC de EC-09 lo asigna automáticamente por intentos fallidos | Revisar si el administrador puede asignar `BLOQUEADA` manualmente o si ese estado solo lo gestiona `MonitorIntentosService`. |

---

## 5. Arquitectura de la solución

### 5a. Mapeo de componentes (backend)

| Capa | Componente | Módulo | Responsabilidad |
|---|---|---|---|
| Controller | `AdminController` | `administracion` | Recibe `PUT /api/admin/usuarios/{usuarioId}/estado`, valida JWT/rol, delega al service. |
| Service | `AdministracionService` | `administracion` | Orquesta: valida existencia usuario, llama `IGestionCuentas`, llama `IAuditLog`. |
| Interface | `IGestionCuentas` | `autenticacion` (proveedor) | `cambiarEstadoUsuario(Long usuarioId, EstadoCuenta nuevoEstado)`. Actualiza columna `estado_cuenta` en tabla `usuario`. |
| Interface | `IAuditLog` | `trazabilidad` (proveedor) | `registrar(TipoEvento.USUARIO_ADMIN_GESTIONADO, correoUsuario, detalle)`. |
| DTO | `CambiarEstadoCuentaDTO` | `administracion/dto` | `nuevoEstado: EstadoCuenta`, `motivo: String (nullable)`. |
| Exception | `UsuarioNoEncontradoException` | `shared/exceptions` | Lanzada si `usuarioId` no existe → 404. |
| Enum | `EstadoCuenta` | `autenticacion/model` | Valores: `ACTIVA`, `SUSPENDIDA`, `RESTRINGIDA`, `BLOQUEADA`, `ELIMINADA`. |

### 5b. Mapeo de componentes (frontend)

| Componente | Archivo | Responsabilidad |
|---|---|---|
| `AdminDashboardComponent` | `admin/admin-dashboard.component.ts` | Muestra lista de usuarios y botones de cambio de estado. |
| `ApiService` | `core/api.service.ts` | Llama `PUT /api/admin/usuarios/{id}/estado` con JWT. |
| `ToastService` | `core/toast.service.ts` | Muestra confirmación o error tras la operación. |

### 5c. Modelo de datos

Tabla afectada: `usuario` (módulo `autenticacion`).

```
usuario
  id               BIGINT PK
  estado_cuenta    VARCHAR  -- ACTIVA | SUSPENDIDA | RESTRINGIDA | BLOQUEADA | ELIMINADA
  actualizado_en   TIMESTAMPTZ
```

No se agregan nuevas columnas. El cambio es una actualización de `estado_cuenta` y `actualizado_en`.

### 5d. Contratos de API

```
PUT /api/admin/usuarios/{usuarioId}/estado
Authorization: Bearer <JWT_ADMIN>
Content-Type: application/json

Request:
{
  "nuevoEstado": "SUSPENDIDA",   // enum: ACTIVA | SUSPENDIDA | RESTRINGIDA | BLOQUEADA
  "motivo": "Actividad sospechosa detectada"  // opcional
}

Responses:
200 OK    → { "mensaje": "Estado actualizado exitosamente" }
400       → { "error": "Estado inválido" }
401/403   → { "error": "No autorizado" }
404       → { "error": "Usuario no encontrado" }
```

---

## 6. Grafo de dependencias entre tareas

```
T1.1 (verificar enum EstadoCuenta)
    └─► T1.2 (verificar CambiarEstadoCuentaDTO)
            └─► T1.3 (verificar IGestionCuentas.cambiarEstadoUsuario)
                    └─► T2.1 (integración AdministracionService + IAuditLog)
                            └─► T2.2 (test unitario service)
                                └─► T3.1 (test integración endpoint)
                                        └─► T3.2 (validación frontend)
                                                └─► T4.1 (DoD + PROGRESO.md)
```

---

## 7. Estrategia de tests

- **Unitario `AdministracionService`:** mockear `IGestionCuentas` e `IAuditLog`; verificar que se llaman con los argumentos correctos ante estado válido, usuario inexistente y estado idéntico al actual.
- **Unitario `MonitorIntentosService` / `AutenticacionService`:** verificar que login retorna 401 si `estado_cuenta = SUSPENDIDA`.
- **Integración `MockMvc`:** `PUT /api/admin/usuarios/{id}/estado` con JWT válido de admin → 200; sin JWT → 401; rol INVERSIONISTA → 403; id inexistente → 404; estado inválido → 400.
- **Naming:** `cambiarEstado_suspender_usuarioActivo_retorna200`, `cambiarEstado_usuarioInexistente_retorna404`, `cambiarEstado_rolInversionista_retorna403`.

---

## 8. Trazabilidad criterios de aceptación → artefacto

| Criterio (SPEC) | Test o mecanismo |
|---|---|
| `PUT /api/admin/.../estado` actualiza `estado_cuenta` | Test integración MockMvc + verificación en BD. |
| Usuario no encontrado → 404 | `cambiarEstado_usuarioInexistente_retorna404` |
| Estado inválido → 400 | `cambiarEstado_estadoInvalido_retorna400` |
| Sin JWT o rol incorrecto → 401/403 | `cambiarEstado_sinJwt_retorna401`, `cambiarEstado_rolInversionista_retorna403` |
| Evento `CAMBIO_ESTADO_CUENTA` en auditoría | Verificar invocación de `IAuditLog.registrar` con `TipoEvento.USUARIO_ADMIN_GESTIONADO` en test unitario. |
| Validación en entorno integrado (pendiente) | Prueba E2E manual en panel admin del frontend. |

---

## 9. Preguntas abiertas

| # | Pregunta | Propuesta |
|---|---|---|
| 1 | ¿Debe el sistema notificar al usuario afectado (por correo/SMS) cuando un administrador suspende o reactiva su cuenta? | Sí, según CONVENCIONES.md §2.7 la suspensión/reactivación es evento auditable; la notificación sería via `INotificacion.notificarAdmin` o un nuevo método. Requiere decisión del equipo. |
| 2 | ¿Puede el administrador asignar manualmente `BLOQUEADA` como `nuevoEstado`? | El SPEC lo incluye en el enum del DTO, pero `BLOQUEADA` normalmente la asigna `MonitorIntentosService` (EC-09). Ambigüedad: ¿es intencional o error? |
| 3 | El SPEC no menciona qué sucede con órdenes activas de un inversionista cuando su cuenta pasa a `SUSPENDIDA` | ¿Se cancelan automáticamente? ¿Se dejan en el estado actual? Requiere decisión de negocio. |
| 4 | ¿El cambio de estado `RESTRINGIDA` impide solo operar o también ver el dashboard? | El SPEC describe `RESTRINGIDA` como "puede autenticarse pero no operar", pero no define qué endpoints quedan bloqueados. |

---

## 10. Definition of Done

- [ ] `PUT /api/admin/usuarios/{id}/estado` actualiza `estado_cuenta` y `actualizado_en` en BD.
- [ ] Usuario con estado `SUSPENDIDA` no puede iniciar sesión.
- [ ] Usuario no encontrado retorna 404.
- [ ] Estado inválido retorna 400.
- [ ] Sin JWT o rol incorrecto retorna 401/403.
- [ ] Evento `USUARIO_ADMIN_GESTIONADO` (o `CAMBIO_ESTADO_CUENTA`) registrado vía `IAuditLog`.
- [ ] Tests unitarios del service con cobertura ≥ 80%.
- [ ] Test de integración MockMvc verde.
- [ ] Validación E2E en panel de administración del frontend.
- [ ] `docs/PROGRESO.md` marcado con ✅ para HU-38.
- [ ] Preguntas abiertas §9 respondidas o documentadas como decisión diferida.
