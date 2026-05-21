# Historia de Usuario

## Titulo
Configuracion de feriados por mercado.

## Descripcion
Como administrador
Quiero agregar y eliminar feriados en el calendario de cada mercado
Para evitar operaciones en dias no habiles.

## Contexto
HU-34 cubre EC-19. Los feriados se persisten en BD y son consultados desde Mercado por medio de `IAdministracion.esFeriadoMercado`.

## Flujo funcional
1. El administrador entra a `/admin`.
2. Selecciona un mercado en el panel Mercados.
3. Angular carga `GET /api/admin/mercados/{codigo}/feriados`.
4. El administrador registra fecha y descripcion.
5. Frontend envia `POST /api/admin/mercados/{codigo}/feriados`.
6. Backend evita duplicados por mercado y fecha.
7. El administrador puede eliminar un feriado con `DELETE`.
8. Cada cambio se audita.

## Reglas de negocio
- Un feriado se identifica por mercado y fecha.
- No se permiten feriados duplicados para el mismo mercado.
- Los feriados afectan operaciones futuras.
- Solo administradores con MFA y perfil manual pueden gestionarlos.

## Componentes involucrados
- `frontend/src/app/admin/admin-dashboard.component.*`
- `backend/.../administracion/controller/AdminController.java`
- `backend/.../administracion/service/AdministracionService.java`
- `backend/.../administracion/model/FeriadoMercado.java`
- `backend/.../administracion/repository/FeriadoMercadoRepository.java`
- `backend/.../mercado/service/MercadoService.java`

## Backend
`AdministracionService.crearFeriado` valida duplicados y guarda el registro. `esFeriadoMercado` retorna si la fecha actual esta bloqueada para el mercado.

## Frontend
El panel de mercados muestra feriados del mercado seleccionado y permite agregarlos o eliminarlos.

## Base de datos
Tabla `feriado_mercado`: mercado, fecha, descripcion y fecha de creacion.

## API / Endpoints
- `GET /api/admin/mercados/{codigo}/feriados`
- `POST /api/admin/mercados/{codigo}/feriados`
- `DELETE /api/admin/mercados/{codigo}/feriados/{feriadoId}`

## Validaciones
- Fecha y descripcion obligatorias.
- Mercado normalizado a codigo interno.
- Rechazo 409 si ya existe el feriado.

## Seguridad
Control por rol `ADMINISTRADOR`; auditoria `PARAMETRO_ADMIN_ACTUALIZADO`.

## Consideraciones tecnicas
La existencia de feriados se consulta desde Mercado sin importar si el cambio vino de UI o de carga manual.

## Dependencias
Depende de Administracion, Mercado y Trazabilidad.

## Criterios de aceptacion
- [x] Admin lista feriados por mercado.
- [x] Admin agrega feriado no duplicado.
- [x] Admin elimina feriado.
- [x] Mercado consulta feriados desde BD.
