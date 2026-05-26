# SPEC — Configuración de preferencias de operación

---

## Ficha de la historia

| Campo | Valor |
|---|---|
| ID | HU-9 |
| Sprint | 2 |
| Prioridad MoSCoW | Must Have |
| Estado | Completada |
| Épica | Autenticación / Gestión de perfil |
| CU asociado | CU-09 |
| Autor | Juan Diego Triana Mejia |
| Creada | 2026-05-20 |
| Última revisión | 2026-05-24 |
| Versión de esta spec | 1.0 |

**Trazabilidad:**

| Tipo | ID | Descripción |
|---|---|---|
| Requerimiento funcional | RF-08 | Configuración del tipo de orden por defecto y vista del portafolio |
| Escenario de calidad | EC-12 | Trazabilidad de PREFERENCIAS_OPERACION_ACTUALIZADAS |
| Historia relacionada | HU-17..20 | El tipo de orden default se usa al crear órdenes desde el formulario rápido |
| Historia relacionada | HU-15 | La vista del portafolio (LISTA/GRAFICO) afecta la presentación de HU-15 |

---

## Historia de usuario

**Como** inversionista autenticado,
**quiero** configurar mi tipo de orden por defecto y la vista preferida del portafolio,
**para** que el sistema precargue mis opciones favoritas y pueda operar más rápido.

---

## Actores y precondiciones

### Actores

| Actor | Rol en el sistema | Participación en este flujo |
|---|---|---|
| Inversionista autenticado | `INVERSIONISTA` | Iniciador — actualiza sus preferencias de operación |
| `PerfilService` | Módulo `autenticacion` | Persiste las preferencias en `inversionista` |
| `AuditLogService` | Módulo `trazabilidad` (vía `IAuditLog`) | Registra evento PREFERENCIAS_OPERACION_ACTUALIZADAS |

### Precondiciones

- JWT válido en cabecera `Authorization: Bearer`.
- Existe `inversionista` vinculado al usuario del JWT.

### Postcondiciones

- Campos `tipo_orden_default` y `vista_portafolio` actualizados en `inversionista`.
- Evento `PREFERENCIAS_OPERACION_ACTUALIZADAS` registrado en auditoría.

---

## Flujo principal

1. Usuario modifica `tipoOrdenDefault` y/o `vistaPortafolio` en la sección de preferencias de `/perfil`.
2. Frontend envía `PUT /api/perfil/preferencias/operacion` con `PreferenciasOperacionDTO` y JWT.

**Backend — `PerfilService.actualizarPreferenciasOperacion(correo, dto)`:**

3. Spring Security extrae `correo` del JWT.
4. `usuarioRepository.findByCorreo` → `inversionistaRepository.findById(usuario.id)` (PK compartida).
5. Si `dto.tipoOrdenDefault != null`: actualiza `inversionista.tipoOrdenDefault`.
6. Si `dto.vistaPortafolio != null`: actualiza `inversionista.vistaPortafolio`.
7. `inversionistaRepository.save(inversionista)`.
8. `IAuditLog.registrar(PREFERENCIAS_OPERACION_ACTUALIZADAS, correo, "Preferencias de operación actualizadas")`.
9. Responde `200 OK` con `RespuestaDTO{mensaje: "Preferencias de operación actualizadas"}`.

---

## Flujos de error

### Error 1 — No autenticado

| Campo | Valor |
|---|---|
| Condición | JWT ausente, inválido o expirado |
| HTTP | 401 Unauthorized |
| Evento de auditoría | Ninguno |

### Error 2 — Valor de enum inválido

| Campo | Valor |
|---|---|
| Condición | `tipoOrdenDefault` o `vistaPortafolio` con valor no reconocido por Jackson |
| HTTP | 400 Bad Request |
| Cuerpo | `RespuestaDTO{error: "..."}` (manejo de deserialización de Jackson) |
| Evento de auditoría | Ninguno |

### Error 3 — Error técnico genérico

| Campo | Valor |
|---|---|
| Condición | Falla BD u otro error inesperado |
| HTTP | 500 Internal Server Error |
| Cuerpo | `RespuestaDTO{error: "Error interno del servidor"}` |

---

## Contrato de API

### Endpoint — `PUT /api/perfil/preferencias/operacion`

