# Historia de Usuario

## Título
Encolamiento de órdenes fuera de horario.

## Descripción
Como inversionista autenticado
Quiero colocar órdenes fuera del horario de mercado
Para que se procesen automáticamente cuando abra el mercado.

## Contexto
Cubre HU-23 y EC-04. La orden se persiste como `EN_COLA`; un scheduler revisa cada minuto si el mercado US abrió.

## Flujo funcional
1. Usuario confirma orden cuando `mercadoAbierto=false`.
2. `OrdenService.crearOrden` reserva fondos/verifica holdings.
3. Guarda orden con estado `EN_COLA`.
4. Audita `ORDEN_ENCOLADA`.
5. `ColaOrdenesService.procesarColaAlAbrirMercado` corre cada 60 segundos.
6. Si mercado US está abierto, carga cola por fecha ascendente.
7. Revalida usuario activo.
8. Envía a Alpaca si US; para globales ejecuta internamente.
9. Actualiza estado y audita.

## Reglas de negocio
- Usuario inactivo cancela orden encolada.
- Compra encolada mantiene fondos reservados.
- Orden US se envía a Alpaca al abrir.
- Orden global se ejecuta internamente con precio registrado.

## Componentes involucrados
- `backend/.../ordenes/service/OrdenService.java`
- `backend/.../ordenes/service/ColaOrdenesService.java`
- `backend/.../ordenes/repository/OrdenRepository.java`
- `backend/.../ordenes/model/EstadoOrden.java`
- `backend/.../mercado/service/MercadoService.java`

## Backend
La creación y el procesamiento están separados. El scheduler usa horario US propio, no `MercadoService`.

## Frontend
La previsualización muestra advertencia de mercado cerrado; la orden creada puede aparecer como `EN_COLA`.

## Base de datos
Tabla `orden`: estado `EN_COLA`, `creada_en`; luego `ENVIADA`, `EJECUTADA` o `CANCELADA`.

## API / Endpoints
- `POST /api/ordenes`
- `GET /api/ordenes/activas`

## Validaciones
- Fondos/holdings se validan al crear.
- Al procesar se revalida estado activo del usuario.

## Seguridad
Creación requiere JWT; scheduler interno no expone endpoint.

## Consideraciones técnicas
EC-04 menciona procesamiento paralelo, pero el código actual procesa secuencialmente por simplicidad académica.

## Dependencias
Depende de mercado, Alpaca, saldo, portafolio y auditoría.

## Criterios de aceptación
- [ ] Orden fuera de horario queda `EN_COLA`.
- [ ] Se muestra advertencia en previsualización.
- [ ] Scheduler procesa cola al abrir mercado.
- [ ] Usuario inactivo cancela cola.

## Notas
No contempla feriados ni mercados globales con scheduler específico por zona.
