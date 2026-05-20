# Historia de Usuario

## Titulo
Creacion de cuenta de Comisionista.

## Descripcion
Como administrador
Quiero crear cuentas de comisionista
Para habilitar asesores que no se auto-registran.

## Contexto
HU-36 pertenece a Gestion de Usuarios. Solo INVERSIONISTA se auto-registra.

## Flujo funcional
1. El actor autorizado abre la funcionalidad.
2. El frontend envia la solicitud al backend.
3. Backend valida rol, estado y datos de entrada.
4. El servicio aplica la regla de negocio y persiste cambios si corresponde.
5. Se audita el evento y se notifica cuando aplica.

## Reglas de negocio
- Operacion protegida por rol.
- Mantener integridad historica y trazabilidad.
- No exponer datos sensibles.

## Componentes involucrados
- Frontend Angular del rol correspondiente.
- Servicio backend propietario de la funcionalidad.
- Modelos y repositorios del dominio.
- IAuditLog e INotificacion cuando aplique.

## Backend
Pendiente endpoint administrativo para crear usuarios con rol COMISIONISTA y MFA obligatorio.

## Frontend
Debe presentar controles de accion, confirmacion para cambios criticos y mensajes claros.

## Base de datos
Usa tablas del dominio y conserva registros historicos cuando sea necesario.

## API / Endpoints
- Pendiente: POST /api/admin/comisionistas

## Validaciones
- JWT requerido.
- Rol correcto.
- Datos obligatorios y transicion de estado valida.

## Seguridad
Control de acceso por rol. Eventos criticos auditados sin registrar secretos.

## Consideraciones tecnicas
Debe alinearse con los servicios SOA y consumir integraciones por interfaces.

## Dependencias
Depende de Autenticacion, Administracion, Integracion y Trazabilidad segun caso.

## Criterios de aceptacion
- [ ] Flujo principal completado.
- [ ] Acceso no autorizado rechazado.
- [ ] Persistencia o consulta coherente.
- [ ] Auditoria registrada.

## Notas
Spec creado para completar carpeta numerada del backlog.
