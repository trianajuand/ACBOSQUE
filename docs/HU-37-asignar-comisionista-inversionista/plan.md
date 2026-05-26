# plan.md — HU-37 Asignar comisionista a inversionista

> Derivado de `docs/HU-37-asignar-comisionista-inversionista/SPEC.md`.
> Estado: PENDIENTE DE APROBACIÓN HUMANA.
> No se escribe código hasta que el humano apruebe este documento.

---

## 1. Qué construye esta historia

HU-37 le permite al administrador asignar un comisionista a un inversionista creando un registro en `asignacion_comisionista`. Se implementa un único endpoint:

- `PUT /api/admin/inversionistas/{inversionistaId}/comisionista/{comisionistaId}`: verifica existencia y roles de ambos usuarios, valida que no exista asignación previa activa, persiste la asignación y registra auditoría.

Esta asignación habilita el flujo de los módulos posteriores: el comisionista puede consultar el portafolio del cliente (HU-28), crear propuestas (HU-30) y firmar órdenes (HU-32). También activa el split de comisión 60/40 en `OrdenService` cuando `IAsignacionComisionista.tieneComisionistaAsignado(inversionistaId)` retorna `true`.

---

## 2. Decisiones técnicas

| # | Decisión | Justificación |
|---|---|---|
| D1 | Solo el rol `ADMINISTRADOR` puede crear asignaciones; no existe auto-asignación | SPEC §precondiciones y CLAUDE.md §Regla 4. El inversionista puede solicitar un comisionista (HU-6 "solicitar comisionista"), pero la asignación formal la hace el admin. |
| D2 | La asignación delega la persistencia a `IGestionCuentas.asignarComisionista(inversionistaId, comisionistaId)` en el módulo Autenticación | ARQUITECTURA §5 regla 1: módulo Administración nunca toca `AsignacionComisionistaRepository` directamente. |
| D3 | La tabla `asignacion_comisionista` tiene `UNIQUE (inversionista_id)` — un inversionista tiene máximo un comisionista activo | SPEC §Modelo de datos. Si ya existe asignación activa, se debe decidir si reemplazarla o retornar 409 (ver §9 Q1). |
| D4 | Validar que `inversionistaId` tiene `rol = INVERSIONISTA` y que `comisionistaId` tiene `rol = COMISIONISTA` antes de persistir | SPEC §Error 3: roles incorrectos → 400. Esta validación ocurre en `IGestionCuentas` (módulo Autenticación, dueño de la lógica de roles). |
| D5 | El campo `asignadoPor` en `asignacion_comisionista` recibe el correo del admin extraído del JWT | Trazabilidad de quién realizó la asignación. |
| D6 | El evento de auditoría usa `TipoEvento.USUARIO_ADMIN_GESTIONADO` (nombre más genérico) ya que `COMISIONISTA_ASIGNADO` no aparece en `ARQUITECTURA.md §TipoEvento` | Ver §9 Q2. |

---

## 3. Cambios de dependencias

No se requieren dependencias Maven nuevas. Se usa:
- `IGestionCuentas.asignarComisionista()` (módulo Autenticación).
- `IAuditLog`.

---

## 4. Deuda técnica o hallazgos previos

| Hallazgo | Acción dentro de esta HU |
|---|---|
| El SPEC indica "Estado En desarrollo" — el endpoint existe y persiste, pero no ha sido validado en entorno integrado. | Ejecutar los criterios de verificación de este plan.md como lista de validación pendiente. |
| El SPEC §Modelo de datos tiene `UNIQUE (inversionista_id)` sin el campo `activa`, lo que implica que solo puede haber un registro por inversionista (no histórico de asignaciones). Sin embargo, el campo `activa BOOLEAN` está en el DDL. Hay ambigüedad. | Ver §9 Q1. |
| La interfaz `IGestionCuentas` en `ARQUITECTURA.md` declara `asignarComisionista(Long comisionistaId, Long inversionistaId)`. El SPEC muestra la ruta con `{inversionistaId}` primero y `{comisionistaId}` segundo. Verificar el orden de parámetros. | Usar el orden del SPEC para la ruta REST; el orden en la interfaz se adapta según el código real. Confirmar. |
| El evento `COMISIONISTA_ASIGNADO` aparece en el SPEC pero no en la lista de `TipoEvento` de `ARQUITECTURA.md`. | Ver §9 Q2. |

