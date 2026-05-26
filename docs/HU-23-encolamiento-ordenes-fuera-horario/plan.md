# Plan de Implementación — HU-23: Encolamiento de órdenes fuera de horario de mercado

---

## Resumen ejecutivo

HU-23 garantiza que las órdenes creadas fuera del horario de mercado no se pierdan. Cuando el mercado está cerrado, `OrdenService.crearOrden` persiste la orden con estado `EN_COLA` en lugar de enviarla inmediatamente a Alpaca. Un proceso programado (`ColaOrdenesService` con `@Scheduled`) verifica cada 60 segundos si el mercado está abierto; cuando abre, procesa todas las órdenes en cola: las de símbolos US se envían a Alpaca, las de símbolos globales se ejecutan internamente. Esta historia no expone endpoint REST propio.

---

## Alcance

| Incluido | Excluido |
|---|---|
| Lógica de encolamiento en `OrdenService.crearOrden` | Endpoint REST propio (HU-23 es proceso interno) |
| `ColaOrdenesService` con `@Scheduled(fixedDelay = 60000)` | Priorización de órdenes en cola (FIFO simple) |
| Procesamiento de símbolos US (envío a Alpaca al abrir) | Mecanismo de webhook de Alpaca para confirmar ejecución |
| Procesamiento de símbolos globales (ejecución interna al abrir) | Soporte para pre-market / after-hours trading |
| Verificación de estado de cuenta antes de procesar | |
| Notificación al inversionista cuando su cola se procesa | |
| Cancelación automática si cuenta suspendida al abrir el mercado | |
| Consideración de feriados de mercado (integración con `IAdministracion`) | |

---

## Decisiones técnicas

| Decisión | Justificación |
|---|---|
| `@Scheduled(fixedDelay = 60000)` en lugar de cron | `fixedDelay` garantiza 60s entre el fin de una ejecución y el inicio de la siguiente; evita solapamiento si el procesamiento tarda más de 60s |
| Procesamiento por orden individual, tolerante a fallos | Si una orden falla, continuar con las siguientes; no cancelar el lote completo |
| `EN_COLA` como estado diferente de `PENDIENTE` | Permite distinguir "esperando aprobación de usuario" (PENDIENTE) de "esperando apertura de mercado" (EN_COLA) |
| Verificación de horario usando `IVerificacionMercado` | El módulo Mercado es dueño del horario; `ordenes` no debe duplicar esa lógica |
| Símbolos globales ejecutados internamente | Alpaca solo soporta mercados US; los símbolos con punto (ej. `RIO.LON`) se ejecutan al precio de caché |

---

## Módulos involucrados

| Módulo | Componente | Cambio |
|---|---|---|
| `ordenes` | `OrdenService.crearOrden` | Si mercado cerrado → `estado = EN_COLA`, audit `ORDEN_EN_COLA` |
| `ordenes` | `ColaOrdenesService` | Nueva clase con `@Scheduled`; procesa la cola al abrir el mercado |
| `ordenes` | `OrdenRepository` | `findByEstado(EN_COLA)` |
| `mercado` | `IVerificacionMercado` | `esMercadoAbierto(String mercado)` consumido por `ColaOrdenesService` |
| `integracion` | `AlpacaAdapter` | `enviarOrden` para símbolos US encolados |
| `integracion` | `INotificacion` | `notificarAperturaMercado(correo)` cuando la cola se procesa |
| `administracion` | `IAdministracion` | `esFeriadoMercado(LocalDate, String mercado)` para no procesar en feriados |
| `trazabilidad` | `AuditLogService` | `ORDEN_EN_COLA`, `ORDEN_ENVIADA_ALPACA`, `ORDEN_EJECUTADA`, `ORDEN_RECHAZADA_FONDOS`, `ORDEN_FALLO_ALPACA` |

---

## Flujo de implementación: encolamiento

