# SPEC — Activación o desactivación de MFA opcional para inversionista

---

## Ficha de la historia

| Campo | Valor |
|---|---|
| ID | HU-10 |
| Sprint | 2 |
| Prioridad MoSCoW | Must Have |
| Estado | Completada |
| Épica | Autenticación / Gestión de perfil |
| CU asociado | CU-10 |
| Autor | Juan Diego Triana Mejia |
| Creada | 2026-05-20 |
| Última revisión | 2026-05-24 |
| Versión de esta spec | 1.0 |

**Trazabilidad:**

| Tipo | ID | Descripción |
|---|---|---|
| Requerimiento funcional | RF-09 | MFA opcional para inversionista — activar/desactivar desde el perfil |
| Escenario de calidad | EC-10 | Authenticate Actors — MFA fortalece la autenticación del inversionista |
| Escenario de calidad | EC-12 | Trazabilidad de MFA_ACTIVADO y MFA_DESACTIVADO |
| Historia que habilita | HU-4 | Cuando MFA está activado, HU-4 es el paso 2 del login del inversionista |

---

## Historia de usuario

**Como** inversionista autenticado,
**quiero** activar o desactivar el segundo factor de autenticación (MFA) en mi perfil,
**para** agregar seguridad adicional a mi cuenta o simplificar el acceso según mis preferencias.

---

## Motivación y contexto

### Por qué existe esta historia

MFA es obligatorio para COMISIONISTA, ADMINISTRADOR y RESPONSABLE_LEGAL (siempre en HU-3). Para INVERSIONISTA, es opcional y configurable. Esta historia permite al inversionista habilitar o deshabilitar MFA sin salir de su sesión actual, efectivo en el próximo login.

### Restricción de rol

Este endpoint **solo está disponible para el rol `INVERSIONISTA`**. Si un COMISIONISTA o ADMINISTRADOR intenta usarlo, recibe 403 — para ellos, MFA es obligatorio y no puede desactivarse.

---

## Actores y precondiciones

### Actores

| Actor | Rol en el sistema | Participación en este flujo |
|---|---|---|
| Inversionista autenticado | `INVERSIONISTA` | Iniciador — activa o desactiva MFA |
| `PerfilService` | Módulo `autenticacion` | Valida rol, actualiza `mfa_habilitado` en `usuario` |
| `AuditLogService` | Módulo `trazabilidad` (vía `IAuditLog`) | Registra MFA_ACTIVADO o MFA_DESACTIVADO |

### Precondiciones

- JWT válido con rol `INVERSIONISTA`.
- Existe `usuario` vinculado al correo del JWT.

### Postcondiciones (activar)

- `usuario.mfa_habilitado = true`.
- El próximo login del inversionista requerirá código MFA (flujo de HU-4).
- Evento `MFA_ACTIVADO` registrado en auditoría.

### Postcondiciones (desactivar)

- `usuario.mfa_habilitado = false`.
- El próximo login del inversionista emitirá JWT directamente.
- Evento `MFA_DESACTIVADO` registrado en auditoría.

---

## Flujo principal

1. Usuario activa/desactiva el toggle de MFA en `/perfil`.
2. Frontend envía `PUT /api/perfil/mfa?activar=true` (o `?activar=false`) con JWT.

**Backend — `PerfilService.toggleMfa(correo, activar)`:**

3. Spring Security extrae `correo` y `rol` del JWT.
4. Si `rol != INVERSIONISTA`: lanza excepción → 403.
5. `usuarioRepository.findByCorreo(correo)` → carga `Usuario`.
6. `usuario.mfaHabilitado = activar`.
7. `usuarioRepository.save(usuario)`.
8. `IAuditLog.registrar(activar ? MFA_ACTIVADO : MFA_DESACTIVADO, correo, "MFA " + (activar ? "activado" : "desactivado"))`.
9. Responde `200 OK` con `RespuestaDTO{mensaje: "MFA " + (activar ? "activado" : "desactivado") + " exitosamente"}`.

---

## Flujos de error

### Error 1 — No autenticado

| Campo | Valor |
|---|---|
| Condición | JWT ausente, inválido o expirado |
| HTTP | 401 Unauthorized |
| Evento de auditoría | Ninguno |

### Error 2 — Rol no autorizado (no es INVERSIONISTA)

| Campo | Valor |
|---|---|
| Condición | `rol != INVERSIONISTA` (COMISIONISTA, ADMINISTRADOR, RESPONSABLE_LEGAL) |
| HTTP | 403 Forbidden |
| Cuerpo | `RespuestaDTO{error: "Solo los inversionistas pueden gestionar MFA opcional"}` |
| Evento de auditoría | Ninguno |

### Error 3 — Parámetro `activar` ausente o inválido

| Campo | Valor |
|---|---|
| Condición | `?activar` ausente o con valor no booleano |
| HTTP | 400 Bad Request |
| Cuerpo | Error de Spring MVC (parámetro requerido) |
| Evento de auditoría | Ninguno |

### Error 4 — Error técnico genérico

| Campo | Valor |
|---|---|
| Condición | Falla BD u otro error inesperado |
| HTTP | 500 Internal Server Error |
| Cuerpo | `RespuestaDTO{error: "Error interno del servidor"}` |

---

## Contrato de API

### Endpoint — `PUT /api/perfil/mfa`

