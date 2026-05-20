# Historia de Usuario

## Título
Configuración de preferencias de operación.

## Descripción
Como inversionista autenticado
Quiero definir tipo de orden predeterminado y vista de portafolio
Para adaptar la plataforma a mi forma de operar.

## Contexto
La historia cubre HU-9. Las preferencias se usan al devolver el portafolio y para inicializar configuración del usuario.

## Flujo funcional
1. Usuario abre configuración.
2. Frontend carga valores desde perfil.
3. Usuario selecciona tipo de orden y vista.
4. Angular envía `PUT /api/perfil/preferencias/operacion`.
5. Backend actualiza `tipoOrdenDefault` y `vistaPortafolio`.
6. Se audita el cambio.

## Reglas de negocio
- Tipos esperados: `MARKET`, `LIMIT`, `STOP_LOSS`, `TAKE_PROFIT`.
- Vistas esperadas: `LISTA`, `GRAFICO`, `DETALLADA`.
- Si no hay vista al consultar portafolio, se usa `LISTA`.

## Componentes involucrados
- `frontend/src/app/dashboard/dashboard.component.ts`
- `frontend/src/app/dashboard/dashboard.component.html`
- `backend/.../autenticacion/controller/PerfilController.java`
- `backend/.../autenticacion/service/PerfilService.java`
- `backend/.../autenticacion/dto/PreferenciasOperacionDTO.java`
- `backend/.../ordenes/service/PortafolioService.java`

## Backend
`PerfilService.actualizarPreferenciasOperacion` guarda los valores en `Inversionista`. `OrdenService.obtenerPortafolio` consulta `inversionista.getVistaPortafolio()` y lo pasa a `PortafolioService`.

## Frontend
La configuración usa selects y guarda junto con preferencias de notificación.

## Base de datos
Tabla `inversionista`: `tipo_orden_default`, `vista_portafolio`, `fecha_actualizacion`.

## API / Endpoints
- `PUT /api/perfil/preferencias/operacion`
- `GET /api/portafolio` consume `vistaPortafolio`.

## Validaciones
- DTO sin Bean Validation; los valores se restringen principalmente por selects de frontend.

## Seguridad
Endpoint autenticado. Cambio auditado con `PREFERENCIAS_OPERACION_ACTUALIZADAS`.

## Consideraciones técnicas
No hay catálogo relacional para estas opciones; son strings controlados por UI y servicios.

## Dependencias
Depende de perfil y portafolio.

## Criterios de aceptación
- [ ] Usuario puede guardar tipo de orden predeterminado.
- [ ] Usuario puede guardar vista de portafolio.
- [ ] `GET /api/perfil` retorna los nuevos valores.
- [ ] Se audita el cambio.

## Notas
El formulario de creación de orden todavía inicializa `MARKET` localmente.
