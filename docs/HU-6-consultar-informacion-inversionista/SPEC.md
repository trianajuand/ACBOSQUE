# SPEC — Consulta de perfil del inversionista

---

## Ficha de la historia

| Campo | Valor |
|---|---|
| ID | HU-6 |
| Sprint | 2 |
| Prioridad MoSCoW | Must Have |
| Estado | Completada |
| Épica | Autenticación / Gestión de perfil |
| CU asociado | CU-06 |
| Autor | Juan Diego Triana Mejia |
| Creada | 2026-05-20 |
| Última revisión | 2026-05-24 |
| Versión de esta spec | 1.0 |

**Trazabilidad:**

| Tipo | ID | Descripción |
|---|---|---|
| Requerimiento funcional | RF-05 | Consulta del perfil del inversionista autenticado |
| Escenario de calidad | EC-12 | Trazabilidad de PERFIL_CONSULTADO |
| Historia que precede a esta | HU-1 | Datos del perfil creados durante el registro |
| Historia que precede a esta | HU-3 | JWT necesario para autenticación del endpoint |

---

## Historia de usuario

**Como** inversionista autenticado,
**quiero** consultar mi información de perfil completa,
**para** ver mis datos personales, preferencias, estado de suscripción y asignación de comisionista en un solo lugar.

---

## Motivación y contexto

### Por qué existe esta historia

El perfil es el hub central del inversionista: datos de identidad, financieros, preferencias de operación, canales de notificación, estado premium y comisionista asignado. Esta historia provee un único endpoint GET que el frontend carga al entrar al dashboard de perfil.

### Dependencias hacia atrás

| Componente | Qué provee | Sin esto... |
|---|---|---|
| HU-1 (registro) | Datos de `usuario` e `inversionista` en BD | No hay perfil que consultar |
| JWT válido (HU-3) | Autenticación del request | 401 Unauthorized |
| `comisionista_asignado` (HU-37) | Datos del comisionista asignado | El campo `comisionistaAsignado` del DTO retorna null |

---

## Actores y precondiciones

### Actores

| Actor | Rol en el sistema | Participación en este flujo |
|---|---|---|
| Inversionista autenticado | `INVERSIONISTA` | Iniciador — accede a `/perfil` |
| `PerfilService` | Módulo `autenticacion` | Consulta `usuario` e `inversionista` y construye el DTO de respuesta |
| `AuditLogService` | Módulo `trazabilidad` (vía `IAuditLog`) | Registra evento PERFIL_CONSULTADO |

### Precondiciones

- JWT válido en cabecera `Authorization: Bearer`.
- Existen registros en `usuario` e `inversionista` para el correo del JWT.

### Postcondiciones

- Respuesta 200 con `PerfilInversionistaDTO` completo.
- Evento `PERFIL_CONSULTADO` registrado en auditoría.

---

## Flujo principal

1. Usuario navega a `/perfil` en Angular.
2. El componente Angular envía `GET /api/perfil` con `Authorization: Bearer <jwt>`.

**Backend — `PerfilService.obtenerPerfil(correo)`:**

3. Spring Security extrae `correo` del JWT del `SecurityContextHolder`.
4. `usuarioRepository.findByCorreo(correo)` → carga `Usuario`.
5. `inversionistaRepository.findById(usuario.id)` → carga `Inversionista` (PK compartida).
6. Construye `PerfilInversionistaDTO` con todos los campos (ver modelo de datos).
7. Busca asignación activa en `asignacion_comisionista` donde `inversionista_id = usuario.id AND activa = true`. Si existe, construye `ComisionistaAsignadoDTO` con nombre y correo del comisionista. (`Inversionista` no tiene campo `comisionistaAsignadoId` — la asignación vive en tabla separada.)
8. `IAuditLog.registrar(PERFIL_CONSULTADO, correo, "Perfil consultado")`.
9. Responde `200 OK` con `PerfilInversionistaDTO`.

