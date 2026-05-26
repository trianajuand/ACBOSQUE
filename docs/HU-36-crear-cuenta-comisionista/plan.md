# plan.md — HU-36 Crear cuenta de comisionista

> Derivado de `docs/HU-36-crear-cuenta-comisionista/SPEC.md`.
> Estado: PENDIENTE DE APROBACIÓN HUMANA.
> No se escribe código hasta que el humano apruebe este documento.

---

## 1. Qué construye esta historia

HU-36 le permite al administrador crear cuentas de comisionista. Los comisionistas **no se auto-registran** (regla dura del sistema, CLAUDE.md §Reglas duras #4); solo un Administrador autenticado puede crear estas cuentas. Se implementa un único endpoint:

- `POST /api/admin/comisionistas`: recibe `CrearComisionistaDTO`, crea un `Usuario` con `rol = COMISIONISTA`, `mfa_habilitado = true` (obligatorio) y `estado_cuenta = ACTIVA`. La contraseña se hashea con BCrypt. Retorna 201 si exitoso; 409 si el correo ya está registrado.

La creación delega la persistencia al módulo de Autenticación a través de `IGestionCuentas.crearComisionista()`, respetando el principio de que el módulo de Administración no importa clases internas del módulo de Autenticación.

---

## 2. Decisiones técnicas

| # | Decisión | Justificación |
|---|---|---|
| D1 | Solo el rol `ADMINISTRADOR` puede crear comisionistas; no existe auto-registro para este rol | CLAUDE.md §Reglas duras #4 y SPEC §precondiciones. Regla de seguridad no negociable del sistema. |
| D2 | La creación delega a `IGestionCuentas.crearComisionista(CrearComisionistaDTO)` — el módulo Administración nunca toca directamente `UsuarioRepository` ni `ComisionistaRepository` | ARQUITECTURA §5 regla 1: un módulo nunca importa clases internas de otro. |
| D3 | `mfa_habilitado = true` se fuerza en el service/IGestionCuentas, no se deja al criterio del administrador | CLAUDE.md §Reglas duras #15: MFA obligatorio para Comisionistas. No es un campo configurable en `CrearComisionistaDTO`. |
| D4 | La contraseña se hashea con BCrypt en `IGestionCuentas` antes de persistir | CONVENCIONES §2.1: BCrypt obligatorio. El hash ocurre en el módulo Autenticación, que es dueño de la entidad `Usuario`. |
| D5 | El comisionista creado no tiene perfil `Inversionista`, `Suscripcion` ni `IntegracionInversionista` | SPEC §Modelo de datos: "no hay registro en inversionista, suscripcion ni integracion_inversionista". Solo existe el `Usuario` base y el perfil `Comisionista`. |
| D6 | El evento de auditoría registra el correo del nuevo comisionista y el correo del admin que lo creó en el campo `detalle` | CONVENCIONES §2.7: creación de usuario es evento auditable. SPEC §paso 4. |

---

## 3. Cambios de dependencias

No se requieren dependencias Maven nuevas. Se usa:
- `IGestionCuentas` (módulo Autenticación, ya declarada en `ARQUITECTURA.md §9.2`).
- `IAuditLog`.
- Bean Validation en DTO.
- `BCryptPasswordEncoder` (ya presente en `shared/security/BCryptConfig`).

---

## 4. Deuda técnica o hallazgos previos

| Hallazgo | Acción dentro de esta HU |
|---|---|
| El SPEC indica "Estado En desarrollo" — el endpoint existe y persiste, pero los criterios de verificación completos no han sido validados en entorno integrado. | Los criterios de verificación de este plan.md sirven como lista de validación pendiente. |
| `IGestionCuentas` está declarada en `ARQUITECTURA.md` con método `crearComisionista(CrearComisionistaDTO dto)` pero no se ha confirmado que la firma del método en el código real coincida. | Verificar la interfaz `IGestionCuentas.java` y la implementación en el service de autenticación antes de implementar. |
| El SPEC no especifica si el comisionista recibe un correo de bienvenida con sus credenciales. | Ver §9 Q1. |
| El SPEC no especifica qué campos son obligatorios más allá de `correo`, `nombreCompleto` y `contrasena`. El `telefono` es nullable. | Confirmar si se requieren más campos (especialidad, etc.) en §9 Q2. |

---

## 5. Arquitectura de la solución

### 5a. Mapeo de componentes (backend)

```
administracion/
├── controller/
│   └── AdminController.java              ← agrega POST /api/admin/comisionistas
├── service/
│   └── AdministracionService.java        ← agrega crearComisionista(dto, correoAdmin)
└── dto/
    └── CrearComisionistaDTO.java         ← verificar: correo, nombreCompleto, contrasena, telefono(nullable)

autenticacion/ (solo a través de interfaz)
└── interfaces/
    └── IGestionCuentas.java              ← verificar método crearComisionista(CrearComisionistaDTO)
```

Flujo de llamadas:
```
AdminController
    → AdministracionService.crearComisionista(dto, correoAdmin)
        → IGestionCuentas.crearComisionista(dto)   [módulo autenticacion]
            → BCrypt.hash(dto.getContrasena())
            → UsuarioRepository.save(usuario)
            → ComisionistaRepository.save(comisionista)
        → IAuditLog.registrar(USUARIO_ADMIN_GESTIONADO, dto.getCorreo(), "Creado por " + correoAdmin)
```

### 5b. Mapeo de componentes (frontend)

```
frontend/src/app/admin/
└── admin-dashboard.component.ts          ← sección "Usuarios / Comisionistas": formulario de creación
    admin-dashboard.component.html
```

Flujo UI:
1. Botón "Nuevo Comisionista" abre modal con formulario: correo, nombre completo, contraseña, teléfono (opcional).
2. Submit llama `POST /api/admin/comisionistas`.
3. Éxito: mensaje "Comisionista creado exitosamente"; actualiza lista de usuarios.
4. Error 409: mensaje "El correo ya está registrado".

### 5c. Modelo de datos

No se crean tablas nuevas. Se reutilizan:

```sql
-- tabla usuario (ya existe):
-- rol = 'COMISIONISTA'
-- mfa_habilitado = TRUE
-- estado_cuenta = 'ACTIVA'
-- contrasena = BCrypt(contrasena_proporcionada)

-- tabla comisionista (ya existe, PK compartida con usuario.id):
-- id = FK de usuario.id (sin auto-generado)
```

No existe registro en `inversionista`, `suscripcion` ni `integracion_inversionista` para el comisionista.

### 5d. Contratos de API

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| `POST` | `/api/admin/comisionistas` | `ADMINISTRADOR` | Crea una nueva cuenta de comisionista |

**Request body:**
```json
{
  "correo": "comisionista@ejemplo.com",
  "nombreCompleto": "Carlos López",
  "contrasena": "Segura123!",
  "telefono": "+573001234567"
}
```

**Response 201:**
```json
{
  "mensaje": "Comisionista creado exitosamente"
}
```

**Response 409:**
```json
{
  "error": "El correo ya está registrado"
}
```

---

## 6. Grafo de dependencias entre tareas

```
T1.1 (verificar CrearComisionistaDTO + IGestionCuentas)
    └─► T2.1 (AdministracionService.crearComisionista)
            └─► T2.2 (AdminController POST /api/admin/comisionistas)
                    └─► T3.1 (tests unitarios)
                            └─► T3.2 (tests integración)
                                    └─► T4.1 (frontend — formulario de creación)
                                            └─► T5.1 (DoD e2e)
```

---

## 7. Estrategia de tests

| Tipo | Herramienta | Qué prueba |
|---|---|---|
| Unitario — `AdministracionService` | JUnit 5 + Mockito | `crearComisionista_datosValidos_crea`, `crearComisionista_correoExistente_lanza409` |
| Integración — `AdminController` | `@SpringBootTest` + MockMvc | POST válido retorna 201; POST correo duplicado retorna 409; sin JWT retorna 401; JWT de INVERSIONISTA retorna 403. |
| BD | Query SQL | Tras POST: `SELECT rol, mfa_habilitado, estado_cuenta FROM usuario WHERE correo = 'comis@test.com'` debe retornar `COMISIONISTA, true, ACTIVA`. La contraseña no debe ser texto plano: `SELECT contrasena FROM usuario WHERE correo = 'comis@test.com'` debe empezar con `$2a$`. |

---

## 8. Trazabilidad criterios de aceptación → artefacto

| Escenario/Criterio | Test o mecanismo |
|---|---|
| Creación exitosa → 201 + usuario con rol COMISIONISTA y mfa_habilitado=true | Test integración + query BD |
| Correo duplicado → 409 | `crearComisionista_correoExistente_lanza409` |
| Sin JWT → 401 | Test integración sin Authorization |
| JWT de inversionista → 403 | Test integración con JWT de otro rol |
| Contraseña hasheada con BCrypt (nunca texto plano) | Query BD: `contrasena LIKE '$2a$%'` |
| Evento USUARIO_ADMIN_GESTIONADO registrado | `verify(auditLog).registrar(TipoEvento.USUARIO_ADMIN_GESTIONADO, ...)` |

---

## 9. Preguntas abiertas

| # | Pregunta | Propuesta |
|---|---|---|
| Q1 | ¿El administrador envía la contraseña en texto plano en el request y el sistema la hashea, o se genera una contraseña temporal y se envía por correo? | El SPEC incluye `contrasena` en `CrearComisionistaDTO`, lo que implica que el admin la provee. Propuesta: el admin provee la contraseña inicial y el comisionista debe cambiarla en su primer login. El envío por correo es fuera de alcance de esta HU (ver HU-41). Confirmar. |
| Q2 | ¿Se requieren más campos para el perfil del comisionista (ej. `especialidades`, `licencia`, etc.)? El SPEC solo lista `correo`, `nombreCompleto`, `contrasena`, `telefono`. | Información no disponible en SPEC — menciona solo esos 4 campos. Propuesta: limitar a los campos del SPEC en el MVP. Confirmar si hay campos adicionales requeridos. |
| Q3 | ¿El endpoint debe retornar el `id` del comisionista creado en la respuesta, o solo el mensaje de éxito? | El SPEC retorna solo `RespuestaDTO{mensaje: "Comisionista creado exitosamente"}`. Propuesta: agregar el `id` para facilitar el flujo de HU-37 (asignación). Confirmar. |
| Q4 | ¿Se debe validar la fortaleza de la contraseña (longitud mínima, caracteres especiales)? | El SPEC no especifica reglas de fortaleza para la contraseña del comisionista. Propuesta: aplicar la misma validación que en HU-1 (`@Size(min=8)`, `@Pattern` para caracteres). Confirmar. |

---

## 10. Definition of Done

- [ ] `POST /api/admin/comisionistas` crea usuario con `rol = COMISIONISTA` y `mfa_habilitado = true`.
- [ ] `estado_cuenta = ACTIVA` al crear.
- [ ] Contraseña hasheada con BCrypt (verificado en BD).
- [ ] Correo duplicado retorna 409 con mensaje descriptivo.
- [ ] Sin JWT o rol incorrecto retorna 401/403.
- [ ] Evento `USUARIO_ADMIN_GESTIONADO` registrado en auditoría.
- [ ] Tests unitarios de `AdministracionService` pasan (mínimo 2 casos).
- [ ] Tests de integración de `AdminController` para creación de comisionista pasan.
- [ ] Frontend muestra formulario de creación funcional con manejo de errores.
- [ ] Validación completa en entorno integrado con frontend.
- [ ] `docs/PROGRESO.md` marcado con ✅ para HU-36.
