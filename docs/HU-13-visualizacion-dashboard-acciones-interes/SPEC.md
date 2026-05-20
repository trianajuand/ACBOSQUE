# Historia de Usuario

## Título
Dashboard de acciones de interés.

## Descripción
Como inversionista autenticado
Quiero ver cotizaciones de las acciones configuradas como intereses
Para monitorear rápidamente los activos relevantes para mi estrategia.

## Contexto
Cubre HU-13 y EC-01/EC-05. El dashboard consume preferencias del usuario y usa caché persistida para reducir llamadas a proveedores externos.

## Flujo funcional
1. Angular carga `/dashboard`.
2. `DashboardComponent.cargarMercado` llama `GET /api/mercado/dashboard`.
3. Backend obtiene el usuario autenticado.
4. `MercadoService` parsea `interesesMercado`; si está vacío usa símbolos default US.
5. Para cada símbolo obtiene cotización desde caché, Alpaca o Alpha Vantage.
6. Devuelve lista de `CotizacionDTO`.
7. Frontend muestra tarjetas/tablas con precio, variación y estado de mercado.

## Reglas de negocio
- Símbolos US usan Alpaca.
- Símbolos globales usan Alpha Vantage.
- Caché US tiene TTL de 3 minutos.
- Caché global tiene TTL de 60 minutos.
- Si mercado está cerrado y hay caché, se retorna caché.

## Componentes involucrados
- `frontend/src/app/dashboard/dashboard.component.ts`
- `frontend/src/app/dashboard/dashboard.component.html`
- `backend/.../mercado/controller/MercadoController.java`
- `backend/.../mercado/service/MercadoService.java`
- `backend/.../mercado/model/PrecioCache.java`
- `backend/.../integracion/adaptadores/alpaca/AlpacaAdapter.java`
- `backend/.../integracion/adaptadores/alphavantage/AlphaVantageAdapter.java`

## Backend
`MercadoService.obtenerDashboard` itera símbolos normalizados y omite inválidos. `obtenerCotizacion` valida formato, consulta `PrecioCacheRepository` y refresca si corresponde.

## Frontend
`DashboardComponent` carga mercado junto con perfil, saldo, portafolio y órdenes. También calcula símbolos preferidos para la UI.

## Base de datos
Tabla `precio_cache`: símbolo, precios OHLC, variación, volumen, mercado, fuente y fecha de actualización.

## API / Endpoints
- `GET /api/mercado/dashboard`
- `GET /api/mercado/simbolos`

## Validaciones
- Regex de símbolo: `^[A-Z0-9]{1,6}(\.(T|L|AX|TO))?$`.
- Normalización de sufijos `.LON`, `.AUS`, `.TYO`, `.TSE`.

## Seguridad
Dashboard requiere JWT. El catálogo de símbolos es público para registro/perfil.

## Consideraciones técnicas
Implementa la táctica Maintain Multiple Copies of Data de EC-01 mediante `precio_cache` y refresco programado.

## Dependencias
Depende de perfil, proveedores Alpaca/Alpha Vantage y caché.

## Criterios de aceptación
- [ ] Muestra cotizaciones de intereses del usuario.
- [ ] Usa default si no hay intereses.
- [ ] Evita fallar por símbolos inválidos individuales.
- [ ] Indica si el mercado está abierto.

## Notas
Existe `app.mercado.sandbox-siempre-abierto` para pruebas fuera de horario.