---

## Flujos de error

### Error 1 — No autenticado

| Campo | Valor |
|---|---|
| Condición | JWT ausente, inválido o expirado |
| HTTP | 401 Unauthorized |
| Cuerpo | Respuesta estándar de Spring Security |
| Evento de auditoría | Ninguno |

### Error 2 — Usuario no encontrado (inconsistencia de datos)

| Campo | Valor |
|---|---|
| Condición | JWT válido pero correo no existe en `usuario` (caso extremo) |
| Excepción Java | `UsuarioNoEncontradoException` |
| HTTP | 404 Not Found |
| Cuerpo | `RespuestaDTO{error: "Usuario no encontrado: {correo}"}` |
| Evento de auditoría | Ninguno |

### Error 3 — Error técnico genérico

| Campo | Valor |
|---|---|
| Condición | Falla BD u otro error inesperado |
| HTTP | 500 Internal Server Error |
| Cuerpo | `RespuestaDTO{error: "Error interno del servidor"}` |
| Evento de auditoría | Ninguno |

---

## Contrato de API

### Endpoint — `GET /api/perfil`

```yaml
GET /api/perfil:
  summary: Consulta el perfil completo del inversionista autenticado
  security:
    - bearerAuth: []
  parameters: []  # Sin parámetros — correo tomado del JWT
  responses:
    '200':
      description: Perfil del inversionista
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/PerfilInversionistaDTO'
          example:
            nombreCompleto: "Ana Gómez Torres"
            correo: "ana.gomez@correo.com"
            nivelExperiencia: "INTERMEDIO"
            interesesMercado: ["AAPL", "TSLA", "NVDA"]
            telefono: "+573101234567"
            tipoIdentificacion: "CC"
            numeroIdentificacion: "1020304050"
            fechaNacimiento: "1992-04-15"
            direccion: "Cra 7 #45-12"
            ciudad: "Bogotá"
            codigoPostal: "110111"
            pais: "CO"
            estiloTrading: "SWING"
            ingresosMin: 5000000
            ingresosMax: 10000000
            solicitaComisionista: false
            comisionistaAsignado: null
            mfaHabilitado: false
            planSuscripcion: "BASICO"
            esPremium: false
            notificacionesActivas: true
            notificacionEmail: true
            notificacionSms: false
            notificacionWhatsapp: false
            tiposNotificacion: ["ORDENES", "MERCADO", "SEGURIDAD"]
            tipoOrdenDefault: "MARKET"
            vistaPortafolio: "LISTA"
    '401':
      description: No autenticado
    '404':
      description: Usuario no encontrado
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'
    '500':
      description: Error interno del servidor
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'

components:
  schemas:
    PerfilInversionistaDTO:
      type: object
      properties:
        nombreCompleto:
          type: string
        correo:
          type: string
          format: email
        nivelExperiencia:
          type: string
          nullable: true
        interesesMercado:
          type: array
          items:
            type: string
          description: "Convertido de CSV en BD a List<String>"
        telefono:
          type: string
          nullable: true
        tipoIdentificacion:
          type: string
          nullable: true
        numeroIdentificacion:
          type: string
          nullable: true
        fechaNacimiento:
          type: string
          nullable: true
        direccion:
          type: string
          nullable: true
        ciudad:
          type: string
          nullable: true
        codigoPostal:
          type: string
          nullable: true
        pais:
          type: string
          nullable: true
        estiloTrading:
          type: string
          nullable: true
        ingresosMin:
          type: number
          format: double
          nullable: true
        ingresosMax:
          type: number
          format: double
          nullable: true
        solicitaComisionista:
          type: boolean
        comisionistaAsignado:
          $ref: '#/components/schemas/ComisionistaAsignadoDTO'
          nullable: true
        mfaHabilitado:
          type: boolean
        planSuscripcion:
          type: string
        esPremium:
          type: boolean
        notificacionesActivas:
          type: boolean
        notificacionEmail:
          type: boolean
        notificacionSms:
          type: boolean
        notificacionWhatsapp:
          type: boolean
        tiposNotificacion:
          type: array
          items:
            type: string
          description: "Convertido de CSV en BD a List<String>"
        tipoOrdenDefault:
          type: string
        vistaPortafolio:
          type: string
    ComisionistaAsignadoDTO:
      type: object
      nullable: true
      properties:
        nombre:
          type: string
        correo:
          type: string
          format: email
```

