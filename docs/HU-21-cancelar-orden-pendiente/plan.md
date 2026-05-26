# Plan de Implementación — HU-21: Cancelación de orden pendiente

---

## Resumen ejecutivo

HU-21 implementa el endpoint `DELETE /api/ordenes/{ordenId}` que permite a un inversionista cancelar una orden en estado `PENDIENTE`, `ENVIADA` o `EN_COLA`. Si la orden estaba en Alpaca (`ENVIADA`), se cancela también en el broker externo. Los fondos reservados por una orden de compra se devuelven al saldo disponible. El evento `ORDEN_CANCELADA` queda registrado en auditoría.

---

## Alcance

| Incluido | Excluido |
|---|---|
| `DELETE /api/ordenes/{ordenId}` | Cancelación masiva de órdenes |
| Validación de propiedad (orden pertenece al inversionista del JWT) | Cancelación iniciada por el sistema (por cuenta suspendida — cubierto en HU-23) |
| Cancelación en Alpaca si `estado == ENVIADA` | Cancelación de órdenes EJECUTADAS (flujo de reversión — fuera de MVP) |
| Liberación de fondos reservados (órdenes de compra) | |
| Auditoría `ORDEN_CANCELADA` | |

---

## Decisiones técnicas

| Decisión | Justificación |
|---|---|
| `DELETE /api/ordenes/{ordenId}` | Semántica REST correcta para eliminar/cancelar un recurso |
| Verificar propiedad antes de estado | Evitar revelar información sobre órdenes ajenas (403 > 400 en orden de validaciones) |
| Llamada a Alpaca solo si `estado == ENVIADA` | Las órdenes `PENDIENTE` y `EN_COLA` no se enviaron a Alpaca; intentar cancelarlas allí produciría un error innecesario |
| Liberación de fondos en `OrdenService`, no en callback de Alpaca | La liberación es inmediata al cancelar localmente; no depender de webhook de Alpaca |

---

## Módulos involucrados

| Módulo | Componente | Cambio |
|---|---|---|
| `ordenes` | `OrdenController` | `@DeleteMapping("/{ordenId}")` que llama a `OrdenService.cancelarOrden` |
| `ordenes` | `OrdenService` | Lógica de cancelación: lookup, ownership, estado válido, Alpaca si aplica, liberar fondos |
| `ordenes` | `SaldoService` | `liberarFondosReservados(inversionistaId, monto)` |
| `integracion` | `AlpacaAdapter` | `cancelarOrden(alpacaOrderId)` — llamada `DELETE` a Alpaca Broker API |
| `trazabilidad` | `AuditLogService` | `ORDEN_CANCELADA` |

---

## Flujo de implementación

```
OrdenController.cancelarOrden(ordenId, JWT)
  └─ OrdenService.cancelarOrden(ordenId, correo)
       ├─ OrdenRepository.findById(ordenId) → 404 si no existe
       ├─ orden.inversionistaId == usuario.id → 403 si no coincide
       ├─ orden.estado IN (PENDIENTE, ENVIADA, EN_COLA) → 400 si no
       ├─ si ENVIADA: AlpacaAdapter.cancelarOrden(alpacaOrderId) → tolerante a fallo
       ├─ orden.estado = CANCELADA → save
       ├─ si COMPRA: SaldoService.liberarFondosReservados(totalADebitar)
       └─ IAuditLog.registrar(ORDEN_CANCELADA, correo, "Orden {id} cancelada")
```

---

## Dependencias

- HU-17 a HU-20 completadas (las órdenes a cancelar deben existir).
- `AlpacaAdapter` implementa `cancelarOrden(String alpacaOrderId)` mediante `DELETE https://broker-api.sandbox.alpaca.markets/v1/trading/accounts/{accountId}/orders/{alpacaOrderId}`.
- `SaldoService` tiene método para liberar fondos reservados.

---

## Criterios de aceptación (resumen ejecutivo)

1. `DELETE /api/ordenes/42` retorna 200 y la orden queda en `CANCELADA`.
2. Si era de compra: `fondos_reservados` disminuye, `saldo_disponible` aumenta.
3. Orden `EJECUTADA` o `CANCELADA` retorna 400.
4. Orden de otro usuario retorna 403.
5. Orden inexistente retorna 404.
6. `ORDEN_CANCELADA` en `audit.log`.

---

## Estrategia de pruebas

| Tipo | Herramienta | Escenario clave |
|---|---|---|
| Integración manual | Postman | DELETE orden EN_COLA → 200, fondos liberados |
| Integración manual | Postman | DELETE orden EJECUTADA → 400 |
| Integración manual | Postman | DELETE orden de otro usuario → 403 |
| Integración manual | Postman | DELETE orden inexistente → 404 |
| Trazabilidad | `logs/audit.log` | `ORDEN_CANCELADA` registrado |

---

## Estimación

| Tarea | Puntos de historia |
|---|---|
| `DELETE` en controller + service (lookup + ownership + estado) | 2 |
| Cancelación en Alpaca (AlpacaAdapter.cancelarOrden) | 2 |
| Liberación de fondos en SaldoService | 1 |
| Auditoría + pruebas manuales | 1 |
| **Total** | **6** |

---

## Estado

- [x] Implementación completada
- [x] Pruebas manuales pasadas
- [x] `PROGRESO.md` actualizado
