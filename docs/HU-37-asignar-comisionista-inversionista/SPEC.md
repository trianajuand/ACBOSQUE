# Historia de Usuario

## Titulo
Asignacion manual de comisionista a inversionista.

## Descripcion
Como administrador
Quiero asignar un comisionista a un inversionista
Para habilitar seguimiento, consulta de portafolio y propuestas asesoradas.

## Contexto
HU-37 es la base del control por relacion usado por HU-28 a HU-32. La asignacion manual se gestiona desde el modulo administrador.

## Flujo funcional
1. Admin accede a `/admin` con MFA.
2. Angular carga usuarios con `GET /api/admin/usuarios`.
3. En el panel Asignaciones selecciona un comisionista para un inversionista.
4. Frontend llama `PUT /api/admin/inversionistas/{inversionistaId}/comisionista/{comisionistaId}`.
5. Backend valida que el destino sea inversionista y el comisionista este activo.
6. Desactiva asignacion anterior si existe.
7. Crea nueva asignacion activa.
8. Audita `COMISIONISTA_ASIGNADO`.

## Reglas de negocio
- Solo inversionistas pueden recibir comisionista.
- Solo comisionistas activos pueden asignarse.
- Un inversionista tiene una asignacion activa a la vez.
- Asignaciones anteriores se conservan como historico inactivo.

## Componentes involucrados
- `frontend/src/app/admin/admin-dashboard.component.*`
- `backend/.../administracion/controller/AdminController.java`
- `backend/.../administracion/service/AdministracionService.java`
- `backend/.../autenticacion/model/AsignacionComisionista.java`
- `backend/.../autenticacion/repository/AsignacionComisionistaRepository.java`

## Backend
`asignarComisionista` valida tipos de usuario y estado, actualiza la asignacion activa y retorna el inversionista como `UsuarioAdminDTO`.

## Frontend
El panel Usuarios muestra inversionistas, comisionista actual y selector de comisionistas activos.

## Base de datos
Tabla `asignacion_comisionista`: inversionista, comisionista, motivo, estado activo y fecha.

## API / Endpoints
- `GET /api/admin/usuarios`
- `PUT /api/admin/inversionistas/{inversionistaId}/comisionista/{comisionistaId}`

## Validaciones
- JWT y rol `ADMINISTRADOR`.
- Inversionista y comisionista existentes.
- Comisionista con estado `ACTIVA`.

## Seguridad
El control por relacion se valida en backend y no depende de lo que oculte el frontend.

## Consideraciones tecnicas
La asignacion alimenta endpoints del modulo comisionista y evita acceso a clientes no asignados.

## Dependencias
Depende de Administracion, Autenticacion, Comisionista y Trazabilidad.

## Criterios de aceptacion
- [x] Admin lista inversionistas y comisionistas.
- [x] Admin asigna comisionista activo.
- [x] Asignacion anterior queda inactiva.
- [x] Evento queda auditado.