---

## 5. Arquitectura de la solución

### 5a. Mapeo de componentes (backend)

```
administracion/
├── controller/
│   └── AdminController.java              ← agrega PUT /api/admin/inversionistas/{invId}/comisionista/{comId}
└── service/
    └── AdministracionService.java        ← agrega asignarComisionista(invId, comId, correoAdmin)

autenticacion/ (solo a través de interfaz)
└── interfaces/
    └── IGestionCuentas.java              ← verificar método asignarComisionista(inversionistaId, comisionistaId)
```

Flujo de llamadas:
```
AdminController
    → AdministracionService.asignarComisionista(invId, comId, correoAdmin)
        → IGestionCuentas.asignarComisionista(invId, comId)   [módulo autenticacion]
            → validar roles de ambos usuarios
            → verificar/reemplazar asignación existente
            → AsignacionComisionistaRepository.save(asignacion)
        → IAuditLog.registrar(TipoEvento.USUARIO_ADMIN_GESTIONADO, correo_inversionista,
                              "Comisionista " + comId + " asignado por " + correoAdmin)
```

### 5b. Mapeo de componentes (frontend)

```
frontend/src/app/admin/
└── admin-dashboard.component.ts          ← sección "Usuarios": dropdown para asignar comisionista
    admin-dashboard.component.html
```

Flujo UI:
1. Tabla de usuarios con columna "Comisionista asignado".
2. Por cada inversionista, selector dropdown con lista de comisionistas disponibles.
3. Al seleccionar, llama `PUT /api/admin/inversionistas/{id}/comisionista/{comId}`.
4. Mensaje de confirmación en UI.

### 5c. Modelo de datos

No se crean tablas nuevas. Se usa `asignacion_comisionista` existente:

```sql
-- Tabla asignacion_comisionista (ya debe existir):
CREATE TABLE IF NOT EXISTS asignacion_comisionista (
    id                  BIGSERIAL PRIMARY KEY,
    inversionista_id    BIGINT NOT NULL REFERENCES inversionista(id),
    comisionista_id     BIGINT NOT NULL REFERENCES usuario(id),
    asignado_en         TIMESTAMP NOT NULL DEFAULT NOW(),
    asignado_por        VARCHAR(255),
    activa              BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_asignacion_inversionista UNIQUE (inversionista_id)
    -- El UNIQUE garantiza que cada inversionista tenga máximo 1 asignación activa
);
```

**Nota:** El `UNIQUE (inversionista_id)` implica que al reasignar, se debe hacer UPDATE del registro existente (cambiar `comisionista_id`) o DELETE + INSERT. Ver §9 Q1.

### 5d. Contratos de API

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| `PUT` | `/api/admin/inversionistas/{inversionistaId}/comisionista/{comisionistaId}` | `ADMINISTRADOR` | Asigna comisionista a inversionista |

**Response 200:**
```json
{
  "mensaje": "Comisionista asignado exitosamente"
}
```

**Response 404:**
```json
{
  "error": "Usuario no encontrado"
}
```

---

## 6. Grafo de dependencias entre tareas

```
T1.1 (verificar IGestionCuentas.asignarComisionista + tabla asignacion_comisionista)
    └─► T2.1 (AdministracionService.asignarComisionista)
            └─► T2.2 (AdminController — endpoint PUT)
                    └─► T3.1 (tests unitarios)
                            └─► T3.2 (tests integración)
                                    └─► T4.1 (frontend — selector de comisionista)
                                            └─► T5.1 (DoD e2e)
```

---

## 7. Estrategia de tests

| Tipo | Herramienta | Qué prueba |
|---|---|---|
| Unitario — `AdministracionService` | JUnit 5 + Mockito | `asignarComisionista_ambosExisten_asignaYAudita`, `asignarComisionista_inversionistaNoExiste_lanza404`, `asignarComisionista_comisionistaNoExiste_lanza404`, `asignarComisionista_rolesIncorrectos_lanza400` |
| Integración — `AdminController` | `@SpringBootTest` + MockMvc | PUT válido retorna 200; inversionista inexistente 404; comisionista inexistente 404; sin JWT 401; JWT de INVERSIONISTA 403. |
| BD | Query SQL | Tras PUT: `SELECT * FROM asignacion_comisionista WHERE inversionista_id = :id AND activa = true` debe retornar exactamente 1 fila con el comisionista correcto. |
| Integración — `IAsignacionComisionista` | `@SpringBootTest` | Tras asignación, `IAsignacionComisionista.tieneComisionistaAsignado(inversionistaId)` debe retornar `true`. |

