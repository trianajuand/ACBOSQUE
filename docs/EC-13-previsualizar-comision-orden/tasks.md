# tasks.md — EC-13 Previsualización de Comisión Antes de Confirmar Orden
> Descomposición del plan.md aprobado (SDD Paso 3).
> Rama: `feat/EC-13-previsualizar-comision-orden`.
Leyenda: ☐ pendiente · ◐ en progreso · ☑ hecho · ✗ cancelada

---

## Lote 1 — Verificación de DTOs y contratos

- ☐ **T1.1** Verificar que `ResumenComisionDTO` en `ordenes/dto/` contiene los campos: `simbolo`, `precio` (BigDecimal), `cantidad` (int), `montoBase`, `comision`, `splitPlataforma`, `splitComisionista`, `totalADebitar`, `totalARecibir`, `mercadoAbierto` (boolean), `advertencia` (String nullable).
  - Artefactos: `ordenes/dto/ResumenComisionDTO.java`
  - Verificación: `mvn compile -pl backend` sin errores.

- ☐ **T1.2** Verificar que `CrearOrdenRequestDTO` en `ordenes/dto/` contiene `simbolo` (`@NotBlank`), `tipo` (TipoOrden, `@NotNull`), `lado` (TipoLado, `@NotNull`), `cantidad` (`@Min(1)`), `precioLimite` (BigDecimal, nullable).
  - Artefactos: `ordenes/dto/CrearOrdenRequestDTO.java`
  - Verificación: `mvn compile -pl backend` sin errores.

**← HITO 1 — DTOs verificados (validación humana)**

---

## Lote 2 — Implementación de OrdenService.previsualizarOrden

- ☐ **T2.1** Implementar / verificar `OrdenService.previsualizarOrden(CrearOrdenRequestDTO request, Long usuarioId)`:
  - Llama `IVerificacionMercado.validarSimboloOperable(simbolo)` → obtiene `CotizacionDTO` con precio.
  - Detecta mercado: `IVerificacionMercado.detectarMercado(simbolo)`.
  - Verifica horario: `IVerificacionMercado.esMercadoAbierto(mercado)`.
  - Lee comisión: `IGestorParametros.obtenerPorcentajeComision()`.
  - Lee split: `IGestorParametros.obtenerSplitPlataforma()`, `obtenerSplitComisionista()`.
  - Verifica comisionista: `IAsignacionComisionista.tieneComisionistaAsignado(usuarioId)`.
  - Calcula: `montoBase = precio × cantidad`; `comision = montoBase × porcentaje / 100` (redondeado a 2 decimales); `totalADebitar` o `totalARecibir` según lado.
  - Si mercado cerrado: setea `mercadoAbierto = false` y `advertencia = "Orden será encolada al abrir el mercado"`.
  - NO llama ningún `save()` o método de escritura.
  - Artefactos: `ordenes/service/OrdenService.java`
  - Verificación: `mvn compile -pl backend` sin errores.

- ☐ **T2.2** Escribir tests unitarios `OrdenServicePrevisualizarTest`:
  - `previsualizarOrden_compraMarket_retornaResumenConComision2Pct`.
  - `previsualizarOrden_ventaMarket_retornaResumenConTotalARecibir`.
  - `previsualizarOrden_sinComisionista_splitPlataforma100Pct`.
  - `previsualizarOrden_mercadoCerrado_retornaAdvertencia`.
  - `previsualizarOrden_simboloInvalido_lanzaSimboloInvalidoException`.
  - `previsualizarOrden_noPersisteDatos_repositoriosNoInvocados`.
  - Artefactos: `backend/src/test/java/.../ordenes/service/OrdenServicePrevisualizarTest.java`
  - Verificación: `mvn test -pl backend -Dtest=OrdenServicePrevisualizarTest` — todos en verde.

**← HITO 2 — OrdenService.previsualizarOrden implementado con tests en verde (validación humana)**

---

## Lote 3 — Controller y test de integración

- ☐ **T3.1** Verificar / completar `OrdenController`:
  - Método `POST /api/ordenes/previsualizar` protegido por JWT (cualquier rol autenticado).
  - Recibe `@Valid @RequestBody CrearOrdenRequestDTO`.
  - Extrae `usuarioId` del JWT vía `IControlAcceso.extraerUsuarioId(token)`.
  - Delega a `OrdenService.previsualizarOrden(request, usuarioId)`.
  - Retorna `ResponseEntity<ResumenComisionDTO>` 200 OK.
  - Artefactos: `ordenes/controller/OrdenController.java`
  - Verificación: `mvn compile -pl backend` sin errores.

- ☐ **T3.2** Escribir test de integración `PrevisualizarOrdenIntegrationTest` con `MockMvc`:
  - JWT válido + body válido → 200 con `ResumenComisionDTO` bien formado.
  - Sin JWT → 401.
  - Símbolo vacío → 400.
  - Cantidad = 0 → 400.
  - Artefactos: `backend/src/test/java/.../ordenes/controller/PrevisualizarOrdenIntegrationTest.java`
  - Verificación: `mvn test -pl backend -Dtest=PrevisualizarOrdenIntegrationTest` — todos en verde.

**← HITO 3 — Endpoint con tests de integración en verde (validación humana)**

---

## Lote 4 — Frontend y cierre

- ☐ **T4.1** Verificar que `DashboardComponent` en Angular:
  - Llama `POST /api/ordenes/previsualizar` al pulsar "Previsualizar".
  - Muestra el resumen devuelto (monto base, comisión, total a debitar/recibir, advertencia si mercado cerrado).
  - El botón "Confirmar Orden" solo se habilita después de recibir el resumen.
  - Artefactos: `frontend/src/app/dashboard/dashboard.component.ts`
  - Verificación: `ng serve` + prueba manual con símbolo válido y mercado abierto/cerrado.

- ☐ **T4.2** Verificación DoD end-to-end: revisar todos los ítems del §10 del plan.md.
  - Artefactos: `docs/EC-13-previsualizar-comision-orden/plan.md`
  - Verificación: todos los checks del DoD marcados.

- ☐ **T4.3** Actualizar `docs/PROGRESO.md` con nota sobre EC-13 implementado.
  - Artefactos: `docs/PROGRESO.md`
  - Verificación: `git diff docs/PROGRESO.md` muestra el cambio.

- ☐ **T4.4** PR abierto con checklist DoD, pipeline verde.
  - Artefactos: Pull Request en repositorio.
  - Verificación: CI/build sin errores, al menos 1 revisor asignado.

**← HITO FINAL — Entrega EC-13**
