# SPEC — Visualización de portafolio de inversiones

---

## Ficha de la historia

| Campo | Valor |
|---|---|
| ID | HU-15 |
| Sprint | 2 |
| Prioridad MoSCoW | Must Have |
| Estado | Completada |
| Épica | Órdenes / Portafolio |
| CU asociado | CU-15 |
| Autor | Juan Diego Triana Mejia |
| Creada | 2026-05-20 |
| Última revisión | 2026-05-24 |
| Versión de esta spec | 1.0 |

**Trazabilidad:**

| Tipo | ID | Descripción |
|---|---|---|
| Requerimiento funcional | RF-14 | Visualización de holdings, valor de mercado y ganancia/pérdida |
| Escenario de calidad | EC-01 | Respuesta del portafolio ≤ 3 s |
| Historia que precede a esta | HU-17..20 | Las órdenes ejecutadas generan holdings en el portafolio |
| Historia relacionada | HU-9 | `vistaPortafolio` (LISTA/GRAFICO) configura la visualización |

---

## Historia de usuario

**Como** inversionista autenticado,
**quiero** ver mis holdings actuales con su valor de mercado y ganancia/pérdida,
**para** evaluar el estado de mi portafolio de inversiones.

---

## Actores y precondiciones

### Actores

| Actor | Rol en el sistema | Participación en este flujo |
|---|---|---|
| Inversionista autenticado | `INVERSIONISTA` | Iniciador — navega a `/portafolio` |
| `PortafolioService` | Módulo `ordenes` | Calcula holdings y P&L usando precios actuales del módulo mercado |
| `MercadoService` | Módulo `mercado` | Provee precios actuales para valorización |

### Precondiciones

- JWT válido en cabecera `Authorization: Bearer`.
- El inversionista tiene al menos una orden ejecutada (holdings pueden estar vacíos).

### Postcondiciones

- Respuesta 200 con `PortafolioDTO` que incluye lista de holdings con P&L.

---

## Flujo principal

1. Frontend llama `GET /api/portafolio` con JWT.

**Backend — `PortafolioService.portafolio(correo)`:**

2. Carga holdings del inversionista (registros en tabla `holding`).
3. Para cada holding: consulta precio actual desde caché de mercado.
4. Calcula:
   - `valorMercado = precio_actual × cantidad`
   - `gananciaPerdida = valorMercado - (precio_promedio_compra × cantidad)`
   - `variacionPorcentaje = (gananciaPerdida / (precio_promedio_compra × cantidad)) × 100`
5. Calcula `valorTotalPortafolio` sumando todos los `valorMercado`.
6. Responde `200 OK` con `PortafolioDTO`.

---

## Flujos de error

### Error 1 — No autenticado

| Campo | Valor |
|---|---|
| Condición | JWT ausente, inválido o expirado |
| HTTP | 401 Unauthorized |

### Error 2 — Portafolio vacío

| Campo | Valor |
|---|---|
| Condición | El inversionista no tiene holdings |
| HTTP | 200 OK |
| Cuerpo | `PortafolioDTO{holdings: [], valorTotalPortafolio: 0.0}` |

---

## Contrato de API

### Endpoint — `GET /api/portafolio`

```yaml
GET /api/portafolio:
  summary: Retorna el portafolio del inversionista con holdings y P&L
  security:
    - bearerAuth: []
  responses:
    '200':
      description: Portafolio del inversionista
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/PortafolioDTO'
          example:
            valorTotalPortafolio: 5678.90
            holdings:
              - ticker: "AAPL"
                cantidad: 10
                precioPromedioCompra: 180.00
                precioActual: 189.50
                valorMercado: 1895.00
                gananciaPerdida: 95.00
                variacionPorcentaje: 5.28
                mercado: "NASDAQ"

components:
  schemas:
    PortafolioDTO:
      type: object
      properties:
        valorTotalPortafolio:
          type: number
          format: double
        holdings:
          type: array
          items:
            $ref: '#/components/schemas/HoldingDTO'
    HoldingDTO:
      type: object
      properties:
        ticker:
          type: string
          description: "Símbolo del activo (ej. AAPL, RIO.LON) — obtenido por JOIN con tabla activo"
        cantidad:
          type: number
        precioPromedioCompra:
          type: number
          format: double
        precioActual:
          type: number
          format: double
        valorMercado:
          type: number
          format: double
        gananciaPerdida:
          type: number
          format: double
        variacionPorcentaje:
          type: number
          format: double
        mercado:
          type: string
          nullable: true
```

