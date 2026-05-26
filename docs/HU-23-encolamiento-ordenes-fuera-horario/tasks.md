# Tasks — HU-23: Encolamiento de órdenes fuera de horario de mercado

---

## Leyenda

| Símbolo | Significado |
|---|---|
| ✅ | Completada |
| 🔄 | En progreso |
| ⬜ | Pendiente |
| ❌ | Bloqueada |

---

## Bloque 1 — Infraestructura: habilitar scheduling

| # | Tarea | Estado | Archivo(s) afectado(s) | Notas |
|---|---|:-:|---|---|
| T1.1 | Verificar que `@EnableScheduling` está en `AccionesElBosqueApplication` | ✅ | `AccionesElBosqueApplication.java` | Añadir si falta |
| T1.2 | Verificar propiedad `app.mercado.sandbox-siempre-abierto` en `application.properties` | ✅ | `src/main/resources/application.properties` | `true` para dev, `false` para prod |

---

## Bloque 2 — Lógica de encolamiento en OrdenService

| # | Tarea | Estado | Archivo(s) afectado(s) | Notas |
|---|---|:-:|---|---|
| T2.1 | En `OrdenService.crearOrden`: detectar si el mercado está abierto usando `IVerificacionMercado` | ✅ | `ordenes/service/OrdenService.java` | Usar `detectarMercado(simbolo)` + `esMercadoAbierto(mercado)` |
| T2.2 | Si mercado cerrado: asignar `orden.estado = EN_COLA` | ✅ | `ordenes/service/OrdenService.java` | |
| T2.3 | Si mercado cerrado: no llamar a `AlpacaAdapter`; reservar fondos igualmente | ✅ | `ordenes/service/OrdenService.java` | Los fondos se reservan al crear la orden, independientemente del estado |
| T2.4 | Emitir evento `ORDEN_EN_COLA` en auditoría cuando se encola | ✅ | `ordenes/service/OrdenService.java` | Incluir `simbolo`, `cantidad`, `tipoOrden` en el mensaje |

---

## Bloque 3 — ColaOrdenesService: job programado

| # | Tarea | Estado | Archivo(s) afectado(s) | Notas |
|---|---|:-:|---|---|
| T3.1 | Crear/verificar clase `ColaOrdenesService` en `ordenes/service/` | ✅ | `ordenes/service/ColaOrdenesService.java` | Anotada con `@Service` |
| T3.2 | Implementar `procesarColaAlAbrirMercado()` con `@Scheduled(fixedDelay = 60000)` | ✅ | `ordenes/service/ColaOrdenesService.java` | `fixedDelay` = 60 segundos entre ejecuciones |
| T3.3 | Verificar apertura del mercado NYSE/NASDAQ antes de procesar | ✅ | `ordenes/service/ColaOrdenesService.java` | Inyectar `IVerificacionMercado`; si cerrado → retornar sin hacer nada |
| T3.4 | Verificar que hoy no es feriado antes de procesar | ✅ | `ordenes/service/ColaOrdenesService.java` | Inyectar `IAdministracion`; llamar `esFeriadoMercado(LocalDate.now(), "NYSE")` |
| T3.5 | Obtener todas las órdenes con `estado = EN_COLA` del repository | ✅ | `ordenes/service/ColaOrdenesService.java` | `OrdenRepository.findByEstado(EstadoOrden.EN_COLA)` |

---

## Bloque 4 — ColaOrdenesService: procesamiento por orden

| # | Tarea | Estado | Archivo(s) afectado(s) | Notas |
|---|---|:-:|---|---|
| T4.1 | Verificar que el usuario de la orden sigue activo (`IConsultaInversionista`) | ✅ | `ordenes/service/ColaOrdenesService.java` | Si suspendido/eliminado: cancelar orden + liberar fondos + audit `ORDEN_CANCELADA` |
| T4.2 | Enviar notificación de apertura de mercado al inversionista (`INotificacion`) | ✅ | `ordenes/service/ColaOrdenesService.java` | Notificar una sola vez por inversionista aunque tenga varias órdenes en cola |
| T4.3 | Para símbolo US (sin punto): llamar `AlpacaAdapter.enviarOrden(...)` | ✅ | `ordenes/service/ColaOrdenesService.java` | Si éxito: `orden.estado = ENVIADA`, guardar `alpacaOrderId`, audit `ORDEN_ENVIADA_ALPACA` |
| T4.4 | Si `AlpacaAdapter` falla: dejar orden en `EN_COLA`, audit `ORDEN_FALLO_ALPACA`, continuar con la siguiente | ✅ | `ordenes/service/ColaOrdenesService.java` | Tolerancia a fallos individuales; reintento automático en el próximo ciclo de 60s |
| T4.5 | Para símbolo global (con punto): ejecutar internamente al precio de caché | ✅ | `ordenes/service/ColaOrdenesService.java` | Actualizar holding, liberar fondos reservados, `orden.estado = EJECUTADA`, audit `ORDEN_EJECUTADA` |

