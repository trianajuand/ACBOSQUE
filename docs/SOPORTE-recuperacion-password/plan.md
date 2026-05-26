# plan.md — SOPORTE Recuperación y Restablecimiento de Contraseña
> Derivado de `docs/SOPORTE-recuperacion-password/SPEC.md`.
> Estado: PENDIENTE DE APROBACIÓN HUMANA.

---

## 1. Qué construye esta historia

Implementa el flujo completo de recuperación de contraseña en dos pasos:
1. `POST /api/auth/forgot-password`: el usuario ingresa su correo → el sistema genera un token de 6 dígitos, lo persiste en `codigo_verificacion` (tipo `RECUPERACION_PASSWORD`, con TTL configurable) y lo envía por correo vía `INotificacion`.
2. `POST /api/auth/reset-password`: el usuario ingresa el token recibido y la nueva contraseña → el sistema valida el token (existencia, no expirado, no usado), marca el token como `usado = true`, y persiste la nueva contraseña con BCrypt.

Los tokens se guardan en BD con TTL, **nunca en `ConcurrentHashMap` en memoria** (mejora crítica vs. Malwatcher).

---

## 2. Decisiones técnicas

| # | Decisión | Justificación |
|---|---|---|
| 1 | Token de 6 dígitos numéricos en BD con TTL, NO en `ConcurrentHashMap` | Regla dura del proyecto (CLAUDE.md §7, CONVENCIONES.md §2.3). El SPEC confirma que el código usa 6 dígitos aunque la convención original mencionaba 64 hex. |
| 2 | TTL del token configurable en `app.seguridad.recuperacion-expiracion-minutos` | Permite ajuste sin redespliegue. |
| 3 | Token anterior se elimina/reemplaza al generar uno nuevo | SPEC lo indica. Evita que existan múltiples tokens válidos simultáneos para el mismo correo. |
| 4 | Contraseña hasheada con BCrypt antes de persistir | Regla dura del proyecto (CLAUDE.md §5, CONVENCIONES.md §2.1). Prohibido texto plano. |
| 5 | Evento `RECUPERACION_INICIADA` y `CONTRASEÑA_CAMBIADA` registrados vía `IAuditLog` | Recuperación de contraseña es evento auditable obligatorio (CONVENCIONES.md §2.7). |
| 6 | Los endpoints son públicos (no requieren JWT) | El usuario no tiene sesión activa durante la recuperación. `SecurityConfig` debe permitir estas rutas. |
| 7 | No se revela si el correo existe o no en la respuesta del `forgot-password` | Seguridad: no exponer si un correo está registrado. La respuesta es idéntica para correos existentes e inexistentes. |

---

## 3. Cambios de dependencias

Ningún cambio en `pom.xml` ni `package.json`. La propiedad `app.seguridad.recuperacion-expiracion-minutos` debe estar en `application.properties`.

---

## 4. Deuda técnica o hallazgos previos

| Hallazgo | Acción |
|---|---|
| El SPEC indica que el correo se almacena temporalmente en `localStorage` bajo `reset_correo` | Es una simplificación para el MVP. En producción, el correo debería pasarse en el body del `reset-password` directamente (sin depender de localStorage). Documentar en §9. |
| El SPEC confirma que se usan 6 dígitos numéricos aunque CONVENCIONES.md menciona 64 chars hex para recuperación | Hay discrepancia de convención. El código real usa 6 dígitos. Documentar decisión en §9 y actualizar CONVENCIONES.md si el equipo decide mantener 6 dígitos. |

---

## 5. Arquitectura de la solución

### 5a. Mapeo de componentes (backend)

