# Plan de implementación — HU-2: Integración con Alpaca al registrar inversionista

| Campo | Valor |
|---|---|
| Historia | HU-2 — Integración Alpaca al registrar inversionista |
| Sprint | 2 |
| Estado | Completada |
| Módulo principal | `integracion` |
| Módulos de soporte | `autenticacion`, `trazabilidad` |

---

## Objetivo

Al activarse una cuenta de inversionista (plan BASICO tras confirmar código, o plan premium tras confirmar pago Stripe), crear automáticamente una cuenta de trading en la API Alpaca Broker y guardar el `alpacaAccountId` en `integracion_inversionista`. Si Alpaca falla, la cuenta local queda activa pero marcada con `pendiente_cuenta_alpaca = true` para reintento on-demand al colocar la primera orden.

---

## Estrategia general

1. **Orquestador en `integracion`:** `OrquestadorRegistro` coordina la secuencia completa: cargar inversionista → construir payload → llamar `AlpacaAdapter` → persistir resultado → auditar.
2. **Adaptador aislado:** `AlpacaAdapter` encapsula todos los detalles del contrato Alpaca Broker API (normalización de campos, construcción del payload, parse de la respuesta). El resto del sistema solo ve `IIntegracionAlpaca`.
3. **Degradación ante fallo:** cualquier excepción en `AlpacaAdapter` se captura, retorna `null`, y el sistema marca `pendiente_cuenta_alpaca = true` sin interrumpir la activación de la cuenta local.
4. **Sin interfaz HTTP hacia el usuario:** este flujo no expone endpoints propios. Es activado internamente por `RegistroService` (plan BASICO) o `OrquestadorSuscripcion` (plan premium).
5. **Reintento on-demand:** si `alpacaAccountId` es null al colocar la primera orden, `OrdenService` detecta la situación y llama `OrquestadorRegistro.crearCuentaAlpaca` antes de intentar enviar la orden (descrito en HU-17).

---

## Fases de implementación

### Fase 1 — Adaptador Alpaca

- Implementar `AlpacaAdapter.crearCuenta(Usuario usuario, Inversionista inversionista)`:
  - Dividir `nombreCompleto` en `nombre` (primera palabra) y `apellido` (resto).
  - Normalizar teléfono: null → `"+573001234567"`; sin `+` → anteponer `+57`; prefijo `57` sin `+` → `+57{resto}`.
  - Normalizar país: `CO` → `COL`; 2 letras → uppercase; otro → usar como está.
  - Generar `tax_id` sandbox: `hash(usuario.id, usuario.correo)` módulo 9 dígitos; si todos iguales → `"123456789"`.
  - Aplicar fallbacks: `direccion → "123 Main St"`, `ciudad → "Bogota"`, `codigoPostal → "110111"`, `fechaNacimiento → "1990-01-01"`.
  - Construir payload JSON con secciones `contact`, `identity`, `disclosures`, `agreements`.
  - Llamar `POST {alpaca.broker.base-url}/v1/accounts` con `Authorization: Basic Base64(apiKey:apiSecret)`.
  - En caso exitoso: extraer campo `"id"` de la respuesta y retornarlo.
  - En caso de cualquier excepción: loguear error → retornar `null`.

### Fase 2 — Orquestador de registro

- Implementar `OrquestadorRegistro.crearCuentaAlpaca(Usuario usuario)`:
  - Cargar `Inversionista` desde `InversionistaRepository.findById(usuario.getId())`.
  - Llamar `IIntegracionAlpaca.crearCuenta(usuario, inversionista)`.
  - Si `alpacaId != null`: actualizar `integracionInversionista.alpacaAccountId = alpacaId`, `pendienteCuentaAlpaca = false`, guardar, auditar `REGISTRO_EXITOSO` con `"Cuenta Alpaca creada: {alpacaId}"`.
  - Si `alpacaId == null`: `pendienteCuentaAlpaca = true`, guardar, auditar `REGISTRO_FALLO_ALPACA` con `"Fallo al crear cuenta Alpaca"`.

### Fase 3 — Puntos de disparo

- Verificar que `RegistroService.confirmarRegistro` llama `orquestadorRegistro.crearCuentaAlpaca(usuario)` al final del flujo BASICO (tras auditar `REGISTRO_EXITOSO`).
- Verificar que `OrquestadorSuscripcion.confirmarPagoCheckout` llama `orquestadorRegistro.crearCuentaAlpaca(usuario)` tras activar la cuenta premium.

### Fase 4 — Configuración

- Verificar que `alpaca.broker.base-url`, `alpaca.broker.api-key`, `alpaca.broker.api-secret` estén en `application.properties` con valores de sandbox reales (no placeholder).

### Fase 5 — Verificación

- Confirmar registro BASICO completo → verificar `SELECT alpaca_account_id FROM integracion_inversionista` retorna UUID válido.
- Deshabilitar credenciales Alpaca temporalmente → confirmar registro → verificar `pendiente_cuenta_alpaca = true` y cuenta local en `ACTIVA`.
- Verificar eventos en `logs/audit.log`: `REGISTRO_EXITOSO` (con alpacaId) o `REGISTRO_FALLO_ALPACA`.

---

## Dependencias externas

| Dependencia | Requerida para | Estado |
|---|---|---|
| Alpaca Broker API sandbox | Creación de cuenta | Implementado en sandbox |
| `alpaca.broker.*` en `application.properties` | Autenticación con Alpaca | Configurado |
| `InversionistaRepository` | Cargar perfil del inversionista | Disponible (módulo `autenticacion`) |
| `IntegracionInversionistaRepository` | Persistir resultado | Disponible |
| `IAuditLog` impl. | Auditoría | Disponible |

---

## Decisiones de diseño clave

- **Degradación sin bloqueo:** el fallo de Alpaca no debe interrumpir la experiencia del usuario. La cuenta local queda activa; el reintento es on-demand.
- **Desviación documentada D1:** `OrquestadorRegistro` accede directamente a `InversionistaRepository`. Deuda técnica: crear `obtenerPerfilCompleto` en `IConsultaInversionista`.
- **Desviación documentada D2:** `IIntegracionAlpaca.crearCuenta` recibe entidades JPA en lugar de DTO. Deuda técnica: crear `PerfilAlpacaDTO`. Documentada en `ARQUITECTURA.md §9.6`.
- **Sandbox tax_id:** derivado de hash para evitar duplicados en Alpaca sandbox.

---

## Riesgos principales

| Riesgo | Impacto | Mitigación |
|---|---|---|
| Alpaca rechaza payload (identidad inválida) | `alpacaId = null`, cuenta pendiente | Log de error; reintento al colocar primera orden |
| `nombreCompleto` sin espacio → apellido repetido | Puede rechazarse en producción | Validar al menos un espacio en registro |
| Acceso directo a `InversionistaRepository` desde `integracion` | Acoplamiento entre módulos | Deuda técnica documentada; crear interfaz post-MVP |