---

## Modelo de datos

### Tablas leídas (creadas en HU-1)

`usuario` y `inversionista` — ver DDL completo en HU-1 SPEC.

**Fuentes de datos y transformaciones en el DTO:**
- `inversionista.intereses_mercado` (CSV `"AAPL,TSLA"`) → `List<String>` en el DTO.
- `usuario.tipos_notificacion` (CSV `"ORDENES,MERCADO"`) → `List<String>` en el DTO.
- `usuario.notificacion_email`, `usuario.notificacion_sms`, `usuario.notificacion_whatsapp`, `usuario.notificaciones_activas` → leídos directamente de `usuario`.
- `usuario.telefono`, `usuario.tipo_identificacion`, `usuario.numero_identificacion`, `usuario.fecha_nacimiento` → leídos de `usuario`.
- `suscripcion.es_premium`, `suscripcion.plan_suscripcion` → leídos de la tabla `suscripcion`.
- `asignacion_comisionista` (tabla separada — `Inversionista` no tiene columna `comisionista_asignado_id`) → `ComisionistaAsignadoDTO{nombre, correo}` si existe asignación activa.

---

## Módulos y arquitectura

### Módulos involucrados

| Módulo | Rol | Componentes específicos |
|---|---|---|
| `autenticacion` | Coordinador del flujo | `PerfilController`, `PerfilService` |
| `trazabilidad` | Registro de eventos | `AuditLogService` (impl. de `IAuditLog`) |

### Interfaces consumidas en este flujo

| Interfaz | Módulo dueño | Métodos usados | Cuándo |
|---|---|---|---|
| `IAuditLog` | `trazabilidad` | `registrar(PERFIL_CONSULTADO, correo, detalle)` | Al responder el perfil |

### Escenarios de calidad y tácticas materializadas

| EC | Táctica | Cómo se materializa en HU-6 |
|---|---|---|
| EC-12 | Audit Trail | `PERFIL_CONSULTADO` registrado en auditoría |
| (implícito) | DTOs en endpoints | `PerfilInversionistaDTO` expone solo campos del perfil; nunca la entidad JPA con `contrasenia` |

---

## Eventos y efectos transversales

### Eventos de auditoría emitidos

| Evento (`TipoEvento`) | Cuándo se emite | Datos en `detalle` |
|---|---|---|
| `PERFIL_CONSULTADO` | Consulta exitosa | `"Perfil consultado"` |

---

## Riesgos

| # | Riesgo | P | I | Mitigación | Test que lo cubre |
|---|---|:-:|:-:|---|---|
| R1 | `PerfilInversionistaDTO` expone datos sensibles (tipo/número de identificación, teléfono) — si el JWT se filtra, un atacante obtiene el perfil | Media | Medio | HTTPS en producción; JWT con TTL corto (1h); este riesgo es inherente a cualquier API de perfil autenticada | No aplica test backend |
| R2 | Inconsistencia si `usuario` existe pero `inversionista` no (registro incompleto) | Baja | Bajo | `findByUsuarioId` lanzaría `UsuarioNoEncontradoException` (404) | Manual: crear `usuario` sin `inversionista` y consultar perfil |

---

## Criterios de verificación

### Escenarios de aceptación (Gherkin)

