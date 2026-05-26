# Tareas de implementación — HU-14: Consulta de detalle de una acción

> Estado: Completada. Todas las tareas fueron ejecutadas durante Sprint 2.

---

## Backend

- [x] **T1 — `MercadoService.detalle(simbolo)`**
  - Orquestar tres consultas: precio actual, overview de empresa, serie histórica.
  - Si símbolo inválido (Alpha Vantage no retorna datos) → lanzar `SimboloInvalidoException`.

- [x] **T2 — `AlphaVantageAdapter.obtenerOverview(simbolo)`**
  - Llamada a Alpha Vantage `OVERVIEW` function.
  - Mapear respuesta a campos: `nombre`, `sector`, `descripcion`, `capitalizacion`.
  - Si la respuesta está vacía o es un error → retornar `null` (degradación).

- [x] **T3 — `AlphaVantageAdapter.obtenerHistorico(simbolo, dias)`**
  - Llamada a Alpha Vantage `TIME_SERIES_DAILY`.
  - Filtrar los últimos `dias` registros del JSON de respuesta.
  - Retornar `List<Map<String, Object>>` con `{"fecha": "YYYY-MM-DD", "precio": 0.0}`.
  - Si la respuesta está vacía o es rate limit → retornar lista vacía.

- [x] **T4 — `DetalleAccionDTO`**
  - Campos: `simbolo`, `nombre`, `sector`, `descripcion`, `precio`, `variacion`, `volumen`, `mercado`, `mercadoAbierto`, `historico`.
  - Todos los campos de empresa e histórico son `nullable`.

- [x] **T5 — `SimboloInvalidoException` → HTTP 400**
  - Registrar en `GlobalExceptionHandler` con mensaje `"Símbolo no válido: {simbolo}"`.

- [x] **T6 — `MercadoController.detalle(simbolo)`**
  - `GET /api/mercado/detalle/{simbolo}`.
  - Anotado `@PreAuthorize("hasRole('INVERSIONISTA')")`.
  - Captura de `SimboloInvalidoException` si no está en el handler global.

---

## Frontend Angular

- [x] **T7 — `MercadoService.getDetalle(simbolo)` en Angular**
  - `GET /api/mercado/detalle/{simbolo}` con JWT.
  - Retorna `Observable<DetalleAccionDTO>`.

- [x] **T8 — Vista de detalle de acción**
  - Panel con nombre de empresa, sector, descripción.
  - Precio actual destacado con variación (color rojo/verde).
  - Tabla o gráfico simple del histórico de precios (últimos 30 días).
  - Botón "Operar" que navega al modal de nueva orden.

- [x] **T9 — Manejo de errores en UI**
  - Si 400: mostrar "Símbolo no encontrado".
  - Si 401: redirigir a login.

---

## Pruebas

- [x] **T10 — Prueba manual: detalle de AAPL**
  - Verificar precio, variación y datos de empresa en la respuesta.

- [x] **T11 — Prueba manual: símbolo inválido**
  - `GET /api/mercado/detalle/INVALIDO_XYZ` → verificar 400 con mensaje de error.

- [x] **T12 — Prueba manual: sin JWT**
  - Verificar respuesta 401.

---

## Documentación

- [x] **T13 — Actualizar `docs/PROGRESO.md`**
  - Marcar HU-14 con ✅ en la tabla del sprint.
