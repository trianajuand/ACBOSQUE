# plan.md — HU-34 Configurar feriados de mercado

> Derivado de `docs/HU-34-configurar-feriados-mercado/SPEC.md`.
> Estado: PENDIENTE DE APROBACIÓN HUMANA.
> No se escribe código hasta que el humano apruebe este documento.

---

## 1. Qué construye esta historia

HU-34 le permite al administrador gestionar el calendario de feriados de cada mercado. Se implementan tres operaciones sobre la tabla `feriado_mercado`:

1. **Consultar feriados de un mercado** (`GET /api/admin/mercados/{codigoMercado}/feriados`): lista todos los feriados registrados para un mercado identificado por su código.
2. **Registrar un feriado** (`POST /api/admin/mercados/{codigoMercado}/feriados`): agrega una fecha como día no hábil para un mercado. Retorna 201 y valida duplicados (409).
3. **Eliminar un feriado** (`DELETE /api/admin/mercados/{codigoMercado}/feriados/{feriadoId}`): elimina un feriado específico.

Los feriados son leídos por el scheduler de `ColaOrdenesService` (HU-23) en cada ciclo, de forma que un feriado recién agregado entra en vigor en el siguiente tick (hasta 60 segundos de latencia). Aplica la táctica Defer Binding definida en `ARQUITECTURA.md §3.4`.

---

## 2. Decisiones técnicas

| # | Decisión | Justificación |
|---|---|---|
| D1 | El endpoint recibe `codigoMercado` (string legible) en el path pero internamente trabaja con `mercado_config_id` (BIGINT FK) | SPEC §Modelo de datos: la FK `mercado_config_id` reemplaza `mercado_codigo VARCHAR`. Se resuelve el ID en el service mediante `MercadoConfigRepository.findByCodigo(codigoMercado)`. |
| D2 | Validar duplicado `(mercado_config_id, fecha)` a nivel de BD con constraint `UNIQUE` y capturar `DataIntegrityViolationException` en el service para retornar 409 | Más robusto que validar solo a nivel de aplicación; la BD garantiza la unicidad incluso bajo concurrencia. |
| D3 | Validar que la fecha tenga formato `YYYY-MM-DD` con Bean Validation `@Pattern` o `@NotNull` + tipo `LocalDate` en el DTO | Convenciones §1.8: Bean Validation en DTOs; el DTO declara `LocalDate fecha` y Jackson se encarga de la deserialización. Formato inválido → 400 antes de llegar al service. |
| D4 | No validar que la fecha sea futura (el SPEC menciona solo advertencia en UI para fechas pasadas) | SPEC §Error 5 dice "fecha anterior a hoy → 400", pero §Riesgo R1 dice "Admin elimina un feriado que ya pasó, sin efecto real". Hay ambigüedad — ver §9 Q1. |
| D5 | La respuesta del GET incluye `mercadoCodigo` (string) derivado por JOIN con `mercado_config`, no solo el ID numérico | SPEC §FeriadoDTO: `mercadoCodigo readOnly: true — derivado por JOIN`. Mejora legibilidad para el frontend. |
| D6 | Al eliminar, verificar que `feriadoId` pertenece al `codigoMercado` del path antes de eliminar | SPEC §Flujo eliminar paso 2a: "valida que el feriado existe y pertenece al mercado". Evita que un admin elimine feriados de otro mercado con IDs conocidos. |

---

## 3. Cambios de dependencias

No se requieren dependencias Maven nuevas. Se usa infraestructura existente:
- `FeriadoMercadoRepository` (ya existe en `administracion/repository/`).
- `MercadoConfigRepository` (ya existe).
- Bean Validation.
- `IAuditLog`.

---

## 4. Deuda técnica o hallazgos previos

| Hallazgo | Acción dentro de esta HU |
|---|---|
| ~~El SPEC del modelo de datos referenciaba `mercado_feriado` pero la entidad JPA usa `feriado_mercado`.~~ | **Resuelto (auditoría 2026-05-25):** El nombre real en la entidad JPA (`@Table(name = "feriado_mercado")`) es `feriado_mercado`. Todos los SPECs y plan.md actualizados para usar el nombre correcto. |
| El seed del SPEC asume `mercado_config.id=1` para NYSE y `id=2` para NASDAQ, lo cual es frágil si el orden de inserción cambió. | El seed debe resolver los IDs por código, no por posición: `INSERT INTO feriado_mercado (...) SELECT id, ... FROM mercado_config WHERE codigo = 'NYSE'`. |
| La interfaz `IAdministracion` en `ARQUITECTURA.md` declara `FeriadoMercadoDTO agregarFeriado(FeriadoMercadoDTO dto)` y `void eliminarFeriado(Long feriadoId)`. El SPEC usa rutas anidadas por mercado. Puede requerir adaptar la firma o agregar métodos. | Ver §9 Q3. |

