# Plan de implementación — HU-14: Consulta de detalle de una acción

## Objetivo

Permitir al inversionista autenticado ver el detalle completo de un activo financiero: precio actual, información de la empresa (sector, descripción), métricas de mercado (volumen, variación) y serie histórica de precios de los últimos 30 días.

---

## Módulos involucrados

| Módulo | Componentes |
|---|---|
| `mercado` | `MercadoController`, `MercadoService` |
| `integracion` | `AlphaVantageAdapter` (overview + histórico), `AlpacaAdapter` (snapshot precio US) |

---

## Estrategia general

1. `GET /api/mercado/detalle/{simbolo}` recibe el ticker como path variable.
2. `MercadoService.detalle(simbolo)` orquesta tres consultas:
   - Precio actual: desde caché o Alpaca/Alpha Vantage.
   - Overview de empresa: sector, descripción, capitalización — Alpha Vantage `OVERVIEW`.
   - Serie histórica: precios de cierre de los últimos 30 días — Alpha Vantage `TIME_SERIES_DAILY` o caché de histórico.
3. Se construye `DetalleAccionDTO` y se responde 200.
4. Si Alpha Vantage no responde, se retornan los campos disponibles en caché; los campos sin caché quedan `null`.

---

## Flujo de datos

```
Frontend GET /api/mercado/detalle/{simbolo}
  → MercadoController.detalle(simbolo)
    → MercadoService.detalle(simbolo)
      → obtenerCotizacion(simbolo)             // precio + variación
      → AlphaVantageAdapter.obtenerOverview(simbolo)   // empresa, sector, descripción
      → AlphaVantageAdapter.obtenerHistorico(simbolo, dias=30) // serie diaria
      → construir DetalleAccionDTO
  ← 200 OK con DetalleAccionDTO
  // Si símbolo inválido → 400 SimboloInvalidoException
```

---

## Decisiones técnicas

- **Sin caché propia del histórico (v1):** el histórico de 30 días se consulta en cada llamada a Alpha Vantage. Si la cuota está agotada, se retorna `historico: null` en lugar de error.
- **Reutilización de caché de precio:** `obtenerCotizacion` utiliza `precio_cache` (ya existente de HU-13); no se duplica lógica.
- **Validación de símbolo:** si Alpha Vantage retorna `Note` o `Information` (rate limit), el servicio lanza `SimboloInvalidoException` → 400 solo si el símbolo realmente no existe. Rate limits se loguean como warning y retornan datos parciales.

---

## Escenarios de calidad cubiertos

| EC | Táctica | Implementación |
|---|---|---|
| EC-01 | Caché de precio | Reutiliza `precio_cache`; precio disponible en < 1 s si cacheado |
| EC-05 | Degradación | Si Alpha Vantage no responde: sector/descripción/histórico = null; precio de caché |

---

## Dependencias previas

- HU-13: `PrecioCache` y `obtenerCotizacion` implementados.
- `AlphaVantageAdapter.obtenerOverview(simbolo)` disponible.
- `AlphaVantageAdapter.obtenerHistorico(simbolo, dias)` disponible.
- `SimboloInvalidoException` registrada en `GlobalExceptionHandler`.

---

## Criterios de aceptación resumidos

- GET /api/mercado/detalle/AAPL → 200 con precio, empresa y serie histórica.
- Símbolo no reconocido → 400 con mensaje de error.
- Sin JWT → 401.
- Si Alpha Vantage no disponible → 200 con datos de caché; campos de empresa/histórico pueden ser null.
