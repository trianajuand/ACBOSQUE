# plan.md — HU-35 Configurar parámetros de comisión

> Derivado de `docs/HU-35-configurar-parametros-comision/SPEC.md`.
> Estado: PENDIENTE DE APROBACIÓN HUMANA.
> No se escribe código hasta que el humano apruebe este documento.

---

## 1. Qué construye esta historia

HU-35 permite al administrador configurar el porcentaje de comisión global y el split plataforma/comisionista. Se implementan dos operaciones sobre la tabla `parametro_comision`:

1. **Consultar parámetros vigentes** (`GET /api/admin/comisiones`): retorna el registro activo de `parametro_comision` (el que tiene `fecha_fin IS NULL`).
2. **Actualizar parámetros** (`PUT /api/admin/comisiones`): crea un nuevo registro vigente y cierra el anterior. El cambio es efectivo de forma inmediata para todas las órdenes nuevas.

El módulo de Órdenes consume estos parámetros vía `IGestorParametros` (táctica Defer Binding de `ARQUITECTURA.md §3.4`), lo que significa que cualquier cambio afecta el cálculo de comisiones desde la próxima orden creada o previsualizada, sin redespliegue.

---

## 2. Decisiones técnicas

| # | Decisión | Justificación |
|---|---|---|
| D1 | Al actualizar, cerrar el registro vigente con `fecha_fin = CURRENT_DATE - 1` e insertar uno nuevo con `fecha_inicio = CURRENT_DATE`, no hacer UPDATE directo | SPEC §Modelo de datos: este patrón mantiene historial de vigencias para trazabilidad y para órdenes encoladas que se calculan en fechas posteriores (ARQUITECTURA §13.3). |
| D2 | Validar `porcentajeComision > 0 && <= 100` y `porcentajePlataforma + porcentajeComisionista == 100` en el service, además de Bean Validation en el DTO | Doble validación: Bean Validation en DTO filtra valores básicos; el service valida la regla de negocio del split. Errores de validación retornan 400 con mensaje descriptivo. |
| D3 | La tabla almacena los porcentajes como decimales en escala 0–1 (ej. 0.0200 para 2%) pero el DTO los expone como porcentajes 0–100 (ej. 2.0) | SPEC §ParametroComisionDTO usa escala 0–100. El service hace la conversión: `dto.getPorcentajeComision() / 100.0` al persistir y `entidad.getPorcentajeComision() * 100.0` al leer. |
| D4 | El campo `actualizado_por` en `parametro_comision` recibe el correo del admin extraído del JWT | CONVENCIONES §2.7: los cambios administrativos deben ser trazables al usuario que los realizó. |
| D5 | `IGestorParametros` implementado en `AdministracionService` siempre lee el registro con `fecha_fin IS NULL` (o `fecha_fin >= CURRENT_DATE`); sin caché en memoria | ARQUITECTURA §13.3: el parámetro vigente se lee desde BD en tiempo real. No se cachea para garantizar inmediatez del cambio. |
| D6 | El endpoint GET también está disponible solo para ADMINISTRADOR, no para otros roles | SPEC §Contrato de API: `security: bearerAuth: [] # Solo ADMINISTRADOR`. El módulo Órdenes lee parámetros internamente vía `IGestorParametros`, no vía este endpoint REST. |

---

## 3. Cambios de dependencias

No se requieren dependencias Maven nuevas. Se usa infraestructura existente:
- `ParametroComisionRepository` (ya existe en `administracion/repository/`).
- `IAuditLog`.
- Bean Validation.

---

## 4. Deuda técnica o hallazgos previos

| Hallazgo | Acción dentro de esta HU |
|---|---|
| El SPEC referencia `IParametroComision.obtenerVigente()` como interfaz que consume `OrdenService`, pero `ARQUITECTURA.md` declara `IGestorParametros` con métodos `obtenerPorcentajeComision()`, `obtenerSplitPlataforma()`, `obtenerSplitComisionista()`. No existe `IParametroComision` en la arquitectura. | Usar `IGestorParametros` como fuente de verdad. Si `OrdenService` usa actualmente `IParametroComision`, refactorizar para consumir `IGestorParametros`. Ver §9 Q1. |
| El SPEC dice que el registro vigente es el que tiene `fecha_fin IS NULL`, pero el DDL también menciona `fecha_fin >= CURRENT_DATE`. La lógica de `findParametroActivo(LocalDate)` en `ParametroComisionRepository` puede necesitar ambas condiciones. | Implementar la query como `fecha_inicio <= :fecha AND (fecha_fin IS NULL OR fecha_fin >= :fecha)` para ser más robusta. |
| Si no existe ningún registro en `parametro_comision` (BD limpia), el GET y la creación de órdenes fallarán. | Verificar que `DatosInicialesAdministracion` siempre crea el seed con los valores iniciales (2%, 60/40). |

