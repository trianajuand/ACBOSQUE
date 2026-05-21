# Historia de Usuario

## Titulo
Baja logica de inversionistas y comisionistas.

## Descripcion
Como administrador
Quiero dar de baja cuentas de inversionistas o comisionistas
Para retirar usuarios que no deben operar sin perder trazabilidad historica.

## Contexto
HU-39 se implementa como baja logica: la cuenta pasa a `INACTIVA`. No se elimina fisicamente para conservar historial financiero, ordenes, comisiones y auditoria.

## Flujo funcional
1. Admin entra a `/admin`.
2. Selecciona un usuario no administrador en la tabla de usuarios.
3. Pulsa Dar baja.
4. Frontend llama `DELETE /api/admin/usuarios/{usuarioId}`.
5. Backend valida administrador.
6. Si el usuario no es admin, cambia estado a `INACTIVA`.
7. Si era inversionista con asignacion activa, la desactiva.
8. Audita la baja.

## Reglas de negocio
- No se eliminan administradores desde este modulo.
- La baja es logica.
- El usuario inactivo no puede operar ni acceder normalmente.
- Las asignaciones activas del inversionista quedan inactivas.

## Componentes involucrados
- `frontend/src/app/admin/admin-dashboard.component.*`
- `backend/.../administracion/controller/AdminController.java`
- `backend/.../administracion/service/AdministracionService.java`
- `backend/.../autenticacion/model/Usuario.java`
- `backend/.../autenticacion/model/AsignacionComisionista.java`

## Backend
`eliminarUsuario` cambia `estadoCuenta` a `INACTIVA`, desactiva asignaciones de inversionista y registra `USUARIO_ADMIN_GESTIONADO`.

## Frontend
La accion Dar baja se muestra para usuarios no administradores.

## Base de datos
Tabla `usuario` conserva el registro con estado `INACTIVA`. Tabla `asignacion_comisionista` conserva historico.

## API / Endpoints
- `DELETE /api/admin/usuarios/{usuarioId}`

## Validaciones
- JWT requerido.
- Rol `ADMINISTRADOR`.
- Usuario existente.
- Rechaza baja de administradores.

## Seguridad
Conserva evidencia y evita borrado fisico de datos financieros.

## Consideraciones tecnicas
La accion retorna mensaje de exito y la UI recarga la lista de usuarios.

## Dependencias
Depende de Administracion, Autenticacion y Trazabilidad.

## Criterios de aceptacion
- [x] Admin da baja logica a inversionista.
- [x] Admin da baja logica a comisionista.
- [x] Admin no puede eliminar administradores desde UI.
- [x] Evento queda auditado.
