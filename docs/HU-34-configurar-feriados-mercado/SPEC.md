# Historia de Usuario

## Titulo
Configuracion de feriados de mercado.

## Descripcion
Como administrador
Quiero agregar feriados al calendario de mercado
Para evitar operaciones en dias no habiles.

## Contexto
HU-34 cubre EC-19. Hoy no se contemplan feriados.

## Flujo funcional
1. El actor abre la funcionalidad correspondiente.
2. El frontend envia la solicitud al backend.
3. El backend valida rol, relacion y datos.
4. El servicio de dominio ejecuta la operacion.
5. Se registra auditoria y se retorna respuesta DTO.

## Reglas de negocio
- Respetar rol autorizado para la HU.
- Validar propiedad o asignacion antes de consultar/modificar datos.
- No exponer entidades JPA ni datos sensibles.

## Componentes involucrados
- Frontend Angular del rol correspondiente.
- Servicio backend segun SOA.
- Repositorios/modelos del dominio.
- IAuditLog para eventos sensibles.

## Backend
Pendiente tabla de feriados y consulta desde MercadoService.

## Frontend
Pendiente o existente segun estado; debe mostrar confirmacion, errores y estado vacio.

## Base de datos
Usa o requiere tablas del dominio asociado.

## API / Endpoints
- Pendiente: /api/admin/mercados/{codigo}/feriados

## Validaciones
- JWT y rol requerido.
- Datos obligatorios con formato valido.
- Relacion comisionista-cliente cuando aplique.

## Seguridad
Control de acceso por rol y relacion, con auditoria de accesos denegados.

## Consideraciones tecnicas
Debe seguir la arquitectura SOA consolidada y consumir otros servicios por interfaces.

## Dependencias
Depende de los servicios relacionados y trazabilidad.

## Criterios de aceptacion
- [ ] Flujo principal cumple la HU.
- [ ] Usuario sin permiso recibe rechazo.
- [ ] Respuesta no filtra datos sensibles.
- [ ] Evento relevante queda auditado.

## Notas
Spec creado para la carpeta numerada de la HU.
