# SPEC — Actualización de datos personales del inversionista

---

## Ficha de la historia

| Campo | Valor |
|---|---|
| ID | HU-7 |
| Sprint | 2 |
| Prioridad MoSCoW | Must Have |
| Estado | Completada |
| Épica | Autenticación / Gestión de perfil |
| CU asociado | CU-07 |
| Autor | Juan Diego Triana Mejia |
| Creada | 2026-05-20 |
| Última revisión | 2026-05-24 |
| Versión de esta spec | 1.0 |

**Trazabilidad:**

| Tipo | ID | Descripción |
|---|---|---|
| Requerimiento funcional | RF-06 | Actualización de datos personales del inversionista |
| Escenario de calidad | EC-12 | Trazabilidad de PERFIL_ACTUALIZADO |
| Historia que precede a esta | HU-6 | El perfil debe existir para poder actualizarlo |

---

## Historia de usuario

**Como** inversionista autenticado,
**quiero** actualizar mis datos personales (nombre, nivel de experiencia, intereses de mercado y teléfono),
**para** mantener mi perfil al día con mi información actual y preferencias financieras.

---

## Motivación y contexto

### Por qué existe esta historia

Los datos registrados en HU-1 pueden cambiar: el nombre puede necesitar corrección, el nivel de experiencia puede crecer, los intereses de mercado pueden cambiar y el teléfono puede variar. Esta historia provee el endpoint de actualización parcial del perfil con los campos que el inversionista puede modificar por sí mismo.

### Campos NO actualizables en este endpoint

- `correo` — identificador del sistema, no modificable.
- `contrasenia` — endpoint separado (SOPORTE-recuperacion-password o HU futura).
- `tipoIdentificacion`, `numeroIdentificacion` — datos de identidad legal, requieren proceso administrativo.
- `planSuscripcion`, `esPremium` — gestionados por HU-11/12.
- Preferencias de notificación — HU-8.
- Preferencias de operación — HU-9.
- MFA — HU-10.

---

## Actores y precondiciones

### Actores

| Actor | Rol en el sistema | Participación en este flujo |
|---|---|---|
| Inversionista autenticado | `INVERSIONISTA` | Iniciador — envía datos actualizados |
| `PerfilService` | Módulo `autenticacion` | Valida y persiste los cambios |
| `AuditLogService` | Módulo `trazabilidad` (vía `IAuditLog`) | Registra evento PERFIL_ACTUALIZADO |

### Precondiciones

- JWT válido en cabecera `Authorization: Bearer`.
- Existe `inversionista` vinculado al usuario del JWT.

### Postcondiciones

- Campos `nombre_completo` (en `usuario`), `nivel_experiencia`, `intereses_mercado` (en `inversionista`), y `telefono` (en `usuario`) actualizados.
- Evento `PERFIL_ACTUALIZADO` registrado en auditoría.
- Responde 200 con el perfil actualizado.

---

## Flujo principal

1. Usuario edita sus datos en `/perfil` y presiona "Guardar".
2. Frontend envía `PUT /api/perfil` con `ActualizarPerfilDTO` y `Authorization: Bearer <jwt>`.

**Backend — `PerfilService.actualizarDatos(correo, dto)`:**

3. Spring Security extrae `correo` del JWT.
4. `usuarioRepository.findByCorreo(correo)` → carga `Usuario`.
5. `inversionistaRepository.findById(usuario.id)` → carga `Inversionista` (PK compartida).
6. Actualiza `usuario.nombreCompleto = dto.nombreCompleto`.
7. Si `dto.nivelExperiencia != null`: actualiza `inversionista.nivelExperiencia`.
8. Si `dto.interesesMercado != null && !vacío`: normaliza (mayúsculas, deduplica, máx 10) y actualiza `inversionista.interesesMercado` como CSV.
9. Si `dto.telefono != null`: actualiza `usuario.telefono` (el teléfono ahora reside en la tabla `usuario`).
10. `usuarioRepository.save(usuario)` y `inversionistaRepository.save(inversionista)`.
11. `IAuditLog.registrar(PERFIL_ACTUALIZADO, correo, "Datos personales actualizados")`.
12. Responde `200 OK` con el `PerfilInversionistaDTO` actualizado.

---

## Flujos de error

### Error 1 — No autenticado

| Campo | Valor |
|---|---|
| Condición | JWT ausente, inválido o expirado |
| HTTP | 401 Unauthorized |
| Evento de auditoría | Ninguno |

### Error 2 — Campos obligatorios ausentes o inválidos

| Campo | Valor |
|---|---|
| Condición | `nombreCompleto` ausente o en blanco (`@NotBlank`) |
| Excepción Java | `MethodArgumentNotValidException` |
| HTTP | 400 Bad Request |
| Cuerpo | `RespuestaDTO{error: "nombreCompleto: El nombre completo es obligatorio"}` |
| Evento de auditoría | Ninguno |

### Error 3 — Usuario no encontrado (inconsistencia)

| Campo | Valor |
|---|---|
| Condición | JWT válido pero correo no existe en BD |
| Excepción Java | `UsuarioNoEncontradoException` |
| HTTP | 404 Not Found |
| Cuerpo | `RespuestaDTO{error: "Usuario no encontrado: {correo}"}` |
| Evento de auditoría | Ninguno |

### Error 4 — Error técnico genérico

| Campo | Valor |
|---|---|
| Condición | Falla BD u otro error inesperado |
| HTTP | 500 Internal Server Error |
| Cuerpo | `RespuestaDTO{error: "Error interno del servidor"}` |
| Evento de auditoría | Ninguno |

---

## Contrato de API

### Endpoint — `PUT /api/perfil`

