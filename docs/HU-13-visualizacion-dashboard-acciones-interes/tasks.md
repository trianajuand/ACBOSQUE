# Tareas de implementación — HU-13: Dashboard de acciones de interés

> Estado: Completada. Todas las tareas fueron ejecutadas durante Sprint 2.

---

## Backend

- [x] **T1 — Entity `PrecioCache`**
  - Mapear tabla `precio_cache` con `activo_id` como PK compartida.
  - Campos: `simbolo`, `precioActual`, `variacionPorcentual`, `volumen`, `mercado`, `fuente`, `actualizadoEn`.

- [x] **T2 — `PrecioCacheRepository`**
  - Método `findBySimboloIgnoreCase(simbolo)`.
  - Método `findAll()` para el job de refresco.

- [x] **T3 — `MercadoService.dashboard(correo)`**
  - Cargar `Inversionista.interesesMercado` (CSV → List).
  - Para cada símbolo: intentar caché → si expirada o ausente, llamar proveedor externo.
  - Construir `List<CotizacionDTO>`.

- [x] **T4 — Lógica de caché (TTL 3 min)**
  - Comparar `actualizadoEn + 3 min` con `LocalDateTime.now()`.
  - Si válida: usar caché. Si expirada: llamar API y actualizar caché.

- [x] **T5 — Detección de proveedor por símbolo**
  - Sin punto → `AlpacaAdapter.obtenerSnapshot(simbolo)` (US Market Data).
  - Con punto → `AlphaVantageAdapter.obtenerCotizacion(simbolo)`.

- [x] **T6 — Degradación cuando API falla**
  - `try/catch` en llamada externa; si excepción → usar caché aunque esté vencida.
  - Si no hay caché: retornar `CotizacionDTO` con `precio = 0.0`.

- [x] **T7 — Job `@Scheduled` de refresco cada 3 min**
  - Método en `MercadoService` o `PrecioCacheJob`.
  - Recorre todos los símbolos en `precio_cache` y los refresca.
  - Anotado con `@Scheduled(fixedDelay = 180000)`.

- [x] **T8 — `MercadoController.dashboard()`**
  - `GET /api/mercado/dashboard`.
  - Anotado `@PreAuthorize("hasRole('INVERSIONISTA')")`.
  - Extrae correo del `Authentication` y delega a `MercadoService`.

- [x] **T9 — `CotizacionDTO`**
  - Campos: `simbolo`, `precio`, `mercado`, `mercadoAbierto`, `variacionPorcentaje`.

---

## Frontend Angular

- [x] **T10 — `MercadoService.getDashboard()` en Angular**
  - `GET /api/mercado/dashboard` con JWT en header.
  - Retorna `Observable<CotizacionDTO[]>`.

- [x] **T11 — Componente dashboard: sección mercado**
  - Tabla de cotizaciones con columnas: símbolo, precio, variación, mercado, estado.
  - Indicador visual si `precio = 0` (dato desactualizado).
  - Auto-refresco cada 3 minutos con `setInterval` o `rxjs/timer`.

- [x] **T12 — Manejo de errores en UI**
  - Si la llamada retorna 401 → redirigir a login.
  - Si retorna 500 → mostrar mensaje "Error al cargar datos de mercado".

---

## Pruebas

- [x] **T13 — Prueba manual: dashboard con JWT válido**
  - Verificar que retorna cotizaciones de los intereses configurados.

- [x] **T14 — Prueba manual: degradación**
  - Configurar `alphavantage.api-key` con clave inválida y verificar que se usan datos de caché.

- [x] **T15 — Prueba manual: sin JWT**
  - Verificar respuesta 401.

---

## Documentación

- [x] **T16 — Actualizar `docs/PROGRESO.md`**
  - Marcar HU-13 con ✅ en la tabla del sprint.
