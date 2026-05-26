# tasks.md — HU-32 Firmar y enviar orden aprobada al mercado

> Descomposición del plan.md aprobado (SDD Paso 3).
> Rama: `feat/HU-32-firmar-enviar-orden-aprobada`.
> Leyenda: ☐ pendiente · ◐ en progreso · ☑ hecho · ✗ cancelada

---

## Lote 1 — Modelo y repositorio

- ☐ **T1.1** Verificar entidad `PropuestaOrden` y agregar campos faltantes
  - Artefactos: `backend/src/main/java/co/edu/unbosque/accioneselbosque/ordenes/model/PropuestaOrden.java`
  - Verificación: confirmar que existen `firmadaEn` (Timestamp) y `ordenId` (Long); si no, agregarlos con las anotaciones JPA correspondientes (`@Column(name = "firmada_en")`, `@Column(name = "orden_id")`).

- ☐ **T1.2** Agregar métodos de consulta a `PropuestaOrdenRepository`
  - Artefactos: `backend/src/main/java/co/edu/unbosque/accioneselbosque/ordenes/repository/PropuestaOrdenRepository.java`
  - Nuevos métodos:
    - `List<PropuestaOrden> findByComisionistaIdAndEstado(Long comisionistaId, EstadoPropuesta estado)`
    - `Optional<PropuestaOrden> findByIdAndComisionistaId(Long id, Long comisionistaId)`
  - Verificación: `mvn test -Dtest=PropuestaOrdenRepositoryTest` (si existe) o revisar compilación con `mvn compile`.

**← HITO 1 — Modelo y repositorio compilados (`mvn compile` sin errores) (validación humana)**

---

## Lote 2 — Lógica de negocio en `OrdenService`

- ☐ **T2.1** Implementar `OrdenService.firmarYEnviarPropuesta(Long propuestaId, Long comisionistaId)`
  - Artefactos: `backend/src/main/java/co/edu/unbosque/accioneselbosque/ordenes/service/OrdenService.java`
  - Pasos de implementación (sin código):
    1. Buscar propuesta por `id`; si no → lanzar `OrdenNoEncontradaException` (404).
    2. Validar `propuesta.comisionistaId == comisionistaId`; si no → lanzar `AccessDeniedException` (403).
    3. Validar `propuesta.estado == APROBADA`; si no → lanzar `IllegalStateException` (400).
    4. Obtener precio actual: `precioCache` JOIN `activo` por `propuesta.activoId`.
    5. Recalcular montos usando `IGestorParametros.obtenerPorcentajeComision()`.
    6. Validar fondos del cliente; si no → `FondosInsuficientesException` (402) + auditoría `ORDEN_RECHAZADA_FONDOS`.
    7. Reservar fondos en `CuentaFondos` del cliente.
    8. Crear registro `Orden` con montos recalculados.
    9. Actualizar `propuesta.firmadaEn = now()` y `propuesta.ordenId = orden.id`.
    10. Determinar destino con `IVerificacionMercado.esMercadoAbierto(mercado)`.
    11. Si abierto + símbolo US: llamar `IIntegracionAlpaca.enviarOrden(...)` → estado `ENVIADA`.
    12. Si mercado cerrado: estado `EN_COLA`.
    13. Registrar auditoría `PROPUESTA_ORDEN_FIRMADA` + evento de estado.
  - Anotación: `@Transactional` en el método para atomicidad.
  - Verificación: `mvn compile` sin errores.

- ☐ **T2.2** Implementar `OrdenService.listarPropuestasAprobadas(Long comisionistaId)`
  - Artefactos: `backend/src/main/java/co/edu/unbosque/accioneselbosque/ordenes/service/OrdenService.java`
  - Lógica: consultar `PropuestaOrdenRepository.findByComisionistaIdAndEstado(comisionistaId, APROBADA)` y mapear a `List<OrdenDTO>`.
  - Verificación: `mvn compile` sin errores.

**← HITO 2 — Lógica de service compilada sin errores (validación humana)**

---

## Lote 3 — Controller

- ☐ **T3.1** Agregar endpoints a `ComisionistaController`
  - Artefactos: `backend/src/main/java/co/edu/unbosque/accioneselbosque/ordenes/controller/ComisionistaController.java`
  - Nuevos métodos:
    - `GET /api/comisionista/propuestas/aprobadas` → delega a `OrdenService.listarPropuestasAprobadas(comisionistaId)`.
    - `POST /api/comisionista/propuestas/{propuestaId}/firmar-enviar` → delega a `OrdenService.firmarYEnviarPropuesta(propuestaId, comisionistaId)`.
  - Extraer `comisionistaId` del JWT mediante `IControlAcceso.extraerUsuarioId(token)`.
  - Sin lógica de negocio en el controller.
  - Verificación: `mvn compile` y arranque de la aplicación: `curl -s http://localhost:8080/api/health` debe retornar 200.

