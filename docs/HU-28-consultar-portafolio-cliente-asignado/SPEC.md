# Historia de Usuario

## Titulo
Consulta de portafolio de cliente asignado.

## Descripcion
Como comisionista
Quiero ver el portafolio de mis clientes asignados
Para hacer seguimiento sin acceder a inversionistas que no me corresponden.

## Contexto
La HU-28 implementa control de acceso por relacion comisionista-cliente. Los comisionistas son usuarios con rol `COMISIONISTA`, MFA obligatorio y especialidades de mercado; la relacion se guarda en `asignacion_comisionista`.

## Flujo funcional
1. El comisionista inicia sesion y completa MFA.
2. Angular carga `/comisionista`.
3. El frontend llama `GET /api/comisionista/clientes`.
4. El comisionista selecciona un cliente asignado.
5. Frontend llama `GET /api/comisionista/clientes/{clienteId}/portafolio`.
6. Backend valida rol `COMISIONISTA` y relacion activa.
7. Se reutiliza `IOrden.obtenerPortafolio(clienteId)`.
8. Se retorna `PortafolioDTO`.

## Reglas de negocio
- Solo comisionistas autenticados pueden usar el modulo.
- Un comisionista solo ve inversionistas presentes en `asignacion_comisionista`.
- Si intenta consultar otro cliente, retorna 403.
- La consulta es solo lectura.

## Componentes involucrados
- `frontend/src/app/comisionista/comisionista-dashboard.component.ts`
- `backend/.../ordenes/controller/ComisionistaController.java`
- `backend/.../autenticacion/service/AsignacionComisionistaService.java`
- `backend/.../autenticacion/model/AsignacionComisionista.java`
- `backend/.../ordenes/service/OrdenService.java`
- `backend/.../ordenes/service/PortafolioService.java`

## Backend
`ComisionistaController.portafolioCliente` resuelve el usuario autenticado, exige rol `COMISIONISTA`, valida asignacion mediante `IAsignacionComisionista` y consulta el portafolio del inversionista con el servicio existente.

## Frontend
Existe ruta `/comisionista` con vista independiente del dashboard de inversionista. Muestra clientes asignados, portafolio, ordenes y propuestas.

## Base de datos
- `usuario`: comisionistas como usuarios con rol `COMISIONISTA`, `mfa_habilitado=true`, `especialidades_mercado`.
- `asignacion_comisionista`: `inversionista_id`, `comisionista_id`, intereses coincidentes, motivo y estado activo.
- `holding`: posiciones del inversionista.

## API / Endpoints
- `GET /api/comisionista/clientes`
- `GET /api/comisionista/clientes/{clienteId}/portafolio`

## Validaciones
- JWT requerido.
- Rol `COMISIONISTA`.
- Relacion activa entre comisionista e inversionista.

## Seguridad
Implementa EC-11: autorizacion por relacion, no solo por rol. Los accesos denegados se auditan con `ACCESO_DENEGADO_CLIENTE_NO_ASIGNADO`.

## Consideraciones tecnicas
La asignacion automatica se ejecuta al activar el registro si el inversionista solicito comisionista, eligiendo el comisionista con mas coincidencias de intereses y menor carga.

## Dependencias
Depende de autenticacion, asignacion de comisionistas, ordenes y portafolio.

## Criterios de aceptacion
- [x] Comisionista lista sus clientes asignados.
- [x] Comisionista consulta portafolio de cliente asignado.
- [x] Cliente no asignado retorna 403.
- [x] MFA es obligatorio para comisionista.

## Notas
Se agregan comisionistas seed de desarrollo con password inicial configurable por `COMISIONISTAS_PASSWORD_DEFAULT`.
