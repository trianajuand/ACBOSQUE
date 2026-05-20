# Historia de Usuario

## Título
Historial de órdenes.

## Descripción
Como inversionista autenticado
Quiero consultar el historial de mis órdenes
Para revisar operaciones pasadas y su estado.

## Contexto
Implementa parcialmente HU-24 a HU-26. El código devuelve historial completo, pero no implementa filtros por período, tipo, activo o estado.

## Flujo funcional
1. Dashboard llama `GET /api/ordenes/historial`.
2. Backend resuelve usuario autenticado.
3. Sincroniza órdenes enviadas con Alpaca.
4. Consulta todas las órdenes del usuario ordenadas por fecha descendente.
5. Mapea entidades a `OrdenDTO`.
6. Frontend muestra tabla de historial.

## Reglas de negocio
- Historial contiene todos los estados.
- Ordenamiento descendente por `creadaEn`.
- Solo devuelve órdenes del usuario.

## Componentes involucrados
- `frontend/src/app/dashboard/dashboard.component.ts`
- `backend/.../ordenes/controller/OrdenController.java`
- `backend/.../ordenes/service/OrdenService.java`
- `backend/.../ordenes/repository/OrdenRepository.java`
- `backend/.../ordenes/dto/OrdenDTO.java`

## Backend
`obtenerHistorialOrdenes` llama `findByUsuarioIdOrderByCreadaEnDesc`.

## Frontend
`cargarOrdenes` almacena historial en la signal `ordenes`.

## Base de datos
Tabla `orden` con fechas de creación, ejecución y cancelación.

## API / Endpoints
- `GET /api/ordenes/historial`

## Validaciones
- JWT requerido.
- Usuario existente.

## Seguridad
La consulta se filtra por `usuarioId` derivado del token.

## Consideraciones técnicas
No hay paginación ni filtros; esto debe considerarse para crecimiento de datos.

## Dependencias
Depende de creación/cancelación de órdenes y Alpaca para sincronización.

## Criterios de aceptación
- [ ] Devuelve órdenes del usuario.
- [ ] Ordena por fecha descendente.
- [ ] Incluye estados ejecutados, cancelados y activos.
- [ ] No expone órdenes de otros usuarios.

## Notas
La HU de filtros está solo parcialmente cubierta.
