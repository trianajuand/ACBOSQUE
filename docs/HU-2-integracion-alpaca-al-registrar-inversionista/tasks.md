# Tareas — HU-2: Integración con Alpaca al registrar inversionista

| Campo | Valor |
|---|---|
| Historia | HU-2 |
| Sprint | 2 |
| Estado | Completada |

---

## Tareas de backend — Módulo `integracion`

### AlpacaAdapter — método `crearCuenta`

- [x] Implementar lógica de normalización de nombre: dividir `nombreCompleto` en nombre (primera palabra) y apellido (todo lo demás).
- [x] Implementar normalización de teléfono: null → `"+573001234567"`; sin `+` → anteponer `+57`; prefijo `57` sin `+` → `+57{resto}`.
- [x] Implementar normalización de país: `"CO"` → `"COL"`; 2 letras → mayúsculas.
- [x] Implementar generación de `tax_id` sandbox: `Math.abs((usuario.getId().toString() + usuario.getCorreo()).hashCode()) % 1_000_000_000`; si todos los dígitos son iguales → `"123456789"`.
- [x] Aplicar fallbacks para campos opcionales vacíos: `direccion`, `ciudad`, `codigoPostal`, `fechaNacimiento`.
- [x] Construir payload JSON con secciones `contact`, `identity`, `disclosures`, `agreements` (margin, account, customer con `signed_at: "2024-01-01T00:00:00Z"`).
- [x] Ejecutar `POST {alpaca.broker.base-url}/v1/accounts` con `Authorization: Basic Base64(apiKey:apiSecret)`.
- [x] En respuesta exitosa: extraer campo `"id"` del JSON y retornar como `String`.
- [x] En cualquier excepción (timeout, 4xx, 5xx, red caída): loguear error con `log.error(...)` → retornar `null`.

### OrquestadorRegistro

- [x] Implementar `OrquestadorRegistro.crearCuentaAlpaca(Usuario usuario)`:
  - [x] Cargar `Inversionista` con `inversionistaRepository.findById(usuario.getId())` (lanza `IllegalStateException` si no existe).
  - [x] Llamar `iIntegracionAlpaca.crearCuenta(usuario, inversionista)`.
  - [x] Si `alpacaId != null`: actualizar `alpacaAccountId`, `pendienteCuentaAlpaca = false`, guardar, auditar `REGISTRO_EXITOSO` con `"Cuenta Alpaca creada: {alpacaId}"`.
  - [x] Si `alpacaId == null`: actualizar `pendienteCuentaAlpaca = true`, guardar, auditar `REGISTRO_FALLO_ALPACA` con `"Fallo al crear cuenta Alpaca"`.

### Puntos de disparo

- [x] Verificar que `RegistroService.confirmarRegistro` (flujo BASICO) llama `orquestadorRegistro.crearCuentaAlpaca(usuario)` tras activar la cuenta.
- [x] Verificar que `OrquestadorSuscripcion.confirmarPagoCheckout` (flujo premium) llama `orquestadorRegistro.crearCuentaAlpaca(usuario)` tras activar la cuenta premium.

### Configuración

- [x] Verificar que `alpaca.broker.base-url` contiene la URL del sandbox de Alpaca Broker API.
- [x] Verificar que `alpaca.broker.api-key` y `alpaca.broker.api-secret` tienen valores reales de sandbox (no placeholder).
- [x] Verificar que `AlpacaAdapter` lee las propiedades mediante `@Value("${alpaca.broker.base-url}")`, etc. (sin hardcodear).

---

## Tareas de verificación

- [x] **Flujo exitoso:** confirmar registro BASICO completo → `SELECT alpaca_account_id, pendiente_cuenta_alpaca FROM integracion_inversionista` → UUID válido y `pendiente = false`.
- [x] **Flujo fallido (simulado):** configurar credenciales Alpaca inválidas → confirmar registro → verificar `alpaca_account_id IS NULL` y `pendiente_cuenta_alpaca = true` en BD.
- [x] **Cuenta local activa tras fallo:** tras el fallo simulado, verificar que `usuario.estado_cuenta = 'ACTIVA'` (el fallo de Alpaca no revierte la activación).
- [x] Verificar evento `REGISTRO_EXITOSO` en `logs/audit.log` con el alpacaId en el detalle.
- [x] Verificar evento `REGISTRO_FALLO_ALPACA` en `logs/audit.log` cuando Alpaca falla.
- [x] Verificar que teléfono vacío genera `"+573001234567"` en el payload (inspección de logs del adaptador).
- [x] Verificar que `pais = "CO"` genera `"COL"` en los campos `country_of_*` del payload.

---

## Deudas técnicas registradas (no bloquean cierre de HU-2)

- [ ] D1: crear método `obtenerPerfilCompleto(Long usuarioId)` en `IConsultaInversionista` para que `OrquestadorRegistro` no acceda directamente a `InversionistaRepository`. Prioridad: post-MVP.
- [ ] D2: crear `PerfilAlpacaDTO` en `autenticacion/dto/` y cambiar firma de `IIntegracionAlpaca.crearCuenta(PerfilAlpacaDTO dto)`. Prioridad: post-MVP.

---

## Actualización de documentación

- [x] Marcar HU-2 como `✅` en `docs/PROGRESO.md`.
- [x] Registrar desviaciones arquitectónicas D1 y D2 en `docs/ARQUITECTURA.md §9.6`.
