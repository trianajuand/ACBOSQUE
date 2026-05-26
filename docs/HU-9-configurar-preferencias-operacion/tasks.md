# Tareas — HU-9: Configurar preferencias de operación

> Estado general: **COMPLETADA**

---

## Checklist de implementación

### Modelo de datos

- [x] **Tabla `inversionista`** — campos añadidos:
  - `tipo_orden_default VARCHAR(50) DEFAULT 'MARKET'`
  - `vista_portafolio VARCHAR(50) DEFAULT 'LISTA'`

- [x] **Entidad `Inversionista`** — campos Java `tipoOrdenDefault` y `vistaPortafolio` con anotaciones JPA

### Backend

- [x] **DTO de entrada** — `PreferenciasOperacionDTO.java` en `autenticacion/dto/`
  - Campos: `tipoOrdenDefault` (String nullable, valores: MARKET/LIMIT/STOP_LOSS/TAKE_PROFIT), `vistaPortafolio` (String nullable, valores: LISTA/GRAFICO)

- [x] **Service** — `PerfilService.actualizarPreferenciasOperacion(String correo, PreferenciasOperacionDTO dto)`
  - Carga `Usuario` por correo
  - Carga `Inversionista` por `usuario.id` (PK compartida)
  - Aplica `tipoOrdenDefault` solo si no nulo
  - Aplica `vistaPortafolio` solo si no nulo
  - Persiste con `inversionistaRepository.save`
  - Llama `IAuditLog.registrar(PREFERENCIAS_OPERACION_ACTUALIZADAS, correo, "...")`
  - Retorna `RespuestaDTO{mensaje: "Preferencias de operación actualizadas"}`

- [x] **Controller** — `PerfilController.actualizarPreferenciasOperacion(@RequestBody PreferenciasOperacionDTO)`
  - Verbo: `PUT /api/perfil/preferencias/operacion`
  - Extrae correo de `Authentication`
  - Delega a service, retorna 200 OK con `RespuestaDTO`

- [x] **Auditoría** — evento `PREFERENCIAS_OPERACION_ACTUALIZADAS` emitido en `PerfilService`

### Integración con otras historias

- [x] **HU-15 (Portafolio)** — frontend lee `vistaPortafolio` del perfil y elige entre vista lista / gráfico de barras
- [x] **HU-17..20 (Órdenes)** — formulario de nueva orden lee `tipoOrdenDefault` del perfil para preseleccionar el tipo

### Frontend (dashboard.html / Angular)

- [x] Sección "Preferencias de operación" en perfil con selector de tipo de orden y selector de vista de portafolio
- [x] Llamada `PUT /api/perfil/preferencias/operacion` con JWT
- [x] Confirmación visual tras 200 OK
- [x] Valores precargados desde `GET /api/perfil` al abrir la sección

### Documentación

- [x] `SPEC.md` creado/actualizado
- [x] `plan.md` creado
- [x] `tasks.md` creado (este archivo)
- [x] `docs/PROGRESO.md` marcado con ✅ para HU-9

---

## Criterios de aceptación verificados

| Criterio | Estado |
|---|:-:|
| PUT con ambos campos devuelve 200 y actualiza BD | ✅ |
| PUT con solo `tipoOrdenDefault` actualiza solo ese campo | ✅ |
| `vistaPortafolio` previo se mantiene si no se envía | ✅ |
| Evento `PREFERENCIAS_OPERACION_ACTUALIZADAS` en audit.log | ✅ |
| Sin JWT devuelve 401 | ✅ |

---

## Archivos modificados / creados

| Archivo | Tipo | Descripción |
|---|---|---|
| `autenticacion/dto/PreferenciasOperacionDTO.java` | Nuevo | DTO de entrada con 2 campos opcionales |
| `autenticacion/model/Inversionista.java` | Modificado | Campos `tipoOrdenDefault` y `vistaPortafolio` añadidos |
| `autenticacion/service/PerfilService.java` | Modificado | Método `actualizarPreferenciasOperacion` añadido |
| `autenticacion/controller/PerfilController.java` | Modificado | `PUT /api/perfil/preferencias/operacion` añadido |