---

## 8. Trazabilidad criterios de aceptación → artefacto

| Escenario/Criterio | Test o mecanismo |
|---|---|
| Asignación exitosa → 200 + registro en asignacion_comisionista | Test integración + query BD |
| Inversionista no encontrado → 404 | `asignarComisionista_inversionistaNoExiste_lanza404` |
| Comisionista no encontrado → 404 | `asignarComisionista_comisionistaNoExiste_lanza404` |
| Roles incorrectos → 400 | `asignarComisionista_rolesIncorrectos_lanza400` |
| Sin JWT → 401 | Test integración sin Authorization |
| Auditoría USUARIO_ADMIN_GESTIONADO registrado | `verify(auditLog).registrar(...)` en unitarios |
| Habilita split 60/40 en OrdenService | `IAsignacionComisionista.tieneComisionistaAsignado` retorna true tras asignación |

---

## 9. Preguntas abiertas

| # | Pregunta | Propuesta |
|---|---|---|
| Q1 | El SPEC §paso 3d dice "Si ya existe asignación activa → la reemplaza o retorna 409 (según política)". ¿Se reemplaza (UPDATE) o se rechaza (409)? El UNIQUE constraint impide dos registros activos, pero no define el comportamiento de reasignación. | Propuesta: reemplazar la asignación existente (cambiar `comisionista_id` y `asignado_en` del registro existente). El historial de asignaciones anteriores se pierde. Si se necesita historial, cambiar a borrado lógico (campo `activa = false`) e insertar uno nuevo. Confirmar con el equipo cuál aplica. |
| Q2 | El SPEC usa el evento `COMISIONISTA_ASIGNADO` pero este nombre no aparece en `TipoEvento` de `ARQUITECTURA.md`. ¿Se debe agregar al enum o usar `USUARIO_ADMIN_GESTIONADO`? | Propuesta: agregar `COMISIONISTA_ASIGNADO` al enum `TipoEvento` para mayor granularidad. Confirmar con el equipo si es necesario o si `USUARIO_ADMIN_GESTIONADO` es suficiente. |
| Q3 | ¿El endpoint debe también retornar el ID de la asignación creada, o solo el mensaje? | El SPEC retorna solo `RespuestaDTO{mensaje: ...}`. Propuesta: mantener solo el mensaje; el ID de asignación no es relevante para el flujo inmediato. Confirmar. |
| Q4 | ¿Qué pasa con las propuestas y órdenes en curso cuando se reasigna el comisionista? El inversionista podría tener propuestas `PENDIENTE_APROBACION` del comisionista anterior. | Información no disponible en SPEC. Propuesta: no cancelar propuestas existentes automáticamente; la reasignación solo afecta futuras propuestas. Confirmar con el equipo. |

---

## 10. Definition of Done

- [ ] `PUT /api/admin/inversionistas/{id}/comisionista/{id}` crea o actualiza registro en `asignacion_comisionista`.
- [ ] Usuarios no encontrados retornan 404 con mensaje descriptivo.
- [ ] Roles incorrectos retornan 400.
- [ ] Sin JWT o rol incorrecto retorna 401/403.
- [ ] Evento de auditoría registrado (`COMISIONISTA_ASIGNADO` o `USUARIO_ADMIN_GESTIONADO`).
- [ ] `IAsignacionComisionista.tieneComisionistaAsignado(inversionistaId)` retorna `true` tras la asignación.
- [ ] Tests unitarios de `AdministracionService` pasan (mínimo 4 casos).
- [ ] Tests de integración de `AdminController` pasan.
- [ ] Frontend muestra selector de comisionista por inversionista funcional.
- [ ] Validación completa en entorno integrado con frontend.
- [ ] `docs/PROGRESO.md` marcado con ✅ para HU-37.
