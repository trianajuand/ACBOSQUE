# Historia de Usuario

## Título
Creación de cuenta Alpaca al activar inversionista.

## Descripción
Como inversionista recién registrado
Quiero que el sistema cree una cuenta de trading en Alpaca asociada a mi perfil
Para poder enviar órdenes reales o sandbox desde la plataforma.

## Contexto
La creación de cuenta Alpaca se ejecuta al confirmar el registro básico o al confirmar pago premium. El código implementa el patrón de orquestador dentro del servicio de Integración para aislar la API externa, como exige EC-14.

## Flujo funcional
1. El usuario confirma código de registro o confirma pago Stripe.
2. `RegistroService` u `OrquestadorSuscripcion` delega a `OrquestadorRegistro`.
3. `OrquestadorRegistro` invoca `IIntegracionAlpaca.crearCuenta(usuario)`.
4. `AlpacaAdapter` arma payload sandbox y llama Broker API.
5. Si Alpaca retorna `id`, se guarda en `usuario.alpacaAccountId`.
6. Si falla, se marca `pendienteCuentaAlpaca=true`.
7. Se registra auditoría de éxito o fallo.

## Reglas de negocio
- La cuenta Alpaca solo se intenta crear para usuarios persistidos.
- Un fallo de Alpaca no elimina la cuenta local.
- Si no hay teléfono, el adaptador usa un teléfono sandbox por defecto.
- El tax id sandbox se deriva del usuario para evitar valores repetidos triviales.

## Componentes involucrados
- `backend/.../integracion/orquestadores/OrquestadorRegistro.java`
- `backend/.../integracion/adaptadores/alpaca/AlpacaAdapter.java`
- `backend/.../integracion/adaptadores/alpaca/IIntegracionAlpaca.java`
- `backend/.../autenticacion/model/Usuario.java`
- `backend/.../autenticacion/repository/UsuarioRepository.java`
- `backend/.../trazabilidad/interfaces/IAuditLog.java`

## Backend
El adaptador usa `RestTemplate`, Basic Auth para Broker API y propiedades `alpaca.broker.*`. El orquestador actualiza el usuario y audita `REGISTRO_EXITOSO` o `REGISTRO_FALLO_ALPACA`.

## Frontend
No hay pantalla específica. El efecto se dispara después del flujo de verificación o pago.

## Base de datos
Tabla `usuario`: columnas `alpaca_account_id` y `pendiente_cuenta_alpaca`.

## API / Endpoints
- Indirecto desde `POST /api/auth/register/confirm`.
- Indirecto desde `GET /api/suscripciones/confirmar-checkout`.

## Validaciones
- `AlpacaAdapter` normaliza teléfono.
- El payload separa nombre y apellido a partir de `nombreCompleto`.

## Seguridad
Credenciales Alpaca se leen de `application.properties`/variables de entorno. No se exponen al frontend. La integración está encapsulada por `IIntegracionAlpaca`.

## Consideraciones técnicas
La implementación usa sandbox y valores legales/identidad simplificados para ambiente académico. El error externo se degrada a estado pendiente.

## Dependencias
Depende de registro, confirmación Stripe, configuración Alpaca y auditoría.

## Criterios de aceptación
- [ ] Al activar una cuenta básica se intenta crear cuenta Alpaca.
- [ ] Al confirmar premium se intenta crear cuenta Alpaca.
- [ ] Si Alpaca responde con id se guarda `alpacaAccountId`.
- [ ] Si Alpaca falla se marca la cuenta local como pendiente.

## Notas
La creación on-demand también existe al enviar órdenes US si el usuario aún no tiene `alpacaAccountId`.
