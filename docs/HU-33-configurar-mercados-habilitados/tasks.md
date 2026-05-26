# tasks.md — HU-33 Configurar mercados habilitados

> Descomposición del plan.md aprobado (SDD Paso 3).
> Rama: `feat/HU-33-configurar-mercados-habilitados`.
> Leyenda: ☐ pendiente · ◐ en progreso · ☑ hecho · ✗ cancelada

---

## Lote 1 — Modelo y repositorio

- ☐ **T1.1** Verificar/completar entidad `MercadoConfig` y DTO `MercadoConfigDTO`
  - Artefactos:
    - `backend/src/main/java/co/edu/unbosque/accioneselbosque/administracion/model/MercadoConfig.java`
    - `backend/src/main/java/co/edu/unbosque/accioneselbosque/administracion/dto/MercadoConfigDTO.java`
  - Verificar que `MercadoConfig` tiene los campos: `id`, `codigo`, `nombre`, `habilitado`, `horaApertura`, `horaCierre`, `zonaHoraria`, `diasOperacion`.
  - Verificar que `MercadoConfigDTO` tiene `List<String> diasOperacion` (no String CSV).
  - Verificación: `mvn compile` sin errores.

- ☐ **T1.2** Verificar/agregar método en `MercadoConfigRepository`
  - Artefactos: `backend/src/main/java/co/edu/unbosque/accioneselbosque/administracion/repository/MercadoConfigRepository.java`
  - Método requerido: `Optional<MercadoConfig> findByCodigo(String codigo)`
  - Verificación: `mvn compile` sin errores.

- ☐ **T1.3** Verificar seed en `DatosInicialesAdministracion`
  - Artefactos: `backend/src/main/java/co/edu/unbosque/accioneselbosque/administracion/service/DatosInicialesAdministracion.java`
  - Confirmar que el seed carga NYSE y NASDAQ si no existen.
  - Verificación: arrancar la app y ejecutar `SELECT * FROM mercado_config;` — debe retornar al menos 2 filas.

**← HITO 1 — Modelo y repositorio compilados y seed verificado (validación humana)**

---

## Lote 2 — Lógica de negocio en `AdministracionService`

- ☐ **T2.1** Implementar/verificar `listarMercados()` en `AdministracionService`
  - Artefactos: `backend/src/main/java/co/edu/unbosque/accioneselbosque/administracion/service/AdministracionService.java`
  - Lógica: `MercadoConfigRepository.findAll()` → mapear a `List<MercadoConfigDTO>` convirtiendo `diasOperacion` de CSV a `List<String>`.
  - Anotar con `@Transactional(readOnly = true)`.
  - Verificación: `mvn compile` sin errores.

- ☐ **T2.2** Implementar/verificar `actualizarMercado(String codigo, MercadoConfigDTO dto)` en `AdministracionService`
  - Artefactos: `backend/src/main/java/co/edu/unbosque/accioneselbosque/administracion/service/AdministracionService.java`
  - Lógica:
    1. `findByCodigo(codigo)` → si no existe, lanzar `UsuarioNoEncontradoException` mapeada a 404 (o crear `MercadoNoEncontradoException`).
    2. Validar `ZoneId.of(dto.getZonaHoraria())` — si inválido, lanzar `IllegalArgumentException` (400).
    3. Actualizar campos en la entidad y persistir.
    4. Llamar `IAuditLog.registrar(TipoEvento.PARAMETRO_MODIFICADO, correoAdmin, "Mercado " + codigo + " actualizado")`.
    5. Retornar `MercadoConfigDTO` actualizado.
  - Anotar con `@Transactional`.
  - Verificación: `mvn compile` sin errores.

**← HITO 2 — Lógica de service compilada sin errores (validación humana)**

---

## Lote 3 — Controller

