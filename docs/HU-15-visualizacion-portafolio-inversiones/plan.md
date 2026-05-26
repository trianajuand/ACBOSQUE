# Plan de implementación — HU-15: Visualización del portafolio de inversiones

## Objetivo

Mostrar al inversionista autenticado todos sus holdings actuales con valorización en tiempo real, ganancia/pérdida por posición y valor total del portafolio. Soportar vista lista y vista gráfico según preferencia configurada en HU-9.

---

## Módulos involucrados

| Módulo | Componentes |
|---|---|
| `ordenes` | `PortafolioController`, `PortafolioService`, `HoldingRepository` |
| `mercado` | `MercadoService` (precios actuales desde `precio_cache`) |
| `autenticacion` | `InversionistaRepository` (lectura de `vistaPortafolio`) |

---

## Estrategia general

1. `GET /api/portafolio` extrae correo del JWT.
2. `PortafolioService.portafolio(correo)` carga todos los holdings del inversionista.
3. Para cada holding, consulta el precio actual desde `precio_cache` (sin llamar a Alpha Vantage para no consumir cuota en cada load de portafolio).
4. Calcula P&L por posición y valor total.
5. Responde con `PortafolioDTO` que incluye lista de `HoldingDTO` y `valorTotalPortafolio`.
6. Si el inversionista no tiene holdings, retorna lista vacía con `valorTotalPortafolio = 0.0`.

---

## Cálculos por holding

```
valorMercado         = precio_actual × cantidad
costoBasis           = precio_promedio_compra × cantidad
gananciaPerdida      = valorMercado - costoBasis
variacionPorcentaje  = (gananciaPerdida / costoBasis) × 100   // null si costoBasis=0
```

---

## Flujo de datos

```
Frontend GET /api/portafolio
  → PortafolioController.portafolio()
    → PortafolioService.portafolio(correo)
      → InversionistaRepository.findByUsuarioCorreo(correo)
      → HoldingRepository.findByInversionistaId(inversionistaId)  // JOIN con activo para ticker
      → para cada holding:
          → PrecioCacheRepository.findBySimboloIgnoreCase(ticker)
          → calcular P&L
      → sumar valorTotalPortafolio
      → mapear a List<HoldingDTO>
  ← 200 OK con PortafolioDTO
```

---

## Modelo de datos clave

- `holding` PK compuesta `(inversionista_id, activo_id)`.
- `activo.ticker` se obtiene por JOIN (no se almacena en `holding`).
- `precio_cache.precio_actual` se usa para valorización.

---

## Decisiones técnicas

- **Sin llamadas a API externa en el portafolio:** solo se usa `precio_cache`; si un activo no tiene caché, su `precioActual` será 0 (precio desconocido). El frontend muestra "N/D" en ese caso.
- **vistaPortafolio (HU-9):** el backend siempre retorna lista de holdings; la decisión de renderizar como lista o gráfico es del frontend según `perfil.vistaPortafolio`.
- **Holdings con cantidad=0:** se filtran de la respuesta (posición cerrada). Solo se retornan holdings con `cantidad > 0`.

---

## Escenarios de calidad cubiertos

| EC | Táctica | Implementación |
|---|---|---|
| EC-01 | Caché de precios | Valorización usa `precio_cache`; no llama Alpha Vantage en cada load |

---

## Dependencias previas

- HU-17..20: las órdenes ejecutadas crean y actualizan holdings.
- `precio_cache` poblado por HU-13 (job de refresco cada 3 min).
- `Holding` entity con PK compuesta y FK a `Activo` implementada.
- `PortafolioService` y `PortafolioController` creados.

---

## Criterios de aceptación resumidos

- GET /api/portafolio → 200 con lista de holdings y `valorTotalPortafolio`.
- Holdings con `cantidad > 0` incluidos; `cantidad = 0` excluidos.
- `gananciaPerdida` y `variacionPorcentaje` calculados correctamente.
- Portafolio vacío → 200 con lista vacía y valor 0.
- Sin JWT → 401.