| Capa | Componente | Módulo | Responsabilidad |
|---|---|---|---|
| Controller | `RecuperacionController` | `autenticacion` | `POST /api/auth/forgot-password` y `POST /api/auth/reset-password`. Endpoints públicos. Delega a `RecuperacionPasswordService`. |
| Service | `RecuperacionPasswordService` | `autenticacion` | `solicitarRecuperacion(RecuperarPasswordDTO)`: busca usuario, elimina tokens previos, genera token, persiste `CodigoVerificacion`, llama `INotificacion`, audita. `resetPassword(ResetPasswordDTO)`: valida token, marca usado, hashea y persiste contraseña. |
| Repository | `CodigoVerificacionRepository` | `autenticacion` | `findByCodigoAndTipoAndUsadoFalse(String, TipoCodigo)`, `deleteByCorreoAndTipo(...)`. |
| Model | `CodigoVerificacion` | `autenticacion` | `correo`, `codigo`, `tipo` (RECUPERACION_PASSWORD), `expiracion` (LocalDateTime), `usado` (boolean). |
| Interface | `INotificacion` | `integracion` | `enviarTokenRecuperacion(String correo, String nombreCompleto, String token)`. |
| Interface | `IAuditLog` | `trazabilidad` | `registrar(RECUPERACION_INICIADA, ...)`, `registrar(CONTRASENIA_CAMBIADA, ...)`. |
| DTO | `RecuperarPasswordDTO` | `autenticacion/dto` | `correo` (`@Email`, `@NotBlank`). |
| DTO | `ResetPasswordDTO` | `autenticacion/dto` | `correo` (`@Email`), `token` (`@NotBlank`), `nuevaContrasenia` (`@Size(min=8)`). |
| Util | `BCryptConfig` / `PasswordEncoder` | `autenticacion/security` | Hashea la nueva contraseña antes de persistir. |

### 5b. Mapeo de componentes (frontend)

| Componente | Archivo | Responsabilidad |
|---|---|---|
| `RecuperarComponent` | `auth/recuperar.component.ts` | Formulario con campo correo. Llama `POST /api/auth/forgot-password`. Guarda correo en `localStorage["reset_correo"]`. Navega a `/reset-password`. |
| `ResetPasswordComponent` | `auth/reset-password.component.ts` | Formulario con token y nueva contraseña. Lee correo de `localStorage`. Llama `POST /api/auth/reset-password`. |
| `ToastService` | `core/toast.service.ts` | Muestra errores y confirmaciones. |

### 5c. Modelo de datos

Tabla: `codigo_verificacion` (módulo `autenticacion`).

```
codigo_verificacion
  id           BIGINT PK (IDENTITY)
  correo       VARCHAR NOT NULL
  codigo       VARCHAR NOT NULL
  tipo         VARCHAR NOT NULL  -- 'RECUPERACION_PASSWORD'
  expiracion   TIMESTAMPTZ NOT NULL
  usado        BOOLEAN NOT NULL DEFAULT FALSE
  creado_en    TIMESTAMPTZ NOT NULL
```

Tabla: `usuario` — actualiza `contrasenia` (BCrypt) y `actualizado_en`.

### 5d. Contratos de API

```
POST /api/auth/forgot-password
Content-Type: application/json
{ "correo": "usuario@test.com" }

Response 200: (siempre, para no revelar si el correo existe)
{ "mensaje": "Si el correo está registrado, recibirás un código de recuperación." }

---

POST /api/auth/reset-password
Content-Type: application/json
{
  "correo": "usuario@test.com",
  "token": "123456",
  "nuevaContrasenia": "NuevaPass2026!"
}

Response 200: { "mensaje": "Contraseña actualizada exitosamente." }
Response 400: token inválido / expirado / ya usado.
Response 400: contraseña < 8 caracteres.
```

---

## 6. Grafo de dependencias entre tareas

```
T1.1 (verificar CodigoVerificacion modelo y repositorio)
    └─► T1.2 (verificar application.properties TTL)
            └─► T2.1 (implementar/verificar RecuperacionPasswordService.solicitarRecuperacion)
                    └─► T2.2 (implementar/verificar RecuperacionPasswordService.resetPassword)
                            └─► T2.3 (test unitario service)
                                    └─► T3.1 (test integración endpoints)
                                            └─► T3.2 (validación frontend flujo completo)
                                                    └─► T4.1 (DoD + PROGRESO.md)
```