```yaml
PUT /api/perfil:
  summary: Actualiza los datos personales del inversionista autenticado
  security:
    - bearerAuth: []
  requestBody:
    required: true
    content:
      application/json:
        schema:
          $ref: '#/components/schemas/ActualizarPerfilDTO'
        example:
          nombreCompleto: "Ana María Gómez Torres"
          nivelExperiencia: "AVANZADO"
          interesesMercado: ["AAPL", "NVDA", "MSFT"]
          telefono: "+573201112233"
  responses:
    '200':
      description: Datos actualizados exitosamente
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/PerfilInversionistaDTO'
    '400':
      description: nombreCompleto ausente o en blanco
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'
          example:
            error: "nombreCompleto: El nombre completo es obligatorio"
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
    ActualizarPerfilDTO:
      type: object
      required: [nombreCompleto]
      properties:
        nombreCompleto:
          type: string
          description: "@NotBlank"
        nivelExperiencia:
          type: string
          nullable: true
          description: "Opcional. PRINCIPIANTE, INTERMEDIO, AVANZADO"
        interesesMercado:
          type: array
          items:
            type: string
          nullable: true
          description: "Opcional. Se normalizan a mayúsculas, deduplicados, máx 10"
        telefono:
          type: string
          nullable: true
          description: "Opcional"
```

**Tabla de validaciones server-side:**

| Campo | Restricción | Mensaje de error |
|---|---|---|
| `nombreCompleto` | `@NotBlank` | "El nombre completo es obligatorio" |

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
| `IAuditLog` | `trazabilidad` | `registrar(PERFIL_ACTUALIZADO, correo, detalle)` | Tras actualización exitosa |

### Escenarios de calidad y tácticas materializadas

| EC | Táctica | Cómo se materializa en HU-7 |
|---|---|---|
| EC-12 | Audit Trail | `PERFIL_ACTUALIZADO` registrado en auditoría |

---

## Eventos y efectos transversales

### Eventos de auditoría emitidos

| Evento (`TipoEvento`) | Cuándo se emite | Datos en `detalle` |
|---|---|---|
| `PERFIL_ACTUALIZADO` | Actualización exitosa | `"Datos personales actualizados"` |

---

## Riesgos

| # | Riesgo | P | I | Mitigación | Test que lo cubre |
|---|---|:-:|:-:|---|---|
| R1 | El nombre en `usuario.nombre_completo` y el nombre mostrado en otras partes del sistema quedan desincronizados si no se recargan | Baja | Bajo | El frontend recarga el perfil tras actualizar | Manual: actualizar nombre y verificar que la barra de nav refleja el nuevo nombre |

---

## Criterios de verificación

### Escenarios de aceptación (Gherkin)

```gherkin
Funcionalidad: Actualización de datos personales

  Antecedentes:
    Dado que "ana@test.com" tiene JWT válido y perfil existente

  Escenario: Actualización exitosa de todos los campos
    Cuando se envía PUT /api/perfil con { nombreCompleto: "Ana M.", nivelExperiencia: "AVANZADO", interesesMercado: ["MSFT"], telefono: "+57300" }
    Entonces el sistema responde 200 OK
    Y usuario.nombre_completo es "Ana M."
    Y inversionista.nivel_experiencia es "AVANZADO"
    Y inversionista.intereses_mercado es "MSFT"
    Y inversionista.telefono es "+57300"
    Y se emite evento PERFIL_ACTUALIZADO en auditoría

  Escenario: Solo nombreCompleto es obligatorio
    Cuando se envía PUT /api/perfil con { nombreCompleto: "Ana M." } (sin los demás campos)
    Entonces el sistema responde 200 OK
    Y los campos opcionales mantienen sus valores previos

  Escenario: Intereses normalizados a mayúsculas
    Cuando se envía PUT /api/perfil con interesesMercado: ["aapl", "tsla", "aapl"]
    Entonces inversionista.intereses_mercado es "AAPL,TSLA" (deduplicado, mayúsculas)

  Escenario: nombreCompleto en blanco rechazado
    Cuando se envía PUT /api/perfil con { nombreCompleto: "" }
    Entonces el sistema responde 400 Bad Request
    Y el cuerpo contiene error sobre nombreCompleto

  Escenario: Sin JWT — 401
    Cuando se envía PUT /api/perfil sin Authorization
    Entonces el sistema responde 401 Unauthorized
```

---

## Interfaz de usuario

### Vistas afectadas

| Ruta Angular | Componente | Cambio introducido en HU-7 |
|---|---|---|
| `/perfil` | `PerfilComponent` | Formulario editable con los 4 campos actualizables; botón "Guardar" llama `PUT /api/perfil` |

---

## Fuera de alcance

- **Actualización de correo** — no permitida en MVP.
- **Actualización de contraseña** — SOPORTE-recuperacion-password.
- **Actualización de tipo/número de identificación** — requiere proceso administrativo.

---

## Definición de terminado

- [x] `PUT /api/perfil` responde 200 con perfil actualizado.
- [x] `nombreCompleto` en blanco responde 400.
- [x] `interesesMercado` normalizados a mayúsculas y deduplicados.
- [x] Campos opcionales nulos mantienen valores previos.
- [x] Evento `PERFIL_ACTUALIZADO` en auditoría.
- [x] `docs/PROGRESO.md` marcado con ✅ para HU-7.

---

## Historial de cambios

| Versión | Fecha | Descripción | Razón |
|---|---|---|---|
| 1.0 | 2026-05-24 | Refactorización a estructura SDD del proyecto. | Unificación de todos los SPEC.md bajo plantilla canónica SDD del proyecto |
