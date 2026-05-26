# plan.md — HU-33 Configurar mercados habilitados

> Derivado de `docs/HU-33-configurar-mercados-habilitados/SPEC.md`.
> Estado: PENDIENTE DE APROBACIÓN HUMANA.
> No se escribe código hasta que el humano apruebe este documento.

---

## 1. Qué construye esta historia

HU-33 le permite al administrador gestionar la configuración de los mercados financieros habilitados en el sistema (NYSE, NASDAQ, BMV, etc.) y sus horarios de operación. Se implementan dos operaciones:

1. **Consultar todos los mercados** (`GET /api/admin/mercados`): retorna la lista completa de configuraciones de `mercado_config`, incluyendo estado (habilitado/deshabilitado), horarios, zona horaria y días de operación.
2. **Actualizar un mercado** (`PUT /api/admin/mercados/{codigo}`): permite al administrador modificar los campos de configuración de un mercado existente. Los cambios son efectivos de forma inmediata para el scheduler de la cola de órdenes (HU-23) y para `IVerificacionMercado`.

Estos cambios son leídos por el módulo de Mercado vía `IAdministracion` y por el módulo de Órdenes vía `IVerificacionMercado`, aplicando la táctica Defer Binding (parámetros en BD, sin redespliegue).

---

## 2. Decisiones técnicas

| # | Decisión | Justificación |
|---|---|---|
| D1 | Solo se permite actualizar mercados existentes; no se crean ni eliminan mercados vía API en esta HU | SPEC §Fuera de alcance: "Creación de nuevos mercados vía UI (los mercados se insertan con datos iniciales)." La creación se hace mediante seed en `DatosInicialesAdministracion`. |
| D2 | Toda la lógica en `AdministracionService`; el controller solo valida JWT y delega | CONVENCIONES §1.3: `@Transactional` y lógica en services. |
| D3 | Validar que `zonaHoraria` sea un `ZoneId` válido de Java antes de persistir | SPEC §Riesgo R2: una zona horaria inválida rompe el cálculo de horario. Usar `ZoneId.of(zonaHoraria)` y capturar `DateTimeException`. |
| D4 | Los parámetros de `MercadoConfigDTO` se mapean manualmente en el service, sin exponer `MercadoConfig` al controller | CONVENCIONES §1.4: DTOs siempre en endpoints; nunca exponer entidades JPA. |
| D5 | El evento de auditoría incluye el código del mercado y los campos modificados en el campo `detalle` | CONVENCIONES §2.7: cambio de parámetro administrativo es evento auditable obligatorio. |
| D6 | `diasOperacion` se almacena como `VARCHAR` con valores separados por coma en BD; el DTO lo expone como `List<String>` | SPEC §Modelo de datos: `dias_operacion VARCHAR(100)` — mapeo manual en service. |

---

## 3. Cambios de dependencias

No se requieren dependencias Maven nuevas. Esta historia usa infraestructura existente:
- `MercadoConfigRepository` (ya existe en `administracion/repository/`).
- `IAuditLog` (ya implementado).
- Bean Validation (`jakarta.validation`) para validar campos del DTO.

---

## 4. Deuda técnica o hallazgos previos

| Hallazgo | Acción dentro de esta HU |
|---|---|
| El SPEC indica que `diasOperacion` se almacena como `VARCHAR(100)` con coma separada. El DTO lo representa como `List<String>`. Puede haber inconsistencia si el mapper no maneja la conversión. | Asegurarse de que `AdministracionService` convierte `List<String>` a String CSV al persistir y de CSV a `List<String>` al leer. |
| La tabla `mercado_config` ya debería existir (HU-23 la definió y el seed la populó). Si no existe, se debe crear con el DDL del SPEC. | Verificar que la tabla y el seed existen antes de implementar. Si no, crear `DatosInicialesAdministracion` con los mercados NYSE y NASDAQ. |
| La interfaz `IAdministracion` ya declara `List<MercadoConfigDTO> listarMercados()` y `MercadoConfigDTO actualizarMercado(String codigo, MercadoConfigDTO dto)` en `ARQUITECTURA.md`. Verificar que la implementación en `AdministracionService` es coherente. | Verificar la firma de los métodos en la implementación actual. |

---

## 5. Arquitectura de la solución

### 5a. Mapeo de componentes (backend)

