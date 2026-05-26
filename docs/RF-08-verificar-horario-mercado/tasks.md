# tasks.md — RF-08 Verificación de Horario de Mercado
> Descomposición del plan.md aprobado (SDD Paso 3).
> Rama: `feat/RF-08-verificar-horario-mercado`.
Leyenda: ☐ pendiente · ◐ en progreso · ☑ hecho · ✗ cancelada

---

## Lote 1 — Configuración y lógica central

- ☐ **T1.1** Verificar que `application.properties` contiene `app.mercado.sandbox-siempre-abierto=true` (o `false` para producción). Si no existe, agregarlo con valor `true` para el entorno de desarrollo.
  - Artefactos: `backend/src/main/resources/application.properties`
  - Verificación: `grep "sandbox-siempre-abierto" application.properties` retorna la línea.

- ☐ **T1.2** Implementar / verificar `MercadoService.esMercadoAbierto(String mercado)`:
  - Si `app.mercado.sandbox-siempre-abierto=true`, retornar `true` directamente.
  - Caso NYSE/NASDAQ/US: `ZoneId.of("America/New_York")`, lunes-viernes 09:30-16:00.
  - Caso TSE: `ZoneId.of("Asia/Tokyo")`, lunes-viernes 09:00-15:00.
  - Caso LSE: `ZoneId.of("Europe/London")`, lunes-viernes 08:00-16:30.
  - Caso ASX: `ZoneId.of("Australia/Sydney")`, lunes-viernes 10:00-16:00.
  - Caso null / desconocido: retornar `false`.
  - Usar `ZonedDateTime.now(zoneId)` para la evaluación.
  - Artefactos: `mercado/service/MercadoService.java`
  - Verificación: `mvn compile -pl backend` sin errores.

- ☐ **T1.3** Implementar / verificar `MercadoService.detectarMercado(String simbolo)`:
  - `.T` al final → `"TSE"`.
  - `.L` al final → `"LSE"`.
  - `.AX` al final → `"ASX"`.
  - Sin sufijo reconocido → `"NYSE"` (US por defecto).
  - null / vacío → `"DESCONOCIDO"`.
  - Artefactos: `mercado/service/MercadoService.java`
  - Verificación: `mvn compile -pl backend` sin errores.

**← HITO 1 — Lógica central de horarios implementada (validación humana)**

---

## Lote 2 — Tests unitarios

- ☐ **T2.1** Escribir tests unitarios `MercadoServiceHorarioTest`:
  - `esMercadoAbierto_nyseEnHorario_retornaTrue`: simular `ZonedDateTime` dentro del horario NYSE (ej. lunes 10:00 ET).
  - `esMercadoAbierto_nyseFueraHorario_retornaFalse`: simular sábado o 17:00 ET.
  - `esMercadoAbierto_tseEnHorario_retornaTrue`.
  - `esMercadoAbierto_lseEnHorario_retornaTrue`.
  - `esMercadoAbierto_asxEnHorario_retornaTrue`.
  - `esMercadoAbierto_mercadoDesconocido_retornaFalse`.
  - `esMercadoAbierto_sandboxActivo_retornaSiempreTrue`.
  - `detectarMercado_simboloConSufixoT_retornaTSE`.
  - `detectarMercado_simboloSinSufijo_retornaUSNYSE`.
  - Artefactos: `backend/src/test/java/.../mercado/service/MercadoServiceHorarioTest.java`
  - Verificación: `mvn test -pl backend -Dtest=MercadoServiceHorarioTest` — todos en verde.

- ☐ **T2.2** Verificar que `OrdenService.crear()` llama `IVerificacionMercado.esMercadoAbierto()` y encola la orden (llama `ColaOrdenesService`) cuando retorna `false`.
  - Artefactos: `ordenes/service/OrdenService.java`
  - Verificación: test unitario `crearOrden_mercadoCerrado_encolaOrden` en verde.

**← HITO 2 — Tests unitarios en verde (validación humana)**

---

## Lote 3 — Endpoint diagnóstico y test de integración

- ☐ **T3.1** Implementar / verificar `MercadoController` endpoint `GET /api/mercado/horario/{mercado}`:
  - Llama `MercadoService.esMercadoAbierto(mercado)`.
  - Retorna JSON con `mercado`, `abierto` (boolean), `zonaHoraria`, `horaActualMercado`.
  - Mercado desconocido retorna 400 o respuesta con `abierto: false`.
  - Protegido por JWT.
  - Artefactos: `mercado/controller/MercadoController.java`
  - Verificación: `mvn compile -pl backend` sin errores.

- ☐ **T3.2** Escribir test de integración `MercadoHorarioIntegrationTest` con `MockMvc`:
  - JWT válido + `GET /api/mercado/horario/NYSE` → 200 con JSON bien formado.
  - Sin JWT → 401.
  - Mercado desconocido → respuesta con `abierto: false` o 400.
  - Artefactos: `backend/src/test/java/.../mercado/controller/MercadoHorarioIntegrationTest.java`
  - Verificación: `mvn test -pl backend -Dtest=MercadoHorarioIntegrationTest` — todos en verde.

**← HITO 3 — Endpoint con tests de integración en verde (validación humana)**

---

## Lote 4 — Cierre y deuda técnica documentada

- ☐ **T4.1** Documentar en `docs/PROGRESO.md` la deuda técnica pendiente: feriados de mercado no consultados en BD y horarios hardcodeados (incumplimiento parcial de EC-19).
  - Artefactos: `docs/PROGRESO.md`
  - Verificación: nota con referencias a EC-18 y EC-19 presente en PROGRESO.md.

- ☐ **T4.2** Verificación DoD end-to-end: revisar todos los ítems del §10 del plan.md.
  - Artefactos: `docs/RF-08-verificar-horario-mercado/plan.md`
  - Verificación: todos los checks del DoD marcados.

- ☐ **T4.3** PR abierto con checklist DoD, pipeline verde.
  - Artefactos: Pull Request en repositorio.
  - Verificación: CI/build sin errores, al menos 1 revisor asignado.

**← HITO FINAL — Entrega RF-08**
