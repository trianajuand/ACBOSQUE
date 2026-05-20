# Historia de Usuario

## Título
Consulta de perfil del inversionista.

## Descripción
Como inversionista autenticado
Quiero ver mi información personal, preferencias y estado de suscripción
Para conocer y administrar los datos asociados a mi cuenta.

## Contexto
El dashboard Angular carga el perfil al iniciar y lo usa para poblar sidebar, formularios, preferencias y mercado de interés. Cubre HU-6.

## Flujo funcional
1. El usuario inicia sesión y entra a `/dashboard`.
2. Angular llama `GET /api/perfil` con JWT.
3. Backend resuelve el correo desde `AuthenticationPrincipal`.
4. `PerfilService` busca `Usuario`.
5. Mapea entidad a `PerfilInversionistaDTO`.
6. Audita `PERFIL_CONSULTADO`.
7. Frontend actualiza formularios y paneles.

## Reglas de negocio
- Solo usuarios autenticados pueden consultar perfil.
- La respuesta es DTO, no entidad JPA.
- Intereses y tipos de notificación se devuelven como listas, aunque se almacenen CSV.

## Componentes involucrados
- `frontend/src/app/dashboard/dashboard.component.ts`
- `frontend/src/app/dashboard/dashboard.component.html`
- `frontend/src/app/core/models.ts`
- `backend/.../autenticacion/controller/PerfilController.java`
- `backend/.../autenticacion/service/PerfilService.java`
- `backend/.../autenticacion/dto/PerfilInversionistaDTO.java`
- `backend/.../autenticacion/model/Usuario.java`

## Backend
`PerfilController` expone `GET /api/perfil`. `PerfilService.mapearDTO` devuelve datos personales, identidad, dirección, experiencia, intereses, MFA, plan, premium, notificaciones y preferencias de operación.

## Frontend
`DashboardComponent.cargarPerfil` actualiza `perfil`, `perfilForm` y `preferenciasForm`.

## Base de datos
Tabla `usuario` contiene todos los datos retornados. No se exponen columnas sensibles como `contrasenia`.

## API / Endpoints
- `GET /api/perfil`

## Validaciones
- JWT requerido.
- Si el correo no existe, se lanza `UsuarioNoEncontradoException`.

## Seguridad
El correo proviene del token y no de un parámetro manipulable. La respuesta excluye contraseña y tokens.

## Consideraciones técnicas
El mapeo CSV se hace con `splitCsv`, que retorna lista vacía para valores nulos o blancos.

## Dependencias
Depende de login/JWT y de la entidad `Usuario`.

## Criterios de aceptación
- [ ] Un usuario autenticado recibe su perfil.
- [ ] No se retorna contraseña.
- [ ] Se audita la consulta.
- [ ] El frontend refleja datos en formularios y paneles.

## Notas
La vista de dashboard usa el perfil para filtrar símbolos preferidos.
