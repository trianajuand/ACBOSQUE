# Historia de Usuario

## Titulo
Dashboard ejecutivo de metricas administrativas.

## Descripcion
Como administrador
Quiero consultar metricas clave de la plataforma por periodo y mercado
Para monitorear rendimiento operativo y financiero y apoyar decisiones estrategicas.

## Contexto
HU-48 estaba listada fuera del MVP, pero el modulo administrador actual ya implementa un dashboard ejecutivo basico. Se documenta como funcionalidad implementada en Administracion.

## Flujo funcional
1. El administrador inicia sesion con MFA obligatorio.
2. Angular navega a `/admin`.
3. El panel Dashboard solicita `GET /api/admin/dashboard`.
4. El administrador puede filtrar por `desde`, `hasta` y `mercado`.
5. Backend valida rol, estado, MFA y perfil manual en `administrador`.
6. `AdministracionService` calcula usuarios activos, crecimiento, transacciones, volumen, comisiones y tendencias por mercado.
7. Frontend muestra tarjetas y barras comparativas por mercado.

## Reglas de negocio
- Las metricas se calculan desde BD, no desde datos hardcodeados.
- El periodo por defecto es ultimo mes hasta el dia actual.
- Filtro de mercado usa codigo normalizado.
- Solo administradores pueden consultar metricas.

## Componentes involucrados
- `frontend/src/app/admin/admin-dashboard.component.*`
- `backend/.../administracion/controller/AdminController.java`
- `backend/.../administracion/service/AdministracionService.java`
- `backend/.../administracion/dto/DashboardEjecutivoDTO.java`
- `backend/.../administracion/dto/TendenciaMercadoDTO.java`
- `backend/.../ordenes/repository/OrdenRepository.java`
- `backend/.../ordenes/repository/ComisionRepository.java`
- `backend/.../autenticacion/repository/UsuarioRepository.java`

## Backend
`obtenerDashboard` recorre usuarios, ordenes y comisiones en BD, aplica rango de fechas y mercado opcional, y agrupa tendencias por mercado detectado desde el simbolo.

## Frontend
El panel administrador usa una vista independiente del dashboard de inversionista/comisionista. Muestra filtros, metricas clave y tendencias visuales por mercado.

## Base de datos
Lee tablas `usuario`, `orden` y `comision`. No escribe datos.

## API / Endpoints
- `GET /api/admin/dashboard`
- `GET /api/admin/dashboard?desde={YYYY-MM-DD}&hasta={YYYY-MM-DD}&mercado={codigo}`

## Validaciones
- JWT requerido.
- Rol `ADMINISTRADOR`.
- Cuenta `ACTIVA`.
- MFA habilitado.
- Perfil existente en tabla `administrador`.
- Fechas en formato `YYYY-MM-DD`.

## Seguridad
No expone contrasenias, tokens ni entidades JPA. La cuenta administradora se crea manualmente en BD y no tiene registro publico.

## Consideraciones tecnicas
La implementacion actual calcula metricas en memoria desde repositorios; si el volumen crece, se puede optimizar con queries agregadas.

## Dependencias
Depende de Administracion, Ordenes, Autenticacion y Trazabilidad.

## Criterios de aceptacion
- [x] Admin consulta metricas generales.
- [x] Admin filtra por periodo.
- [x] Admin filtra tendencias por mercado.
- [x] UI muestra visualizaciones comparativas.
