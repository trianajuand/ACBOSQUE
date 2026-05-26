# Historia de Usuario

## Título
Verificación de horario de mercado.

## Descripción
Como sistema de trading
Quiero conocer si un mercado está abierto
Para decidir si una orden se ejecuta o se encola.

## Contexto
Implementa RF-08 como servicio transversal de Mercado. Es consumido por órdenes y expuesto para diagnóstico.

## Flujo funcional
1. Un flujo envía símbolo o mercado.
2. `MercadoService.detectarMercado` infiere mercado por sufijo.
3. `esMercadoAbierto` evalúa zona horaria y ventana laboral.
4. Si sandbox está activo, retorna abierto siempre.
5. Órdenes usan el resultado para ejecutar o encolar.

## Reglas de negocio
- US/NYSE/NASDAQ: lunes a viernes 09:30-16:00 America/New_York.
- TSE: lunes a viernes 09:00-15:00 Asia/Tokyo.
- LSE: lunes a viernes 08:00-16:30 Europe/London.
- ASX: lunes a viernes 10:00-16:00 Australia/Sydney.
- `app.mercado.sandbox-siempre-abierto=true` fuerza abierto.

## Componentes involucrados
- `backend/.../mercado/service/MercadoService.java`
- `backend/.../mercado/interfaces/IVerificacionMercado.java`
- `backend/.../mercado/controller/MercadoController.java`
- `backend/.../ordenes/service/OrdenService.java`

## Backend
La lógica vive en `MercadoService`, expuesta por interfaz `IVerificacionMercado` y endpoint `GET /api/mercado/horario/{mercado}`.

## Frontend
El dashboard muestra `mercadoAbierto` recibido en cotizaciones y resumen de comisión.

## Base de datos
No usa tablas de horarios; la configuración es por código y propiedad sandbox.

## API / Endpoints
- `GET /api/mercado/horario/{mercado}`

## Validaciones
- Mercado nulo o desconocido retorna cerrado.

## Seguridad
Endpoint protegido por JWT según configuración general.

## Consideraciones técnicas
La implementación todavía no cumple EC-18/EC-19 de horarios configurables en BD.

**Deuda técnica documentada (auditoría 2026-05-25):**
- Los horarios de mercado están hardcodeados en `MercadoService` (switch/case por nombre de mercado). No se leen de `mercado_config` en BD, aunque esa tabla existe y el administrador puede editarla.
- `ColaOrdenesService` tampoco verifica feriados de `feriado_mercado` antes de procesar la cola; solo verifica si el mercado US está abierto por hora del día.
- Severidad: MEDIA — afecta correctitud en días festivos y en horarios modificados por el admin, pero no bloquea el sistema.

## Dependencias
Depende de órdenes, dashboard y detalle de acción.

## Criterios de aceptación
- [ ] Detecta horario US.
- [ ] Detecta horario TSE/LSE/ASX.
- [ ] Permite sandbox siempre abierto.
- [ ] Órdenes fuera de horario se encolan.

## Notas
No contempla feriados de mercado.
