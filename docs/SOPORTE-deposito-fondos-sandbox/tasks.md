# tasks.md — SOPORTE Depósito de Fondos Sandbox
> Descomposición del plan.md aprobado (SDD Paso 3).
> Rama: `feat/SOPORTE-deposito-fondos-sandbox`.
Leyenda: ☐ pendiente · ◐ en progreso · ☑ hecho · ✗ cancelada

---

## Lote 1 — Verificación de modelo y lógica de negocio

- ☐ **T1.1** Verificar que la entidad `CuentaFondos` en `ordenes/model/` tiene los campos: `inversionistaId` (Long, PK compartida), `saldoDisponible` (BigDecimal), `fondosReservados` (BigDecimal), `actualizadoEn` (LocalDateTime/ZonedDateTime).
  - Artefactos: `ordenes/model/CuentaFondos.java`
  - Verificación: `mvn compile -pl backend` sin errores.

- ☐ **T1.2** Verificar / completar `SaldoService.depositar(Long usuarioId, BigDecimal monto)`:
  - Obtiene `CuentaFondos` por `inversionistaId`; si no existe, crea con saldo 0.
  - Suma `monto` a `saldoDisponible`. No toca `fondosReservados`.
  - Persiste con `@Transactional`.
  - Artefactos: `ordenes/service/SaldoService.java`
  - Verificación: `mvn compile -pl backend` sin errores.

- ☐ **T1.3** Decidir y documentar la estrategia de restricción para producción (§9 pregunta 1): agregar en `application.properties` la propiedad `app.sandbox.deposito-habilitado=true` y documentar en comentario que debe ser `false` en producción.
  - Artefactos: `backend/src/main/resources/application.properties`, comentario en `PortafolioController.java`
  - Verificación: propiedad presente en `application.properties`.

**← HITO 1 — Modelo, servicio y guardia sandbox verificados (validación humana)**

---

## Lote 2 — Tests unitarios y de integración

- ☐ **T2.1** Escribir tests unitarios `SaldoServiceDepositoTest`:
  - `depositar_montoValido_incrementaSaldoDisponible`.
  - `depositar_cuentaNoExiste_creaYDepositaFondos`.
  - `depositar_noModificaFondosReservados`.
  - Artefactos: `backend/src/test/java/.../ordenes/service/SaldoServiceDepositoTest.java`
  - Verificación: `mvn test -pl backend -Dtest=SaldoServiceDepositoTest` — todos en verde.

- ☐ **T2.2** Escribir test de integración `DepositoSandboxIntegrationTest` con `MockMvc`:
  - JWT válido + `monto=100` → 200 con saldo actualizado.
  - Sin JWT → 401.
  - `monto=0` → 400.
  - `monto=-5` → 400.
  - `monto=abc` → 400.
  - Artefactos: `backend/src/test/java/.../ordenes/controller/DepositoSandboxIntegrationTest.java`
  - Verificación: `mvn test -pl backend -Dtest=DepositoSandboxIntegrationTest` — todos en verde.

**← HITO 2 — Tests en verde (validación humana)**

---

## Lote 3 — Frontend y cierre

- ☐ **T3.1** Verificar que `DashboardComponent` en Angular:
  - Tiene formulario con campo `monto` con validación mínimo 0.01.
  - Llama `POST /api/portafolio/depositar?monto=...` via `ApiService`.
  - Muestra resultado via `ToastService`.
  - Recarga el saldo tras depósito exitoso.
  - Artefactos: `frontend/src/app/dashboard/dashboard.component.ts`
  - Verificación: `ng serve` + prueba manual: depositar 100 → saldo aumenta en 100.

- ☐ **T3.2** Verificación DoD end-to-end: revisar todos los ítems del §10 del plan.md.
  - Artefactos: `docs/SOPORTE-deposito-fondos-sandbox/plan.md`
  - Verificación: todos los checks del DoD marcados.

- ☐ **T3.3** Actualizar `docs/PROGRESO.md` con nota de soporte sandbox documentado.
  - Artefactos: `docs/PROGRESO.md`
  - Verificación: `git diff docs/PROGRESO.md` muestra el cambio.

- ☐ **T3.4** PR abierto con checklist DoD, pipeline verde.
  - Artefactos: Pull Request en repositorio.
  - Verificación: CI/build sin errores, al menos 1 revisor asignado.

**← HITO FINAL — Entrega SOPORTE-deposito-fondos-sandbox**
