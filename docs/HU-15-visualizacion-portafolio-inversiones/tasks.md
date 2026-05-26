# Tareas de implementación — HU-15: Visualización del portafolio de inversiones

> Estado: Completada. Todas las tareas fueron ejecutadas durante Sprint 2.

---

## Backend

- [x] **T1 — Entity `Holding` con PK compuesta**
  - PK: `(inversionista_id, activo_id)`.
  - Campos: `cantidad` (DECIMAL), `precioPromedioCompra` (DECIMAL).
  - FK a `Inversionista` y a `Activo`.

- [x] **T2 — `HoldingRepository`**
  - `findByInversionistaId(Long inversionistaId)` con JOIN a `Activo` para obtener el ticker.
  - `findByInversionistaIdAndActivoId(Long inversionistaId, Long activoId)`.

- [x] **T3 — `PortafolioService.portafolio(correo)`**
  - Cargar inversionista por correo.
  - Cargar holdings con `cantidad > 0`.
  - Para cada holding: buscar `precio_cache` por ticker del activo.
  - Calcular `valorMercado`, `gananciaPerdida`, `variacionPorcentaje`.
  - Sumar `valorTotalPortafolio`.
  - Mapear a `PortafolioDTO`.

- [x] **T4 — `HoldingDTO`**
  - Campos: `ticker`, `cantidad`, `precioPromedioCompra`, `precioActual`, `valorMercado`, `gananciaPerdida`, `variacionPorcentaje`, `mercado`.

- [x] **T5 — `PortafolioDTO`**
  - Campos: `valorTotalPortafolio` (Double), `holdings` (List<HoldingDTO>).

- [x] **T6 — `PortafolioController.portafolio()`**
  - `GET /api/portafolio`.
  - `@PreAuthorize("hasRole('INVERSIONISTA')")`.
  - Extrae correo del `Authentication` y delega a `PortafolioService`.

- [x] **T7 — Filtro de holdings con cantidad = 0**
  - `HoldingRepository.findByInversionistaIdAndCantidadGreaterThan(id, 0)` o filtrado en servicio.

- [x] **T8 — Precio desconocido (sin caché)**
  - Si `PrecioCacheRepository.findBySimboloIgnoreCase(ticker)` retorna `Optional.empty()`: `precioActual = 0.0`.

---

## Frontend Angular

- [x] **T9 — `PortafolioService.getPortafolio()` en Angular**
  - `GET /api/portafolio` con JWT.
  - Retorna `Observable<PortafolioDTO>`.

- [x] **T10 — Vista portafolio — modo LISTA**
  - Tabla con columnas: Ticker, Cantidad, Precio Promedio, Precio Actual, Valor, G/P, %.
  - G/P en verde si positiva, rojo si negativa.
  - Fila de total al final.

- [x] **T11 — Vista portafolio — modo GRAFICO**
  - Gráfico de barras con valor de mercado por ticker.
  - Activado si `perfil.vistaPortafolio = 'GRAFICO'`.

- [x] **T12 — Portafolio vacío**
  - Si `holdings.length === 0`: mostrar mensaje "No tienes posiciones abiertas. Comienza operando en el mercado."

- [x] **T13 — Manejo de errores en UI**
  - 401 → redirigir a login.

---

## Pruebas

- [x] **T14 — Prueba manual: portafolio con holdings**
  - Colocar una orden MARKET de compra (HU-17) y verificar que aparece en el portafolio.

- [x] **T15 — Prueba manual: portafolio vacío**
  - Usuario nuevo sin órdenes: verificar `holdings = []` y `valorTotalPortafolio = 0`.

- [x] **T16 — Prueba manual: G/P correcta**
  - Verificar que `gananciaPerdida = (precioActual - precioPromedioCompra) × cantidad`.

---

## Documentación

- [x] **T17 — Actualizar `docs/PROGRESO.md`**
  - Marcar HU-15 con ✅ en la tabla del sprint.
