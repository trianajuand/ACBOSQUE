# Historia de Usuario

## Titulo
Creacion de cuenta de comisionista por administrador.

## Descripcion
Como administrador
Quiero crear cuentas de comisionistas certificados
Para garantizar que solo asesores autorizados operen en la plataforma.

## Contexto
Solo el inversionista se auto-registra. Los comisionistas y el administrador se crean por un flujo separado: comisionistas desde `/admin`, administrador manualmente en BD con perfil en tabla `administrador`.

## Flujo funcional
1. El administrador inicia sesion y completa MFA.
2. En `/admin`, abre el panel Usuarios.
3. Diligencia nombre, correo, contrasenia inicial y especialidades de mercado.
4. Angular envia `POST /api/admin/comisionistas`.
5. Backend valida rol `ADMINISTRADOR`, cuenta activa, MFA y perfil manual.
6. `AdministracionService` crea `usuario` con rol `COMISIONISTA`, estado `ACTIVA` y MFA habilitado.
7. Crea perfil en tabla `comisionista`.
8. Audita la creacion.

## Reglas de negocio
- Un comisionista no puede auto-registrarse.
- El correo debe ser unico.
- La contrasenia se guarda con BCrypt.
- MFA queda obligatorio desde la creacion.
- Las especialidades se normalizan como CSV.

## Componentes involucrados
- `frontend/src/app/admin/admin-dashboard.component.*`
- `backend/.../administracion/controller/AdminController.java`
- `backend/.../administracion/service/AdministracionService.java`
- `backend/.../administracion/dto/CrearComisionistaDTO.java`
- `backend/.../autenticacion/model/Usuario.java`
- `backend/.../autenticacion/model/Comisionista.java`

## Backend
`crearComisionista` usa `PasswordEncoder`, persiste `Usuario` y `Comisionista`, y registra auditoria `USUARIO_ADMIN_GESTIONADO`.

## Frontend
La vista administrativa tiene formulario de creacion separado del registro publico de inversionistas.

## Base de datos
Tabla `usuario`: identidad, acceso, rol, estado y MFA. Tabla `comisionista`: perfil del asesor y especialidades.

## API / Endpoints
- `POST /api/admin/comisionistas`

## Validaciones
- JWT requerido.
- Rol `ADMINISTRADOR`.
- Perfil en `administrador`.
- Nombre, correo y contrasenia obligatorios.
- Contrasenia minima de 8 caracteres.

## Seguridad
El administrador tiene MFA obligatorio y debe existir manualmente en BD. El comisionista creado tambien queda con MFA obligatorio.

## Consideraciones tecnicas
No se exponen entidades JPA; se retorna `UsuarioAdminDTO`.

## Dependencias
Depende de Autenticacion, Administracion y Trazabilidad.

## Criterios de aceptacion
- [x] Admin crea comisionista.
- [x] Correo duplicado se rechaza.
- [x] Comisionista queda activo con MFA obligatorio.
- [x] Evento queda auditado.