```
administracion/
├── controller/
│   └── AdminController.java              ← agrega/verifica endpoints GET y PUT de mercados
├── service/
│   └── AdministracionService.java        ← agrega/verifica listarMercados() y actualizarMercado()
├── repository/
│   └── MercadoConfigRepository.java      ← agrega findByCodigo(String codigo)
├── model/
│   └── MercadoConfig.java                ← verificar campos: id, codigo, nombre, habilitado,
│                                            horaApertura, horaCierre, zonaHoraria, diasOperacion
└── dto/
    └── MercadoConfigDTO.java             ← verificar/crear: codigo, nombre, habilitado,
                                             horaApertura, horaCierre, zonaHoraria, diasOperacion(List<String>)
```

Interfaces que exponen datos de esta HU:
- `IAdministracion.listarMercados()` → consumida por el módulo Mercado.
- `IAdministracion.actualizarMercado(String codigo, MercadoConfigDTO dto)` → mutación exclusiva del módulo Administración.

### 5b. Mapeo de componentes (frontend)

```
frontend/src/app/admin/
└── admin-dashboard.component.ts          ← sección "Mercados": tabla + toggle + modal de edición
    admin-dashboard.component.html
```

Flujo UI:
1. Sección "Mercados" carga `GET /api/admin/mercados` al inicializar.
2. Tabla con columnas: código, nombre, estado (toggle habilitado/deshabilitado), horario, zona horaria, días.
3. Toggle de habilitado/deshabilitado llama `PUT /api/admin/mercados/{codigo}` con `{habilitado: !actual}`.
4. Modal de edición permite cambiar horaApertura, horaCierre, zonaHoraria, diasOperacion.
5. Confirmación con resumen del cambio antes de guardar.

### 5c. Modelo de datos

```sql
-- Tabla mercado_config (ya definida en HU-23 — confirmar existencia):
CREATE TABLE IF NOT EXISTS mercado_config (
    id              BIGSERIAL PRIMARY KEY,
    codigo          VARCHAR(20) UNIQUE NOT NULL,
    nombre          VARCHAR(100) NOT NULL,
    habilitado      BOOLEAN NOT NULL DEFAULT TRUE,
    hora_apertura   TIME NOT NULL,
    hora_cierre     TIME NOT NULL,
    zona_horaria    VARCHAR(50) NOT NULL,
    dias_operacion  VARCHAR(100) NOT NULL
);

-- Seed inicial (si no existe):
INSERT INTO mercado_config (codigo, nombre, habilitado, hora_apertura, hora_cierre, zona_horaria, dias_operacion)
VALUES
    ('NYSE',   'New York Stock Exchange', TRUE, '09:30', '16:00', 'America/New_York', 'LUNES,MARTES,MIERCOLES,JUEVES,VIERNES'),
    ('NASDAQ', 'NASDAQ',                  TRUE, '09:30', '16:00', 'America/New_York', 'LUNES,MARTES,MIERCOLES,JUEVES,VIERNES')
ON CONFLICT (codigo) DO NOTHING;
```

### 5d. Contratos de API

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| `GET` | `/api/admin/mercados` | `ADMINISTRADOR` | Lista todos los mercados configurados |
| `PUT` | `/api/admin/mercados/{codigo}` | `ADMINISTRADOR` | Actualiza configuración de un mercado |

**Request PUT (ejemplo):**
```json
{
  "habilitado": true,
  "horaApertura": "09:30",
  "horaCierre": "16:00",
  "zonaHoraria": "America/New_York",
  "diasOperacion": ["LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES"]
}
```

**Response PUT 200:**
```json
{
  "codigo": "NYSE",
  "nombre": "New York Stock Exchange",
  "habilitado": true,
  "horaApertura": "09:30",
  "horaCierre": "16:00",
  "zonaHoraria": "America/New_York",
  "diasOperacion": ["LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES"]
}
```

---

## 6. Grafo de dependencias entre tareas

```
T1.1 (verificar/crear MercadoConfig + MercadoConfigDTO)
    └─► T1.2 (MercadoConfigRepository — findByCodigo)
            └─► T2.1 (AdministracionService — listarMercados + actualizarMercado)
                    └─► T2.2 (AdminController — endpoints)
                            └─► T3.1 (tests unitarios)
                                    └─► T3.2 (tests integración)
                                            └─► T4.1 (frontend — tabla de mercados)
                                                    └─► T5.1 (DoD e2e)
```

