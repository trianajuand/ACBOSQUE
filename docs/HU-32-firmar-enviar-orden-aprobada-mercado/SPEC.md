# Historia de Usuario

## Titulo
Firma y envio de orden aprobada al mercado.

## Descripcion
Como comisionista
Quiero firmar las ordenes aprobadas por el cliente y enviarlas al mercado
Para completar el flujo asesorado respetando la decision del inversionista.

## Contexto
HU-32 cierra el ciclo de propuesta. La firma cambia la propuesta aprobada a flujo normal de orden: valida fondos/holdings, horario y envia a Alpaca o encola.

## Flujo funcional
1. Inversionista aprueba propuesta.
2. Comisionista consulta `GET /api/comisionista/propuestas/aprobadas`.
3. Comisionista pulsa firmar.
4. Frontend llama `POST /api/comisionista/propuestas/{propuestaId}/firmar-enviar`.
5. Backend valida rol, asignacion activa, `comisionista_id` y estado `APROBADA`.
6. Recalcula monto/comision con precio vigente.
7. Reserva fondos o valida holdings.
8. Si mercado esta cerrado, queda `EN_COLA`; si esta abierto, se envia a Alpaca o se ejecuta internamente segun mercado.

## Reglas de negocio
- Solo el comisionista que propuso la orden puede firmarla.
- Solo propuestas `APROBADA` pueden firmarse.
- Deben aplicarse reglas existentes de ordenes: fondos, holdings, comision, horario y Alpaca.
- La comision se vincula al `comisionista_id` cuando la orden ejecuta.

## Componentes involucrados
- `frontend/src/app/comisionista/comisionista-dashboard.component.ts`
- `backend/.../ordenes/controller/ComisionistaController.java`
- `backend/.../ordenes/service/OrdenService.java`
- `backend/.../ordenes/model/Orden.java`
- `backend/.../integracion/adaptadores/alpaca/IIntegracionAlpaca.java`

## Backend
`OrdenService.firmarYEnviarPropuesta` valida la propuesta y llama el flujo de envio asesorado, reutilizando calculos y confirmacion de ejecucion de ordenes.

## Frontend
La vista de comisionista muestra propuestas aprobadas y boton `Firmar`.

## Base de datos
Tabla `orden`: `firmada_en`, `estado`, `alpaca_order_id`, `precio_ejecucion`.
Tabla `comision`: guarda `comisionista_id` y split plataforma/comisionista.

## API / Endpoints
- `GET /api/comisionista/propuestas/aprobadas`
- `POST /api/comisionista/propuestas/{propuestaId}/firmar-enviar`

## Validaciones
- JWT requerido.
- Rol `COMISIONISTA`.
- Propuesta pertenece al comisionista autenticado.
- Cliente sigue asignado.
- Estado `APROBADA`.

## Seguridad
La firma no acepta `clienteId` libre; se valida por `propuestaId`, `comisionista_id` y relacion activa.

## Consideraciones tecnicas
Si Alpaca falla, se conserva el estado pendiente y se libera reserva de fondos cuando corresponde.

## Dependencias
Depende de Alpaca, Mercado, Saldo, Portafolio, AsignacionComisionista y Auditoria.

## Criterios de aceptacion
- [x] Comisionista lista propuestas aprobadas.
- [x] Firma solo propuestas propias aprobadas.
- [x] Orden se envia, ejecuta internamente o encola segun mercado.
- [x] Se audita `PROPUESTA_ORDEN_FIRMADA`.

## Notas
El flujo mantiene MFA obligatorio desde login porque `AutenticacionService` fuerza MFA para `Rol.COMISIONISTA`.