**← HITO 3 — Aplicación arranca con los nuevos endpoints registrados (validación humana)**

---

## Lote 4 — Tests

- ☐ **T4.1** Tests unitarios de `OrdenService` (método `firmarYEnviarPropuesta`)
  - Artefactos: `backend/src/test/java/co/edu/unbosque/accioneselbosque/ordenes/service/OrdenServiceFirmarTest.java`
  - Casos requeridos:
    - `firmarYEnviar_propuestaAprobada_mercadoAbierto_creaOrdenEnviada`
    - `firmarYEnviar_mercadoCerrado_creaOrdenEnCola`
    - `firmarYEnviar_fondosInsuficientes_lanza402`
    - `firmarYEnviar_propuestaOtroComisionista_lanza403`
    - `firmarYEnviar_estadoIncorrecto_lanza400`
    - `firmarYEnviar_alpacaFalla_liberaFondosYRetorna502`
  - Verificación: `mvn test -Dtest=OrdenServiceFirmarTest` — todos en verde.

- ☐ **T4.2** Tests de integración de `ComisionistaController`
  - Artefactos: `backend/src/test/java/co/edu/unbosque/accioneselbosque/ordenes/controller/ComisionistaControllerIntegrationTest.java`
  - Casos requeridos: GET retorna 200 con lista; POST con propuesta válida retorna 200; sin JWT retorna 401; JWT de INVERSIONISTA retorna 403.
  - Verificación: `mvn test -Dtest=ComisionistaControllerIntegrationTest` — todos en verde.

**← HITO 4 — Suite de tests verde (`mvn test` sin fallos en los test de HU-32) (validación humana)**

---

## Lote 5 — Frontend

- ☐ **T5.1** Agregar sección "Propuestas Aprobadas" al dashboard del comisionista
  - Artefactos: `frontend/src/app/comisionista/comisionista-dashboard.component.ts`, `.html`
  - Funcionalidad: llamada a `GET /api/comisionista/propuestas/aprobadas` al cargar la sección; tabla con columnas: símbolo, tipo, lado, cantidad, precio actual, monto estimado, cliente.

- ☐ **T5.2** Implementar modal de confirmación de firma
  - Artefactos: `frontend/src/app/comisionista/comisionista-dashboard.component.html`, `.ts`
  - Funcionalidad: al hacer click en "Firmar y Enviar", mostrar modal con resumen de la orden (precio recalculado, monto total, comisión); al confirmar, llamar `POST /api/comisionista/propuestas/{id}/firmar-enviar`; mostrar toast con estado resultante (ENVIADA / EN_COLA); la propuesta desaparece de la lista.
  - Verificación: `ng build --configuration production` sin errores de compilación TypeScript.

**← HITO 5 — Frontend compila y flujo visual funcional (validación humana)**

---

## Lote 6 — Cierre

- ☐ **T6.1** Verificación DoD end-to-end
  - Flujo completo: login comisionista → ver propuestas aprobadas → firmar → confirmar estado ENVIADA o EN_COLA en `/api/ordenes/activas` del inversionista.
  - Verificar en BD:
    ```sql
    SELECT firmada_en, orden_id FROM propuesta_orden WHERE id = :id;
    -- Debe retornar firmada_en IS NOT NULL y orden_id IS NOT NULL

    SELECT estado, monto_base, alpaca_order_id FROM orden WHERE id = :ordenId;
    -- Debe retornar estado = 'ENVIADA' | 'EN_COLA' | 'EJECUTADA'

    SELECT saldo_disponible, fondos_reservados FROM cuenta_fondos WHERE inversionista_id = :id;
    -- fondos_reservados debe haber aumentado en monto_base + monto_comision
    ```
  - Verificar en logs: `grep "PROPUESTA_ORDEN_FIRMADA" logs/audit.log`

- ☐ **T6.2** PR abierto con checklist DoD, pipeline verde
  - Título del PR: `feat(ordenes): HU-32 firmar y enviar orden aprobada al mercado`
  - Checklist del DoD en la descripción del PR.
  - `mvn test` completo sin fallos.
  - `ng build` sin errores.

**← HITO FINAL — Entrega HU-32**
