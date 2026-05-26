# tasks.md — SOPORTE Health Check del Servicio
> Descomposición del plan.md aprobado (SDD Paso 3).
> Rama: `feat/SOPORTE-health-check-servicio`.
Leyenda: ☐ pendiente · ◐ en progreso · ☑ hecho · ✗ cancelada

---

## Lote 1 — Verificación de componentes existentes

- ☐ **T1.1** Verificar que `HealthController` existe en `shared/config/HealthController.java` y que el método `GET /api/health` retorna texto simple o JSON mínimo con HTTP 200.
  - Artefactos: `backend/src/main/java/co/edu/unbosque/accioneselbosque/shared/config/HealthController.java`
  - Verificación: `mvn compile -pl backend` sin errores. `curl http://localhost:8080/api/health` (con backend levantado) retorna 200.

- ☐ **T1.2** Verificar que `SecurityConfig` en `autenticacion/security/SecurityConfig.java` incluye `/api/health` en la lista de rutas permitidas sin autenticación (`permitAll()`).
  - Artefactos: `autenticacion/security/SecurityConfig.java`
  - Verificación: `curl http://localhost:8080/api/health` sin header `Authorization` retorna 200 (no 401).

**← HITO 1 — Componentes verificados (validación humana)**

---

## Lote 2 — Test de integración

- ☐ **T2.1** Escribir test de integración `HealthCheckIntegrationTest` con `MockMvc`:
  - `GET /api/health` sin JWT → 200 con body no vacío.
  - `GET /api/health` con JWT inválido → 200 (el endpoint no bloquea aunque el token sea inválido).
  - Verificar que el cuerpo no contiene información sensible (no contraseñas, no tokens, no detalles de BD).
  - Artefactos: `backend/src/test/java/.../shared/config/HealthCheckIntegrationTest.java`
  - Verificación: `mvn test -pl backend -Dtest=HealthCheckIntegrationTest` — todos en verde.

**← HITO 2 — Test de integración en verde (validación humana)**

---

## Lote 3 — Cierre y documentación de deuda técnica

- ☐ **T3.1** Documentar en `docs/PROGRESO.md` que el health check actual es ping/echo básico y que EC-06 (detección de fallo de servicios + alerta al admin) requiere implementación adicional en HU-42 (`HealthMonitorService` con `@Scheduled` en módulo Trazabilidad).
  - Artefactos: `docs/PROGRESO.md`
  - Verificación: nota presente con referencia a EC-06 y HU-42.

- ☐ **T3.2** Verificación DoD end-to-end: revisar todos los ítems del §10 del plan.md.
  - Artefactos: `docs/SOPORTE-health-check-servicio/plan.md`
  - Verificación: todos los checks del DoD marcados.

- ☐ **T3.3** PR abierto con checklist DoD, pipeline verde.
  - Artefactos: Pull Request en repositorio.
  - Verificación: CI/build sin errores, al menos 1 revisor asignado.

**← HITO FINAL — Entrega SOPORTE-health-check-servicio**