```
OrdenService.crearOrden(dto, correo)
  ├─ [validaciones existentes: fondos, holdings, activo]
  ├─ Detectar mercado del símbolo (IVerificacionMercado.detectarMercado)
  ├─ Si mercado cerrado:
  │    ├─ orden.estado = EN_COLA
  │    ├─ orden.alpacaOrderId = null
  │    ├─ IAuditLog.registrar(ORDEN_EN_COLA, correo, ...)
  │    └─ Retornar 201 con estado="EN_COLA"
  └─ Si mercado abierto: [flujo normal HU-17..20]
```

---

## Flujo de implementación: procesamiento de cola

```
ColaOrdenesService.procesarColaAlAbrirMercado() [@Scheduled]
  ├─ Si !IVerificacionMercado.esMercadoAbierto("NYSE/NASDAQ"): retornar
  ├─ Si IAdministracion.esFeriadoMercado(hoy, "NYSE"): retornar
  ├─ ordenes = OrdenRepository.findByEstado(EN_COLA)
  └─ Para cada orden:
       ├─ Verificar usuario activo (IConsultaInversionista) → si no: cancelar + liberar fondos + audit
       ├─ INotificacion.notificarAperturaConOrdenesEnCola(correo)
       ├─ Si símbolo US (sin punto):
       │    ├─ AlpacaAdapter.enviarOrden(...) → estado=ENVIADA, guardar alpacaOrderId
       │    ├─ audit ORDEN_ENVIADA_ALPACA
       │    └─ Si fallo: audit ORDEN_FALLO_ALPACA, dejar EN_COLA (reintento próximo ciclo)
       └─ Si símbolo global (con punto):
            ├─ Ejecutar al precio de caché → actualizar holding, liberar fondos
            ├─ orden.estado = EJECUTADA
            └─ audit ORDEN_EJECUTADA
```

---

## Dependencias

- `MercadoService` implementa `IVerificacionMercado.esMercadoAbierto` (implementado en sprint 2/3).
- `AdministracionService` implementa `IAdministracion` con método de feriados (HU-34).
- `AlpacaAdapter.enviarOrden` ya funciona (HU-17).
- `INotificacion` disponible (HU-41 en desarrollo — si no está, loguear en consola como fallback).
- `@EnableScheduling` habilitado en `AccionesElBosqueApplication`.

---

## Criterios de aceptación (resumen ejecutivo)

1. Orden creada con mercado cerrado → estado `EN_COLA`, fondos reservados.
2. Al abrir el mercado, `ColaOrdenesService` procesa la cola automáticamente.
3. Órdenes US en cola → estado `ENVIADA` con `alpacaOrderId`.
4. Órdenes globales en cola → estado `EJECUTADA`, holding actualizado.
5. Inversionista recibe notificación cuando su cola se procesa.
6. Si Alpaca falla al procesar una orden encolada → orden queda `EN_COLA` (reintento en 60s).
7. Si cuenta suspendida → orden cancelada, fondos liberados.
8. Feriados de mercado considerados.

---

## Estrategia de pruebas

| Tipo | Herramienta | Escenario clave |
|---|---|---|
| Integración manual | Postman + `app.mercado.sandbox-siempre-abierto=false` | POST orden fuera de horario → EN_COLA |
| Integración manual | `app.mercado.sandbox-siempre-abierto=true` luego reiniciar | Orden EN_COLA se procesa en el próximo ciclo de 60s |
| Logs | `logs/audit.log` | Verificar eventos `ORDEN_EN_COLA`, `ORDEN_ENVIADA_ALPACA` |
| Edge case | Postman | Cancelar orden EN_COLA (HU-21) → fondos liberados, no se procesa |

---

## Estimación

| Tarea | Puntos de historia |
|---|---|
| Lógica de encolamiento en OrdenService | 2 |
| `ColaOrdenesService` con @Scheduled | 3 |
| Procesamiento símbolos US (Alpaca) | 2 |
| Procesamiento símbolos globales (ejecución interna) | 2 |
| Notificaciones + auditoría | 1 |
| Manejo de cuenta inactiva al procesar | 1 |
| Pruebas manuales | 2 |
| **Total** | **13** |

---

## Estado

- [x] Implementación completada
- [x] Pruebas manuales pasadas
- [x] `PROGRESO.md` actualizado