---

## Modelo de datos

### Tabla `activo` (catálogo de instrumentos financieros)

```sql
CREATE TABLE activo (
    id       BIGSERIAL PRIMARY KEY,
    ticker   VARCHAR(20)  NOT NULL UNIQUE,  -- símbolo de mercado (AAPL, RIO.LON)
    nombre   VARCHAR(255),
    mercado  VARCHAR(50),
    tipo     VARCHAR(50)   -- ACCIONES, ETF, etc.
);
```

### Tabla `holding` (módulo `ordenes` — PK compuesta)

```sql
CREATE TABLE holding (
    inversionista_id        BIGINT NOT NULL REFERENCES inversionista(id),
    activo_id               BIGINT NOT NULL REFERENCES activo(id),
    cantidad                DECIMAL(12,4) NOT NULL DEFAULT 0,
    precio_promedio_compra  DECIMAL(12,4),
    PRIMARY KEY (inversionista_id, activo_id)
);

CREATE INDEX idx_holding_inversionista ON holding (inversionista_id);
```

**Decisiones de esquema:**
- PK compuesta `(inversionista_id, activo_id)` reemplaza el auto-increment `id` y la columna `simbolo`. Garantiza unicidad a nivel BD sin constraint `UNIQUE` separado.
- `activo_id` referencia el catálogo `activo`; el ticker se obtiene por JOIN en lugar de almacenarlo redundantemente en `holding`.
- `inversionista_id` referencia `inversionista.id` (no `usuario.id`) para mantener la integridad referencial con el perfil de inversión.

---

## Módulos y arquitectura

### Módulos involucrados

| Módulo | Rol | Componentes específicos |
|---|---|---|
| `ordenes` | Coordinador del flujo | `PortafolioController`, `PortafolioService` |
| `mercado` | Precios actuales | `MercadoService` (vía interfaz `ICotizacion` o similar) |

### Escenarios de calidad y tácticas materializadas

| EC | Táctica | Cómo se materializa en HU-15 |
|---|---|---|
| EC-01 | Caché de respuestas | Los precios provienen de `precio_cache`; no se llama Alpha Vantage en cada carga del portafolio |

---

## Criterios de verificación

### Escenarios de aceptación (Gherkin)

```gherkin
Funcionalidad: Visualización de portafolio

  Antecedentes:
    Dado que "ana@test.com" tiene JWT válido y holding: AAPL=10 unidades a precio_promedio=180.00

  Escenario: Portafolio con holdings
    Cuando se envía GET /api/portafolio con JWT de "ana@test.com"
    Entonces el sistema responde 200 OK
    Y la respuesta contiene holdings con ticker "AAPL"
    Y el campo gananciaPerdida refleja la diferencia entre precioActual y precioPromedioCompra

  Escenario: Portafolio vacío
    Dado que "nuevo@test.com" no tiene holdings
    Cuando se envía GET /api/portafolio
    Entonces el sistema responde 200 OK
    Y holdings es una lista vacía
    Y valorTotalPortafolio es 0

  Escenario: Sin JWT — 401
    Cuando se envía GET /api/portafolio sin Authorization
    Entonces el sistema responde 401 Unauthorized
```

---

## Definición de terminado

- [x] `GET /api/portafolio` retorna holdings con valorización actual y P&L.
- [x] Portafolio vacío retorna lista vacía con valor total 0.
- [x] Precios de valorización provienen de caché en BD.
- [x] Sin JWT responde 401.
- [x] `docs/PROGRESO.md` marcado con ✅ para HU-15.

---

## Historial de cambios

| Versión | Fecha | Descripción | Razón |
|---|---|---|---|
| 1.0 | 2026-05-24 | Refactorización a estructura SDD del proyecto. | Unificación de todos los SPEC.md bajo plantilla canónica SDD del proyecto |
