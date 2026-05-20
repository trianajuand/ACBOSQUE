# Historia de Usuario

## Titulo
Propuesta de orden para cliente.

## Descripcion
Como comisionista
Quiero crear una propuesta de orden para un cliente asignado
Para recomendar una operacion que el inversionista pueda aprobar o rechazar.

## Contexto
Una propuesta se guarda como una `orden` con estado `PENDIENTE_APROBACION`, `comisionista_id` y comentario del comisionista. No se envia a Alpaca ni reserva fondos hasta que el inversionista apruebe y el comisionista firme.

## Flujo funcional
1. Comisionista selecciona cliente.
2. Completa simbolo, tipo, lado, cantidad y precios.
3. Frontend llama `POST /api/comisionista/clientes/{clienteId}/propuestas`.
4. Backend valida rol, asignacion y simbolo operable.
5. Calcula monto/comision estimados.
6. Guarda la propuesta como `PENDIENTE_APROBACION`.
7. La propuesta queda visible para el inversionista en `/api/propuestas`.

## Reglas de negocio
- Solo comisionistas pueden proponer.
- Solo para clientes asignados.
- La propuesta no ejecuta ni reserva recursos.
- Debe conservar comentario/recomendacion del comisionista.

## Componentes involucrados
- `frontend/src/app/comisionista/comisionista-dashboard.component.ts`
- `backend/.../ordenes/controller/ComisionistaController.java`
- `backend/.../ordenes/dto/CrearPropuestaOrdenDTO.java`
- `backend/.../ordenes/service/OrdenService.java`
- `backend/.../ordenes/model/Orden.java`

## Backend
`OrdenService.crearPropuestaOrden` crea la entidad con `estado=PENDIENTE_APROBACION`, `usuarioId=clienteId` y `comisionistaId` del usuario autenticado.

## Frontend
La pantalla de comisionista incluye formulario de propuesta y selector de cliente asignado.

## Base de datos
Tabla `orden`: se usan columnas de orden normal mas `comisionista_id`, `comentario_comisionista`, fechas de decision/firma.

## API / Endpoints
- `POST /api/comisionista/clientes/{clienteId}/propuestas`
- `GET /api/propuestas` para que el inversionista vea pendientes.

## Validaciones
- JWT requerido.
- Rol `COMISIONISTA`.
- Cliente asignado.
- Simbolo, tipo, lado y cantidad validos.

## Seguridad
El `clienteId` de URL se valida contra `asignacion_comisionista`. Un comisionista no puede proponer para clientes ajenos.

## Consideraciones tecnicas
Se reutiliza `Orden` para evitar duplicar reglas financieras; la diferencia esta en el estado y en que la ejecucion se difiere.

## Dependencias
Depende de Mercado, Ordenes, asignaciones y auditoria.

## Criterios de aceptacion
- [x] Crea propuesta para cliente asignado.
- [x] Queda `PENDIENTE_APROBACION`.
- [x] No se envia a Alpaca al crearla.
- [x] Se audita `PROPUESTA_ORDEN_CREADA`.

## Notas
La notificacion multicanal al inversionista queda preparada para integrarse con HU-41.
