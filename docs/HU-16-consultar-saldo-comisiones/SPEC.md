# Historia de Usuario

## Título
Consulta de saldo, fondos reservados y comisiones.

## Descripción
Como inversionista autenticado
Quiero consultar saldo disponible, fondos reservados e historial de comisiones
Para saber cuánto capital puedo usar y cuánto he pagado.

## Contexto
Cubre HU-16. El saldo se guarda localmente en `cuenta_fondos` y puede sincronizarse con Alpaca.

## Flujo funcional
1. Frontend llama `GET /api/portafolio/saldo`.
2. Backend resuelve usuario.
3. `OrdenService.obtenerSaldo` sincroniza órdenes enviadas.
4. `SaldoService.obtenerSaldoDTO` obtiene o crea cuenta de fondos.
5. Carga comisiones del usuario.
6. Calcula total de comisiones pagadas.
7. Retorna saldo, reservados e historial.

## Reglas de negocio
- Si no existe cuenta de fondos, se crea con saldos en cero.
- Las compras reservan fondos.
- Las cancelaciones liberan fondos.
- Las ventas acreditan neto al saldo disponible.

## Componentes involucrados
- `frontend/src/app/dashboard/dashboard.component.ts`
- `backend/.../ordenes/controller/PortafolioController.java`
- `backend/.../ordenes/service/SaldoService.java`
- `backend/.../ordenes/service/OrdenService.java`
- `backend/.../ordenes/model/CuentaFondos.java`
- `backend/.../ordenes/model/Comision.java`

## Backend
`SaldoService` maneja reserva, liberación, confirmación de compra/venta, depósito sandbox, sincronización con Alpaca y DTO de saldo.

## Frontend
El dashboard carga saldo al iniciar, después de depositar y después de confirmar orden.

## Base de datos
Tablas `cuenta_fondos` y `comision`.

## API / Endpoints
- `GET /api/portafolio/saldo`
- `POST /api/portafolio/sincronizar`
- `POST /api/portafolio/depositar?monto={monto}` como soporte sandbox.

## Validaciones
- Depósito requiere `monto >= 0.01`.
- Fondos insuficientes generan `FondosInsuficientesException`.

## Seguridad
Endpoints protegidos por JWT. El usuario solo accede a su saldo.

## Consideraciones técnicas
`obtenerSaldo` usa transacción no read-only porque puede crear `CuentaFondos`.

## Dependencias
Depende de órdenes, comisiones y Alpaca para sincronización opcional.

## Criterios de aceptación
- [ ] Devuelve saldo disponible.
- [ ] Devuelve fondos reservados.
- [ ] Devuelve total e historial de comisiones.
- [ ] Crea cuenta local si no existe.

## Notas
El depósito es explícitamente de pruebas y no representa funding productivo.
