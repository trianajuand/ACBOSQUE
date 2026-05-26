# Plan de implementación — HU-1: Registro ampliado de inversionista

| Campo | Valor |
|---|---|
| Historia | HU-1 — Registrar Inversionista |
| Sprint | 1 |
| Estado | Completada |
| Módulo principal | `autenticacion` |
| Módulos de soporte | `integracion`, `trazabilidad` |

---

## Objetivo

Permitir que un usuario no registrado cree una cuenta de inversionista mediante un wizard de 4 fases (identidad, perfil financiero, preferencias, plan), enviando un código de verificación al correo y activando la cuenta solo tras confirmarlo. Si el plan es premium, se inicia el flujo de Stripe Checkout antes de activar la cuenta.

---

## Estrategia general

1. **Backend primero:** implementar entidades, repositorios, DTOs y servicios del módulo `autenticacion` antes de tocar el frontend.
2. **Verificación en BD:** los códigos de verificación se persisten en `codigo_verificacion` con TTL (no en memoria), conforme a la regla dura del proyecto.
3. **BCrypt obligatorio:** la contraseña nunca se almacena en texto plano.
4. **Separación de tablas:** `usuario` contiene identidad/acceso; `inversionista` contiene perfil de negocio; `suscripcion` e `integracion_inversionista` son tablas satélite con PK compartida.
5. **Flujo en dos pasos:** `POST /api/auth/register/investor` inicia el registro (crea registros y envía código); `POST /api/auth/register/confirm` confirma el código y activa la cuenta.
6. **Frontend wizard:** 4 fases en Angular con ReactiveFormsModule; validación de correo disponible en tiempo real al salir del campo (`GET /api/auth/register/email-disponible`).

---

## Fases de implementación

### Fase 1 — Modelo y persistencia

- Definir entidades JPA: `Usuario`, `Inversionista`, `CodigoVerificacion`, `Suscripcion`, `IntegracionInversionista`.
- Crear repositorios: `UsuarioRepository`, `InversionistaRepository`, `CodigoVerificacionRepository`.
- Verificar que Hibernate crea las tablas en PostgreSQL con el esquema 3NF normalizado.

### Fase 2 — DTOs y validaciones

- Implementar `RegistroInversionistaDTO` con Bean Validation (`@NotBlank`, `@Email`, `@Size`) en los 8 campos obligatorios.
- Implementar `ConfirmarRegistroDTO` con `@NotBlank @Email` y `@Size(min=6, max=6)`.
- Implementar `ConfirmarRegistroResponseDTO` con `mensaje`, `requierePago`, `stripeCheckoutUrl`.

### Fase 3 — Servicios backend

- `MFAService.generarYGuardarCodigo(correo, tipo)`: elimina código previo del mismo tipo, genera 6 dígitos con `SecureRandom`, persiste con TTL desde `app.seguridad.codigo-expiracion-minutos`.
- `MFAService.validarCodigo(correo, codigo, tipo)`: verifica no usado, no expirado, coincidencia exacta; marca `usado = true`.
- `RegistroService.iniciarRegistro(dto)`: valida correo único, normaliza intereses (mayúsculas, dedup, límite 10, default `AAPL,MSFT,TSLA`), aplica defaults de campos opcionales, crea `Usuario` en `PENDIENTE_VERIFICACION`, crea `Inversionista`, `Suscripcion`, `IntegracionInversionista`, llama `MFAService.generarYGuardarCodigo`, llama `INotificacion.enviarCodigoRegistro`, audita `REGISTRO_INICIADO`.
- `RegistroService.confirmarRegistro(dto)`: valida código, activa cuenta para plan BASICO (`ACTIVA`), inicia Stripe para planes premium (responde con `stripeCheckoutUrl`), llama `IAsignacionComisionista.asignarSiSolicitado` si aplica, audita `REGISTRO_EXITOSO`, llama `OrquestadorRegistro.crearCuentaAlpaca`.

### Fase 4 — Controladores backend

- `RegistroController.emailDisponible(@RequestParam correo)`: delega a `RegistroService.correoDisponible`.
- `RegistroController.iniciarRegistro(@Valid @RequestBody RegistroInversionistaDTO)`: delega a `RegistroService.iniciarRegistro`, responde 201.
- `RegistroController.confirmarRegistro(@Valid @RequestBody ConfirmarRegistroDTO)`: delega a `RegistroService.confirmarRegistro`, responde 200.

### Fase 5 — Frontend Angular

- `RegistroComponent`: wizard de 4 fases con `FormGroup` por fase; validación de correo disponible al blur del campo correo; navegación condicional a `/verificar-registro` o `stripeCheckoutUrl`.
- `VerificarRegistroComponent`: campo de código de 6 dígitos; lee correo de `localStorage`; navega a `/login` (plan BASICO) o a URL de Stripe (plan premium).

### Fase 6 — Pruebas y verificación

- Ejecutar los escenarios Gherkin del SPEC (correo disponible, registro exitoso, correo duplicado, código expirado, intereses vacíos, campos obligatorios faltantes).
- Verificar en BD que `contrasenia` empieza con `$2a$` (BCrypt).
- Verificar en `logs/audit.log` los eventos `REGISTRO_INICIADO` y `REGISTRO_EXITOSO`.

---

## Dependencias externas

| Dependencia | Requerida para | Estado |
|---|---|---|
| PostgreSQL nativo en `localhost:5432` | Persistencia de todas las entidades | Disponible |
| SMTP Gmail (`application.properties`) | Envío del código de verificación | Disponible |
| `PasswordEncoder` bean (BCrypt) | Hash de contraseña en `iniciarRegistro` | Disponible |
| Stripe (`stripe.secret-key`) | Flujo premium en `confirmarRegistro` | Placeholder configurado |
| `INotificacion` impl. (`DespachadorNotificaciones`) | Envío de correo | Disponible |
| `IAuditLog` impl. (`AuditLogService`) | Auditoría de eventos | Disponible |

---

## Decisiones de diseño clave

- **PK compartida:** `inversionista.id = usuario.id` (Table-Per-Type). No hay columna `usuario_id` separada.
- **Intereses como CSV:** `inversionista.intereses_mercado` almacena CSV (ej. `"AAPL,MSFT,TSLA"`). Facilita queries simples; se acepta la limitación de consultas por interés individual.
- **Verificación en BD:** `CodigoVerificacion` en tabla, no en `ConcurrentHashMap`. Mejora explícita sobre proyecto Malwatcher.
- **Deuda técnica documentada:** `OrquestadorRegistro` y `OrquestadorSuscripcion` son invocados directamente (sin interfaz `I...`). Plan de resolución: crear `IOrquestadorRegistro` e `IOrquestadorSuscripcion` post-MVP.

---

## Riesgos principales

| Riesgo | Impacto | Mitigación |
|---|---|---|
| SMTP falla al enviar código | Usuario queda en `PENDIENTE_VERIFICACION` | No hay reenvío implementado (pregunta abierta) |
| Race condition de correo duplicado | 500 en lugar de 409 | `UNIQUE` en BD como segunda línea de defensa |
| Stripe no configurado (placeholder) | Registro premium falla siempre | Solo afecta planes no BASICO |
