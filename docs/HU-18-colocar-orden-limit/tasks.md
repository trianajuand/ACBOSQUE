# Tareas de implementación — HU-18: Colocación de orden Limit

> Estado: Completada. Las tareas incrementales sobre HU-17 fueron ejecutadas durante Sprint 3.

---

## Notas previas

HU-18 es completamente incremental sobre HU-17. Todas las entidades, repositorios, DTOs, controladores y flujo base ya están implementados. Las tareas de esta historia son exclusivamente las diferencias que introduce la orden Limit.

---

## Backend

- [x] **T1 — Validación de `precioLimite` en `OrdenService.crearOrden`**
  - Si `tipoOrden == LIMIT && (precioLimite == null || precioLimite <= 0)`:
    - Lanzar `OrdenInvalidaException("El precio límite es obligatorio para órdenes Limit")`.
    - Retornar HTTP 400.

- [x] **T2 — Precio estimado = `precioLimite` para orden Limit**
  - En `previsualizarOrden`: si `LIMIT`, usar `precioLimite` como precio base en lugar del precio de mercado.
  - En `crearOrden`: calcular comisión sobre `precioLimite × cantidad`.

- [x] **T3 — Almacenar `precio_limite` en la entidad `Orden`**
  - Campo `precioLimite` ya existe en la entidad (creado en HU-17).
  - Verificar que se persiste correctamente para tipo LIMIT.

- [x] **T4 — Envío a Alpaca con `type: "limit"`**
  - En `AlpacaAdapter.crearOrden`: si `tipoOrden == LIMIT`:
    - `body.put("type", "limit")`
    - `body.put("limit_price", precioLimite.toString())`
    - `body.put("time_in_force", "gtc")`

- [x] **T5 — Ejecución interna (símbolo global) al `precioLimite`**
  - Si el símbolo tiene punto y el mercado está abierto: ejecutar al `precioLimite` directamente.

---

## Frontend Angular

- [x] **T6 — Mostrar campo `precioLimite` en el modal de orden**
  - Condicional: visible y requerido solo cuando `tipoOrden === 'LIMIT'`.
  - Validación: número positivo mayor a 0.

- [x] **T7 — Mostrar `precioLimite` en el resumen de previsualización**
  - El campo "Precio estimado" debe mostrar `precioLimite`, no el precio de mercado.

- [x] **T8 — Manejo de error 400 por `precioLimite` ausente**
  - Mostrar mensaje: "El precio límite es obligatorio para órdenes Limit".

---

## Pruebas

- [x] **T9 — Prueba: orden Limit con `precioLimite` válido → 201**
  - Verificar que `orden.precio_limite = 185.00` en BD.
  - Verificar que la comisión se calculó sobre `185.00 × cantidad`.

- [x] **T10 — Prueba: Limit sin `precioLimite` → 400**

- [x] **T11 — Prueba: previsualización Limit usa `precioLimite` como precio estimado**
  - `precioEstimado` en `ResumenComisionDTO` debe ser igual a `precioLimite` enviado.

- [x] **T12 — Prueba: Alpaca recibe `type: "limit"` con `limit_price`**
  - Revisar los logs del adaptador o Alpaca Dashboard para confirmar el tipo de orden.

---

## Documentación

- [x] **T13 — Actualizar `docs/PROGRESO.md`**
  - Marcar HU-18 con ✅.
