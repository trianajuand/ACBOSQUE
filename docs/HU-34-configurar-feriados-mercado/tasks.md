# tasks.md — HU-34 Configurar feriados de mercado

> Descomposición del plan.md aprobado (SDD Paso 3).
> Rama: `feat/HU-34-configurar-feriados-mercado`.
> Leyenda: ☐ pendiente · ◐ en progreso · ☑ hecho · ✗ cancelada

---

## Lote 1 — Modelo y repositorio

- ☐ **T1.1** Verificar/completar entidad `FeriadoMercado` y DTO `FeriadoMercadoDTO`
  - Artefactos:
    - `backend/src/main/java/co/edu/unbosque/accioneselbosque/administracion/model/FeriadoMercado.java`
    - `backend/src/main/java/co/edu/unbosque/accioneselbosque/administracion/dto/FeriadoMercadoDTO.java`
  - Verificar que `FeriadoMercado` tiene: `id` (BIGINT), `mercadoConfig` (ManyToOne FK), `fecha` (LocalDate), `descripcion` (String nullable).
  - Verificar que `FeriadoMercadoDTO` expone: `id`, `mercadoCodigo` (readOnly, derivado del join), `fecha` (LocalDate), `descripcion`.
  - Verificación: `mvn compile` sin errores.

- ☐ **T1.2** Agregar métodos en `FeriadoMercadoRepository`
  - Artefactos: `backend/src/main/java/co/edu/unbosque/accioneselbosque/administracion/repository/FeriadoMercadoRepository.java`
  - Métodos requeridos:
    - `List<FeriadoMercado> findByMercadoConfigId(Long mercadoConfigId)`
    - `Optional<FeriadoMercado> findByIdAndMercadoConfigId(Long id, Long mercadoConfigId)`
    - `boolean existsByMercadoConfigIdAndFecha(Long mercadoConfigId, LocalDate fecha)`
  - Verificación: `mvn compile` sin errores.

- ☐ **T1.3** Verificar seed de feriados en `DatosInicialesAdministracion`
  - Artefactos: `backend/src/main/java/co/edu/unbosque/accioneselbosque/administracion/service/DatosInicialesAdministracion.java`
  - El seed debe resolver los IDs de mercado por código (no por posición de inserción).
  - Verificación: `SELECT * FROM feriado_mercado;` tras arranque — debe retornar filas de feriados NYSE y NASDAQ para 2026.

**← HITO 1 — Modelo y repositorio compilados y seed verificado (validación humana)**

---

## Lote 2 — Lógica de negocio en `AdministracionService`

- ☐ **T2.1** Implementar `listarFeriados(String codigoMercado)`
  - Artefactos: `backend/src/main/java/co/edu/unbosque/accioneselbosque/administracion/service/AdministracionService.java`
  - Lógica:
    1. `MercadoConfigRepository.findByCodigo(codigoMercado)` → si no existe, lanzar excepción 404.
    2. `FeriadoMercadoRepository.findByMercadoConfigId(mercadoConfig.getId())`.
    3. Mapear a `List<FeriadoMercadoDTO>` incluyendo `mercadoCodigo = mercadoConfig.getCodigo()`.
  - Anotar con `@Transactional(readOnly = true)`.
  - Verificación: `mvn compile` sin errores.

- ☐ **T2.2** Implementar `agregarFeriado(String codigoMercado, FeriadoMercadoDTO dto)`
  - Artefactos: `backend/src/main/java/co/edu/unbosque/accioneselbosque/administracion/service/AdministracionService.java`
  - Lógica:
    1. Resolver `mercadoConfig` por código → 404 si no existe.
    2. Verificar duplicado con `existsByMercadoConfigIdAndFecha(id, dto.getFecha())` → 409 si ya existe.
    3. Crear y persistir `FeriadoMercado`.
    4. `IAuditLog.registrar(TipoEvento.PARAMETRO_ADMIN_ACTUALIZADO, correoAdmin, "Feriado " + dto.getFecha() + " agregado a " + codigoMercado)`.
    5. Retornar `FeriadoMercadoDTO` con `id` generado.
  - Anotar con `@Transactional`.
  - Verificación: `mvn compile` sin errores.

