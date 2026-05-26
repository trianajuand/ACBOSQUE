# Tareas — HU-6: Consulta de perfil del inversionista

| Campo | Valor |
|---|---|
| Historia | HU-6 |
| Sprint | 2 |
| Estado | Completada |

---

## Tareas de backend

### DTOs

- [x] Crear `PerfilInversionistaDTO` (`autenticacion/dto/PerfilInversionistaDTO.java`) con todos los campos del perfil:
  - [x] Datos personales: `nombreCompleto`, `correo`, `telefono`, `tipoIdentificacion`, `numeroIdentificacion`, `fechaNacimiento`, `direccion`, `ciudad`, `codigoPostal`, `pais`.
  - [x] Perfil financiero: `nivelExperiencia`, `interesesMercado` (List<String>), `estiloTrading`, `ingresosMin` (BigDecimal), `ingresosMax` (BigDecimal), `solicitaComisionista` (boolean).
  - [x] Comisionista: `comisionistaAsignado` (ComisionistaAsignadoDTO, nullable).
  - [x] Seguridad: `mfaHabilitado` (boolean).
  - [x] Suscripción: `planSuscripcion` (String), `esPremium` (boolean).
  - [x] Notificaciones: `notificacionesActivas` (boolean), `notificacionEmail` (boolean), `notificacionSms` (boolean), `notificacionWhatsapp` (boolean), `tiposNotificacion` (List<String>).
  - [x] Preferencias: `tipoOrdenDefault` (String), `vistaPortafolio` (String).
- [x] Crear `ComisionistaAsignadoDTO` (`autenticacion/dto/ComisionistaAsignadoDTO.java`) con `nombre` (String) y `correo` (String).
- [x] Verificar que `contrasenia` no está en ningún campo de `PerfilInversionistaDTO`.

### PerfilService

- [x] Implementar `PerfilService.obtenerPerfil(String correo)`:
  - [x] Cargar `Usuario` con `usuarioRepository.findByCorreo(correo)` → lanzar `UsuarioNoEncontradoException("Usuario no encontrado: {correo}")` si no existe.
  - [x] Cargar `Inversionista` con `inversionistaRepository.findById(usuario.getId())`.
  - [x] Cargar `Suscripcion` (si existe) para obtener `planSuscripcion` y `esPremium`.
  - [x] Convertir `inversionista.interesesMercado` (CSV) a `List<String>`: `Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(toList())`. Si CSV nulo → lista vacía.
  - [x] Convertir `usuario.tiposNotificacion` (CSV) a `List<String>` con la misma lógica.
  - [x] Si `inversionista.comisionistaAsignadoId != null`: cargar comisionista y construir `ComisionistaAsignadoDTO{nombre, correo}`.
  - [x] Construir `PerfilInversionistaDTO` mapeando todos los campos de `usuario`, `inversionista`, `suscripcion` y `comisionistaAsignado`.
  - [x] Auditar `PERFIL_CONSULTADO` con detalle `"Perfil consultado"`.
  - [x] Retornar el DTO.
- [x] Anotar `obtenerPerfil` con `@Transactional(readOnly = true)`.
- [x] Inyección por constructor (no `@Autowired` en campo).

### Controlador

- [x] Implementar `PerfilController.obtenerPerfil(Authentication auth)` en `PerfilController`:
  - [x] Extraer `correo` del principal: `auth.getName()`.
  - [x] Delegar a `perfilService.obtenerPerfil(correo)`.
  - [x] Retornar `ResponseEntity.ok(perfilDTO)`.
- [x] Configurar `GET /api/perfil` como endpoint protegido, accesible para rol `INVERSIONISTA` en `SecurityConfig`.

### Manejo de errores

- [x] `UsuarioNoEncontradoException` mapeada a `404 Not Found` con mensaje `"Usuario no encontrado: {correo}"` en `GlobalExceptionHandler`.

---

## Tareas de frontend

- [x] Componente de perfil en el dashboard Angular (`dashboard.component.ts`):
  - [x] Llamar `GET /api/perfil` con `Authorization: Bearer <jwt>` al cargar el tab/sección de perfil.
  - [x] Mostrar todos los campos del `PerfilInversionistaDTO` en la UI (nombre, correo, nivel de experiencia, intereses, preferencias, estado premium, comisionista asignado).
  - [x] Manejar `401`: redirigir a `/login`.
  - [x] Manejar `404`: mostrar mensaje de error.
  - [x] Manejar `500`: mostrar toast de error.

---

## Tareas de verificación

- [x] **Consulta exitosa:** `GET /api/perfil` con JWT válido de inversionista → 200 con DTO completo incluyendo `correo` del usuario.
- [x] **Sin contraseña en respuesta:** verificar que el JSON de respuesta no contiene el campo `contrasenia`.
- [x] **Intereses como lista:** si `inversionista.intereses_mercado = "AAPL,TSLA,NVDA"`, verificar que `interesesMercado` en la respuesta es `["AAPL","TSLA","NVDA"]`.
- [x] **Tipos de notificación como lista:** si `usuario.tipos_notificacion = "ORDENES,MERCADO"`, verificar que `tiposNotificacion` es `["ORDENES","MERCADO"]`.
- [x] **Sin comisionista asignado:** verificar que `comisionistaAsignado` es null cuando no hay asignación.
- [x] **Sin JWT:** `GET /api/perfil` sin cabecera Authorization → 401.
- [x] **JWT de otro rol (administrador):** `GET /api/perfil` con JWT de administrador → 403 (endpoint restringido a INVERSIONISTA).
- [x] Verificar evento `PERFIL_CONSULTADO` en `logs/audit.log`.

---

## Actualización de documentación

- [x] Marcar HU-6 como `✅` en `docs/PROGRESO.md`.
