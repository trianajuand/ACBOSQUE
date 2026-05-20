# Historia de Usuario

## Título
Consulta de órdenes activas.

## Descripción
Como inversionista autenticado
Quiero ver mis órdenes activas
Para monitorear operaciones pendientes, enviadas o en cola.

## Contexto
Cubre HU-22. Antes de devolver órdenes, el backend sincroniza órdenes enviadas con Alpaca para detectar fills.

## Flujo funcional
1. Dashboard llama `GET /api/ordenes/activas`.
2. Backend resuelve usuario.
3. `OrdenService.obtenerOrdenesActivas` sincroniza enviadas con Alpaca.
4. Consulta estados `PENDIENTE`, `ENVIADA`, `EN_COLA`, `PENDIENTE_APROBACION`.
5. Retorna lista `OrdenDTO`.
6. Frontend actualiza tabla de órdenes activas.

## Reglas de negocio
- Solo se devuelven órdenes del usuario autenticado.
- Estados activos incluyen cola y pendiente de aprobación.
- Si Alpaca reporta filled, la orden se marca ejecutada antes de responder.

## Componentes involucrados
- `frontend/src/app/dashboard/dashboard.component.ts`
- `backend/.../ordenes/controller/OrdenController.java`
- `backend/.../ordenes/service/OrdenService.java`
- `backend/.../ordenes/repository/OrdenRepository.java`
- `backend/.../integracion/adaptadores/alpaca/AlpacaAdapter.java`

## Backend
La sincronización consulta Alpaca por órdenes `ENVIADA` con `alpacaOrderId` y símbolo US; si están filled actualiza saldo/holding/comisión.

## Frontend
`cargarOrdenes` solicita historial y activas en paralelo.

## Base de datos
Tabla `orden`; potencialmente actualiza `estado`, `precio_ejecucion`, `ejecutada_en`, `holding`, `cuenta_fondos` y `comision`.

## API / Endpoints
- `GET /api/ordenes/activas`

## Validaciones
- JWT requerido.
- Usuario debe existir.

## Seguridad
Filtro por usuario autenticado. No hay parámetro de usuario en la API.

## Consideraciones técnicas
La consulta tiene efectos laterales por sincronización con Alpaca.

## Dependencias
Depende de Alpaca, órdenes y portafolio.

## Criterios de aceptación
- [ ] Devuelve órdenes no terminales del usuario.
- [ ] Sincroniza fills con Alpaca.
- [ ] No devuelve órdenes de otros usuarios.
- [ ] Frontend muestra estados actualizados.

## Notas
`PENDIENTE_APROBACION` está contemplado aunque el flujo comisionista no está implementado.
