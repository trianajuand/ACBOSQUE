# Historia de Usuario

## Título
Previsualización de comisión antes de confirmar orden.

## Descripción
Como inversionista autenticado
Quiero ver el costo, comisión y total de una orden antes de enviarla
Para confirmar con transparencia el impacto financiero.

## Contexto
Implementa EC-13 y RNF-15. Es un paso separado de confirmación en frontend antes de `POST /api/ordenes`.

## Flujo funcional
1. Usuario diligencia símbolo, tipo, lado, cantidad y precios.
2. Pulsa previsualizar.
3. Angular envía `POST /api/ordenes/previsualizar`.
4. Backend valida símbolo operable.
5. Calcula monto base con precio actual.
6. Calcula comisión con `app.comision.porcentaje`.
7. Calcula split plataforma/comisionista.
8. Devuelve resumen y advertencia si mercado está cerrado.

## Reglas de negocio
- Comisión por defecto: 2%.
- Si no hay comisionista, toda la comisión se asigna a plataforma.
- Compra muestra `totalADebitar`.
- Venta muestra `totalARecibir`.
- Mercado cerrado genera advertencia de encolamiento.

## Componentes involucrados
- `frontend/src/app/dashboard/dashboard.component.ts`
- `backend/.../ordenes/controller/OrdenController.java`
- `backend/.../ordenes/service/OrdenService.java`
- `backend/.../ordenes/dto/ResumenComisionDTO.java`
- `backend/.../mercado/service/MercadoService.java`

## Backend
`OrdenService.previsualizarOrden` usa cotización real/cacheada y propiedades de comisión. No persiste orden.

## Frontend
`previsualizar` guarda `resumen` y habilita al usuario a confirmar orden desde UI.

## Base de datos
No escribe datos. Lee usuario para determinar potencial comisionista, aunque actualmente `tieneComisionista=false`.

## API / Endpoints
- `POST /api/ordenes/previsualizar`

## Validaciones
- `CrearOrdenRequestDTO`: símbolo obligatorio, tipo, lado y cantidad mayor a cero.
- Símbolo debe tener precio válido.

## Seguridad
Endpoint protegido por JWT. Usa usuario resuelto desde token.

## Consideraciones técnicas
Cumple la táctica Verify Message Integrity del EC-13 al separar previsualización de confirmación.

## Dependencias
Depende de Mercado, configuración de comisiones y creación de órdenes.

## Criterios de aceptación
- [ ] Calcula monto base.
- [ ] Calcula comisión.
- [ ] Informa total a debitar o recibir.
- [ ] Advierte si mercado está cerrado.

## Notas
El split comisionista se encuentra preparado para Sprint 4, pero no activo.
