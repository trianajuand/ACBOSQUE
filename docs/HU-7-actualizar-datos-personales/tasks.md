# Tareas — HU-7: Actualizar datos personales

> Estado general: **COMPLETADA**

---

## Checklist de implementación

### Backend

- [x] **DTO de entrada** — `ActualizarPerfilDTO.java` en `autenticacion/dto/`
  - Campos: `nombreCompleto` (`@NotBlank`), `nivelExperiencia` (nullable), `interesesMercado` (nullable, `List<String>`), `telefono` (nullable)

- [x] **Service** — `PerfilService.actualizarDatos(String correo, ActualizarPerfilDTO dto)`
  - Carga `Usuario` por correo (`UsuarioNoEncontradoException` → 404 si falla)
  - Carga `Inversionista` por `usuario.id` (PK compartida)
  - Aplica `nombreCompleto` siempre (ya validado `@NotBlank`)
  - Aplica `nivelExperiencia` solo si no nulo
  - Aplica `interesesMercado` solo si lista no nula y no vacía (normaliza a mayúsculas, deduplica, máx 10, guarda como CSV)
  - Aplica `telefono` solo si no nulo
  - Persiste con `usuarioRepository.save` e `inversionistaRepository.save`
  - Llama `IAuditLog.registrar(PERFIL_ACTUALIZADO, correo, "Datos personales actualizados")`
  - Retorna `PerfilInversionistaDTO` construido desde entidades actualizadas

- [x] **Controller** — `PerfilController.actualizarDatos(@Valid @RequestBody ActualizarPerfilDTO)`
  - Verbo: `PUT /api/perfil`
  - Extrae correo de `Authentication` (SecurityContext)
  - Delega a `PerfilService.actualizarDatos`
  - Retorna `ResponseEntity<PerfilInversionistaDTO>` con 200 OK
  - Anotado con `@PreAuthorize("hasRole('INVERSIONISTA')")`

- [x] **Manejo de errores** — `GlobalExceptionHandler` captura:
  - `MethodArgumentNotValidException` → 400 con mensaje de campo
  - `UsuarioNoEncontradoException` → 404
  - `Exception` genérica → 500

- [x] **Auditoría** — evento `PERFIL_ACTUALIZADO` emitido en `PerfilService` tras `save` exitoso

### Frontend (dashboard.html / Angular)

- [x] Formulario editable en sección "Perfil" con los 4 campos actualizables
- [x] Botón "Guardar" llama `PUT /api/perfil` con JWT en header
- [x] Mensaje de confirmación visible al usuario tras 200 OK
- [x] Campos opcionales nulos no enviados (o enviados como `null`) para que el backend los ignore

### Documentación

- [x] `SPEC.md` creado/actualizado con contrato de API, flujos de error y criterios Gherkin
- [x] `plan.md` creado con decisiones de diseño y flujo de implementación
- [x] `tasks.md` creado (este archivo)
- [x] `docs/PROGRESO.md` marcado con ✅ para HU-7

---

## Criterios de aceptación verificados

| Criterio | Estado |
|---|:-:|
| PUT /api/perfil con todos los campos devuelve 200 y perfil actualizado | ✅ |
| `nombreCompleto` en blanco devuelve 400 con mensaje de error | ✅ |
| Campos opcionales nulos no sobreescriben valores existentes | ✅ |
| `interesesMercado` se normaliza a mayúsculas y deduplicado | ✅ |
| Evento `PERFIL_ACTUALIZADO` aparece en `audit.log` | ✅ |
| Sin JWT devuelve 401 | ✅ |

---

## Archivos modificados / creados

| Archivo | Tipo | Descripción |
|---|---|---|
| `autenticacion/dto/ActualizarPerfilDTO.java` | Nuevo | DTO de entrada con validaciones |
| `autenticacion/service/PerfilService.java` | Modificado | Método `actualizarDatos` añadido |
| `autenticacion/controller/PerfilController.java` | Modificado | `PUT /api/perfil` añadido |
| `autenticacion/dto/PerfilInversionistaDTO.java` | Existente | Reutilizado como DTO de salida |
