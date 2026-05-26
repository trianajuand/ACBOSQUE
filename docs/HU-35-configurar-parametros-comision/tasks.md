# tasks.md — HU-35 Configurar parámetros de comisión

> Descomposición del plan.md aprobado (SDD Paso 3).
> Rama: `feat/HU-35-configurar-parametros-comision`.
> Leyenda: ☐ pendiente · ◐ en progreso · ☑ hecho · ✗ cancelada

---

## Lote 1 — Modelo y repositorio

- ☐ **T1.1** Verificar/completar entidad `ParametroComision` y DTO `ParametroComisionDTO`
  - Artefactos:
    - `backend/src/main/java/co/edu/unbosque/accioneselbosque/administracion/model/ParametroComision.java`
    - `backend/src/main/java/co/edu/unbosque/accioneselbosque/administracion/dto/ParametroComisionDTO.java`
  - `ParametroComision` debe tener: `id`, `porcentajeComision` (DECIMAL 5,4), `porcentajePlataforma`, `porcentajeComisionista`, `fechaInicio` (LocalDate), `fechaFin` (LocalDate nullable), `actualizadoPor`.
  - `ParametroComisionDTO` debe exponer: `porcentajeComision`, `porcentajePlataforma`, `porcentajeComisionista` en escala 0–100.
  - Agregar Bean Validation: `@DecimalMin("0.01")`, `@DecimalMax("100")` en los tres campos.
  - Verificación: `mvn compile` sin errores.

- ☐ **T1.2** Agregar método en `ParametroComisionRepository`
  - Artefactos: `backend/src/main/java/co/edu/unbosque/accioneselbosque/administracion/repository/ParametroComisionRepository.java`
  - Método requerido:
    ```java
    @Query("SELECT p FROM ParametroComision p WHERE p.fechaInicio <= :fecha AND (p.fechaFin IS NULL OR p.fechaFin >= :fecha)")
    Optional<ParametroComision> findParametroActivo(@Param("fecha") LocalDate fecha);
    ```
  - Verificación: `mvn compile` sin errores.

- ☐ **T1.3** Verificar seed en `DatosInicialesAdministracion`
  - Artefactos: `backend/src/main/java/co/edu/unbosque/accioneselbosque/administracion/service/DatosInicialesAdministracion.java`
  - El seed debe crear el registro inicial solo si no existe ningún registro con `fecha_fin IS NULL`.
  - Verificación: `SELECT * FROM parametro_comision WHERE fecha_fin IS NULL;` — debe retornar exactamente 1 fila.

**← HITO 1 — Modelo, repositorio y seed verificados (validación humana)**

---

## Lote 2 — Lógica de negocio en `AdministracionService` e `IGestorParametros`

- ☐ **T2.1** Implementar/verificar `obtenerParametrosVigentes()` en `AdministracionService`
  - Artefactos: `backend/src/main/java/co/edu/unbosque/accioneselbosque/administracion/service/AdministracionService.java`
  - Lógica: `findParametroActivo(LocalDate.now())` → mapear a `ParametroComisionDTO` (multiplicar por 100 para escala de UI).
  - Anotar con `@Transactional(readOnly = true)`.
  - Verificación: `mvn compile` sin errores.

- ☐ **T2.2** Implementar/verificar `actualizarParametrosComision(ParametroComisionDTO dto, String correoAdmin)` en `AdministracionService`
  - Artefactos: `backend/src/main/java/co/edu/unbosque/accioneselbosque/administracion/service/AdministracionService.java`
  - Lógica:
    1. Validar `dto.getPorcentajeComision() > 0 && <= 100` — 400 si falla.
    2. Validar `dto.getPorcentajePlataforma() + dto.getPorcentajeComisionista() == 100` — 400 si falla.
    3. `findParametroActivo(LocalDate.now())` → `setFechaFin(LocalDate.now().minusDays(1))`.
    4. Crear nuevo `ParametroComision` con `fechaInicio = LocalDate.now()`, `fechaFin = null`, `actualizadoPor = correoAdmin`, valores en escala 0–1.
    5. `IAuditLog.registrar(TipoEvento.PARAMETRO_MODIFICADO, correoAdmin, "Comisión actualizada: " + dto.getPorcentajeComision() + "%")`.
    6. Retornar `ParametroComisionDTO` del nuevo registro.
  - Anotar con `@Transactional`.
  - Verificación: `mvn compile` sin errores.

- ☐ **T2.3** Verificar implementación de `IGestorParametros` en `AdministracionService`
  - Artefactos: `backend/src/main/java/co/edu/unbosque/accioneselbosque/administracion/service/AdministracionService.java`
  - Los tres métodos de la interfaz deben leer desde `findParametroActivo(LocalDate.now())`:
    - `obtenerPorcentajeComision()` → `BigDecimal` (escala 0–1, ej. `0.0200`).
    - `obtenerSplitPlataforma()` → `BigDecimal`.
    - `obtenerSplitComisionista()` → `BigDecimal`.
  - Verificar que `OrdenService` inyecta `IGestorParametros` (no constantes hardcodeadas):
    ```
    grep -r "porcentajeComision\|0.02\|IGestorParametros" \
      backend/src/main/java/co/edu/unbosque/accioneselbosque/ordenes/service/OrdenService.java
    ```
  - Verificación: `mvn compile` sin errores.

