# Historia de Usuario

## Titulo
Aprobacion o rechazo de propuesta del comisionista.

## Descripcion
Como inversionista
Quiero aprobar o rechazar propuestas recibidas
Para mantener control final sobre las operaciones recomendadas por mi comisionista.

## Contexto
El inversionista consulta sus propuestas pendientes en su dashboard y decide. Una propuesta aprobada queda en `APROBADA`; una rechazada queda en `RECHAZADA`.

## Flujo funcional
1. Inversionista inicia sesion.
2. Dashboard carga `GET /api/propuestas`.
3. Usuario aprueba o rechaza una propuesta.
4. Backend valida que la propuesta pertenece al inversionista autenticado.
5. Cambia estado y guarda comentario opcional.
6. Se audita la decision.

## Reglas de negocio
- Solo el inversionista propietario decide.
- Solo propuestas `PENDIENTE_APROBACION` pueden aprobarse o rechazarse.
- Aprobar no envia al mercado; falta firma del comisionista.
- Rechazar cierra la propuesta.

## Componentes involucrados
- `frontend/src/app/dashboard/dashboard.component.ts`
- `backend/.../ordenes/controller/PropuestaController.java`
- `backend/.../ordenes/dto/DecisionPropuestaDTO.java`
- `backend/.../ordenes/service/OrdenService.java`
- `backend/.../ordenes/model/EstadoOrden.java`

## Backend
`PropuestaController` expone listado, aprobar y rechazar. `OrdenService.aprobarPropuesta` y `rechazarPropuesta` validan propietario y estado.

## Frontend
El dashboard del inversionista tiene panel `Propuestas` con acciones Aprobar/Rechazar.

## Base de datos
Tabla `orden`: `estado`, `comentario_inversionista`, `aprobada_en`, `rechazada_en`.

## API / Endpoints
- `GET /api/propuestas`
- `POST /api/propuestas/{propuestaId}/aprobar`
- `POST /api/propuestas/{propuestaId}/rechazar`

## Validaciones
- JWT requerido.
- Rol `INVERSIONISTA` o `INVERSIONISTA_PREMIUM`.
- Propuesta pertenece al usuario autenticado.
- Estado debe ser `PENDIENTE_APROBACION`.

## Seguridad
No se acepta `usuarioId` desde frontend; se deriva del token. Propuestas ajenas retornan no encontradas o prohibidas segun flujo.

## Consideraciones tecnicas
Despues de aprobar, el comisionista ve la propuesta en `GET /api/comisionista/propuestas/aprobadas`.

## Dependencias
Depende de propuestas, ordenes y auditoria.

## Criterios de aceptacion
- [x] Inversionista lista propuestas pendientes.
- [x] Inversionista aprueba propuesta propia.
- [x] Inversionista rechaza propuesta propia.
- [x] Decision queda auditada.

## Notas
La UI usa comentarios por defecto; se puede ampliar a campo editable.