---

## 7. Estrategia de tests

| Tipo | Herramienta | Qué prueba |
|---|---|---|
| Unitario — `AdministracionService` | JUnit 5 + Mockito | `listarMercados_retornaListaCompleta`, `actualizarMercado_codigoValido_actualizaYRetornaDTO`, `actualizarMercado_codigoInexistente_lanza404`, `actualizarMercado_zonaHorariaInvalida_lanza400` |
| Integración — `AdminController` | `@SpringBootTest` + MockMvc | GET retorna 200; PUT con código válido retorna 200; PUT con código inexistente retorna 404; sin JWT retorna 401; JWT de INVERSIONISTA retorna 403. |
| BD | Query SQL | Tras PUT: `SELECT habilitado, hora_apertura FROM mercado_config WHERE codigo = 'NYSE'` refleja valores actualizados. |

---

## 8. Trazabilidad criterios de aceptación → artefacto

| Escenario/Criterio | Test o mecanismo |
|---|---|
| GET retorna NYSE y NASDAQ en la lista | `listarMercados_retornaListaCompleta` |
| Deshabilitar mercado → habilitado=false + auditoría | `actualizarMercado_codigoValido_actualizaYRetornaDTO` + verify `IAuditLog` |
| Actualizar horario → horaApertura y horaCierre reflejados | `actualizarMercado_codigoValido_actualizaYRetornaDTO` |
| Mercado inexistente → 404 | `actualizarMercado_codigoInexistente_lanza404` |
| Sin JWT → 401 | Test de integración sin header Authorization |
| Evento PARAMETRO_ADMIN_ACTUALIZADO registrado | `verify(auditLog).registrar(TipoEvento.PARAMETRO_MODIFICADO, ...)` |
| EC-12 Audit Trail | `AuditLogService` recibe evento con código del mercado en detalle |

---

## 9. Preguntas abiertas

| # | Pregunta | Propuesta |
|---|---|---|
| Q1 | ¿Se debe auditar también la lectura `GET /api/admin/mercados` o solo las escrituras? | No auditar lecturas (no es un evento sensible). Solo auditar `PUT`. Confirmar con el equipo. |
| Q2 | El SPEC dice "los mercados se insertan con datos iniciales" pero no especifica cuántos mercados deben existir en el seed. ¿Solo NYSE y NASDAQ, o también BMV, LSE, TSE, ASX? | El SPEC muestra solo NYSE y NASDAQ en los ejemplos. La arquitectura menciona más mercados. Propuesta: seed con NYSE y NASDAQ; los demás se agregan manualmente. Confirmar con el product owner. |
| Q3 | ¿`PUT /api/admin/mercados/{codigo}` permite cambiar el `nombre` del mercado o solo sus parámetros operativos? | El DTO incluye `nombre` como campo. El SPEC no lo excluye explícitamente. Propuesta: permitir actualizar `nombre`. Confirmar. |
| Q4 | El campo `diasOperacion` en BD es `VARCHAR(100)` CSV. ¿Existe riesgo de que un administrador envíe nombres de días en idioma diferente? | Propuesta: validar que cada elemento de `diasOperacion` pertenezca al enum `{LUNES, MARTES, MIERCOLES, JUEVES, VIERNES, SABADO, DOMINGO}` con Bean Validation `@Pattern`. Confirmar. |

---

## 10. Definition of Done

- [ ] `GET /api/admin/mercados` retorna 200 con lista completa de `mercado_config`.
- [ ] `PUT /api/admin/mercados/{codigo}` actualiza `habilitado`, `horaApertura`, `horaCierre`, `zonaHoraria`, `diasOperacion`.
- [ ] Mercado no encontrado retorna 404 con mensaje descriptivo.
- [ ] Campos inválidos (zona horaria, formato de hora) retornan 400.
- [ ] Sin JWT o rol incorrecto retorna 401/403.
- [ ] Evento `PARAMETRO_MODIFICADO` registrado en auditoría por cada PUT exitoso.
- [ ] Tests unitarios de `AdministracionService` pasan (mínimo 4 casos).
- [ ] Tests de integración de `AdminController` para mercados pasan.
- [ ] Frontend muestra tabla de mercados con toggle y modal de edición funcionales.
- [ ] `docs/PROGRESO.md` marcado con ✅ para HU-33.
