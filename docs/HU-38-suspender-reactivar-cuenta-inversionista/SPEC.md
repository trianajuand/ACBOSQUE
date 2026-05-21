# Historia de Usuario

## Titulo
Suspension, reactivacion y restriccion operativa de cuentas.

## Descripcion
Como administrador
Quiero cambiar el estado de una cuenta de inversionista o comisionista
Para controlar accesos y bloquear nuevas operaciones ante irregularidades.

## Contexto
HU-38 cubre suspension/reactivacion y se extiende con el estado `OPERACIONES_RESTRINGIDAS`, requerido para impedir nuevas ordenes sin eliminar la cuenta.

## Flujo funcional
1. Admin entra al panel Usuarios.
2. Angular lista usuarios desde `GET /api/admin/usuarios`.
3. Admin elige Activar, Suspender o Restringir.
4. Frontend llama `PUT /api/admin/usuarios/{usuarioId}/estado`.
5. Backend valida administrador.
6. Cambia `estado_cuenta` y actualiza fecha.
7. Audita `CAMBIO_ESTADO_CUENTA`.

## Reglas de negocio
- `ACTIVA` permite acceso normal.
- `INACTIVA` bloquea login.
- `OPERACIONES_RESTRINGIDAS` mantiene la cuenta bajo investigacion e impide nuevas ordenes.
- El cambio debe conservar trazabilidad.

## Componentes involucrados
- `frontend/src/app/admin/admin-dashboard.component.*`
- `backend/.../administracion/controller/AdminController.java`
- `backend/.../administracion/service/AdministracionService.java`
- `backend/.../administracion/dto/CambiarEstadoCuentaDTO.java`
- `backend/.../autenticacion/model/EstadoCuenta.java`
- `backend/.../ordenes/service/OrdenService.java`

## Backend
`cambiarEstadoUsuario` actualiza el estado de `Usuario`. `OrdenService` valida estados antes de nuevas ordenes y bloquea cuentas no habilitadas para operar.

## Frontend
La tabla de usuarios muestra estado y acciones rapidas para activar, suspender o restringir.

## Base de datos
Tabla `usuario`: columna `estado_cuenta`.

## API / Endpoints
- `PUT /api/admin/usuarios/{usuarioId}/estado`

## Validaciones
- JWT requerido.
- Rol `ADMINISTRADOR`.
- Estado valido en enum `EstadoCuenta`.
- Usuario existente.

## Seguridad
Evento sensible auditado. No se registra informacion sensible en logs.

## Consideraciones tecnicas
La vista usa `UsuarioAdminDTO`; no expone entidades JPA.

## Dependencias
Depende de Administracion, Autenticacion, Ordenes y Trazabilidad.

## Criterios de aceptacion
- [x] Admin activa una cuenta.
- [x] Admin suspende una cuenta.
- [x] Admin asigna `OPERACIONES_RESTRINGIDAS`.
- [x] El cambio queda auditado.