---

## 5. Arquitectura de la solución

### 5a. Mapeo de componentes (backend)

```
administracion/
├── controller/
│   └── AdminController.java              ← agrega 3 endpoints: GET, POST, DELETE de feriados
├── service/
│   └── AdministracionService.java        ← agrega listarFeriados(), agregarFeriado(), eliminarFeriado()
├── repository/
│   └── FeriadoMercadoRepository.java     ← agrega findByMercadoConfigId, findByIdAndMercadoConfigId,
│                                            existsByMercadoConfigIdAndFecha
└── dto/
    └── FeriadoMercadoDTO.java            ← verificar/crear: id, mercadoConfigId, mercadoCodigo(readOnly),
                                             fecha(LocalDate), descripcion
```

### 5b. Mapeo de componentes (frontend)

```
frontend/src/app/admin/
└── admin-dashboard.component.ts          ← sección "Feriados": selector de mercado + tabla + agregar/eliminar
    admin-dashboard.component.html
```

Flujo UI:
1. Selector de mercado (dropdown con los mercados de `GET /api/admin/mercados`).
2. Al seleccionar mercado, carga `GET /api/admin/mercados/{codigo}/feriados`.
3. Tabla con: fecha, descripción, botón eliminar.
4. Botón "Agregar feriado" abre formulario con `date picker` y campo descripción.
5. Confirmación antes de eliminar feriados futuros.

### 5c. Modelo de datos

```sql
-- Tabla (confirmar nombre real en BD — propuesta sujeta a confirmación):
CREATE TABLE IF NOT EXISTS feriado_mercado (
    id                BIGSERIAL PRIMARY KEY,
    mercado_config_id BIGINT NOT NULL REFERENCES mercado_config(id),
    fecha             DATE NOT NULL,
    descripcion       VARCHAR(200),
    CONSTRAINT uq_feriado_mercado_fecha UNIQUE (mercado_config_id, fecha)
);

-- Seed (feriados NYSE/NASDAQ 2026 — resuelve IDs por código, no por posición):
INSERT INTO feriado_mercado (mercado_config_id, fecha, descripcion)
SELECT mc.id, '2026-01-01', 'New Year''s Day'
  FROM mercado_config mc WHERE mc.codigo IN ('NYSE', 'NASDAQ')
ON CONFLICT ON CONSTRAINT uq_feriado_mercado_fecha DO NOTHING;
-- (repetir para 2026-07-04 y 2026-12-25)
```

### 5d. Contratos de API

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| `GET` | `/api/admin/mercados/{codigoMercado}/feriados` | `ADMINISTRADOR` | Lista feriados del mercado |
| `POST` | `/api/admin/mercados/{codigoMercado}/feriados` | `ADMINISTRADOR` | Registra un feriado |
| `DELETE` | `/api/admin/mercados/{codigoMercado}/feriados/{feriadoId}` | `ADMINISTRADOR` | Elimina un feriado |

**Request POST body:**
```json
{
  "fecha": "2026-07-04",
  "descripcion": "Independence Day"
}
```

**Response POST 201:**
```json
{
  "id": 3,
  "mercadoCodigo": "NYSE",
  "fecha": "2026-07-04",
  "descripcion": "Independence Day"
}
```

---

## 6. Grafo de dependencias entre tareas

```
T1.1 (verificar/crear FeriadoMercado + FeriadoMercadoDTO)
    └─► T1.2 (FeriadoMercadoRepository — métodos de consulta)
            └─► T2.1 (AdministracionService — listarFeriados + agregarFeriado + eliminarFeriado)
                    └─► T2.2 (AdminController — 3 endpoints)
                            └─► T3.1 (tests unitarios)
                                    └─► T3.2 (tests integración)
                                            └─► T4.1 (frontend — sección feriados)
                                                    └─► T5.1 (DoD e2e)
```

---

## 7. Estrategia de tests