```yaml
PUT /api/perfil/preferencias/operacion:
  summary: Actualiza las preferencias de operación del inversionista
  security:
    - bearerAuth: []
  requestBody:
    required: true
    content:
      application/json:
        schema:
          $ref: '#/components/schemas/PreferenciasOperacionDTO'
        example:
          tipoOrdenDefault: "LIMIT"
          vistaPortafolio: "GRAFICO"
  responses:
    '200':
      description: Preferencias actualizadas exitosamente
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'
          example:
            mensaje: "Preferencias de operación actualizadas"
    '400':
      description: Valor inválido para tipoOrdenDefault o vistaPortafolio
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'
    '401':
      description: No autenticado
    '500':
      description: Error interno del servidor
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'

components:
  schemas:
    PreferenciasOperacionDTO:
      type: object
      properties:
        tipoOrdenDefault:
          type: string
          nullable: true
          enum: [MARKET, LIMIT, STOP_LOSS, TAKE_PROFIT]
          description: "Tipo de orden preseleccionado en el formulario de nueva orden"
        vistaPortafolio:
          type: string
          nullable: true
          enum: [LISTA, GRAFICO]
          description: "Modo de visualización del portafolio en HU-15"
```

---

## Modelo de datos

### Campos en tabla `inversionista` (actualizados en HU-9)

| Campo | Tipo | Valores válidos |
|---|---|---|
| `tipo_orden_default` | `VARCHAR(50) DEFAULT 'MARKET'` | MARKET, LIMIT, STOP_LOSS, TAKE_PROFIT |
| `vista_portafolio` | `VARCHAR(50) DEFAULT 'LISTA'` | LISTA, GRAFICO |

---

## Módulos y arquitectura

### Módulos involucrados

| Módulo | Rol | Componentes específicos |
|---|---|---|
| `autenticacion` | Coordinador del flujo | `PerfilController`, `PerfilService` |
| `trazabilidad` | Registro de eventos | `AuditLogService` (impl. de `IAuditLog`) |

### Escenarios de calidad y tácticas materializadas

| EC | Táctica | Cómo se materializa en HU-9 |
|---|---|---|
| EC-12 | Audit Trail | `PREFERENCIAS_OPERACION_ACTUALIZADAS` registrado |

---

## Eventos y efectos transversales

### Eventos de auditoría emitidos

| Evento (`TipoEvento`) | Cuándo se emite | Datos en `detalle` |
|---|---|---|
| `PREFERENCIAS_OPERACION_ACTUALIZADAS` | Actualización exitosa | `"Preferencias de operación actualizadas"` |

---

## Criterios de verificación

### Escenarios de aceptación (Gherkin)

```gherkin
Funcionalidad: Configuración de preferencias de operación

  Antecedentes:
    Dado que "ana@test.com" tiene JWT válido y perfil existente

  Escenario: Actualización de tipo de orden y vista
    Cuando se envía PUT /api/perfil/preferencias/operacion con { tipoOrdenDefault: "LIMIT", vistaPortafolio: "GRAFICO" }
    Entonces el sistema responde 200 OK
    Y inversionista.tipo_orden_default es "LIMIT"
    Y inversionista.vista_portafolio es "GRAFICO"
    Y se emite evento PREFERENCIAS_OPERACION_ACTUALIZADAS en auditoría

  Escenario: Solo se actualiza tipoOrdenDefault
    Cuando se envía PUT con { tipoOrdenDefault: "STOP_LOSS" } (sin vistaPortafolio)
    Entonces el sistema responde 200 OK
    Y inversionista.tipo_orden_default es "STOP_LOSS"
    Y inversionista.vista_portafolio mantiene su valor previo

  Escenario: Sin JWT — 401
    Cuando se envía PUT sin Authorization
    Entonces el sistema responde 401 Unauthorized
```

---

## Definición de terminado

- [x] `PUT /api/perfil/preferencias/operacion` actualiza `tipoOrdenDefault` y `vistaPortafolio`.
- [x] Campos nulos no sobreescriben valores existentes.
- [x] Evento `PREFERENCIAS_OPERACION_ACTUALIZADAS` en auditoría.
- [x] Sin JWT responde 401.
- [x] `docs/PROGRESO.md` marcado con ✅ para HU-9.

---

## Historial de cambios

| Versión | Fecha | Descripción | Razón |
|---|---|---|---|
| 1.0 | 2026-05-24 | Refactorización a estructura SDD del proyecto. | Unificación de todos los SPEC.md bajo plantilla canónica SDD del proyecto |