**← HITO 2 — Service e IGestorParametros compilados sin errores (validación humana)**

---

## Lote 3 — Controller

- ☐ **T3.1** Agregar endpoints en `AdminController`
  - Artefactos: `backend/src/main/java/co/edu/unbosque/accioneselbosque/administracion/controller/AdminController.java`
  - Endpoints:
    - `GET /api/admin/comisiones` → `AdministracionService.obtenerParametrosVigentes()` → 200
    - `PUT /api/admin/comisiones` → `@Valid @RequestBody ParametroComisionDTO` → `AdministracionService.actualizarParametrosComision(dto, correoAdmin)` → 200
  - Extraer `correoAdmin` del JWT para auditoría.
  - Verificación:
    ```
    curl -s -X GET http://localhost:8080/api/admin/comisiones \
         -H "Authorization: Bearer <jwt_admin>" | jq .
    # Debe retornar {"porcentajeComision":2.0,"porcentajePlataforma":60.0,"porcentajeComisionista":40.0}
    ```

**← HITO 3 — Aplicación arranca y endpoints responden (validación humana)**

---

## Lote 4 — Tests

- ☐ **T4.1** Tests unitarios de `AdministracionService` (parámetros de comisión)
  - Artefactos: `backend/src/test/java/co/edu/unbosque/accioneselbosque/administracion/service/AdministracionServiceComisionTest.java`
  - Casos requeridos:
    - `obtenerParametrosVigentes_retornaDTO`
    - `actualizarParametros_valoresValidos_creaRegistroNuevo`
    - `actualizarParametros_porcentajeCero_lanza400`
    - `actualizarParametros_splitIncoherente_lanza400`
    - `obtenerPorcentajeComision_retornaBigDecimalEnEscalaDecimal`
    - `actualizarParametros_cierraRegistroPrevio`
  - Verificación: `mvn test -Dtest=AdministracionServiceComisionTest` — todos en verde.

- ☐ **T4.2** Tests de integración de `AdminController` (comisiones)
  - Artefactos: `backend/src/test/java/co/edu/unbosque/accioneselbosque/administracion/controller/AdminControllerComisionIntegrationTest.java`
  - Casos: GET 200; PUT válido 200; PUT porcentaje 0 → 400; PUT split 110 → 400; sin JWT → 401.
  - Verificación: `mvn test -Dtest=AdminControllerComisionIntegrationTest` — todos en verde.

- ☐ **T4.3** Verificar que `OrdenService` usa `IGestorParametros` (no constantes)
  - Verificación estática:
    ```
    grep -n "0\.02\|2\.0\|comision.*=" \
      backend/src/main/java/co/edu/unbosque/accioneselbosque/ordenes/service/OrdenService.java
    # No debe haber constantes numéricas de comisión hardcodeadas
    ```

**← HITO 4 — Suite de tests verde (validación humana)**

---

## Lote 5 — Frontend

- ☐ **T5.1** Panel de parámetros de comisión en `admin-dashboard`
  - Artefactos: `frontend/src/app/admin/admin-dashboard.component.ts`, `.html`
  - Funcionalidad: cargar `GET /api/admin/comisiones`; formulario editable con 3 campos; indicador en tiempo real de la suma del split (debe ser exactamente 100%); botón "Guardar" deshabilitado si suma ≠ 100%.
  - Confirmación modal antes de guardar.
  - Verificación: `ng build --configuration production` sin errores TypeScript.

**← HITO 5 — Frontend compila y UI funcional (validación humana)**

---

## Lote 6 — Cierre

- ☐ **T6.1** Verificación DoD end-to-end
  - Flujo: login admin → cambiar porcentaje a 1.5% → verificar en BD:
    ```sql
    SELECT porcentaje_comision, fecha_inicio, fecha_fin FROM parametro_comision ORDER BY id DESC LIMIT 2;
    -- Registro más reciente: porcentaje_comision=0.0150, fecha_fin IS NULL
    -- Registro anterior: fecha_fin = CURRENT_DATE - 1
    ```
  - Crear una orden con `montoBase = 1000` → verificar `monto_comision = 15.00` (1.5% de 1000).
  - Verificar en logs: `grep "PARAMETRO_MODIFICADO" logs/audit.log`

- ☐ **T6.2** PR abierto con checklist DoD, pipeline verde
  - Título del PR: `feat(administracion): HU-35 configurar parametros de comision`
  - `mvn test` sin fallos; `ng build` sin errores.

**← HITO FINAL — Entrega HU-35**
