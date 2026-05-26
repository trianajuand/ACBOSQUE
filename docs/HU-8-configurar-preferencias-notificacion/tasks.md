# Tareas — HU-8: Configurar preferencias de notificación

> Estado general: **COMPLETADA**

---

## Checklist de implementación

### Modelo de datos

- [x] **Migración 3NF** — campos de notificación movidos de `inversionista` a `usuario`:
  - `notificaciones_activas BOOLEAN NOT NULL DEFAULT TRUE`
  - `notificacion_email BOOLEAN NOT NULL DEFAULT TRUE`
  - `notificacion_sms BOOLEAN NOT NULL DEFAULT FALSE`
  - `notificacion_whatsapp BOOLEAN NOT NULL DEFAULT FALSE`
  - `tipos_notificacion VARCHAR(500)`

- [x] **Entidad `Usuario`** — campos Java añadidos con anotaciones JPA correspondientes

### Backend

- [x] **DTO de entrada** — `PreferenciasNotificacionDTO.java` en `autenticacion/dto/`
  - Campos: `notificacionesActivas` (Boolean, default true), `notificacionEmail`, `notificacionSms`, `notificacionWhatsapp`, `tiposNotificacion` (List<String>, nullable)

- [x] **Service** — `PerfilService.actualizarPreferenciasNotificacion(String correo, PreferenciasNotificacionDTO dto)`
  - Carga `Usuario` por correo
  - Aplica los 4 flags booleanos directamente
  - Aplica `tiposNotificacion` como CSV solo si la lista no es nula
  - Persiste con `usuarioRepository.save`
  - Llama `IAuditLog.registrar(PREFERENCIAS_NOTIFICACION_ACTUALIZADAS, correo, "...")`
  - Retorna `RespuestaDTO{mensaje: "Preferencias de notificación actualizadas"}`

- [x] **Controller** — `PerfilController.actualizarPreferenciasNotificacion(@RequestBody PreferenciasNotificacionDTO)`
  - Verbo: `PUT /api/perfil/preferencias/notificaciones`
  - Extrae correo de `Authentication`
  - Delega a service, retorna 200 OK con `RespuestaDTO`

- [x] **Auditoría** — evento `PREFERENCIAS_NOTIFICACION_ACTUALIZADAS` emitido en `PerfilService`

### Integración con DespachadorNotificaciones (HU-41)

- [x] `IConsultaInversionista.obtenerPreferenciasNotificacion(Long usuarioId)` definido en interfaz
- [x] `ConsultaInversionistaService` implementa el método leyendo desde `usuarioRepository`
- [x] `DespachadorNotificaciones` inyecta `IConsultaInversionista` y consulta preferencias antes de enviar
- [x] `NotificacionPreferenciasDTO` como DTO neutro que cruza la frontera `autenticacion → integracion`

### Frontend (dashboard.html / Angular)

- [x] Sección "Notificaciones" en perfil con 4 toggles (master switch + 3 canales)
- [x] Selector múltiple para tipos de notificación (ORDENES, MERCADO, SEGURIDAD)
- [x] Llamada `PUT /api/perfil/preferencias/notificaciones` con JWT
- [x] Confirmación visual tras 200 OK

### Documentación

- [x] `SPEC.md` creado/actualizado
- [x] `plan.md` creado
- [x] `tasks.md` creado (este archivo)
- [x] `docs/PROGRESO.md` marcado con ✅ para HU-8

---

## Criterios de aceptación verificados

| Criterio | Estado |
|---|:-:|
| PUT actualiza los 5 campos correctamente | ✅ |
| `tiposNotificacion` almacenado como CSV en BD | ✅ |
| `notificacionesActivas = false` desactiva todas las notificaciones en DespachadorNotificaciones | ✅ |
| Evento `PREFERENCIAS_NOTIFICACION_ACTUALIZADAS` en audit.log | ✅ |
| Sin JWT devuelve 401 | ✅ |

---

## Archivos modificados / creados

| Archivo | Tipo | Descripción |
|---|---|---|
| `autenticacion/dto/PreferenciasNotificacionDTO.java` | Nuevo | DTO de entrada |
| `autenticacion/dto/NotificacionPreferenciasDTO.java` | Nuevo | DTO neutro para cruce de frontera módulos |
| `autenticacion/model/Usuario.java` | Modificado | 5 campos de notificación añadidos |
| `autenticacion/service/PerfilService.java` | Modificado | Método `actualizarPreferenciasNotificacion` añadido |
| `autenticacion/controller/PerfilController.java` | Modificado | `PUT /api/perfil/preferencias/notificaciones` añadido |
| `autenticacion/interfaces/IConsultaInversionista.java` | Modificado | Método `obtenerPreferenciasNotificacion` añadido |
| `autenticacion/service/ConsultaInversionistaService.java` | Modificado | Implementación del nuevo método |
