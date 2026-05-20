# Historia de Usuario

## Titulo
Historial de ordenes filtrado por periodo.

## Descripcion
Como inversionista autenticado
Quiero consultar historial por rango de fechas
Para revisar operaciones de un periodo especifico.

## Contexto
HU-24 forma parte del bloque de historial con filtros. El endpoint de historial acepta rango de fechas y el dashboard Angular expone controles de periodo.

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
`GET /api/ordenes/historial` acepta `desde` y `hasta` en formato `YYYY-MM-DD`. `OrdenService.obtenerHistorialOrdenes` filtra por `creadaEn` del usuario autenticado.

## Frontend
Debe exponer controles claros para ejecutar la HU y mostrar resultado, error o estado vacio.

## Base de datos
Usa las tablas del dominio asociado y conserva trazabilidad historica cuando aplique.

## API / Endpoints
- `GET /api/ordenes/historial?desde={YYYY-MM-DD}&hasta={YYYY-MM-DD}`

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
- [x] Filtro de periodo conectado en frontend.

## Notas
Spec creado para separar la HU en carpeta numerada.
