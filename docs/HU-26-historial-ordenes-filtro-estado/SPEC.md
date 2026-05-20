# Historia de Usuario

## Titulo
Historial de ordenes filtrado por estado.

## Descripcion
Como inversionista autenticado
Quiero filtrar historial por estado
Para diferenciar ordenes ejecutadas, canceladas, activas o en cola.

## Contexto
HU-26 forma parte del bloque de historial con filtros. El endpoint de historial acepta estado y el dashboard Angular expone un selector con estados disponibles.

## Flujo funcional
1. El usuario inicia el flujo desde la interfaz correspondiente.
2. El frontend envia la solicitud al backend.
3. El backend valida rol, datos y reglas de negocio.
4. El sistema persiste cambios o retorna la informacion solicitada.
5. Se registra auditoria cuando la operacion es sensible.

## Reglas de negocio
- La operacion debe respetar rol y propiedad del recurso.
- Los datos se devuelven mediante DTOs, no entidades JPA.
- Los errores deben mostrarse en espanol y sin datos sensibles.

## Componentes involucrados
- Frontend Angular de la funcionalidad.
- Servicio backend responsable segun arquitectura SOA.
- Repositorios/modelos del dominio relacionado.
- Servicio de trazabilidad cuando aplique.

## Backend
`GET /api/ordenes/historial` acepta `estado`. `OrdenService.obtenerHistorialOrdenes` valida el enum `EstadoOrden` y filtra antes de mapear a DTO.

## Frontend
Debe exponer controles claros para ejecutar la HU y mostrar resultado, error o estado vacio.

## Base de datos
Usa las tablas del dominio asociado y conserva trazabilidad historica cuando aplique.

## API / Endpoints
- `GET /api/ordenes/historial?estado={PENDIENTE|ENVIADA|EJECUTADA|CANCELADA|EN_COLA|PENDIENTE_APROBACION|APROBADA|RECHAZADA}`

## Validaciones
- JWT requerido salvo flujos publicos documentados.
- Datos obligatorios y formatos validos.
- Reglas de negocio verificadas en backend.

## Seguridad
Control de acceso por rol y por relacion cuando aplique. No se exponen secretos ni datos sensibles.

## Consideraciones tecnicas
La implementacion debe seguir ARQUITECTURA.md y CONVENCIONES.md.

## Dependencias
Depende de los servicios indicados en el backlog y de auditoria transversal.

## Criterios de aceptacion
- [x] Flujo principal completado correctamente.
- [x] Casos invalidos rechazados con mensaje claro.
- [x] No se exponen recursos de otros usuarios.
- [x] Filtro de estado conectado en frontend.

## Notas
Spec creado para separar la HU en carpeta numerada.
