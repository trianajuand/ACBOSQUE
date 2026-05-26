# Plan de implementación — HU-6: Consulta de perfil del inversionista

| Campo | Valor |
|---|---|
| Historia | HU-6 — Consultar información de inversionista |
| Sprint | 2 |
| Estado | Completada |
| Módulo principal | `autenticacion` |
| Módulos de soporte | `trazabilidad` |

---

## Objetivo

Proveer un endpoint `GET /api/perfil` que retorne el perfil completo del inversionista autenticado: datos de identidad, financieros, preferencias de operación y notificación, estado de suscripción premium y comisionista asignado. El correo del inversionista se extrae del JWT (no del path ni del body), garantizando que cada inversionista solo pueda consultar su propio perfil.

---

## Estrategia general

1. **Un solo endpoint GET:** el correo se obtiene del `SecurityContextHolder` (claim `sub` del JWT). No hay parámetros de ruta.
2. **DTO sin datos sensibles:** `PerfilInversionistaDTO` nunca expone `contrasenia` ni ningún otro campo interno de seguridad.
3. **Transformaciones en el servicio:**
   - `inversionista.intereses_mercado` (CSV `"AAPL,TSLA"`) → `List<String>`.
   - `usuario.tipos_notificacion` (CSV `"ORDENES,MERCADO"`) → `List<String>`.
4. **Comisionista asignado:** si existe asignación activa, incluir `ComisionistaAsignadoDTO{nombre, correo}` en la respuesta. Si no, retornar null.
5. **Suscripción premium:** leer `esPremium` y `planSuscripcion` desde la tabla `suscripcion`.
6. **Trazabilidad:** registrar `PERFIL_CONSULTADO` en auditoría en cada consulta exitosa.

---

## Fases de implementación

### Fase 1 — DTOs

- Crear o verificar `PerfilInversionistaDTO` con todos los campos del perfil (ver contrato de API en SPEC):
  - Datos personales: `nombreCompleto`, `correo`, `telefono`, `tipoIdentificacion`, `numeroIdentificacion`, `fechaNacimiento`, `direccion`, `ciudad`, `codigoPostal`, `pais`.
  - Perfil financiero: `nivelExperiencia`, `interesesMercado` (List<String>), `estiloTrading`, `ingresosMin`, `ingresosMax`, `solicitaComisionista`.
  - Comisionista: `comisionistaAsignado` (ComisionistaAsignadoDTO o null).
  - Seguridad: `mfaHabilitado`.
  - Suscripción: `planSuscripcion`, `esPremium`.
  - Notificaciones: `notificacionesActivas`, `notificacionEmail`, `notificacionSms`, `notificacionWhatsapp`, `tiposNotificacion` (List<String>).
  - Preferencias: `tipoOrdenDefault`, `vistaPortafolio`.
- Crear o verificar `ComisionistaAsignadoDTO` con `nombre` y `correo`.

### Fase 2 — Servicio

- `PerfilService.obtenerPerfil(String correo)`:
  1. Cargar `Usuario` con `usuarioRepository.findByCorreo(correo)` → lanzar `UsuarioNoEncontradoException` si no existe.
  2. Cargar `Inversionista` con `inversionistaRepository.findById(usuario.getId())`.
  3. Cargar `Suscripcion` con `suscripcionRepository.findById(inversionista.getId())` (si existe).
  4. Convertir `interesesMercado` CSV a `List<String>` (split por coma, trim, filtrar vacíos).
  5. Convertir `tiposNotificacion` CSV a `List<String>` (ídem).
  6. Si `inversionista.comisionistaAsignadoId != null`: cargar comisionista y construir `ComisionistaAsignadoDTO`.
  7. Construir y retornar `PerfilInversionistaDTO` completo.
  8. Auditar `PERFIL_CONSULTADO` con `"Perfil consultado"`.
- Anotar con `@Transactional(readOnly = true)`.

### Fase 3 — Controlador

- `PerfilController.obtenerPerfil(Authentication auth)`:
  - Extraer `correo` del principal autenticado: `auth.getName()` (Spring Security popula el principal con el `sub` del JWT).
  - Delegar a `PerfilService.obtenerPerfil(correo)`.
  - Retornar `200 OK` con `PerfilInversionistaDTO`.
- Configurar endpoint `GET /api/perfil` con rol `INVERSIONISTA` en `SecurityConfig`.

### Fase 4 — Frontend

- El componente de perfil en el dashboard Angular llama `GET /api/perfil` al cargar.
- Mostrar todos los campos del DTO en la pantalla.
- Manejar `401` → redirigir a `/login`.
- Manejar `404` → mostrar mensaje de error.

### Fase 5 — Verificación

- Ejecutar los escenarios Gherkin del SPEC: consulta exitosa, intereses como lista, sin comisionista, sin JWT.
- Verificar que `contrasenia` no aparece en la respuesta (buscar en JSON).
- Verificar evento `PERFIL_CONSULTADO` en `logs/audit.log`.

---

## Dependencias

| Dependencia | Requerida para | Estado |
|---|---|---|
| JWT válido (HU-3) | Autenticación del request | Disponible |
| `usuario` e `inversionista` en BD (HU-1) | Datos del perfil | Disponible |
| `suscripcion` en BD (HU-1) | Estado premium | Disponible |
| Asignación de comisionista (HU-37) | `comisionistaAsignado` | Null si no asignado |
| `IAuditLog` impl. | Trazabilidad | Disponible |

---

## Decisiones de diseño

- **Correo del JWT, no del path:** garantiza que cada usuario solo consulte su propio perfil sin necesidad de un guard explícito de ownership.
- **DTOs, nunca entidades JPA:** `contrasenia` y otros campos internos nunca expuestos.
- **CSV → List<String> en el servicio:** `intereses_mercado` y `tipos_notificacion` almacenados como CSV en BD; el DTO los expone como listas para facilitar el consumo en Angular.
- **`@Transactional(readOnly = true)`:** consulta pura; optimiza performance en PostgreSQL.

---

## Riesgos principales

| Riesgo | Impacto | Mitigación |
|---|---|---|
| `usuario` existe pero `inversionista` no | 404 por inconsistencia de registro | Flujo de registro de HU-1 garantiza ambas entidades |
| Datos sensibles expuestos si JWT se filtra | Medio | HTTPS en producción; TTL corto del JWT |