```gherkin
Funcionalidad: Consulta de perfil del inversionista

  Antecedentes:
    Dado que el backend está corriendo en http://localhost:8080
    Y existe usuario "ana@test.com" con perfil completo en BD
    Y "ana@test.com" tiene JWT válido

  Escenario: Consulta exitosa del perfil
    Cuando se envía GET /api/perfil con Authorization: Bearer <jwt_de_ana>
    Entonces el sistema responde 200 OK
    Y el cuerpo contiene el campo "correo": "ana@test.com"
    Y el cuerpo contiene el campo "nombreCompleto" no nulo
    Y el campo "contrasenia" NO está presente en la respuesta
    Y se emite evento PERFIL_CONSULTADO en auditoría

  Escenario: Intereses devueltos como lista (no CSV)
    Dado que inversionista.intereses_mercado="AAPL,TSLA,NVDA"
    Cuando se envía GET /api/perfil con JWT válido
    Entonces el campo "interesesMercado" es ["AAPL","TSLA","NVDA"]

  Escenario: Sin comisionista asignado
    Dado que no existe registro activo en asignacion_comisionista para el inversionista
    Cuando se envía GET /api/perfil
    Entonces el campo "comisionistaAsignado" es null

  Escenario: Sin JWT — 401
    Cuando se envía GET /api/perfil sin cabecera Authorization
    Entonces el sistema responde 401 Unauthorized
```

---

## Interfaz de usuario

### Vistas afectadas

| Ruta Angular | Componente | Cambio introducido en HU-6 |
|---|---|---|
| `/perfil` | `PerfilComponent` | Muestra datos de `PerfilInversionistaDTO` al cargar la ruta |

---

## Fuera de alcance

- **Actualización de datos** — HU-7.
- **Configurar preferencias de notificación** — HU-8.
- **Configurar preferencias de operación** — HU-9.
- **Activar/desactivar MFA** — HU-10.
- **Consulta del perfil por Administrador** — no implementado en MVP.

---

## Decisiones y preguntas abiertas

| # | Pregunta / Decisión | Responsable | Fecha | Estado |
|---|---|---|---|---|
| 1 | **Decisión tomada:** El correo del inversionista se obtiene del JWT (`SecurityContextHolder`), no del path ni del body. Un inversionista solo puede consultar su propio perfil. | Juan Diego Triana Mejia | 2026-05-20 | Resuelta |
| 2 | **Decisión tomada:** `intereses_mercado` y `tipos_notificacion` se convierten de CSV a `List<String>` en el DTO para facilitar el consumo en Angular. | Juan Diego Triana Mejia | 2026-05-20 | Resuelta |

---

## Definición de terminado

- [x] `GET /api/perfil` responde 200 con `PerfilInversionistaDTO` completo.
- [x] El campo `contrasenia` nunca aparece en la respuesta.
- [x] `interesesMercado` y `tiposNotificacion` retornados como `List<String>` (no CSV).
- [x] `comisionistaAsignado` retorna null si no hay comisionista asignado.
- [x] Evento `PERFIL_CONSULTADO` registrado en auditoría.
- [x] `GET /api/perfil` sin JWT responde 401.
- [x] `docs/PROGRESO.md` marcado con ✅ para HU-6.

---

## Historial de cambios

| Versión | Fecha | Descripción | Razón |
|---|---|---|---|
| 1.0 | 2026-05-24 | Refactorización a estructura SDD del proyecto. Spec narrativo reemplazado con contrato de API completo, flujos de error, criterios Gherkin y decisiones documentadas. | Unificación de todos los SPEC.md bajo plantilla canónica SDD del proyecto |
| 1.1 | 2026-05-26 | Auditoría SDD: corrección de referencia a `inversionista.comisionistaAsignadoId` (campo inexistente) → acceso via tabla `asignacion_comisionista`. | `Inversionista` no tiene columna `comisionista_asignado_id`; la asignación vive en tabla separada. |
