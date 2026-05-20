# Historia de Usuario

## Título
Colocación de órdenes Market, Limit, Stop Loss y Take Profit.

## Descripción
Como inversionista autenticado
Quiero colocar órdenes de compra o venta en diferentes modalidades
Para ejecutar estrategias de trading sobre mis activos.

## Contexto
Agrupa HU-17 a HU-20 porque el código usa un solo endpoint y servicio parametrizado por `tipoOrden`. Soporta ejecución Alpaca para símbolos US e interna para mercados globales.

## Flujo funcional
1. Usuario completa formulario de orden.
2. Previsualiza comisión.
3. Confirma y Angular envía `POST /api/ordenes`.
4. Backend valida símbolo y precio.
5. Calcula monto base, comisión y neto.
6. Para compra reserva fondos; para venta verifica holdings.
7. Si mercado está cerrado, guarda orden `EN_COLA`.
8. Si está abierto, guarda `PENDIENTE` y envía a Alpaca si es US.
9. Si es global, evalúa condiciones de precio y puede ejecutar internamente.
10. Actualiza estado, saldo, holdings, comisiones y auditoría según resultado.

## Reglas de negocio
- `MARKET` usa precio actual.
- `LIMIT` y `TAKE_PROFIT` usan `precioLimite` cuando aplica.
- `STOP_LOSS` usa `precioStop`.
- Compra requiere fondos disponibles para monto + comisión.
- Venta requiere holding suficiente.
- Órdenes fuera de horario quedan `EN_COLA`.

## Componentes involucrados
- `frontend/src/app/dashboard/dashboard.component.ts`
- `backend/.../ordenes/controller/OrdenController.java`
- `backend/.../ordenes/service/OrdenService.java`
- `backend/.../ordenes/dto/CrearOrdenRequestDTO.java`
- `backend/.../ordenes/model/Orden.java`
- `backend/.../ordenes/model/TipoOrden.java`
- `backend/.../ordenes/model/TipoLado.java`
- `backend/.../integracion/adaptadores/alpaca/AlpacaAdapter.java`
- `backend/.../ordenes/service/SaldoService.java`
- `backend/.../ordenes/service/PortafolioService.java`

## Backend
`OrdenService.crearOrden` concentra validaciones, persistencia y envío. Para US traduce tipos a Alpaca (`market`, `limit`, `stop`). Para globales evalúa condiciones con precio de Alpha Vantage.

## Frontend
`DashboardComponent` usa `orderForm`, muestra campos condicionales de precio límite/stop y refresca órdenes, saldo y portafolio tras confirmar.

## Base de datos
Tablas `orden`, `cuenta_fondos`, `holding`, `comision`, `usuario`.

## API / Endpoints
- `POST /api/ordenes`
- `POST /api/ordenes/previsualizar`

## Validaciones
- Cantidad mayor a `0.000001`.
- Tipo y lado obligatorios.
- Formato de símbolo y precio operable validado por Mercado.
- Fondos/holdings suficientes.

## Seguridad
JWT obligatorio. La orden se asocia al `usuarioId` derivado del token. Se registra IP de origen.

## Consideraciones técnicas
El servicio implementa lógica transaccional. La ejecución real de Alpaca puede quedar `ENVIADA` hasta confirmar fill.

## Dependencias
Depende de saldo, portafolio, mercado, Alpaca y auditoría.

## Criterios de aceptación
- [ ] Permite crear orden Market.
- [ ] Permite crear orden Limit.
- [ ] Permite crear orden Stop Loss.
- [ ] Permite crear orden Take Profit.
- [ ] Rechaza compra sin fondos y venta sin holdings.
- [ ] Audita creación/envío/ejecución.

## Notas
No hay endpoint separado por tipo de orden; el tipo se recibe en el DTO.
