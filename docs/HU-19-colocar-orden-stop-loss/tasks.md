# Tareas de implementación — HU-19: Colocación de orden Stop Loss

> Estado: Completada. Las tareas incrementales sobre HU-17 fueron ejecutadas durante Sprint 3.

---

## Notas previas

HU-19 es completamente incremental sobre HU-17. Las tareas de esta historia son exclusivamente las diferencias que introduce el tipo `STOP_LOSS` con el campo `precioStop`.

---

## Backend

- [x] **T1 — Validación de `precioStop` en `OrdenService.crearOrden`**
  - Si `tipoOrden == STOP_LOSS && (precioStop == null || precioStop <= 0)`:
    - Lanzar `OrdenInvalidaException("El precio stop es obligatorio para órdenes Stop Loss")`.
    - Retornar HTTP 400.

- [x] **T2 — Precio estimado = `precioStop` para orden Stop Loss**
  - En `previsualizarOrden`: si `STOP_LOSS`, usar `precioStop` como precio base.
  - En `crearOrden`: calcular comisión sobre `precioStop × cantidad`.

- [x] **T3 — Almacenar `precio_stop` en la entidad `Orden`**
  - Campo `precioStop` ya existe en la entidad (creado en HU-17).
  - Verificar que se persiste correctamente para tipo STOP_LOSS.

- [x] **T4 — Envío a Alpaca con `type: "stop"`**
  - En `AlpacaAdapter.crearOrden`: si `tipoOrden == STOP_LOSS`:
    - `body.put("type", "stop")`
    - `body.put("stop_price", precioStop.toString())`
    - `body.put("time_in_force", "gtc")`

- [x] **T5 — Ejecución interna (símbolo global) al `precioStop`**
  - Si símbolo con punto y mercado abierto: ejecutar al `precioStop` directamente.

- [x] **T6 — Sin bloqueo de holding para VENTA Stop Loss**
  - A diferencia de una compra, la venta Stop Loss no reserva fondos ni bloquea el holding en BD local.
  - El holding sigue disponible hasta que Alpaca confirme la ejecución.

---

## Frontend Angular

- [x] **T7 — Mostrar campo `precioStop` en el modal de orden**
  - Condicional: visible y requerido solo cuando `tipoOrden === 'STOP_LOSS'`.
  - Validación: número positivo mayor a 0.
  - Label: "Precio de activación (Stop)".

- [x] **T8 — Mostrar `precioStop` en el resumen de previsualización**
  - El campo "Precio estimado" debe mostrar `precioStop`.

- [x] **T9 — Manejo de error 400 por `precioStop` ausente**
  - Mostrar mensaje: "El precio stop es obligatorio para órdenes Stop Loss".

- [x] **T10 — Texto informativo en el modal**
  - Tooltip o nota: "La orden se ejecutará automáticamente cuando el precio caiga hasta el nivel stop indicado."

---

## Pruebas

- [x] **T11 — Prueba: orden Stop Loss VENTA con `precioStop` válido → 201**
  - Verificar que `orden.precio_stop = 175.00` en BD.
  - Verificar que el estado es `ENVIADA` (mercado abierto).

- [x] **T12 — Prueba: Stop Loss sin `precioStop` → 400**

- [x] **T13 — Prueba: Alpaca recibe `type: "stop"` con `stop_price`**
  - Verificar en logs del adaptador o Alpaca Dashboard.

- [x] **T14 — Prueba: previsualización Stop Loss usa `precioStop` como precio estimado**

- [x] **T15 — Prueba: holding insuficiente para venta Stop Loss → 422**
  - Usuario sin holding del activo: verificar 422.

---

## Documentación

- [x] **T16 — Actualizar `docs/PROGRESO.md`**
  - Marcar HU-19 con ✅.
