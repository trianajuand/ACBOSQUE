# Historia de Usuario

## Título
Registro ampliado de inversionista con verificación por correo.

## Descripción
Como usuario no registrado
Quiero crear una cuenta de inversionista diligenciando datos de acceso, identidad, perfil financiero, preferencias y plan
Para acceder a la plataforma con un perfil inicial suficiente para operar y recibir comunicaciones.

## Contexto
El registro es la puerta de entrada del servicio de Autenticación. La implementación actual exige un flujo Angular por fases y separa `Usuario` como identidad/acceso general de `Inversionista` como perfil de negocio, dejando la cuenta en estado `PENDIENTE_VERIFICACION` hasta validar el código de correo. El diseño sigue RF-01, EC-10 y las reglas de seguridad de persistir códigos en BD.

## Flujo funcional
1. El usuario abre `/registro` en Angular.
2. La primera fase captura nombre, correo, contraseña, teléfono, documento, fecha de nacimiento, país, dirección, ciudad y código postal.
3. Antes de avanzar, el frontend consulta disponibilidad del correo.
4. La segunda fase captura intereses de mercado desde opciones predefinidas, estilo de trading, rango de ingresos y experiencia.
5. La tercera fase define tipo de orden por defecto, vista del portafolio, canales de notificación y solicitud de comisionista.
6. La cuarta fase selecciona plan `BASICO`, `PREMIUM_MENSUAL` o `PREMIUM_ANUAL`.
7. El frontend envía `POST /api/auth/register/investor`.
8. El backend crea el usuario pendiente, genera código de registro y lo envía por email.
9. El frontend redirige a `/verificar-registro`.

## Reglas de negocio
- Solo se auto-registra el rol `INVERSIONISTA`.
- El correo debe ser único.
- La contraseña se almacena con BCrypt.
- Los intereses de mercado se normalizan a mayúsculas, se deduplican y se limitan a 10.
- Si no hay intereses válidos, se usan `AAPL`, `MSFT`, `TSLA`.
- La cuenta queda pendiente hasta verificar correo y, si aplica, completar Stripe.

## Componentes involucrados
- `frontend/src/app/auth/registro.component.ts`
- `frontend/src/app/auth/registro.component.html`
- `frontend/src/app/auth/auth-card.scss`
- `frontend/src/app/core/api.service.ts`
- `backend/.../autenticacion/controller/RegistroController.java`
- `backend/.../autenticacion/service/RegistroService.java`
- `backend/.../autenticacion/dto/RegistroInversionistaDTO.java`
- `backend/.../autenticacion/dto/CorreoDisponibleDTO.java`
- `backend/.../autenticacion/model/Usuario.java`
- `backend/.../autenticacion/model/Inversionista.java`
- `backend/.../integracion/notificaciones/DespachadorNotificaciones.java`
- `backend/.../autenticacion/service/MFAService.java`

## Backend
`RegistroController` expone el registro público. `RegistroService.iniciarRegistro` valida duplicidad, crea `Usuario` con datos generales de acceso, crea `Inversionista` con identidad extendida, perfil financiero, preferencias, plan y datos de integración, y delega la generación de código a `MFAService`.

## Frontend
`RegistroComponent` implementa un wizard de 4 fases con `ReactiveForms`. Los intereses se seleccionan como chips y el botón siguiente valida la fase actual. Al terminar, guarda `reg_correo` en `localStorage` y navega a `/verificar-registro`.

## Base de datos
Tabla `usuario`: `nombre_completo`, `correo`, `contrasenia`, `rol`, `estado_cuenta`, `mfa_habilitado` y fechas. Tabla `inversionista`: datos de identidad extendida, experiencia, intereses, Alpaca, Stripe, premium, preferencias y notificaciones. Tabla `codigo_verificacion`: código de registro, tipo `REGISTRO`, expiración y flag `usado`.

## API / Endpoints
- `GET /api/auth/register/email-disponible?correo={correo}`
- `POST /api/auth/register/investor`
- `POST /api/auth/register/confirm`

## Validaciones
- Bean Validation: nombre, correo, contraseña mínima de 8, tipo y número de identificación, dirección, ciudad y código postal obligatorios.
- Angular: validadores requeridos, email y longitud mínima.
- Disponibilidad de correo antes de pasar la primera fase.

## Seguridad
Endpoint público permitido en `SecurityConfig`. La contraseña se codifica con `PasswordEncoder`. El usuario no queda activo hasta completar verificación. No se exponen entidades JPA al frontend.

## Consideraciones técnicas
Se conserva arquitectura SOA consolidada. La funcionalidad pertenece a Autenticación, pero consume Integración para email y Trazabilidad vía `IAuditLog`.

## Dependencias
Depende de envío de correo, `MFAService`, repositorio `UsuarioRepository`, `CodigoVerificacionRepository` y configuración SMTP.

## Criterios de aceptación
- [ ] Un correo disponible permite avanzar desde la primera fase.
- [ ] Un correo existente bloquea el avance y muestra error.
- [ ] El registro crea usuario `INVERSIONISTA` en `PENDIENTE_VERIFICACION`.
- [ ] El código de verificación se guarda en BD y se envía por correo.
- [ ] El frontend redirige a `/verificar-registro`.

## Notas
El formulario actual almacena algunos datos financieros como strings controlados por opciones UI, no como catálogos relacionales.