```yaml
PUT /api/perfil/mfa:
  summary: Activa o desactiva MFA opcional para el inversionista autenticado
  security:
    - bearerAuth: []  # Solo rol INVERSIONISTA
  parameters:
    - name: activar
      in: query
      required: true
      schema:
        type: boolean
      description: "true para activar MFA, false para desactivarlo"
      example: true
  responses:
    '200':
      description: MFA actualizado exitosamente
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'
          examples:
            activado:
              value: { "mensaje": "MFA activado exitosamente" }
            desactivado:
              value: { "mensaje": "MFA desactivado exitosamente" }
    '401':
      description: No autenticado
    '403':
      description: Rol no permitido (solo INVERSIONISTA puede cambiar MFA)
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'
          example:
            error: "Solo los inversionistas pueden gestionar MFA opcional"
    '500':
      description: Error interno del servidor
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'
```

---

## Modelo de datos

### Campo en tabla `usuario` (actualizado en HU-10)

| Campo | Tipo | Descripción |
|---|---|---|
| `mfa_habilitado` | `BOOLEAN NOT NULL DEFAULT FALSE` | Determina si el inversionista requiere MFA en el próximo login |

---

## Módulos y arquitectura

### Módulos involucrados

| Módulo | Rol | Componentes específicos |
|---|---|---|
| `autenticacion` | Coordinador del flujo | `PerfilController`, `PerfilService` |
| `trazabilidad` | Registro de eventos | `AuditLogService` (impl. de `IAuditLog`) |

### Escenarios de calidad y tácticas materializadas

| EC | Táctica | Cómo se materializa en HU-10 |
|---|---|---|
| EC-10 | Authenticate Actors | MFA activado → siguiente login requerirá código de 6 dígitos además de contraseña |
| EC-12 | Audit Trail | `MFA_ACTIVADO` o `MFA_DESACTIVADO` registrado en auditoría |

---

## Eventos y efectos transversales

### Eventos de auditoría emitidos

| Evento (`TipoEvento`) | Cuándo se emite | Datos en `detalle` |
|---|---|---|
| `MFA_ACTIVADO` | `activar=true` procesado exitosamente | `"MFA activado"` |
| `MFA_DESACTIVADO` | `activar=false` procesado exitosamente | `"MFA desactivado"` |

---

## Riesgos

| # | Riesgo | P | I | Mitigación | Test que lo cubre |
|---|---|:-:|:-:|---|---|
| R1 | El inversionista desactiva MFA y su cuenta queda con menor protección. No hay confirmación adicional requerida | Media | Medio | El toggle está protegido por JWT (sesión activa). Para MVP, no se requiere re-autenticación al cambiar MFA | Manual: desactivar MFA y verificar que el siguiente login no pide código |

---

## Criterios de verificación

### Escenarios de aceptación (Gherkin)

```gherkin
Funcionalidad: Activar/desactivar MFA opcional

  Antecedentes:
    Dado que "ana@test.com" tiene JWT válido con rol=INVERSIONISTA y mfa_habilitado=false

  Escenario: Activar MFA exitosamente
    Cuando se envía PUT /api/perfil/mfa?activar=true con JWT de "ana@test.com"
    Entonces el sistema responde 200 OK
    Y el cuerpo contiene { "mensaje": "MFA activado exitosamente" }
    Y usuario.mfa_habilitado para "ana@test.com" es true
    Y se emite evento MFA_ACTIVADO en auditoría

  Escenario: Desactivar MFA exitosamente
    Dado que usuario.mfa_habilitado para "ana@test.com" es true
    Cuando se envía PUT /api/perfil/mfa?activar=false con JWT de "ana@test.com"
    Entonces el sistema responde 200 OK
    Y el cuerpo contiene { "mensaje": "MFA desactivado exitosamente" }
    Y usuario.mfa_habilitado para "ana@test.com" es false
    Y se emite evento MFA_DESACTIVADO en auditoría

  Escenario: Rol no permitido recibe 403
    Dado que existe usuario con JWT de rol=ADMINISTRADOR
    Cuando se envía PUT /api/perfil/mfa?activar=false
    Entonces el sistema responde 403 Forbidden
    Y el cuerpo contiene error sobre rol no permitido

  Escenario: Login posterior requiere MFA cuando está activado
    Dado que "ana@test.com" tiene mfa_habilitado=true
    Cuando se envía POST /api/auth/login con credenciales válidas de "ana@test.com"
    Entonces el sistema responde 200 OK con "requiereMfa": true

  Escenario: Sin JWT — 401
    Cuando se envía PUT /api/perfil/mfa?activar=true sin Authorization
    Entonces el sistema responde 401 Unauthorized
```

---

## Interfaz de usuario

### Vistas afectadas

| Ruta Angular | Componente | Cambio introducido en HU-10 |
|---|---|---|
| `/perfil` | `PerfilComponent` (sección seguridad) | Toggle switch para MFA; visible solo para rol INVERSIONISTA |

---

## Fuera de alcance

- **MFA obligatorio para otros roles** — controlado en HU-3 (`AutenticacionService.iniciarSesion`), no configurable.
- **Canales MFA alternativos (SMS, WhatsApp)** — no implementado en MVP.

---

## Definición de terminado

- [x] `PUT /api/perfil/mfa?activar=true` actualiza `usuario.mfa_habilitado = true` y responde 200.
- [x] `PUT /api/perfil/mfa?activar=false` actualiza `usuario.mfa_habilitado = false` y responde 200.
- [x] Rol distinto a INVERSIONISTA recibe 403.
- [x] Siguiente login del inversionista refleja el nuevo estado de MFA.
- [x] Eventos `MFA_ACTIVADO` / `MFA_DESACTIVADO` en auditoría.
- [x] Sin JWT responde 401.
- [x] `docs/PROGRESO.md` marcado con ✅ para HU-10.

---

## Historial de cambios

| Versión | Fecha | Descripción | Razón |
|---|---|---|---|
| 1.0 | 2026-05-24 | Refactorización a estructura SDD del proyecto. | Unificación de todos los SPEC.md bajo plantilla canónica SDD del proyecto |
