# SPEC — Dashboard de acciones de interés

---

## Ficha de la historia

| Campo | Valor |
|---|---|
| ID | HU-13 |
| Sprint | 2 |
| Prioridad MoSCoW | Must Have |
| Estado | Completada |
| Épica | Mercado / Visualización |
| CU asociado | CU-13 |
| Autor | Juan Diego Triana Mejia |
| Creada | 2026-05-20 |
| Última revisión | 2026-05-24 |
| Versión de esta spec | 1.0 |

**Trazabilidad:**

| Tipo | ID | Descripción |
|---|---|---|
| Requerimiento funcional | RF-12 | Dashboard con cotizaciones de acciones de interés del inversionista |
| Escenario de calidad | EC-01 | Tiempo de respuesta del dashboard ≤ 2 s (caché de precios) |
| Escenario de calidad | EC-05 | Disponibilidad de datos de mercado con caché como táctica de resiliencia |
| Historia que precede a esta | HU-1 / HU-7 | Intereses configurados en registro o actualizados en perfil |
| Historia relacionada | HU-14 | Desde el dashboard se navega al detalle de cada acción |

---

## Historia de usuario

**Como** inversionista autenticado,
**quiero** ver las cotizaciones actuales de las acciones que marqué como intereses,
**para** monitorear rápidamente los activos relevantes para mi estrategia de trading.

---

## Motivación y contexto

### Por qué existe esta historia

El dashboard es la primera pantalla que ve el inversionista al iniciar sesión. Debe cargar rápidamente y mostrar cotizaciones actualizadas sin sobrecargar la API de Alpha Vantage (que tiene límites de llamadas). El sistema mantiene una caché en BD que se actualiza periódicamente.

### Dependencias hacia atrás

| Componente | Qué provee | Sin esto... |
|---|---|---|
| `inversionista.intereses_mercado` | Lista de símbolos a mostrar | El dashboard retorna datos vacíos o los defaults (AAPL, MSFT, TSLA) |
| Caché de precios en BD | Datos recientes sin llamar a Alpha Vantage cada vez | Cada solicitud de dashboard llama a Alpha Vantage → quota agotada |
| Alpha Vantage API (`app.alpha-vantage.api-key`) | Datos de precios actualizados | La caché no se actualiza; se muestran datos desactualizados |

---

## Actores y precondiciones

### Actores

| Actor | Rol en el sistema | Participación en este flujo |
|---|---|---|
| Inversionista autenticado | `INVERSIONISTA` | Iniciador — carga el dashboard |
| `MercadoService` | Módulo `mercado` | Consulta caché de precios y los intereses del inversionista |
| `AlphaVantageAdapter` | Módulo `integracion` | Fuente de datos de precios (consultada con rate limiting) |

### Precondiciones

- JWT válido en cabecera `Authorization: Bearer`.
- `inversionista.intereses_mercado` configurado (al menos los defaults AAPL, MSFT, TSLA).

### Postcondiciones

- Respuesta 200 con lista de `CotizacionDTO` para cada símbolo de interés.
- Los datos provienen de caché en BD (si disponibles) o de Alpha Vantage.

---

## Flujo principal

1. Usuario navega a `/dashboard` en Angular.
2. Frontend envía `GET /api/mercado/dashboard` con JWT.

**Backend — `MercadoController → MercadoService.obtenerDashboard(interesesMercado)`:**

> ⚠️ **Deuda técnica DT-10:** La implementación actual en `MercadoController.dashboard()` pasa `""` como argumento a `MercadoService.obtenerDashboard("")`, lo que hace que el dashboard siempre muestre los símbolos por defecto (`AAPL, MSFT, GOOGL, AMZN, TSLA, META, NVDA, JPM`) en lugar de los intereses personalizados del inversionista. El flujo descrito a continuación es el **diseñado**; el paso 4 aún no está implementado.

3. Spring Security extrae `correo` del JWT.
4. **[Pendiente DT-10]** Debería cargar `Inversionista` → obtener `interesesMercado` (CSV → List). Actualmente se usa la lista de símbolos por defecto.
5. Para cada símbolo:
   - Consulta `precio_cache` en BD por símbolo.
   - Si existe caché válida (no expirada): usa precio en caché.
   - Si caché expirada o no existe: consulta Alpha Vantage API → actualiza caché en BD.
6. Construye `List<CotizacionDTO>` con precios, estado del mercado (abierto/cerrado) y variación.
7. Responde `200 OK` con la lista.

---

## Flujos de error

### Error 1 — No autenticado

| Campo | Valor |
|---|---|
| Condición | JWT ausente, inválido o expirado |
| HTTP | 401 Unauthorized |

### Error 2 — Alpha Vantage no disponible (degradación)

| Campo | Valor |
|---|---|
| Condición | API de Alpha Vantage retorna error o timeout |
| HTTP | 200 OK (datos desactualizados de caché, o precio 0 si sin caché) |
| Cuerpo | Lista con datos de caché disponibles; símbolos sin caché pueden mostrar precio 0 |

### Error 3 — Error técnico genérico

| Campo | Valor |
|---|---|
| Condición | Falla BD |
| HTTP | 500 Internal Server Error |

---

## Contrato de API

### Endpoint — `GET /api/mercado/dashboard`