---

## 5. Arquitectura de la solución

### 5a. Mapeo de componentes (backend)

```
administracion/
├── controller/
│   └── AdminController.java              ← agrega endpoints GET y PUT /api/admin/comisiones
├── service/
│   └── AdministracionService.java        ← agrega obtenerParametrosVigentes(), actualizarParametrosComision()
│                                            implementa IGestorParametros
├── repository/
│   └── ParametroComisionRepository.java  ← agrega findParametroActivo(LocalDate fecha)
├── model/
│   └── ParametroComision.java            ← verificar: id, porcentajeComision, porcentajePlataforma,
│                                            porcentajeComisionista, fechaInicio, fechaFin, actualizadoPor
└── interfaces/
    └── IGestorParametros.java            ← verificar que los 3 métodos están implementados
```

Interfaces que otros módulos consumen:
- `IGestorParametros.obtenerPorcentajeComision()` → retorna `BigDecimal` (ej. `0.0200`).
- `IGestorParametros.obtenerSplitPlataforma()` → retorna `BigDecimal` (ej. `0.6000`).
- `IGestorParametros.obtenerSplitComisionista()` → retorna `BigDecimal` (ej. `0.4000`).

### 5b. Mapeo de componentes (frontend)

```
frontend/src/app/admin/
└── admin-dashboard.component.ts          ← sección "Comisiones": formulario de parámetros
    admin-dashboard.component.html
```

Flujo UI:
1. Sección "Comisiones" carga `GET /api/admin/comisiones`.
2. Formulario con campos: porcentaje de comisión (%), split plataforma (%), split comisionista (%).
3. Indicador en tiempo real de la suma del split (debe sumar 100%).
4. Botón "Guardar cambios" con confirmación: "Este cambio aplicará a todas las órdenes nuevas. ¿Continuar?".
5. Historial de últimas actualizaciones (últimos N registros de `parametro_comision`).

### 5c. Modelo de datos

```sql
-- Tabla parametro_comision (confirmar existencia):
CREATE TABLE IF NOT EXISTS parametro_comision (
    id                        BIGSERIAL PRIMARY KEY,
    porcentaje_comision       DECIMAL(5,4) NOT NULL DEFAULT 0.0200,
    porcentaje_plataforma     DECIMAL(5,4) NOT NULL DEFAULT 0.6000,
    porcentaje_comisionista   DECIMAL(5,4) NOT NULL DEFAULT 0.4000,
    fecha_inicio              DATE NOT NULL,
    fecha_fin                 DATE,
    actualizado_por           VARCHAR(255)
);

-- Seed inicial (si no existe ningún registro vigente):
INSERT INTO parametro_comision (porcentaje_comision, porcentaje_plataforma, porcentaje_comisionista, fecha_inicio)
SELECT 0.0200, 0.6000, 0.4000, CURRENT_DATE
WHERE NOT EXISTS (SELECT 1 FROM parametro_comision WHERE fecha_fin IS NULL);
```

Operación de actualización (lógica en service):
```sql
-- Paso 1: cerrar registro vigente
UPDATE parametro_comision
   SET fecha_fin = CURRENT_DATE - INTERVAL '1 day'
 WHERE fecha_fin IS NULL;

-- Paso 2: insertar nuevo registro
INSERT INTO parametro_comision (porcentaje_comision, porcentaje_plataforma, porcentaje_comisionista, fecha_inicio, actualizado_por)
VALUES (:porc, :plat, :comis, CURRENT_DATE, :correoAdmin);
```

### 5d. Contratos de API

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| `GET` | `/api/admin/comisiones` | `ADMINISTRADOR` | Consulta parámetros de comisión vigentes |
| `PUT` | `/api/admin/comisiones` | `ADMINISTRADOR` | Actualiza parámetros de comisión |

**Request PUT:**
```json
{
  "porcentajeComision": 1.5,
  "porcentajePlataforma": 65.0,
  "porcentajeComisionista": 35.0
}
```

**Response PUT 200:**
```json
{
  "porcentajeComision": 1.5,
  "porcentajePlataforma": 65.0,
  "porcentajeComisionista": 35.0
}
```

---

## 6. Grafo de dependencias entre tareas

```
T1.1 (verificar ParametroComision + ParametroComisionDTO)
    └─► T1.2 (ParametroComisionRepository — findParametroActivo)
            └─► T2.1 (AdministracionService — obtenerVigentes + actualizar)
                    ├─► T2.2 (IGestorParametros — verificar implementación)
                    └─► T2.3 (AdminController — endpoints)
                                └─► T3.1 (tests unitarios)
                                        └─► T3.2 (tests integración)
                                                └─► T3.3 (test integración OrdenService usa nuevo parámetro)
                                                        └─► T4.1 (frontend)
                                                                └─► T5.1 (DoD e2e)
```

---

## 7. Estrategia de tests

