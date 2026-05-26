# Plan de implementación — HU-17: Colocación de orden Market

## Objetivo

Permitir al inversionista autenticado colocar órdenes Market de compra o venta que se ejecuten al precio actual del mercado, con previsualización de comisión antes de confirmar (EC-13), reserva de fondos, envío a Alpaca y registro de trazabilidad completo.

---

## Módulos involucrados

| Módulo | Componentes |
|---|---|
| `ordenes` | `OrdenController`, `OrdenService`, `OrdenRepository`, `SaldoService` |
| `administracion` | `IGestorParametros` (porcentaje comisión y splits) |
| `integracion` | `AlpacaAdapter` (envío de orden para símbolos US) |
| `trazabilidad` | `IAuditLog` / `AuditLogService` |
| `autenticacion` | `IConsultaInversionista` (validar estado de cuenta, alpacaAccountId) |

---

## Estrategia general

### Fase 1 — Previsualización (EC-13)
El usuario primero envía `POST /api/ordenes/previsualizar` para ver el desglose de comisión sin crear ninguna orden en BD. El frontend muestra el modal de confirmación con los valores calculados.

### Fase 2 — Confirmación
El usuario confirma y el frontend envía `POST /api/ordenes`. El servicio ejecuta la lógica completa: validación, cálculo de comisión, reserva de fondos, persistencia de la orden, envío a Alpaca (si aplica) y auditoría.

---

## Flujo principal detallado

```
POST /api/ordenes
  → OrdenController.crearOrden(dto, principal)
    → OrdenService.crearOrden(dto, correo)
      1. IConsultaInversionista.validarPuedeOperar(correo)   // estado_cuenta activo
      2. MercadoService.obtenerCotizacion(simbolo)            // precio actual
      3. IGestorParametros.obtenerPorcentajeComision()        // 2% default
      4. calcularComision(precio, cantidad, lado, porcentaje, split)
      5. si COMPRA: SaldoService.verificarFondos(id, totalADebitar)  → 402 si insuficiente
         si VENTA:  HoldingRepository.verificarHolding(id, activoId, cantidad)  → 422 si insuf.
      6. orden = new Orden(..., estado=PENDIENTE)
         OrdenRepository.save(orden)
      7. si COMPRA: SaldoService.reservarFondos(id, totalADebitar)
      8. IAuditLog.registrar(ORDEN_PENDIENTE, correo, "Orden MARKET id=" + orden.id)
      9. si esMercadoAbierto(mercado):
           si símbolo US (sin punto):
             AlpacaAdapter.crearOrden(alpacaAccountId, dto, tipo="market")
             orden.estado = ENVIADA; orden.alpacaOrderId = alpacaResponse.id
             IAuditLog.registrar(ORDEN_ENVIADA_ALPACA, correo, ...)
           si símbolo global (con punto):
             ejecutarInternamente(orden, precioActual)
             orden.estado = EJECUTADA; actualizar holding; liberar fondos reservados; abonar venta
             IAuditLog.registrar(ORDEN_EJECUTADA, correo, ...)
         si mercado cerrado:
           orden.estado = EN_COLA
           IAuditLog.registrar(ORDEN_EN_COLA, correo, ...)
      10. OrdenRepository.save(orden)  // actualizar estado
      11. INotificacion.notificarOrdenCreada(usuario, orden)
  ← 201 Created con OrdenDTO
```

---

## Cálculo de comisión

```
montoBase          = precioEstimado × cantidad
montoComision      = montoBase × (porcentajeComision / 100)
montoPlatforma     = montoComision × splitPlataforma      // 60% default
montoComisionista  = montoComision × splitComisionista    // 40% si tiene comisionista, sino 0%
totalADebitar      = montoBase + montoComision             // COMPRA
totalARecibir      = montoBase - montoComision             // VENTA
```

---

## Gestión de fondos

| Evento | Efecto en `cuenta_fondos` |
|---|---|
| Orden COMPRA creada | `saldo_disponible -= totalADebitar`; `fondos_reservados += totalADebitar` |
| Orden EJECUTADA (compra) | `fondos_reservados -= totalADebitar` (fondos ya consumidos) |
| Orden EJECUTADA (venta) | `saldo_disponible += totalARecibir` |
| Orden CANCELADA | `fondos_reservados -= totalADebitar`; `saldo_disponible += totalADebitar` |

---

## Gestión de holdings

| Evento | Efecto en `holding` |
|---|---|
| Orden COMPRA ejecutada | `holding.cantidad += cantidad`; recalcular `precio_promedio_compra` |
| Orden VENTA ejecutada | `holding.cantidad -= cantidad`; si `cantidad = 0`, mantener registro (no borrar) |

---

## Decisiones técnicas

- **activoId en lugar de símbolo en el request:** el DTO usa `activoId` (FK al catálogo `activo`); el ticker se resuelve internamente. Esto garantiza unicidad y evita ambigüedades entre proveedores.
- **Snapshot del parámetro de comisión:** `orden.parametro_comision_id` guarda el ID del parámetro vigente al crear la orden (trazabilidad ante cambios futuros de comisión).
- **Fallo de Alpaca:** si `AlpacaAdapter.crearOrden` lanza excepción, la orden queda en estado `PENDIENTE` (no se revierte el reservado de fondos); el usuario puede cancelarla manualmente (HU-21).

---

## Escenarios de calidad cubiertos

| EC | Táctica | Implementación |
|---|---|---|
| EC-13 | Previsualizar antes de confirmar | `POST /api/ordenes/previsualizar` sin persistencia |
| EC-12 | Audit Trail | Eventos ORDEN_PENDIENTE, ORDEN_ENVIADA_ALPACA, ORDEN_EN_COLA, ORDEN_EJECUTADA |

---

## Dependencias previas

- `Activo` entity y `ActivoRepository` con `findByTicker`.
- `IGestorParametros` implementado con `obtenerPorcentajeComision()` y splits.
- `IConsultaInversionista.validarPuedeOperar(correo)` implementado.
- `AlpacaAdapter.crearOrden(accountId, simbolo, tipo, lado, cantidad, precioLimite, precioStop)` disponible.
- `IAuditLog` y `INotificacion` inyectados en `OrdenService`.
- `MercadoService.esMercadoAbierto(mercado)` implementado.

---

## Criterios de aceptación resumidos

- POST /api/ordenes/previsualizar → 200 con desglose de comisión, sin crear orden.
- POST /api/ordenes MARKET/COMPRA → 201; orden ENVIADA (mercado abierto US); fondos reservados.
- POST /api/ordenes MARKET/VENTA → 201; holding verificado; si sin holding → 422.
- Fondos insuficientes → 402.
- Mercado cerrado → orden EN_COLA.
- Eventos de auditoría en cada transición de estado.
- Sin JWT → 401; cuenta suspendida → 403.
