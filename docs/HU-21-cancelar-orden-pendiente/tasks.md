# Tasks — HU-21: Cancelación de orden pendiente

---

## Leyenda

| Símbolo | Significado |
|---|---|
| ✅ | Completada |
| 🔄 | En progreso |
| ⬜ | Pendiente |
| ❌ | Bloqueada |

---

## Bloque 1 — Controller

| # | Tarea | Estado | Archivo(s) afectado(s) | Notas |
|---|---|:-:|---|---|
| T1.1 | Añadir `@DeleteMapping("/{ordenId}")` en `OrdenController` | ✅ | `ordenes/controller/OrdenController.java` | Extraer `correo` del JWT con `SecurityContextHolder` o `@AuthenticationPrincipal` |
| T1.2 | Retornar `ResponseEntity<RespuestaDTO>` con 200 en éxito | ✅ | `ordenes/controller/OrdenController.java` | Mensaje: "Orden cancelada exitosamente" |

---

## Bloque 2 — Service: validaciones

| # | Tarea | Estado | Archivo(s) afectado(s) | Notas |
|---|---|:-:|---|---|
| T2.1 | `OrdenService.cancelarOrden(Long ordenId, String correo)` — lookup por ID | ✅ | `ordenes/service/OrdenService.java` | Lanzar `OrdenNoEncontradaException` → 404 si no existe |
| T2.2 | Verificar que `orden.inversionistaId == usuario.id` (resuelto desde correo) | ✅ | `ordenes/service/OrdenService.java` | Lanzar `AccesoDenegadoException` o equivalente → 403 |
| T2.3 | Verificar que `orden.estado IN (PENDIENTE, ENVIADA, EN_COLA)` | ✅ | `ordenes/service/OrdenService.java` | Lanzar `EstadoInvalidoException` → 400 |

---

## Bloque 3 — Service: cancelación en Alpaca

| # | Tarea | Estado | Archivo(s) afectado(s) | Notas |
|---|---|:-:|---|---|
| T3.1 | Si `orden.estado == ENVIADA && orden.alpacaOrderId != null`: llamar `AlpacaAdapter.cancelarOrden(alpacaOrderId)` | ✅ | `ordenes/service/OrdenService.java` | Tolerar fallo: si Alpaca falla, igual cancelar localmente y loguear |
| T3.2 | Implementar `AlpacaAdapter.cancelarOrden(String alpacaOrderId)` | ✅ | `integracion/adaptadores/alpaca/AlpacaAdapter.java` | `DELETE /v1/trading/accounts/{accountId}/orders/{orderId}` en Alpaca Broker API sandbox |

---

## Bloque 4 — Service: actualización de estado y fondos

| # | Tarea | Estado | Archivo(s) afectado(s) | Notas |
|---|---|:-:|---|---|
| T4.1 | Actualizar `orden.estado = CANCELADA` y persistir | ✅ | `ordenes/service/OrdenService.java` | `ordenRepository.save(orden)` |
| T4.2 | Si `orden.lado == COMPRA`: llamar `SaldoService.liberarFondosReservados(inversionistaId, totalADebitar)` | ✅ | `ordenes/service/OrdenService.java`, `ordenes/service/SaldoService.java` | `fondos_reservados -= totalADebitar`, `saldo_disponible += totalADebitar` |

---

## Bloque 5 — Auditoría

| # | Tarea | Estado | Archivo(s) afectado(s) | Notas |
|---|---|:-:|---|---|
| T5.1 | `IAuditLog.registrar(ORDEN_CANCELADA, correo, "Orden {ordenId} cancelada por inversionista")` | ✅ | `ordenes/service/OrdenService.java` | Llamar después de persistir el cambio de estado |

---

## Bloque 6 — Manejo de excepciones

| # | Tarea | Estado | Archivo(s) afectado(s) | Notas |
|---|---|:-:|---|---|
| T6.1 | Verificar que `GlobalExceptionHandler` maneja `OrdenNoEncontradaException` → 404 | ✅ | `shared/exception/GlobalExceptionHandler.java` | Reutilizar excepción existente si ya existe |
| T6.2 | Verificar que `GlobalExceptionHandler` maneja excepción de estado inválido → 400 | ✅ | `shared/exception/GlobalExceptionHandler.java` | |

---

## Bloque 7 — Pruebas manuales

| # | Tarea | Estado | Herramienta | Escenario |
|---|---|:-:|---|---|
| T7.1 | DELETE orden EN_COLA propia → 200, estado=CANCELADA, fondos liberados | ✅ | Postman | Verificar saldo con GET /api/portafolio/saldo |
| T7.2 | DELETE orden ENVIADA propia → 200, cancelada también en Alpaca | ✅ | Postman | Verificar en dashboard Alpaca sandbox |
| T7.3 | DELETE orden EJECUTADA → 400 con mensaje de error | ✅ | Postman | |
| T7.4 | DELETE orden de otro inversionista → 403 | ✅ | Postman | Usar JWT de usuario diferente |
| T7.5 | DELETE orden inexistente (id=9999) → 404 | ✅ | Postman | |
| T7.6 | Verificar `audit.log` contiene `ORDEN_CANCELADA` | ✅ | `logs/audit.log` | |

---

## Bloque 8 — Documentación y cierre

| # | Tarea | Estado | Archivo(s) afectado(s) | Notas |
|---|---|:-:|---|---|
| T8.1 | Marcar HU-21 como ✅ en `docs/PROGRESO.md` | ✅ | `docs/PROGRESO.md` | |
| T8.2 | Crear `docs/HU-21-cancelar-orden-pendiente/plan.md` y `tasks.md` | ✅ | Este archivo | |

---

## Notas de implementación

- La tolerancia a fallos de Alpaca en T3.1 es intencionada: si Alpaca no puede cancelar (ej. la orden ya se ejecutó en el mercado antes de nuestra solicitud), igual marcamos localmente como `CANCELADA` y registramos el fallo. El saldo se libera de todas formas para no bloquear al usuario.
- El campo `totalADebitar` en `Orden` debe haberse calculado y persistido al crear la orden (HU-17). Si no está, recalcular como `cantidad * precioEjecucion * (1 + tasaComision)`.
- La verificación de propiedad (T2.2) debe hacerse antes de la verificación de estado (T2.3) para no revelar información sobre órdenes ajenas.
