# Historia de Usuario

## Título
Depósito de fondos sandbox para pruebas.

## Descripción
Como usuario de pruebas autenticado
Quiero depositar fondos simulados en mi cuenta
Para probar órdenes de compra sin integración real de funding.

## Contexto
No corresponde a una HU del backlog final; es soporte técnico implementado para validar flujos de órdenes. Se documenta porque aparece como endpoint y UI real.

## Flujo funcional
1. Usuario abre panel portafolio/órdenes.
2. Ingresa monto en formulario de depósito.
3. Angular llama `POST /api/portafolio/depositar?monto=...`.
4. Backend valida monto mínimo.
5. `SaldoService.depositar` obtiene o crea cuenta y suma monto.
6. Frontend recarga saldo.

## Reglas de negocio
- Monto mínimo `0.01`.
- El depósito aumenta `saldoDisponible`.
- No afecta `fondosReservados`.

## Componentes involucrados
- `frontend/src/app/dashboard/dashboard.component.ts`
- `backend/.../ordenes/controller/PortafolioController.java`
- `backend/.../ordenes/service/SaldoService.java`
- `backend/.../ordenes/model/CuentaFondos.java`

## Backend
Endpoint autenticado de soporte. Usa `@DecimalMin("0.01")` y transacción en servicio.

## Frontend
`depositoForm` tiene monto mínimo y `depositar` muestra resultado por toast.

## Base de datos
Tabla `cuenta_fondos`: actualiza `saldo_disponible` y `actualizado_en`.

## API / Endpoints
- `POST /api/portafolio/depositar?monto={monto}`

## Validaciones
- Monto requerido y mayor o igual a `0.01`.
- Usuario autenticado y existente.

## Seguridad
Protegido por JWT. No está restringido a administrador en el código actual.

**Deuda técnica ALTA (auditoría 2026-05-25):** `POST /api/portafolio/depositar` en `PortafolioController` no tiene `@PreAuthorize` ni restricción de rol. Cualquier usuario autenticado (INVERSIONISTA, COMISIONISTA, ADMINISTRADOR) puede depositar fondos simulados arbitrarios. Antes de pasar a producción debe ser eliminado o protegido con `@PreAuthorize("hasRole('ADMINISTRADOR')")` o reemplazado por integración real de funding.

## Consideraciones técnicas
Debe retirarse o restringirse antes de producción.

## Dependencias
Depende de saldo y órdenes de compra.

## Criterios de aceptación
- [ ] Un monto válido aumenta saldo disponible.
- [ ] Un monto inválido es rechazado.
- [ ] La UI refresca saldo.

## Notas
Documentado como sandbox para no confundirlo con un requerimiento financiero productivo.
