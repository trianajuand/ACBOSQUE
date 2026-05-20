# Historia de Usuario

## Título
Configuración de preferencias de notificación.

## Descripción
Como inversionista autenticado
Quiero configurar canales y tipos de notificación
Para recibir comunicaciones por los medios que prefiera.

## Contexto
La preferencia se gestiona desde configuración en Angular y se persiste en columnas del perfil `inversionista`. Cubre HU-8.

## Flujo funcional
1. Usuario abre configuración.
2. Frontend carga preferencias desde `GET /api/perfil`.
3. Usuario activa o desactiva Email, SMS y WhatsApp.
4. Angular envía `PUT /api/perfil/preferencias/notificaciones`.
5. Backend guarda canales y tipos de notificación.
6. Se audita `PREFERENCIAS_NOTIFICACION_ACTUALIZADAS`.

## Reglas de negocio
- Los canales se almacenan como booleanos.
- Los tipos de notificación se almacenan como CSV si se envían.
- Email por defecto se interpreta activo si `notificacionEmail` no es explícitamente `false`.

## Componentes involucrados
- `frontend/src/app/dashboard/dashboard.component.ts`
- `frontend/src/app/dashboard/dashboard.component.html`
- `backend/.../autenticacion/controller/PerfilController.java`
- `backend/.../autenticacion/service/PerfilService.java`
- `backend/.../autenticacion/dto/PreferenciasNotificacionDTO.java`
- `backend/.../autenticacion/model/Usuario.java`

## Backend
`PerfilService.actualizarPreferenciasNotificacion` persiste `notificacionEmail`, `notificacionSms`, `notificacionWhatsapp` y `tiposNotificacion`.

## Frontend
`guardarPreferencias` envía notificaciones y operación en paralelo. Actualmente envía tipos `ORDER_EXECUTED` y `LOGIN`.

## Base de datos
Tabla `inversionista`: `notificacion_email`, `notificacion_sms`, `notificacion_whatsapp`, `tipos_notificacion`.

## API / Endpoints
- `PUT /api/perfil/preferencias/notificaciones`

## Validaciones
- DTO sin Bean Validation estricta; acepta booleanos y lista opcional.
- Endpoint requiere usuario autenticado.

## Seguridad
El usuario modifica solo sus preferencias. Auditoría transversal vía `IAuditLog`.

## Consideraciones técnicas
Aunque el sistema solo envía email actualmente, el modelo ya guarda SMS y WhatsApp para extensión posterior.

## Dependencias
Depende de perfil y del despachador de notificaciones para uso futuro.

## Criterios de aceptación
- [ ] Canales seleccionados se persisten.
- [ ] Los tipos enviados se guardan.
- [ ] La UI muestra confirmación o error.
- [ ] Se audita el cambio.

## Notas
No existen adaptadores SMS/WhatsApp implementados en el código actual.