- ☐ **T2.3** Implementar `eliminarFeriado(String codigoMercado, Long feriadoId)`
  - Artefactos: `backend/src/main/java/co/edu/unbosque/accioneselbosque/administracion/service/AdministracionService.java`
  - Lógica:
    1. Resolver `mercadoConfig` por código → 404 si no existe.
    2. `FeriadoMercadoRepository.findByIdAndMercadoConfigId(feriadoId, mercadoConfig.getId())` → 404 si no existe o no pertenece al mercado.
    3. Eliminar el registro.
    4. `IAuditLog.registrar(TipoEvento.PARAMETRO_ADMIN_ACTUALIZADO, correoAdmin, "Feriado " + feriadoId + " eliminado de " + codigoMercado)`.
  - Anotar con `@Transactional`.
  - Verificación: `mvn compile` sin errores.

**← HITO 2 — Lógica de service compilada sin errores (validación humana)**

---

## Lote 3 — Controller

- ☐ **T3.1** Agregar 3 endpoints de feriados en `AdminController`
  - Artefactos: `backend/src/main/java/co/edu/unbosque/accioneselbosque/administracion/controller/AdminController.java`
  - Endpoints:
    - `GET /api/admin/mercados/{codigoMercado}/feriados` → 200 `List<FeriadoMercadoDTO>`
    - `POST /api/admin/mercados/{codigoMercado}/feriados` → 201 `FeriadoMercadoDTO`
    - `DELETE /api/admin/mercados/{codigoMercado}/feriados/{feriadoId}` → 200
  - Extraer `correoAdmin` del JWT para pasarlo al service para auditoría.
  - Verificación:
    ```
    curl -s -X POST http://localhost:8080/api/admin/mercados/NYSE/feriados \
         -H "Authorization: Bearer <jwt_admin>" \
         -H "Content-Type: application/json" \
         -d '{"fecha":"2026-11-26","descripcion":"Thanksgiving"}' | jq .
    # Debe retornar 201 con el feriado creado
    ```

**← HITO 3 — Aplicación arranca con los 3 endpoints funcionales (validación humana)**

---

## Lote 4 — Tests

- ☐ **T4.1** Tests unitarios de `AdministracionService` (métodos de feriados)
  - Artefactos: `backend/src/test/java/co/edu/unbosque/accioneselbosque/administracion/service/AdministracionServiceFeriadoTest.java`
  - Casos requeridos:
    - `listarFeriados_mercadoValido_retornaLista`
    - `agregarFeriado_fechaNueva_creaYRetornaDTO`
    - `agregarFeriado_fechaDuplicada_lanza409`
    - `agregarFeriado_mercadoInexistente_lanza404`
    - `eliminarFeriado_valido_elimina`
    - `eliminarFeriado_noPerteneceMercado_lanza404`
  - Verificación: `mvn test -Dtest=AdministracionServiceFeriadoTest` — todos en verde.

- ☐ **T4.2** Tests de integración de `AdminController` (endpoints de feriados)
  - Artefactos: `backend/src/test/java/co/edu/unbosque/accioneselbosque/administracion/controller/AdminControllerFeriadoIntegrationTest.java`
  - Casos requeridos: GET 200; POST válido 201; POST duplicado 409; DELETE válido 200; mercado inexistente 404; sin JWT 401.
  - Verificación: `mvn test -Dtest=AdminControllerFeriadoIntegrationTest` — todos en verde.

**← HITO 4 — Suite de tests verde (validación humana)**

---

## Lote 5 — Frontend

- ☐ **T5.1** Sección de feriados por mercado en `admin-dashboard`
  - Artefactos: `frontend/src/app/admin/admin-dashboard.component.ts`, `.html`
  - Funcionalidad: dropdown para seleccionar mercado; tabla de feriados (fecha, descripción, botón eliminar); formulario de agregar feriado con date picker y campo descripción.
  - Confirmación antes de eliminar un feriado.
  - Verificación: `ng build --configuration production` sin errores TypeScript.

**← HITO 5 — Frontend compila y UI funcional (validación humana)**

---

## Lote 6 — Cierre

- ☐ **T6.1** Verificación DoD end-to-end
  - Flujo: login admin → agregar feriado NYSE 2026-11-26 → verificar en BD:
    ```sql
    SELECT * FROM feriado_mercado
     WHERE fecha = '2026-11-26'
       AND mercado_config_id = (SELECT id FROM mercado_config WHERE codigo = 'NYSE');
    -- Debe retornar 1 fila
    ```
  - Eliminar el feriado → verificar que ya no aparece.
  - Verificar en logs: `grep "PARAMETRO_ADMIN_ACTUALIZADO" logs/audit.log` — debe tener 2 entradas.

- ☐ **T6.2** PR abierto con checklist DoD, pipeline verde
  - Título del PR: `feat(administracion): HU-34 configurar feriados de mercado`
  - `mvn test` sin fallos; `ng build` sin errores.

**← HITO FINAL — Entrega HU-34**
