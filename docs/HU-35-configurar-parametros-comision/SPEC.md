# Historia de Usuario

## Titulo
Configuracion de porcentaje y split de comision.

## Descripcion
Como administrador
Quiero modificar el porcentaje de comision y su distribucion
Para mantener actualizados los parametros financieros aplicables a transacciones futuras.

## Contexto
HU-35 cubre EC-18. La comision ya se muestra al inversionista en la previsualizacion de orden y ahora se obtiene desde BD mediante `IAdministracion`.

## Flujo funcional
1. El administrador entra al panel Comisiones.
2. Angular solicita `GET /api/admin/comisiones`.
3. El administrador modifica porcentaje, split plataforma y split comisionista.
4. Frontend valida que el split sume 100.
5. Backend recibe `PUT /api/admin/comisiones`.
6. `AdministracionService` desactiva parametros anteriores y crea un parametro activo nuevo.
7. Ordenes futuras usan los nuevos valores.

## Reglas de negocio
- El split plataforma + comisionista debe sumar 100%.
- Los cambios no recalculan ordenes ya ejecutadas.
- Siempre existe fallback 2% y split 60/40 si no hay parametro activo.
- Solo administradores pueden cambiar parametros financieros.

## Componentes involucrados
- `frontend/src/app/admin/admin-dashboard.component.*`
- `backend/.../administracion/controller/AdminController.java`
- `backend/.../administracion/service/AdministracionService.java`
- `backend/.../administracion/model/ParametroComision.java`
- `backend/.../ordenes/service/OrdenService.java`

## Backend
`AdministracionService.obtenerPorcentajeComision`, `obtenerSplitPlataforma` y `obtenerSplitComisionista` implementan `IAdministracion` y son consumidos por `OrdenService`.

## Frontend
El panel Comisiones muestra los valores activos y una barra de split para validar visualmente la distribucion.

## Base de datos
Tabla `parametro_comision`: porcentaje, split plataforma, split comisionista, activo y actualizado en.

## API / Endpoints
- `GET /api/admin/comisiones`
- `PUT /api/admin/comisiones`

## Validaciones
- JWT y rol `ADMINISTRADOR`.
- Perfil manual en tabla `administrador`.
- Porcentajes entre 0 y 100.
- Split total igual a 100.

## Seguridad
Evento auditado como `PARAMETRO_ADMIN_ACTUALIZADO`.

## Consideraciones tecnicas
El patron de versionado por `activo` conserva historico simple de parametros previos.

## Dependencias
Depende de Administracion, Ordenes y Trazabilidad.

## Criterios de aceptacion
- [x] Admin consulta parametros activos.
- [x] Admin actualiza comision y split.
- [x] Split invalido se rechaza.
- [x] Ordenes futuras usan la nueva configuracion.
