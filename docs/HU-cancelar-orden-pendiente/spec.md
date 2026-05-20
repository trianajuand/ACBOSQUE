# Historia de Usuario

## Título
Cancelación de orden pendiente.

## Descripción
Como inversionista autenticado
Quiero cancelar una orden que aún no se ha ejecutado
Para liberar recursos y evitar que llegue al mercado.

## Contexto
Cubre HU-21. La cancelación aplica a estados no terminales y libera fondos reservados de compras.

## Flujo funcional
1. Frontend muestra órdenes activas o historial.
2. Usuario pulsa cancelar en una orden.
3. Angular llama `DELETE /api/ordenes/{ordenId}`.
4. Backend busca orden por id y usuario.
5. Si está `EJECUTADA` o `CANCELADA`, retorna error.
6. Si tiene `alpacaOrderId`, intenta cancelar en Alpaca.
7. Si era compra pendiente/enviada/en cola, libera fondos.
8. Marca estado `CANCELADA` y fecha.
9. Audita `ORDEN_CANCELADA`.

## Reglas de negocio
- Solo el dueño puede cancelar su orden.
- Órdenes ejecutadas no son cancelables.
- Órdenes ya canceladas no son cancelables.
- Fondos reservados se liberan solo para compras.

## Componentes involucrados
- `frontend/src/app/dashboard/dashboard.component.ts`
- `backend/.../ordenes/controller/OrdenController.java`
- `backend/.../ordenes/service/OrdenService.java`
- `backend/.../ordenes/repository/OrdenRepository.java`
- `backend/.../ordenes/service/SaldoService.java`
- `backend/.../integracion/adaptadores/alpaca/AlpacaAdapter.java`

## Backend
`cancelarOrden` usa `findByIdAndUsuarioId`, cancela en Alpaca si aplica y actualiza estado transaccionalmente.

## Frontend
`cancelarOrden` llama endpoint, muestra toast y recarga órdenes si fue exitoso.

## Base de datos
Tabla `orden`: `estado=CANCELADA`, `cancelada_en`. Tabla `cuenta_fondos` puede actualizar saldos.

## API / Endpoints
- `DELETE /api/ordenes/{ordenId}`

## Validaciones
- Orden debe existir para el usuario.
- Estado no debe ser terminal.

## Seguridad
JWT obligatorio y filtro por `usuarioId`. No se aceptan ids de otros usuarios.

## Consideraciones técnicas
El resultado de cancelación en Alpaca no bloquea la cancelación local si el adaptador falla.

## Dependencias
Depende de creación de órdenes, saldo y Alpaca.

## Criterios de aceptación
- [ ] Orden pendiente se cancela.
- [ ] Orden ejecutada no se cancela.
- [ ] Fondos reservados se liberan en compras.
- [ ] Evento queda auditado.

## Notas
La UI no solicita confirmación modal antes de cancelar.
