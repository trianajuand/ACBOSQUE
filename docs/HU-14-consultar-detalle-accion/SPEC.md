# Historia de Usuario

## Título
Consulta de detalle completo de acción.

## Descripción
Como inversionista autenticado
Quiero consultar precio, métricas, empresa e histórico de una acción
Para tomar decisiones antes de operar.

## Contexto
Cubre HU-14. El detalle combina cotización actual, overview de empresa y serie/histórico según proveedor.

## Flujo funcional
1. Usuario selecciona o busca un símbolo en el panel mercado.
2. Frontend llama `GET /api/mercado/cotizacion/{simbolo}`.
3. Luego llama `GET /api/mercado/detalle/{simbolo}`.
4. Backend detecta mercado.
5. Si es US, consulta Alpaca snapshot/barras y Alpha Vantage overview.
6. Si es global, consulta Alpha Vantage quote, overview y serie diaria.
7. Frontend renderiza estadísticas y gráfica SVG calculada desde histórico.

## Reglas de negocio
- Símbolos US no tienen sufijo.
- LSE usa `.L`, TSE `.T`, ASX `.AX`.
- Overview se cachea en memoria 24 horas.
- Serie diaria se cachea en memoria 8 horas.

## Componentes involucrados
- `frontend/src/app/dashboard/dashboard.component.ts`
- `frontend/src/app/dashboard/dashboard.component.html`
- `backend/.../mercado/controller/MercadoController.java`
- `backend/.../mercado/service/MercadoService.java`
- `backend/.../mercado/dto/DetalleAccionDTO.java`
- `backend/.../integracion/adaptadores/alphavantage/AlphaVantageAdapter.java`
- `backend/.../integracion/adaptadores/alpaca/AlpacaAdapter.java`

## Backend
`obtenerDetalle` normaliza y valida símbolo, detecta mercado y rellena datos. Para no US usa conversiones `.L` a `.LON` y `.T` a `.TYO` para Alpha Vantage.

## Frontend
`seleccionarSimboloMercado` guarda búsqueda, carga detalle y muestra variación, precios, volumen, sector e histórico.

## Base de datos
Puede leer/escribir `precio_cache` para cotización actual. El overview y serie se cachean en memoria, no BD.

## API / Endpoints
- `GET /api/mercado/cotizacion/{simbolo}`
- `GET /api/mercado/detalle/{simbolo}`

## Validaciones
- Formato de símbolo por regex.
- Si la cotización no tiene precio válido en operaciones, se lanza `SimboloInvalidoException`.

## Seguridad
Ambos endpoints requieren autenticación salvo `simbolos`. No se usa dato sensible.

## Consideraciones técnicas
Alpha Vantage tiene circuit breaker local ante rate limit hasta medianoche.

## Dependencias
Depende de proveedores de mercado y caché.

## Criterios de aceptación
- [ ] Permite consultar símbolo válido.
- [ ] Devuelve precio actual y mercado.
- [ ] Incluye histórico cuando el proveedor responde.
- [ ] Rechaza formato inválido.

## Notas
La UI usa nombres locales de empresas como fallback si el proveedor no retorna nombre.
