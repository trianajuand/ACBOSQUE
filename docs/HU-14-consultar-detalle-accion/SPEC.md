# SPEC — Consulta de detalle completo de una acción

---

## Ficha de la historia

| Campo | Valor |
|---|---|
| ID | HU-14 |
| Sprint | 2 |
| Prioridad MoSCoW | Must Have |
| Estado | Completada |
| Épica | Mercado / Visualización |
| CU asociado | CU-14 |
| Autor | Juan Diego Triana Mejia |
| Creada | 2026-05-20 |
| Última revisión | 2026-05-24 |
| Versión de esta spec | 1.0 |

**Trazabilidad:**

| Tipo | ID | Descripción |
|---|---|---|
| Requerimiento funcional | RF-13 | Consulta de detalle completo de una acción (empresa, métricas, histórico) |
| Escenario de calidad | EC-01 | Tiempo de respuesta del detalle ≤ 3 s |
| Escenario de calidad | EC-05 | Disponibilidad de datos con caché como resiliencia |
| Historia relacionada | HU-13 | Desde el dashboard se navega al detalle de cada símbolo |
| Historia relacionada | HU-17..20 | El usuario puede colocar orden desde la vista de detalle |

---

## Historia de usuario

**Como** inversionista autenticado,
**quiero** consultar el detalle completo de una acción (precio, empresa, métricas, histórico de precios),
**para** tomar decisiones informadas antes de operar.

---

## Actores y precondiciones

### Actores

| Actor | Rol en el sistema | Participación en este flujo |
|---|---|---|
| Inversionista autenticado | `INVERSIONISTA` | Iniciador — selecciona un símbolo para ver su detalle |
| `MercadoService` | Módulo `mercado` | Consulta datos de precio, empresa e histórico |
| `AlphaVantageAdapter` | Módulo `integracion` | Fuente de datos fundamentales y serie histórica |

### Precondiciones

- JWT válido en cabecera `Authorization: Bearer`.
- El símbolo solicitado es válido y conocido por el sistema.

### Postcondiciones

- Respuesta 200 con `DetalleAccionDTO` completo.

---

## Flujo principal

1. Usuario selecciona un símbolo en el dashboard o en el buscador.
2. Frontend envía `GET /api/mercado/detalle/{simbolo}` con JWT.

**Backend — `MercadoService.detalle(simbolo)`:**

3. Consulta precio actual desde caché o Alpha Vantage.
4. Consulta overview de la empresa (sector, descripción, etc.) desde Alpha Vantage.
5. Consulta serie histórica de precios (últimos 30 días o desde caché).
6. Construye y retorna `DetalleAccionDTO`.
7. Responde `200 OK`.

---

## Flujos de error

### Error 1 — No autenticado

| Campo | Valor |
|---|---|
| Condición | JWT ausente, inválido o expirado |
| HTTP | 401 Unauthorized |

### Error 2 — Símbolo inválido o no encontrado

| Campo | Valor |
|---|---|
| Condición | Alpha Vantage no retorna datos para el símbolo |
| Excepción Java | `SimboloInvalidoException` |
| HTTP | 400 Bad Request |
| Cuerpo | `RespuestaDTO{error: "Símbolo no válido: {simbolo}"}` |

### Error 3 — Alpha Vantage no disponible (degradación)

| Campo | Valor |
|---|---|
| Condición | API externa no responde |
| HTTP | 200 OK con datos parciales de caché, o datos vacíos en campos no cacheados |

---

## Contrato de API

### Endpoint — `GET /api/mercado/detalle/{simbolo}`

```yaml
GET /api/mercado/detalle/{simbolo}:
  summary: Retorna el detalle completo de una acción
  security:
    - bearerAuth: []
  parameters:
    - name: simbolo
      in: path
      required: true
      schema:
        type: string
      example: "AAPL"
  responses:
    '200':
      description: Detalle completo de la acción
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/DetalleAccionDTO'
          example:
            simbolo: "AAPL"
            nombre: "Apple Inc."
            sector: "Technology"
            descripcion: "Apple Inc. designs, manufactures..."
            precio: 189.50
            variacion: 1.25
            volumen: 52000000
            mercado: "NASDAQ"
            mercadoAbierto: true
            historico: [{"fecha": "2026-05-23", "precio": 187.20}, ...]
    '400':
      description: Símbolo inválido
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'
    '401':
      description: No autenticado
    '500':
      description: Error interno del servidor

components:
  schemas:
    DetalleAccionDTO:
      type: object
      properties:
        simbolo:
          type: string
        nombre:
          type: string
          nullable: true
        sector:
          type: string
          nullable: true
        descripcion:
          type: string
          nullable: true
        precio:
          type: number
          format: double
        variacion:
          type: number
          format: double
          nullable: true
        volumen:
          type: integer
          nullable: true
        mercado:
          type: string
          nullable: true
        mercadoAbierto:
          type: boolean
        historico:
          type: array
          items:
            type: object
            properties:
              fecha:
                type: string
              precio:
                type: number
          nullable: true
```

---

## Módulos y arquitectura

### Módulos involucrados

| Módulo | Rol | Componentes específicos |
|---|---|---|
| `mercado` | Coordinador del flujo | `MercadoController`, `MercadoService` |
| `integracion` | Datos externos | `AlphaVantageAdapter` |

### Escenarios de calidad y tácticas materializadas

| EC | Táctica | Cómo se materializa en HU-14 |
|---|---|---|
| EC-01 | Caché de respuestas | Precio en caché; datos fundamentales pueden ser también cacheados |
| EC-05 | Degradación | Si Alpha Vantage no disponible, retorna datos parciales disponibles |

---

## Criterios de verificación

### Escenarios de aceptación (Gherkin)

```gherkin
Funcionalidad: Consulta de detalle de acción

  Escenario: Detalle exitoso de AAPL
    Dado que "ana@test.com" tiene JWT válido
    Cuando se envía GET /api/mercado/detalle/AAPL
    Entonces el sistema responde 200 OK
    Y el cuerpo contiene el campo "simbolo": "AAPL"
    Y el cuerpo contiene el campo "precio" mayor a 0

  Escenario: Símbolo inválido retorna 400
    Cuando se envía GET /api/mercado/detalle/INVALIDO
    Entonces el sistema responde 400 Bad Request

  Escenario: Sin JWT — 401
    Cuando se envía GET /api/mercado/detalle/AAPL sin Authorization
    Entonces el sistema responde 401 Unauthorized
```

---

## Definición de terminado

- [x] `GET /api/mercado/detalle/{simbolo}` retorna detalle completo con datos de empresa, precio e histórico.
- [x] Símbolo inválido retorna 400.
- [x] Sin JWT responde 401.
- [x] `docs/PROGRESO.md` marcado con ✅ para HU-14.

---

## Historial de cambios

| Versión | Fecha | Descripción | Razón |
|---|---|---|---|
| 1.0 | 2026-05-24 | Refactorización a estructura SDD del proyecto. | Unificación de todos los SPEC.md bajo plantilla canónica SDD del proyecto |