- ☐ **T3.1** Agregar/verificar endpoints en `AdminController`
  - Artefactos: `backend/src/main/java/co/edu/unbosque/accioneselbosque/administracion/controller/AdminController.java`
  - Endpoints:
    - `GET /api/admin/mercados` → `AdministracionService.listarMercados()` → `ResponseEntity<List<MercadoConfigDTO>>`
    - `PUT /api/admin/mercados/{codigo}` → `AdministracionService.actualizarMercado(codigo, dto)` → `ResponseEntity<MercadoConfigDTO>`
  - Asegurar `@PreAuthorize("hasRole('ADMINISTRADOR')")` o equivalente en `SecurityConfig`.
  - Sin `@CrossOrigin` (prohibido por CONVENCIONES §1.5).
  - Verificación:
    ```
    # Arrancar app y probar:
    curl -s -X GET http://localhost:8080/api/admin/mercados \
         -H "Authorization: Bearer <jwt_admin>" | jq .
    # Debe retornar 200 con lista de mercados
    ```

**← HITO 3 — Aplicación arranca y endpoints responden (validación humana)**

---

## Lote 4 — Tests

- ☐ **T4.1** Tests unitarios de `AdministracionService` (métodos de mercados)
  - Artefactos: `backend/src/test/java/co/edu/unbosque/accioneselbosque/administracion/service/AdministracionServiceMercadoTest.java`
  - Casos requeridos:
    - `listarMercados_retornaListaCompleta`
    - `actualizarMercado_codigoValido_actualizaYRetornaDTO`
    - `actualizarMercado_codigoInexistente_lanza404`
    - `actualizarMercado_zonaHorariaInvalida_lanza400`
  - Verificación: `mvn test -Dtest=AdministracionServiceMercadoTest` — todos en verde.

- ☐ **T4.2** Tests de integración de `AdminController` (endpoints de mercados)
  - Artefactos: `backend/src/test/java/co/edu/unbosque/accioneselbosque/administracion/controller/AdminControllerMercadoIntegrationTest.java`
  - Casos requeridos: GET 200; PUT código válido 200; PUT código inexistente 404; sin JWT 401; JWT INVERSIONISTA 403.
  - Verificación: `mvn test -Dtest=AdminControllerMercadoIntegrationTest` — todos en verde.

**← HITO 4 — Suite de tests verde (validación humana)**

---

## Lote 5 — Frontend

- ☐ **T5.1** Tabla de mercados en `admin-dashboard`
  - Artefactos: `frontend/src/app/admin/admin-dashboard.component.ts`, `.html`
  - Funcionalidad: llamada a `GET /api/admin/mercados`; tabla con: código, nombre, toggle habilitado, horario, zona horaria, días.
  - Toggle llama `PUT /api/admin/mercados/{codigo}` con `{habilitado: !actual}`.

- ☐ **T5.2** Modal de edición de mercado
  - Artefactos: `frontend/src/app/admin/admin-dashboard.component.html`, `.ts`
  - Campos editables: horaApertura, horaCierre, zonaHoraria, diasOperacion (checkboxes por día).
  - Confirmación antes de guardar.
  - Verificación: `ng build --configuration production` sin errores TypeScript.

**← HITO 5 — Frontend compila y UI funcional (validación humana)**

---

## Lote 6 — Cierre

- ☐ **T6.1** Verificación DoD end-to-end
  - Flujo completo: login admin → ver mercados → deshabilitar NYSE → verificar en BD:
    ```sql
    SELECT habilitado FROM mercado_config WHERE codigo = 'NYSE';
    -- Debe retornar false
    ```
  - Verificar en logs: `grep "PARAMETRO_MODIFICADO" logs/audit.log`
  - Verificar que el scheduler de cola de órdenes (HU-23) respeta el cambio en el siguiente ciclo.

- ☐ **T6.2** PR abierto con checklist DoD, pipeline verde
  - Título del PR: `feat(administracion): HU-33 configurar mercados habilitados`
  - `mvn test` sin fallos; `ng build` sin errores.

**← HITO FINAL — Entrega HU-33**
