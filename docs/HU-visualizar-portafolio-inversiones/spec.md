# Historia de Usuario

## Título
Visualización de portafolio de inversiones.

## Descripción
Como inversionista autenticado
Quiero consultar mis holdings, valor de mercado y ganancia/pérdida
Para evaluar el estado de mi portafolio.

## Contexto
Cubre HU-15. El portafolio combina holdings persistidos con precios actuales del servicio de Mercado.

## Flujo funcional
1. Frontend llama `GET /api/portafolio`.
2. Backend resuelve usuario autenticado.
3. `OrdenService.obtenerPortafolio` sincroniza órdenes enviadas con Alpaca.
4. `PortafolioService` carga holdings por usuario.
5. Para cada holding consulta cotización actual.
6. Calcula valor, costo, ganancia/pérdida y porcentaje.
7. Retorna `PortafolioDTO`.

## Reglas de negocio
- Holdings con cantidad menor o igual a cero no se muestran.
- Valor total = precio actual por cantidad.
- Ganancia/pérdida = valor actual menos costo promedio.
- Vista preferida proviene de `usuario.vistaPortafolio` o `LISTA`.

## Componentes involucrados
- `frontend/src/app/dashboard/dashboard.component.ts`
- `backend/.../ordenes/controller/PortafolioController.java`
- `backend/.../ordenes/service/OrdenService.java`
- `backend/.../ordenes/service/PortafolioService.java`
- `backend/.../ordenes/model/Holding.java`
- `backend/.../ordenes/repository/HoldingRepository.java`

## Backend
`PortafolioService.obtenerPortafolio` es read-only y calcula métricas en memoria. Las compras/ventas actualizan holdings desde `registrarCompra` y `registrarVenta`.

## Frontend
El dashboard muestra valor total, ganancia/pérdida y holdings; refresca después de órdenes.

## Base de datos
Tabla `holding`: `usuario_id`, `simbolo`, `cantidad`, `precio_promedio`, `actualizado_en`; constraint único por usuario/símbolo.

## API / Endpoints
- `GET /api/portafolio`

## Validaciones
- Usuario autenticado.
- Si cotización falla, precio actual queda en cero para ese holding.

## Seguridad
La consulta filtra por `usuarioId` derivado del JWT.

## Consideraciones técnicas
Se sincronizan órdenes Alpaca antes de calcular portafolio para reflejar ejecuciones recientes.

## Dependencias
Depende de órdenes ejecutadas, mercado y perfil.

## Criterios de aceptación
- [ ] Devuelve holdings del usuario.
- [ ] Calcula valor total y P/L.
- [ ] Respeta vista preferida.
- [ ] No muestra holdings en cero.

## Notas
No existe histórico gráfico de portafolio; solo estado actual.
