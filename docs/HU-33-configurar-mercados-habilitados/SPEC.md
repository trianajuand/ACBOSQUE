# Historia de Usuario

## Titulo
Configuracion de mercados habilitados y horarios.

## Descripcion
Como administrador
Quiero activar o desactivar mercados y ajustar sus horarios
Para controlar en que mercados puede operar la plataforma y aplicar cierres anticipados.

## Contexto
HU-33 pertenece al Servicio de Administracion. La configuracion se persiste en BD en `mercado_config` y es consumida por Mercado mediante la interfaz `IAdministracion`.

## Flujo funcional
1. El administrador inicia sesion con MFA obligatorio.
2. Angular carga `/admin` y solicita `GET /api/admin/mercados`.
3. Selecciona un mercado y modifica estado, zona horaria, apertura, cierre o cierre anticipado.
4. Frontend envia `PUT /api/admin/mercados/{codigo}`.
5. Backend valida rol `ADMINISTRADOR`, cuenta activa, MFA habilitado y perfil en tabla `administrador`.
6. `AdministracionService` guarda la configuracion y audita el cambio.
7. Mercado usa esa configuracion para determinar si un mercado esta abierto.

## Reglas de negocio
- Solo un usuario con rol `ADMINISTRADOR` y perfil manual en `administrador` puede modificar mercados.
- `habilitado=false` impide considerar el mercado abierto.
- `cierreAnticipado` reemplaza temporalmente la hora regular de cierre.
- Los cambios aplican a operaciones futuras.

## Componentes involucrados
- `frontend/src/app/admin/admin-dashboard.component.*`
- `backend/.../administracion/controller/AdminController.java`
- `backend/.../administracion/service/AdministracionService.java`
- `backend/.../administracion/model/MercadoConfig.java`
- `backend/.../administracion/repository/MercadoConfigRepository.java`
- `backend/.../mercado/service/MercadoService.java`

## Backend
`AdminController` expone `GET /api/admin/mercados` y `PUT /api/admin/mercados/{codigo}`. `AdministracionService` implementa `IAdministracion.obtenerConfiguracionMercado`, usado por `MercadoService`.

## Frontend
La vista de administrador tiene panel propio de mercados, listado de mercados y formulario para horario, zona horaria, estado y cierre anticipado.

## Base de datos
Tabla `mercado_config`: codigo, nombre, zona horaria, hora de apertura, hora de cierre, habilitado y cierre anticipado.

## API / Endpoints
- `GET /api/admin/mercados`
- `PUT /api/admin/mercados/{codigo}`

## Validaciones
- JWT requerido.
- Rol `ADMINISTRADOR`.
- Perfil existente en tabla `administrador`.
- DTO con codigo/nombre/zona/horarios obligatorios.

## Seguridad
El administrador no se auto-registra; se crea manualmente en BD. MFA es obligatorio para login y para acceder al modulo.

## Consideraciones tecnicas
La configuracion se consume por interfaz para respetar la arquitectura SOA consolidada.

## Dependencias
Depende de Autenticacion, Administracion, Mercado y Trazabilidad.

## Criterios de aceptacion
- [x] Admin lista mercados desde BD.
- [x] Admin activa o desactiva mercados.
- [x] Admin modifica horarios y cierre anticipado.
- [x] Cambios quedan auditados.