---

## 7. Estrategia de tests

- **Unitario `RecuperacionPasswordService`:**
  - `solicitarRecuperacion_correoExistente_generaTokenYNotifica`.
  - `solicitarRecuperacion_correoInexistente_retornaRespuestaGenericaSinRevelar`.
  - `resetPassword_tokenCorrecto_actualizaContraseniaBcrypt`.
  - `resetPassword_tokenExpirado_lanzaInvalidTokenException` (test obligatorio CONVENCIONES.md §8).
  - `resetPassword_tokenUsado_rechazaOperacion` (test obligatorio CONVENCIONES.md §8).
  - `resetPassword_tokenInexistente_lanzaInvalidTokenException`.
- **Integración `MockMvc`:** flujo completo en dos pasos con token válido e inválido.

---

## 8. Trazabilidad criterios de aceptación → artefacto

| Criterio (SPEC) | Test o mecanismo |
|---|---|
| Usuario existente recibe código de recuperación | `solicitarRecuperacion_correoExistente_generaTokenYNotifica` + verificar invocación de `INotificacion`. |
| Token correcto permite cambiar contraseña | `resetPassword_tokenCorrecto_actualizaContraseniaBcrypt`. |
| Token expirado / usado / incorrecto es rechazado | `resetPassword_tokenExpirado_lanzaInvalidTokenException`, `resetPassword_tokenUsado_rechazaOperacion`. |
| Nueva contraseña queda hasheada | Verificar `BCryptPasswordEncoder.matches(nueva, hash)` en test unitario. |
| Token en BD con TTL (no ConcurrentHashMap) | Inspección del código: `RecuperacionPasswordService` no usa `ConcurrentHashMap`; usa `CodigoVerificacionRepository`. |

---

## 9. Preguntas abiertas

| # | Pregunta | Propuesta |
|---|---|---|
| 1 | Token de 6 dígitos vs. 64 chars hex (discrepancia entre CONVENCIONES.md y código real). ¿Cuál es el estándar a adoptar? | El código real usa 6 dígitos. Propuesta: documentar la decisión en CONVENCIONES.md §2.3 como excepción acordada para recuperación. 6 dígitos es menos seguro que 64 hex para recuperación de contraseña. |
| 2 | ¿El correo en `localStorage["reset_correo"]` es aceptable para producción? | No ideal: si el usuario cierra el navegador pierde el contexto. Propuesta alternativa: pedir el correo de nuevo en el formulario `/reset-password`. Requiere cambio en frontend. |
| 3 | ¿Se debe añadir rate limiting al endpoint `forgot-password` para evitar spam de correos? | SPEC no lo menciona. Recomendado: limitar a N solicitudes por IP por hora. Puede implementarse como deuda técnica post-MVP. |

---

## 10. Definition of Done

- [ ] `POST /api/auth/forgot-password` genera token en `codigo_verificacion` y envía correo vía `INotificacion`.
- [ ] Token anterior del mismo correo se elimina/invalida al generar uno nuevo.
- [ ] `POST /api/auth/reset-password` valida token, marca `usado = true`, persiste contraseña con BCrypt.
- [ ] Token expirado, ya usado o inexistente retorna 400.
- [ ] Contraseña con menos de 8 caracteres retorna 400.
- [ ] Ambos endpoints son públicos (sin JWT).
- [ ] `app.seguridad.recuperacion-expiracion-minutos` declarado en `application.properties`.
- [ ] Eventos `RECUPERACION_INICIADA` y `CONTRASENIA_CAMBIADA` auditados vía `IAuditLog`.
- [ ] Tests unitarios del service en verde (incluidos `tokenExpirado` y `tokenUsado`).
- [ ] Tests de integración MockMvc en verde.
- [ ] Flujo completo validado en frontend (`/recuperar` → correo → `/reset-password` → nueva contraseña → login exitoso).
- [ ] Discrepancia token 6 dígitos vs. 64 hex resuelta y documentada.
- [ ] `docs/PROGRESO.md` actualizado.