---

## Bloque 5 — Repository

| # | Tarea | Estado | Archivo(s) afectado(s) | Notas |
|---|---|:-:|---|---|
| T5.1 | Añadir/verificar `findByEstado(EstadoOrden estado)` en `OrdenRepository` | ✅ | `ordenes/repository/OrdenRepository.java` | Spring Data JPA genera la query automáticamente |

---

## Bloque 6 — Auditoría

| # | Tarea | Estado | Archivo(s) afectado(s) | Notas |
|---|---|:-:|---|---|
| T6.1 | Verificar que `TipoEvento` incluye: `ORDEN_EN_COLA`, `ORDEN_ENVIADA_ALPACA`, `ORDEN_EJECUTADA`, `ORDEN_RECHAZADA_FONDOS`, `ORDEN_FALLO_ALPACA` | ✅ | `trazabilidad/model/TipoEvento.java` o equivalente | Añadir los que falten |
| T6.2 | Emitir eventos correctos en cada rama del procesamiento de cola | ✅ | `ordenes/service/ColaOrdenesService.java` | Ver tabla de eventos en SPEC |

---

## Bloque 7 — Pruebas manuales

| # | Tarea | Estado | Herramienta | Escenario |
|---|---|:-:|---|---|
| T7.1 | Configurar `sandbox-siempre-abierto=false`, crear orden → verificar estado `EN_COLA` | ✅ | Postman | Confirmar en BD y GET /activas |
| T7.2 | Cambiar a `sandbox-siempre-abierto=true`, esperar 60s → verificar orden procesada | ✅ | Postman + logs | Estado debe cambiar a ENVIADA o EJECUTADA |
| T7.3 | Crear orden EN_COLA, luego cancelarla (HU-21), verificar que no se procesa | ✅ | Postman | Estado debe ser CANCELADA antes de que el job corra |
| T7.4 | Crear feriado para hoy (HU-34), verificar que la cola no se procesa | ✅ | Postman + logs | |
| T7.5 | Verificar `audit.log` con todos los eventos del procesamiento | ✅ | `logs/audit.log` | |
| T7.6 | Verificar que el inversionista recibe email de "mercado abierto" | ✅ | Email / logs | Solo si `app.notificaciones.email.habilitado=true` |

---

## Bloque 8 — Documentación y cierre

| # | Tarea | Estado | Archivo(s) afectado(s) | Notas |
|---|---|:-:|---|---|
| T8.1 | Marcar HU-23 como ✅ en `docs/PROGRESO.md` | ✅ | `docs/PROGRESO.md` | |
| T8.2 | Crear `docs/HU-23-encolamiento-ordenes-fuera-horario/plan.md` y `tasks.md` | ✅ | Este archivo | |

---

## Notas de implementación

- La notificación de apertura (T4.2) debe deduplicarse por inversionista: si el mismo usuario tiene 3 órdenes EN_COLA, enviar solo 1 notificación. Usar un `Set<Long>` de inversionistas ya notificados dentro del ciclo de procesamiento.
- El procesamiento de símbolos globales (T4.5) usa el precio de caché de `MercadoService`. Si el precio de caché es 0 o nulo, cancelar la orden con audit `ORDEN_RECHAZADA_FONDOS` en lugar de ejecutar a precio 0.
- La lógica de encolamiento en `OrdenService` (Bloque 2) y el procesamiento en `ColaOrdenesService` (Bloque 3-4) deben estar marcados con `@Transactional` para garantizar atomicidad en cada orden individual.
