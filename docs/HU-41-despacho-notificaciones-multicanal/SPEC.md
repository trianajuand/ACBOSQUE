# Historia de Usuario

## Título
Notificaciones transaccionales por correo.

## Descripción
Como usuario del sistema
Quiero recibir códigos y alertas relevantes por correo
Para completar flujos de seguridad y enterarme de eventos críticos.

## Contexto
Implementa parte de HU-41 y soporta registro, MFA, recuperación y bloqueo de cuenta. SMS y WhatsApp no están implementados.

## Flujo funcional
1. Un servicio requiere notificar al usuario.
2. Invoca `DespachadorNotificaciones`.
3. El despachador delega a `EmailSender`.
4. Se arma correo HTML según caso.
5. Se envía mediante configuración SMTP.
6. También se loguea el código/token para pruebas locales.

## Reglas de negocio
- Registro usa asunto/cuerpo de código de verificación.
- MFA usa código de segundo factor.
- Recuperación usa token de recuperación.
- Bloqueo envía alerta al titular.
- Admin puede recibir notificaciones por correo configurado.

## Componentes involucrados
- `backend/.../integracion/notificaciones/DespachadorNotificaciones.java`
- `backend/.../integracion/notificaciones/canales/EmailSender.java`
- `backend/src/main/resources/application.properties`
- `RegistroService`
- `AutenticacionService`
- `RecuperacionPasswordService`

## Backend
`DespachadorNotificaciones` actúa como fachada/orquestador. `EmailSender` usa Jakarta Mail y propiedades `spring.mail.*`.

## Frontend
Las pantallas muestran instrucciones para revisar el correo, pero no gestionan envío directamente.

## Base de datos
No persiste notificaciones enviadas. Los códigos relacionados se guardan en `codigo_verificacion`.

## API / Endpoints
Indirecto desde:
- `POST /api/auth/register/investor`
- `POST /api/auth/login`
- `POST /api/auth/forgot-password`

## Validaciones
- Depende de correo válido en DTOs de cada flujo.

## Seguridad
Secretos SMTP se configuran por propiedades/variables. No hay credenciales hardcodeadas. No se envían contraseñas.

## Consideraciones técnicas
La implementación usa Adapter por canal, alineada con Tailor Interface de EC-17, aunque solo Email está activo.

## Dependencias
Depende de SMTP y de los flujos de autenticación.

## Criterios de aceptación
- [ ] Envía código de registro.
- [ ] Envía código MFA.
- [ ] Envía token de recuperación.
- [ ] Envía alerta de bloqueo.

## Notas
Los códigos se escriben en log para facilitar pruebas, lo cual debe deshabilitarse en producción.
