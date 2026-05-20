# Historia de Usuario

## Titulo
Colocacion de orden Stop Loss.

## Descripcion
Como inversionista autenticado
Quiero colocar una orden Stop Loss con precio stop
Para limitar perdidas segun mi estrategia.

## Contexto
HU-19 usa el flujo parametrizado de ordenes con tipoOrden=STOP_LOSS.

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
Implementado dentro de OrdenService.crearOrden usando precioStop.

## Frontend
Debe exponer controles claros para ejecutar la HU y mostrar resultado, error o estado vacio.

## Base de datos
Usa las tablas del dominio asociado y conserva trazabilidad historica cuando aplique.

## API / Endpoints
- POST /api/ordenes/previsualizar; POST /api/ordenes

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
- [ ] Flujo principal completado correctamente.
- [ ] Casos invalidos rechazados con mensaje claro.
- [ ] No se exponen recursos de otros usuarios.
- [ ] Evento sensible auditado.

## Notas
Spec creado para separar la HU en carpeta numerada.
