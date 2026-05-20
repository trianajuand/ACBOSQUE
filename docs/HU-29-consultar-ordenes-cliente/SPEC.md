# Historia de Usuario

## Titulo
Consulta de ordenes activas e historicas del cliente.

## Descripcion
Como comisionista
Quiero consultar ordenes activas e historicas de mis clientes asignados
Para revisar su actividad operativa y hacer recomendaciones informadas.

## Contexto
La HU-29 extiende la vista del comisionista usando el mismo modelo `orden`, pero obliga a validar la relacion comisionista-cliente antes de devolver datos.

## Flujo funcional
1. Comisionista entra a `/comisionista`.
2. Selecciona un cliente asignado.
3. Frontend llama endpoints de ordenes activas e historial.
4. Backend valida rol y asignacion.
5. `OrdenService` sincroniza ordenes enviadas con Alpaca cuando aplica.
6. Se retornan listas `OrdenDTO`.

## Reglas de negocio
- Solo se consultan ordenes de clientes asignados.
- Ordenes activas incluyen `PENDIENTE`, `ENVIADA`, `EN_COLA` y `PENDIENTE_APROBACION`.
- Historial incluye todos los estados.
- La consulta es de solo lectura.

## Componentes involucrados
- `frontend/src/app/comisionista/comisionista-dashboard.component.ts`
- `backend/.../ordenes/controller/ComisionistaController.java`
- `backend/.../ordenes/service/OrdenService.java`
- `backend/.../ordenes/repository/OrdenRepository.java`
- `backend/.../autenticacion/interfaces/IAsignacionComisionista.java`

## Backend
`ComisionistaController` expone endpoints especificos para clientes asignados y delega en `IOrden.obtenerOrdenesActivas` / `obtenerHistorialOrdenes` despues de validar permisos.

## Frontend
La vista de comisionista muestra dos tablas: ordenes activas e historial del cliente seleccionado.

## Base de datos
Tablas `orden`, `usuario`, `asignacion_comisionista`; puede actualizar ordenes enviadas si Alpaca reporta ejecucion.

## API / Endpoints
- `GET /api/comisionista/clientes/{clienteId}/ordenes/activas`
- `GET /api/comisionista/clientes/{clienteId}/ordenes/historial`

## Validaciones
- JWT requerido.
- Rol `COMISIONISTA`.
- Cliente asignado y activo en la relacion.

## Seguridad
No existe parametro para consultar cualquier usuario sin validacion. La relacion se revisa en backend aunque el frontend oculte clientes no asignados.

## Consideraciones tecnicas
La consulta puede tener efectos laterales por sincronizacion de fills con Alpaca, igual que en el flujo del inversionista.

## Dependencias
Depende de Ordenes, Alpaca, AsignacionComisionista y Auditoria.

## Criterios de aceptacion
- [x] Comisionista ve ordenes activas de cliente asignado.
- [x] Comisionista ve historial de cliente asignado.
- [x] No puede consultar clientes no asignados.
- [x] Reutiliza DTOs de orden existentes.

## Notas
Los filtros finos de historial siguen siendo alcance de HU-24 a HU-26.
