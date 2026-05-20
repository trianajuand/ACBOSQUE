# Historia de Usuario

## Título
Cierre de sesión desde el dashboard.

## Descripción
Como usuario autenticado
Quiero cerrar sesión desde la aplicación
Para abandonar mi sesión local y registrar el evento de salida.

## Contexto
El logout implementado cubre el flujo UI y auditoría, pero no invalida tokens en BD. La historia HU-5 está parcialmente implementada frente al requerimiento de revocación JWT.

## Flujo funcional
1. El usuario pulsa cerrar sesión en el dashboard.
2. Angular llama `POST /api/auth/logout` con JWT.
3. Backend extrae el correo desde el token y audita `LOGOUT`.
4. Angular elimina el token local.
5. Angular navega a `/login`.

## Reglas de negocio
- Solo usuarios autenticados pueden invocar logout.
- El cierre elimina el token local del navegador.
- La auditoría es obligatoria.

## Componentes involucrados
- `frontend/src/app/dashboard/dashboard.component.ts`
- `frontend/src/app/core/api.service.ts`
- `backend/.../autenticacion/controller/AuthController.java`
- `backend/.../autenticacion/service/AutenticacionService.java`
- `backend/.../autenticacion/security/JwtUtil.java`

## Backend
`AutenticacionService.cerrarSesion` toma el header `Authorization`, remueve el prefijo `Bearer` y registra el evento.

## Frontend
`DashboardComponent.cerrarSesion` intenta llamar logout, limpia `localStorage` y redirige a login aun si el backend falla.

## Base de datos
No hay tabla `token_revocado` implementada. Se persiste auditoría en `evento_auditoria`.

## API / Endpoints
- `POST /api/auth/logout`

## Validaciones
- Header `Authorization` requerido por el método controller.
- Token debe poder parsearse para auditar correo.

## Seguridad
El endpoint está protegido por JWT. La sesión local se elimina; la revocación server-side queda pendiente.

## Consideraciones técnicas
La implementación actual no cumple completamente HU-5 porque el filtro JWT no consulta tokens revocados.

## Dependencias
Depende de login/JWT y auditoría.

## Criterios de aceptación
- [ ] El usuario puede cerrar sesión desde dashboard.
- [ ] El token local se elimina.
- [ ] El evento `LOGOUT` se registra.
- [ ] El usuario vuelve a `/login`.

## Notas
Para cumplimiento completo se debe agregar entidad/repositorio `TokenRevocado` y validación en `JwtAuthenticationFilter`.
