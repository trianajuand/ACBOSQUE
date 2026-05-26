# Plan de implementación — HU-13: Dashboard de acciones de interés

## Objetivo

Proveer al inversionista autenticado un dashboard que muestre cotizaciones actualizadas de los activos que marcó como intereses, con degradación elegante cuando la API externa no responde.

---

## Módulos involucrados

| Módulo | Componentes |
|---|---|
| `mercado` | `MercadoController`, `MercadoService`, `PrecioCache` (entity), `PrecioCacheRepository` |
| `integracion` | `AlphaVantageAdapter`, `AlpacaAdapter` (market data US) |
| `autenticacion` | `InversionistaRepository` (lectura de `intereses_mercado`) |

---

## Estrategia general

1. El endpoint `GET /api/mercado/dashboard` extrae el correo del JWT.
2. `MercadoService.dashboard(correo)` carga los intereses del inversionista y, para cada símbolo, obtiene la cotización desde caché o desde el proveedor externo apropiado.
3. Se mantiene una caché en BD (`precio_cache`) con TTL de 3 minutos; si la caché es válida se responde sin llamar a APIs externas.
4. Si la API externa falla, se devuelven los datos de caché aunque estén vencidos (degradación con indicador visual).
5. Un job `@Scheduled` refresca la caché automáticamente cada 3 minutos.

---

## Flujo de datos

```
Frontend GET /api/mercado/dashboard
  → MercadoController.dashboard()
    → MercadoService.dashboard(correo)
      → InversionistaRepository.findByUsuarioCorreo(correo)  // obtiene intereses
      → para cada símbolo:
          → PrecioCacheRepository.findBySimboloIgnoreCase(simbolo)
          → si caché válida (<3 min): usar precio cacheado
          → si caché expirada / inexistente:
              → símbolo US (sin punto): AlpacaAdapter.obtenerSnapshot(simbolo)
              → símbolo global (con punto): AlphaVantageAdapter.obtenerCotizacion(simbolo)
              → actualizar precio_cache en BD
      → construir List<CotizacionDTO>
  ← 200 OK con lista
```

---

## Decisiones técnicas

- **Proveedor por tipo de símbolo:** los símbolos sin punto (AAPL, TSLA) se consultan en Alpaca Market Data; los que tienen punto (RIO.LON, 9984.TSE) se consultan en Alpha Vantage.
- **Caché en BD vs. en memoria:** se usa `precio_cache` en BD para sobrevivir reinicios y compartir datos entre threads (Maintain Multiple Copies — EC-01).
- **Degradación (EC-05):** si ambos proveedores fallan, se retorna la caché disponible aunque esté vencida, con el campo `mercadoAbierto` calculado a partir del horario configurado.
- **Defaults de intereses:** si `intereses_mercado` está vacío se usan los defaults AAPL, MSFT, TSLA.

---

## Escenarios de calidad cubiertos

| EC | Táctica | Implementación |
|---|---|---|
| EC-01 | Maintain Multiple Copies | `precio_cache` en BD; respuesta ≤ 2 s con caché válida |
| EC-05 | Graceful Degradation | Retorna caché vencida si API externa no responde |

---

## Dependencias previas

- HU-1 / HU-7: los intereses del inversionista están configurados en BD.
- `PrecioCache` entity y `PrecioCacheRepository` creados.
- `AlphaVantageAdapter` y `AlpacaAdapter` implementados con sus claves en `application.properties`.
- `MercadoService.esMercadoAbierto()` y `detectarMercado()` implementados.

---

## Criterios de aceptación resumidos

- GET /api/mercado/dashboard con JWT válido → 200 con lista de cotizaciones para los intereses del usuario.
- Si caché válida disponible: no se llama a APIs externas.
- Si API externa falla: retorna datos de caché con precio válido o precio 0 (sin error 500).
- Sin JWT: 401.
- Tiempo de respuesta con caché caliente ≤ 2 s.