| Tipo | Herramienta | Qué prueba |
|---|---|---|
| Unitario — `AdministracionService` | JUnit 5 + Mockito | `obtenerParametrosVigentes_retornaRegistroConFechaFinNull`, `actualizarParametros_valoresValidos_cierraPrevioYCreaUno`, `actualizarParametros_porcentajeCero_lanza400`, `actualizarParametros_splitIncoherente_lanza400` |
| Unitario — `IGestorParametros` | JUnit 5 + Mockito | `obtenerPorcentajeComision_retornaBigDecimalCorrecto`, `obtenerSplitPlataforma_retornaBigDecimalCorrecto` |
| Integración — `AdminController` | `@SpringBootTest` + MockMvc | GET 200; PUT válido 200; PUT porcentaje cero 400; PUT split 110% 400; sin JWT 401. |
| Integración — `OrdenService` | `@SpringBootTest` | Tras PUT de nuevo porcentaje, crear orden y verificar que `montoComision = montoBase × nuevoPorc`. |

---

## 8. Trazabilidad criterios de aceptación → artefacto

| Escenario/Criterio | Test o mecanismo |
|---|---|
| GET retorna porcentajeComision=2.0, split=60/40 | `obtenerParametrosVigentes_retornaRegistroConFechaFinNull` |
| PUT válido → 200 + nuevo porcentaje en GET | `actualizarParametros_valoresValidos_cierraPrevioYCreaUno` |
| Split suma ≠ 100 → 400 con mensaje | `actualizarParametros_splitIncoherente_lanza400` |
| Porcentaje = 0 → 400 | `actualizarParametros_porcentajeCero_lanza400` |
| Nueva orden usa parámetro actualizado | Test integración `OrdenService` post-PUT |
| Auditoría PARAMETRO_MODIFICADO | `verify(auditLog).registrar(TipoEvento.PARAMETRO_MODIFICADO, ...)` |
| Sin JWT → 401 | Test integración sin Authorization header |
| EC-13 Commission Preview — OrdenService usa IGestorParametros | Verificación estática de que `OrdenService` importa `IGestorParametros`, no constantes |

---

## 9. Preguntas abiertas

| # | Pregunta | Propuesta |
|---|---|---|
| Q1 | El SPEC menciona `IParametroComision.obtenerVigente()` pero `ARQUITECTURA.md` define `IGestorParametros` con tres métodos separados. ¿`IParametroComision` existe o es un error en el SPEC? | Usar `IGestorParametros` como fuente de verdad. Si `OrdenService` tiene referencias a `IParametroComision`, refactorizar a `IGestorParametros`. Requiere confirmación humana. |
| Q2 | ¿Se debe exponer un endpoint de historial de parámetros (lista de registros pasados) en esta HU, o solo el vigente? | El SPEC menciona "historial de cambios" en la UI pero no define un endpoint GET de historial. Propuesta: no implementar en esta HU; mostrar en UI solo los últimos N registros obtenidos del endpoint GET del vigente. Confirmar con el equipo. |
| Q3 | ¿Las órdenes en estado `EN_COLA` que se ejecuten tras un cambio de comisión deben usar el parámetro vigente al momento de ejecución o el de cuando se creó la propuesta? | El SPEC §Riesgo R1 dice "recalculan al momento de envío". Propuesta: usar el parámetro vigente al ejecutar. Confirmar — tiene implicaciones en HU-32 y HU-23. |
| Q4 | Los valores en BD están en escala 0–1 (0.0200) pero el DTO los usa en escala 0–100 (2.0). ¿Dónde se hace la conversión — en el mapper del service o en el constructor del DTO? | Propuesta: la conversión se hace en el mapper del service al construir el DTO (multiplicar por 100 al leer, dividir por 100 al persistir). Confirmar. |

---

## 10. Definition of Done

- [ ] `GET /api/admin/comisiones` retorna 200 con parámetros vigentes de `parametro_comision`.
- [ ] `PUT /api/admin/comisiones` cierra registro anterior y crea uno nuevo; retorna 200.
- [ ] Validación: porcentaje > 0 y ≤ 100; split suma exactamente 100%.
- [ ] Cambio inválido retorna 400 con mensaje descriptivo.
- [ ] Sin JWT o rol incorrecto retorna 401/403.
- [ ] Evento `PARAMETRO_MODIFICADO` registrado en auditoría.
- [ ] `OrdenService` lee parámetros vía `IGestorParametros` (verificado con grep — sin constantes hardcodeadas).
- [ ] Tests unitarios de `AdministracionService` y `IGestorParametros` pasan (mínimo 6 casos).
- [ ] Tests de integración de `AdminController` para comisiones pasan.
- [ ] Frontend muestra formulario editable con indicador de suma del split y confirmación.
- [ ] `docs/PROGRESO.md` marcado con ✅ para HU-35.
