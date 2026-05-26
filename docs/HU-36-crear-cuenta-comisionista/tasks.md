# tasks.md — HU-36 Crear cuenta de comisionista

> Descomposición del plan.md aprobado (SDD Paso 3).
> Rama: `feat/HU-36-crear-cuenta-comisionista`.
> Leyenda: ☐ pendiente · ◐ en progreso · ☑ hecho · ✗ cancelada

---

## Lote 1 — Verificación de interfaces y DTO

- ☐ **T1.1** Verificar `CrearComisionistaDTO` y `IGestionCuentas`
  - Artefactos:
    - `backend/src/main/java/co/edu/unbosque/accioneselbosque/administracion/dto/CrearComisionistaDTO.java`
    - `backend/src/main/java/co/edu/unbosque/accioneselbosque/autenticacion/interfaces/IGestionCuentas.java`
  - Verificar que `CrearComisionistaDTO` tiene: `correo` (`@NotBlank @Email`), `nombreCompleto` (`@NotBlank`), `contrasena` (`@NotBlank @Size(min=8)`), `telefono` (nullable).
  - Verificar que `IGestionCuentas` declara `void crearComisionista(CrearComisionistaDTO dto)` (o equivalente).
  - Si la firma no existe, documentar la discrepancia y pausar hasta confirmación humana.
  - Verificación: `mvn compile` sin errores.

**← HITO 1 — DTO e interfaz verificados (validación humana)**

---

## Lote 2 — Lógica de negocio en `AdministracionService`

- ☐ **T2.1** Implementar/verificar `crearComisionista(CrearComisionistaDTO dto, String correoAdmin)` en `AdministracionService`
  - Artefactos: `backend/src/main/java/co/edu/unbosque/accioneselbosque/administracion/service/AdministracionService.java`
  - Lógica:
    1. Llamar `IGestionCuentas.crearComisionista(dto)` — la persistencia y el BCrypt ocurren en el módulo Autenticación.
    2. Si `IGestionCuentas` lanza `EmailAlreadyExistsException` → dejar que suba al `GlobalExceptionHandler` (409).
    3. `IAuditLog.registrar(TipoEvento.USUARIO_ADMIN_GESTIONADO, dto.getCorreo(), "Comisionista creado por " + correoAdmin)`.
  - Anotar con `@Transactional`.
  - Verificación: `mvn compile` sin errores.

**← HITO 2 — Lógica de service compilada sin errores (validación humana)**

---

## Lote 3 — Controller

- ☐ **T3.1** Agregar endpoint `POST /api/admin/comisionistas` en `AdminController`
  - Artefactos: `backend/src/main/java/co/edu/unbosque/accioneselbosque/administracion/controller/AdminController.java`
  - Método:
    - `@PostMapping("/comisionistas")`
    - `@Valid @RequestBody CrearComisionistaDTO dto`
    - Extraer `correoAdmin` del JWT.
    - Delegar a `AdministracionService.crearComisionista(dto, correoAdmin)`.
    - Retornar `ResponseEntity.status(201).body(RespuestaDTO{"mensaje": "Comisionista creado exitosamente"})`.
  - Sin lógica de negocio en el controller.
  - Verificación:
    ```
    curl -s -X POST http://localhost:8080/api/admin/comisionistas \
         -H "Authorization: Bearer <jwt_admin>" \
         -H "Content-Type: application/json" \
         -d '{"correo":"nuevo@test.com","nombreCompleto":"Carlos","contrasena":"Segura123!"}' | jq .
    # Debe retornar 201
    ```

**← HITO 3 — Aplicación arranca y endpoint responde (validación humana)**

---

## Lote 4 — Tests

- ☐ **T4.1** Tests unitarios de `AdministracionService` (crear comisionista)
  - Artefactos: `backend/src/test/java/co/edu/unbosque/accioneselbosque/administracion/service/AdministracionServiceComisionistaTest.java`
  - Casos requeridos:
    - `crearComisionista_datosValidos_delegaAIGestionCuentasYAudita`
    - `crearComisionista_correoExistente_propagaEmailAlreadyExistsException`
  - Verificación: `mvn test -Dtest=AdministracionServiceComisionistaTest` — todos en verde.

- ☐ **T4.2** Tests de integración de `AdminController` (crear comisionista)
  - Artefactos: `backend/src/test/java/co/edu/unbosque/accioneselbosque/administracion/controller/AdminControllerComisionistaIntegrationTest.java`
  - Casos requeridos: POST válido 201; POST correo duplicado 409; sin JWT 401; JWT de INVERSIONISTA 403; campos faltantes 400.
  - Verificación: `mvn test -Dtest=AdminControllerComisionistaIntegrationTest` — todos en verde.

- ☐ **T4.3** Verificación de seguridad en BD
  - Verificación:
    ```sql
    -- Tras crear comisionista:
    SELECT rol, mfa_habilitado, estado_cuenta FROM usuario WHERE correo = 'nuevo@test.com';
    -- Debe retornar: COMISIONISTA | true | ACTIVA

    SELECT contrasena FROM usuario WHERE correo = 'nuevo@test.com';
    -- Debe empezar con '$2a$' (BCrypt)
    ```

**← HITO 4 — Suite de tests verde y BD verificada (validación humana)**

---

## Lote 5 — Frontend

- ☐ **T5.1** Formulario de creación de comisionista en `admin-dashboard`
  - Artefactos: `frontend/src/app/admin/admin-dashboard.component.ts`, `.html`
  - Funcionalidad: sección "Usuarios" o "Comisionistas"; botón "Nuevo Comisionista" abre modal con campos: correo, nombre completo, contraseña, teléfono (opcional); Reactive Form con validadores síncronos; submit llama `POST /api/admin/comisionistas`; manejo de 409 con mensaje de error; manejo de 400 con errores de campo.
  - Verificación: `ng build --configuration production` sin errores TypeScript.

**← HITO 5 — Frontend compila y formulario funcional (validación humana)**

---

## Lote 6 — Cierre

- ☐ **T6.1** Verificación DoD end-to-end
  - Flujo completo: login admin → crear comisionista → intentar login con las credenciales del comisionista (debe llegar al paso MFA).
  - Verificar en BD que el comisionista aparece en la tabla `comisionista` con el `id` correcto.
  - Intentar crear el mismo correo de nuevo → verificar que retorna 409.

- ☐ **T6.2** PR abierto con checklist DoD, pipeline verde
  - Título del PR: `feat(administracion): HU-36 crear cuenta de comisionista`
  - `mvn test` sin fallos; `ng build` sin errores.

**← HITO FINAL — Entrega HU-36**