```yaml
GET /api/mercado/dashboard:
  summary: Retorna cotizaciones de los símbolos de interés del inversionista autenticado
  security:
    - bearerAuth: []
  responses:
    '200':
      description: Lista de cotizaciones de interés
      content:
        application/json:
          schema:
            type: array
            items:
              $ref: '#/components/schemas/CotizacionDTO'
          example:
            - simbolo: "AAPL"
              precio: 189.50
              mercado: "NASDAQ"
              mercadoAbierto: true
              variacionPorcentaje: 1.25
            - simbolo: "TSLA"
              precio: 245.30
              mercado: "NASDAQ"
              mercadoAbierto: true
              variacionPorcentaje: -0.75
    '401':
      description: No autenticado
    '500':
      description: Error interno del servidor

components:
  schemas:
    CotizacionDTO:
      type: object
      properties:
        simbolo:
          type: string
        precio:
          type: number
          format: double
        mercado:
          type: string
        mercadoAbierto:
          type: boolean
        variacionPorcentaje:
          type: number
          format: double
          nullable: true
```

---

## Modelo de datos

### Tabla `precio_cache` (módulo `mercado`)

```sql
CREATE TABLE precio_cache (
    activo_id           BIGINT PRIMARY KEY REFERENCES activo(id),  -- PK compartida con activo
    simbolo             VARCHAR(20)  NOT NULL UNIQUE,              -- denormalizado para lecturas rápidas
    nombre_empresa      VARCHAR(200),
    mercado             VARCHAR(20),
    precio_actual       DECIMAL(18,4),
    precio_apertura     DECIMAL(18,4),
    precio_cierre_anterior DECIMAL(18,4),
    precio_maximo       DECIMAL(18,4),
    precio_minimo       DECIMAL(18,4),
    variacion_porcentual DECIMAL(8,4),
    volumen             BIGINT,
    fuente              VARCHAR(20),
    actualizado_en      TIMESTAMP NOT NULL
);

-- simbolo es denormalizado (también en activo.ticker); facilita JOINs y lookups por ticker.
-- No hay auto-increment id: activo_id es la PK (shared PK pattern).
```

---

## Módulos y arquitectura

### Módulos involucrados

| Módulo | Rol | Componentes específicos |
|---|---|---|
| `mercado` | Coordinador del flujo | `MercadoController`, `MercadoService` |
| `integracion` | Datos de precios externos | `AlphaVantageAdapter` (vía `ICotizacion` o similar) |
| `autenticacion` | Intereses del inversionista | `InversionistaRepository` |

### Escenarios de calidad y tácticas materializadas

| EC | Táctica | Cómo se materializa en HU-13 |
|---|---|---|
| EC-01 | Caché de respuestas | `precio_cache` en BD evita llamadas repetidas a Alpha Vantage; respuesta ≤ 2 s |
| EC-05 | Degradación | Si Alpha Vantage no disponible, se usan datos de caché aunque estén vencidos |

---

## Riesgos

| # | Riesgo | P | I | Mitigación | Test que lo cubre |
|---|---|:-:|:-:|---|---|
| R1 | Cuota de Alpha Vantage agotada (plan gratuito: 5 req/min) → caché desactualizada para todos los usuarios | Alta (dev) | Medio | Caché en BD reduce llamadas; TTL ajustado para no expirar muy seguido | Manual: agotar quota y verificar que se usan datos de caché |
| R2 | Caché sin dato para símbolo nuevo → primer request siempre llama Alpha Vantage | Baja | Bajo | Comportamiento esperado; la caché se puebla en el primer acceso | No aplica |

---

## Criterios de verificación

### Escenarios de aceptación (Gherkin)

```gherkin
Funcionalidad: Dashboard de acciones de interés

  Antecedentes:
    Dado que "ana@test.com" tiene JWT válido e intereses_mercado="AAPL,TSLA"

  Escenario: Dashboard carga cotizaciones de intereses
    Cuando se envía GET /api/mercado/dashboard con JWT de "ana@test.com"
    Entonces el sistema responde 200 OK
    Y la respuesta es una lista con 2 elementos (AAPL y TSLA)
    Y cada elemento contiene campos simbolo, precio y mercadoAbierto

  Escenario: Dashboard usa caché cuando está disponible
    Dado que precio_cache tiene datos para AAPL actualizados hace 1 minuto
    Cuando se envía GET /api/mercado/dashboard
    Entonces el sistema responde en menos de 500ms
    Y no se realizaron llamadas a Alpha Vantage API

  Escenario: Sin JWT — 401
    Cuando se envía GET /api/mercado/dashboard sin Authorization
    Entonces el sistema responde 401 Unauthorized
```

---

## Definición de terminado

- [ ] `GET /api/mercado/dashboard` retorna cotizaciones de los intereses del inversionista. **Pendiente DT-10:** actualmente siempre retorna los 8 símbolos por defecto.
- [x] La caché en BD se usa cuando está disponible.
- [x] Alpha Vantage se consulta solo cuando la caché es inválida o no existe.
- [x] Degradación: si Alpha Vantage falla, se usan datos de caché aunque vencidos.
- [x] Sin JWT responde 401.
- [x] `docs/PROGRESO.md` marcado con ✅ para HU-13.

---

## Historial de cambios

| Versión | Fecha | Descripción | Razón |
|---|---|---|---|
| 1.0 | 2026-05-24 | Refactorización a estructura SDD del proyecto. | Unificación de todos los SPEC.md bajo plantilla canónica SDD del proyecto |
| 1.1 | 2026-05-26 | Auditoría SDD: nota de deuda técnica DT-10 añadida al flujo principal. Dashboard actualmente ignora los intereses del usuario y siempre muestra los 8 símbolos por defecto. | `MercadoController.dashboard()` pasa `""` a `obtenerDashboard`. |