| Tipo | Herramienta | Qué prueba |
|---|---|---|
| Unitario — `AdministracionService` | JUnit 5 + Mockito | `listarFeriados_mercadoValido_retornaLista`, `agregarFeriado_fechaNueva_retorna201`, `agregarFeriado_fechaDuplicada_lanza409`, `agregarFeriado_mercadoInexistente_lanza404`, `eliminarFeriado_valido_elimina`, `eliminarFeriado_noPerteneceMercado_lanza404` |
| Integración — `AdminController` | `@SpringBootTest` + MockMvc | GET 200; POST válido 201; POST duplicado 409; DELETE válido 200; sin JWT 401; rol incorrecto 403. |
| BD | Query SQL | Tras POST: `SELECT COUNT(*) FROM feriado_mercado WHERE fecha = '2026-11-26'` debe ser 1. Tras DELETE: `SELECT COUNT(*) FROM feriado_mercado WHERE id = :id` debe ser 0. |

---

## 8. Trazabilidad criterios de aceptación → artefacto

| Escenario/Criterio | Test o mecanismo |
|---|---|
| GET retorna lista de feriados con formato YYYY-MM-DD | `listarFeriados_mercadoValido_retornaLista` |
| POST válido → 201 + aparece en GET | `agregarFeriado_fechaNueva_retorna201` + test de integración |
| POST duplicado → 409 | `agregarFeriado_fechaDuplicada_lanza409` |
| DELETE válido → 200 + ya no aparece en GET | `eliminarFeriado_valido_elimina` + test de integración |
| Mercado inexistente → 404 | `agregarFeriado_mercadoInexistente_lanza404` |
| Auditoría PARAMETRO_MODIFICADO en POST y DELETE | `verify(auditLog).registrar(...)` en unitarios |
| EC-12 Audit Trail | `AuditLogService` recibe evento con fecha y mercado en detalle |

---

## 9. Preguntas abiertas

| # | Pregunta | Propuesta |
|---|---|---|
| Q1 | El SPEC §Error 5 dice "fecha anterior a hoy → 400" pero §Riesgo R1 dice que la operación es válida con advertencia en UI. ¿Se valida la fecha como futura en el backend o solo se advierte en el frontend? | Propuesta: validar en el backend que la fecha sea >= hoy para el POST; permitir DELETE de fechas pasadas. Requiere confirmación humana. |
| Q2 | `ARQUITECTURA.md §12` lista la tabla como `feriado_mercado` pero el SPEC modelo usa `mercado_feriado`. ¿Cuál es el nombre real en la BD actual? | Verificar nombre con `\dt feriado*` en psql. Usar el nombre existente. Confirmar antes de implementar. |
| Q3 | La interfaz `IAdministracion` en `ARQUITECTURA.md` tiene `agregarFeriado(FeriadoMercadoDTO dto)` sin el `codigoMercado` como parámetro separado. El SPEC usa rutas anidadas `/mercados/{codigo}/feriados`. ¿Cómo se pasa el mercado a la interfaz? | Propuesta: incluir `mercadoConfigId` en el `FeriadoMercadoDTO` y resolver el ID del código en el controller antes de llamar al service. Confirmar con arquitecto. |
| Q4 | ¿El DELETE debe ser físico (borrado real) o lógico (campo `activo = false`)? | El SPEC dice `AdministracionService.eliminarFeriado(...) → b. Elimina el registro`, lo que implica borrado físico. Propuesta: borrado físico. Confirmar. |

---

## 10. Definition of Done

- [ ] `GET /api/admin/mercados/{codigo}/feriados` retorna 200 con lista de feriados del mercado.
- [ ] `POST /api/admin/mercados/{codigo}/feriados` crea feriado; retorna 201.
- [ ] Feriado duplicado retorna 409 con mensaje descriptivo.
- [ ] `DELETE /api/admin/mercados/{codigo}/feriados/{id}` elimina el feriado; retorna 200.
- [ ] Feriado o mercado no encontrado retorna 404.
- [ ] Sin JWT o rol incorrecto retorna 401/403.
- [ ] Evento `PARAMETRO_MODIFICADO` registrado en auditoría por cada POST y DELETE exitoso.
- [ ] Tests unitarios de `AdministracionService` pasan (mínimo 6 casos).
- [ ] Tests de integración de `AdminController` para feriados pasan.
- [ ] Frontend muestra sección de feriados con selector de mercado, tabla y formulario de agregar funcionales.
- [ ] `docs/PROGRESO.md` marcado con ✅ para HU-34.
