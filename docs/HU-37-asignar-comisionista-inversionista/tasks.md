# tasks.md — HU-37 Asignar comisionista a inversionista

> Descomposición del plan.md aprobado (SDD Paso 3).
> Rama: `feat/HU-37-asignar-comisionista-inversionista`.
> Leyenda: ☐ pendiente · ◐ en progreso · ☑ hecho · ✗ cancelada

---

## Lote 1 — Verificación de interfaces y tabla

- ☐ **T1.1** Verificar `IGestionCuentas.asignarComisionista` y tabla `asignacion_comisionista`
  - Artefactos:
    - `backend/src/main/java/co/edu/unbosque/accioneselbosque/autenticacion/interfaces/IGestionCuentas.java`
    - BD: `\d asignacion_comisionista` en psql
  - Verificar que `IGestionCuentas` declara `void asignarComisionista(Long inversionistaId, Long comisionistaId)` (o equivalente).
  - Verificar que la tabla `asignacion_comisionista` existe con los campos: `id`, `inversionista_id` (FK inversionista), `comisionista_id` (FK usuario), `asignado_en`, `asignado_por`, `activa`, `UNIQUE (inversionista_id)`.
  - Si la tabla no existe: documentar DDL propuesto y pausar hasta confirmación humana.
  - Verificación: `mvn compile` sin errores.

**← HITO 1 — Interfaz y tabla verificadas (validación humana)**

---

## Lote 2 — Lógica de negocio en `AdministracionService`

- ☐ **T2.1** Implementar/verificar `asignarComisionista(Long inversionistaId, Long comisionistaId, String correoAdmin)` en `AdministracionService`
  - Artefactos: `backend/src/main/java/co/edu/unbosque/accioneselbosque/administracion/service/AdministracionService.java`
  - Lógica:
    1. Llamar `IGestionCuentas.asignarComisionista(inversionistaId, comisionistaId)`.
       - `IGestionCuentas` valida existencia de ambos usuarios y sus roles.
       - Si no existe → lanzar `UsuarioNoEncontradoException` (404).
       - Si roles incorrectos → lanzar `IllegalArgumentException` (400).
       - Persistir en `asignacion_comisionista` (reemplazar si ya existe según política confirmada en §9 Q1).
    2. Obtener correo del inversionista para la auditoría (requiere un método de consulta en `IGestionCuentas` o en `IConsultaInversionista`).
    3. `IAuditLog.registrar(TipoEvento.USUARIO_ADMIN_GESTIONADO, correoInversionista, "Comisionista " + comisionistaId + " asignado por " + correoAdmin)`.
  - Anotar con `@Transactional`.
  - Verificación: `mvn compile` sin errores.

**← HITO 2 — Lógica de service compilada sin errores (validación humana)**

---

## Lote 3 — Controller

- ☐ **T3.1** Agregar endpoint en `AdminController`
  - Artefactos: `backend/src/main/java/co/edu/unbosque/accioneselbosque/administracion/controller/AdminController.java`
  - Método:
    - `@PutMapping("/inversionistas/{inversionistaId}/comisionista/{comisionistaId}")`
    - Extraer `correoAdmin` del JWT.
    - Delegar a `AdministracionService.asignarComisionista(inversionistaId, comisionistaId, correoAdmin)`.
    - Retornar `ResponseEntity.ok(RespuestaDTO{"mensaje": "Comisionista asignado exitosamente"})`.
  - Sin lógica de negocio en el controller.
  - Verificación:
    ```
    curl -s -X PUT \
         "http://localhost:8080/api/admin/inversionistas/5/comisionista/7" \
         -H "Authorization: Bearer <jwt_admin>" | jq .
    # Debe retornar 200 con mensaje de éxito
    ```

**← HITO 3 — Aplicación arranca y endpoint responde (validación humana)**

---

## Lote 4 — Tests

- ☐ **T4.1** Tests unitarios de `AdministracionService` (asignación)
  - Artefactos: `backend/src/test/java/co/edu/unbosque/accioneselbosque/administracion/service/AdministracionServiceAsignacionTest.java`
  - Casos requeridos:
    - `asignarComisionista_ambosExisten_delegaAIGestionCuentasYAudita`
    - `asignarComisionista_inversionistaNoExiste_propagaUsuarioNoEncontrado`
    - `asignarComisionista_comisionistaNoExiste_propagaUsuarioNoEncontrado`
    - `asignarComisionista_rolInversionistaIncorrecto_lanza400`
  - Verificación: `mvn test -Dtest=AdministracionServiceAsignacionTest` — todos en verde.

- ☐ **T4.2** Tests de integración de `AdminController` (asignación)
  - Artefactos: `backend/src/test/java/co/edu/unbosque/accioneselbosque/administracion/controller/AdminControllerAsignacionIntegrationTest.java`
  - Casos requeridos: PUT válido 200; inversionista inexistente 404; comisionista inexistente 404; sin JWT 401; JWT de INVERSIONISTA 403.
  - Verificación: `mvn test -Dtest=AdminControllerAsignacionIntegrationTest` — todos en verde.

- ☐ **T4.3** Verificar `IAsignacionComisionista` tras asignación
  - Verificación en test de integración:
    ```java
    // Tras asignar comisionista 7 a inversionista 5:
    boolean tieneComisionista = iAsignacionComisionista.tieneComisionistaAsignado(5L);
    assertTrue(tieneComisionista);
    ```
  - Verificación en BD:
    ```sql
    SELECT comisionista_id, activa FROM asignacion_comisionista
     WHERE inversionista_id = 5;
    -- Debe retornar: comisionista_id=7, activa=true
    ```

**← HITO 4 — Suite de tests verde y BD verificada (validación humana)**

---

## Lote 5 — Frontend

- ☐ **T5.1** Selector de comisionista por inversionista en `admin-dashboard`
  - Artefactos: `frontend/src/app/admin/admin-dashboard.component.ts`, `.html`
  - Funcionalidad: en la tabla de usuarios, columna "Comisionista" con dropdown de comisionistas disponibles (cargados de `GET /api/admin/mercados` o endpoint de lista de comisionistas — ver §9 Q3 del plan); al cambiar el valor, llama `PUT /api/admin/inversionistas/{id}/comisionista/{comId}`; toast de éxito o error.
  - Verificación: `ng build --configuration production` sin errores TypeScript.

**← HITO 5 — Frontend compila y selector funcional (validación humana)**

---

## Lote 6 — Cierre

- ☐ **T6.1** Verificación DoD end-to-end
  - Flujo completo: login admin → asignar comisionista 7 a inversionista 5 → login como comisionista 7 → verificar que puede ver el portafolio de inversionista 5 en `GET /api/comisionista/clientes`.
  - Verificar en BD:
    ```sql
    SELECT c.nombre_completo, a.asignado_en
      FROM asignacion_comisionista a
      JOIN usuario c ON c.id = a.comisionista_id
     WHERE a.inversionista_id = 5 AND a.activa = TRUE;
    -- Debe retornar el comisionista asignado
    ```
  - Verificar en logs: `grep "USUARIO_ADMIN_GESTIONADO" logs/audit.log`

- ☐ **T6.2** PR abierto con checklist DoD, pipeline verde
  - Título del PR: `feat(administracion): HU-37 asignar comisionista a inversionista`
  - `mvn test` sin fallos; `ng build` sin errores.

**← HITO FINAL — Entrega HU-37**
