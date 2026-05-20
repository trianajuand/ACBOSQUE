# Historia de Usuario

## Título
Actualización de datos personales del inversionista.

## Descripción
Como inversionista autenticado
Quiero editar nombre, teléfono, experiencia e intereses de mercado
Para mantener actualizado mi perfil operativo.

## Contexto
La edición de perfil está disponible desde el panel `perfil` del dashboard. Cubre HU-7 en su alcance actual; la identidad/documento no se edita desde este flujo.

## Flujo funcional
1. Usuario entra al panel de perfil.
2. Frontend carga valores actuales en `perfilForm`.
3. Usuario edita nombre, teléfono, experiencia o intereses.
4. Angular envía `PUT /api/perfil`.
5. Backend actualiza campos permitidos y `fechaActualizacion`.
6. Audita `PERFIL_ACTUALIZADO`.
7. Frontend recarga perfil y mercado.

## Reglas de negocio
- `nombreCompleto` es obligatorio.
- Intereses se reemplazan por la lista enviada.
- El correo no se actualiza en este endpoint.
- El teléfono solo se actualiza si viene en el DTO.

## Componentes involucrados
- `frontend/src/app/dashboard/dashboard.component.ts`
- `frontend/src/app/dashboard/dashboard.component.html`
- `backend/.../autenticacion/controller/PerfilController.java`
- `backend/.../autenticacion/service/PerfilService.java`
- `backend/.../autenticacion/dto/ActualizarPerfilDTO.java`

## Backend
`PerfilService.actualizarDatos` busca al usuario autenticado, actualiza nombre, experiencia, intereses y teléfono, y persiste en transacción.

## Frontend
`guardarPerfil` arma intereses desde un input separado por comas, envía DTO y vuelve a cargar perfil y cotizaciones.

## Base de datos
Tabla `usuario`: `nombre_completo`, `nivel_experiencia`, `intereses_mercado`, `telefono`, `fecha_actualizacion`.

## API / Endpoints
- `PUT /api/perfil`

## Validaciones
- Bean Validation: `nombreCompleto` obligatorio.
- Angular: el formulario no bloquea todos los campos, pero el backend valida nombre.

## Seguridad
Endpoint protegido por JWT. El usuario solo modifica su propio registro identificado por `AuthenticationPrincipal`.

## Consideraciones técnicas
Los intereses se guardan como CSV; el cambio afecta el dashboard de mercado porque se recarga `GET /api/mercado/dashboard`.

## Dependencias
Depende de consulta de perfil y mercado dashboard.

## Criterios de aceptación
- [ ] Usuario autenticado puede actualizar datos permitidos.
- [ ] El correo no puede cambiarse por este endpoint.
- [ ] Se registra auditoría.
- [ ] Dashboard refleja nuevos intereses.

## Notas
Los datos de identidad capturados en registro no tienen flujo de edición dedicado.
