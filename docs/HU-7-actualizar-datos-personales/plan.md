# Plan de implementación — HU-7: Actualizar datos personales

## Contexto

El inversionista ya tiene perfil creado (HU-1/HU-6). Esta historia permite actualizar nombre completo, nivel de experiencia, intereses de mercado y teléfono mediante `PUT /api/perfil`. El correo es inmutable; contraseña, identificación y preferencias tienen sus propios endpoints. Todo se registra en auditoría como `PERFIL_ACTUALIZADO`.

---

## Estado

**Completada** — implementación en `PerfilController` + `PerfilService`. Verificada en dashboard.html.

---

## Decisiones de diseño

| Decisión | Justificación |
|---|---|
| Solo 4 campos actualizables | Correo = identificador del sistema; contraseña = flujo SOPORTE; tipo/número identidad = proceso administrativo; preferencias notificación/operación = HU-8/9; MFA = HU-10; premium = HU-11/12 |
| `nombreCompleto` es `@NotBlank`, el resto opcionales | El nombre es el único campo que no puede quedar vacío. Campos opcionales nulos no sobreescriben valores existentes |
| `interesesMercado` normalizado a CSV mayúsculas deduplicado | Consistencia con búsqueda de mercado y dashboard de acciones de interés (HU-13). Máx 10 símbolos |
| `telefono` reside en tabla `usuario`, no en `inversionista` | Normalización 3NF: el teléfono es dato de contacto transversal (auditoría, MFA, notificaciones), no dato financiero del inversionista |
| Respuesta devuelve `PerfilInversionistaDTO` completo | El frontend puede actualizar el estado local sin hacer un GET adicional |

---

## Módulos involucrados

| Módulo | Componente | Rol |
|---|---|---|
| `autenticacion` | `PerfilController` | Recibe `PUT /api/perfil`, extrae correo del JWT, delega al service |
| `autenticacion` | `PerfilService` | Carga usuario+inversionista, aplica cambios, persiste, devuelve DTO |
| `autenticacion` | `ActualizarPerfilDTO` | DTO de entrada con `@NotBlank nombreCompleto` y 3 campos opcionales |
| `autenticacion` | `PerfilInversionistaDTO` | DTO de salida con el perfil actualizado completo |
| `trazabilidad` | `AuditLogService` (vía `IAuditLog`) | Registra `PERFIL_ACTUALIZADO` tras persistencia exitosa |

---

## Flujo de implementación

```
PUT /api/perfil
  → JwtFilter valida token, pone correo en SecurityContext
  → PerfilController.actualizarDatos(ActualizarPerfilDTO dto)
    → extrae correo de Authentication
    → delega a PerfilService.actualizarDatos(correo, dto)
      → usuarioRepository.findByCorreo(correo)   // 404 si no existe
      → inversionistaRepository.findById(usuario.id)
      → usuario.nombreCompleto = dto.nombreCompleto   // @NotBlank garantiza no vacío
      → if dto.nivelExperiencia != null → inversionista.nivelExperiencia = dto.nivelExperiencia
      → if dto.interesesMercado != null && !vacío
          → normalizar: toUpperCase + distinct + máx 10
          → inversionista.interesesMercado = join(",")
      → if dto.telefono != null → usuario.telefono = dto.telefono
      → usuarioRepository.save(usuario)
      → inversionistaRepository.save(inversionista)
      → IAuditLog.registrar(PERFIL_ACTUALIZADO, correo, "Datos personales actualizados")
      → return PerfilInversionistaDTO.from(usuario, inversionista)
    → 200 OK con PerfilInversionistaDTO
```

---

## Contrato resumido

| Verbo | URL | Auth | Cuerpo | Respuesta exitosa |
|---|---|---|---|---|
| PUT | `/api/perfil` | Bearer JWT (INVERSIONISTA) | `ActualizarPerfilDTO` | 200 `PerfilInversionistaDTO` |

**Códigos de error:**
- `400` — `nombreCompleto` en blanco (`@NotBlank` via `MethodArgumentNotValidException`)
- `401` — JWT ausente, inválido o expirado
- `404` — correo del JWT no existe en BD (inconsistencia)
- `500` — error técnico genérico

---

## Escenarios de calidad cubiertos

| EC | Táctica | Materialización |
|---|---|---|
| EC-12 | Audit Trail | `PERFIL_ACTUALIZADO` registrado con correo y detalle |

---

## Dependencias

| Tipo | ID | Descripción |
|---|---|---|
| Historia previa | HU-1 | Registro crea el perfil que se actualiza aquí |
| Historia previa | HU-6 | La consulta del perfil expone los datos que aquí se editan |
| Interfaz cross-módulo | `IAuditLog` | Consume trazabilidad sin importar la implementación |

---

## Notas para el desarrollador

- Si `dto.interesesMercado` viene como lista vacía `[]`, no actualizar el campo (mantener el valor previo). Solo actualizar si la lista tiene al menos un elemento.
- `telefono` en formato libre — no hay validación de formato en MVP. Se puede añadir `@Pattern` en sprint futuro.
- El frontend debe recargar el nombre visible en la barra de navegación tras actualizar (el componente nav lee del JWT pero el nombre está en la respuesta de `GET /api/perfil`).
