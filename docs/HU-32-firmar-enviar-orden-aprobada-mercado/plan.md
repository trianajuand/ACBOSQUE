# plan.md — HU-32 Firmar y enviar orden aprobada al mercado

> Derivado de `docs/HU-32-firmar-enviar-orden-aprobada-mercado/SPEC.md`.
> Estado: PENDIENTE DE APROBACIÓN HUMANA.
> No se escribe código hasta que el humano apruebe este documento.

---

## 1. Qué construye esta historia

HU-32 cierra el flujo de tres etapas del comisionista: **proponer → aprobar → firmar y enviar**. Tras la aprobación del inversionista (HU-31), la propuesta tiene estado `APROBADA`. Esta historia le permite al comisionista:

1. Consultar la lista de propuestas que sus clientes han aprobado y que están pendientes de firma (`GET /api/comisionista/propuestas/aprobadas`).
2. Firmar y enviar una propuesta específica al mercado (`POST /api/comisionista/propuestas/{propuestaId}/firmar-enviar`), lo que desencadena:
   - Recálculo del monto con el precio actual del caché.
   - Reserva de fondos del cliente.
   - Creación del registro en `orden` con el estado resultante (`ENVIADA`, `EN_COLA` o `EJECUTADA`).
   - Registro de auditoría de `PROPUESTA_ORDEN_FIRMADA` y del evento de estado final.

---

## 2. Decisiones técnicas

| # | Decisión | Justificación |
|---|---|---|
| D1 | Recalcular `montoBase` con precio actual del caché al firmar, no usar el precio de la propuesta original | SPEC §D1 y EC-13: el precio de la propuesta es una estimación; la firma usa el precio real disponible en `precio_cache` al momento de la operación. |
| D2 | Validar `propuesta.comisionistaId == comisionistaId` del JWT antes de cualquier operación | Principio de mínimo privilegio: el comisionista no puede firmar propuestas de otros comisionistas. Alineado con EC-11 del documento de escenarios de calidad. |
| D3 | Orquestar toda la lógica en `OrdenService.firmarYEnviarPropuesta(...)`, no en el controller | CONVENCIONES §1.3: `@Transactional` y lógica de negocio en services. El controller solo delega y devuelve la respuesta. |
| D4 | Envolver la reserva de fondos y el envío a Alpaca en una única transacción JPA; si Alpaca falla, el rollback libera los fondos reservados | SPEC §Error 6 / Riesgo R3: evitar fondos reservados huérfanos ante fallo del adaptador externo. |
| D5 | Determinar el destino (Alpaca / cola / simulado) dentro de `OrdenService` mediante llamada a `IVerificacionMercado.esMercadoAbierto(mercado)` | ARQUITECTURA §5 regla 6: la verificación de horarios se hace siempre a través de `IVerificacionMercado`, nunca con lógica hardcodeada. |
| D6 | Usar `IGestorParametros.obtenerPorcentajeComision()` para el recálculo de comisión al firmar | ARQUITECTURA §13.7: parámetros de comisión se leen siempre desde `IGestorParametros`, no como constantes. |
| D7 | El endpoint de firma recibe únicamente `propuestaId` en el path; sin body | La propuesta ya contiene todos los datos necesarios (activo, cantidad, lado, tipo). No hay campos que el comisionista deba modificar al firmar. |
| D8 | Registro de `PROPUESTA_ORDEN_FIRMADA` y del evento de estado final como dos llamadas secuenciales a `IAuditLog` | SPEC §Flujo principal paso 4-5: son dos eventos distintos con semántica diferente. Se registran por separado para granularidad en la trazabilidad. |

---

## 3. Cambios de dependencias

No se requieren dependencias Maven nuevas. Esta historia usa infraestructura ya presente:
- Spring Data JPA (ya en `pom.xml`): repositorios `PropuestaOrdenRepository`, `OrdenRepository`, `CuentaFondosRepository`.
- `IIntegracionAlpaca` (módulo `integracion`): ya implementado en `AlpacaAdapter`.
- `IVerificacionMercado` (módulo `mercado`): ya implementado en `MercadoService`.
- `IGestorParametros` (módulo `administracion`): ya implementado en `AdministracionService`.
- `IAuditLog` (módulo `trazabilidad`): ya implementado en `AuditLogService`.
- `IAsignacionComisionista` (módulo `autenticacion`): ya implementado.

---

## 4. Deuda técnica o hallazgos previos

| Hallazgo | Acción dentro de esta HU |
|---|---|
| La entidad `PropuestaOrden` debe tener los campos `firmadaEn` (Timestamp) y `ordenId` (Long FK) para que la firma los popule. Si no existen, deben agregarse en esta HU. | Verificar la entidad `PropuestaOrden` y agregar los campos si faltan. Documentar en §5c. |
| El SPEC referencia `IAlpacaAdapter` con método `enviarOrden(OrdenAlpacaDTO)`, pero `ARQUITECTURA.md` declara `IIntegracionAlpaca.enviarOrden(CrearOrdenRequestDTO request, String alpacaAccountId)`. Hay una discrepancia de firma. | Usar la firma declarada en `ARQUITECTURA.md` como fuente de verdad. Ver §9 pregunta abierta Q1. |
| El SPEC declara evento `ORDEN_ENVIADA_ALPACA` y `ORDEN_EN_COLA` en `TipoEvento`, pero `ARQUITECTURA.md` lista `ORDEN_EJECUTADA`, `ORDEN_ENCOLADA`. Posible inconsistencia de nombres. | Ver §9 pregunta abierta Q2 — usar nombres de `ARQUITECTURA.md` como fuente de verdad. |
| El SPEC menciona `ORDEN_RECHAZADA_FONDOS` y `ORDEN_FALLO_ALPACA` como `TipoEvento`, pero no están en la lista de `ARQUITECTURA.md`. | Ver §9 pregunta abierta Q3. |

---

## 5. Arquitectura de la solución

### 5a. Mapeo de componentes (backend)

```
ordenes/
├── controller/
│   └── ComisionistaController.java       ← agrega 2 endpoints nuevos
├── service/
│   └── OrdenService.java                 ← agrega firmarYEnviarPropuesta(propuestaId, comisionistaId)
├── repository/
│   ├── PropuestaOrdenRepository.java     ← agrega findByIdAndComisionistaId(id, comisionistaId)
│   │                                        y findByComisionistaIdAndEstado(comisionistaId, APROBADA)
│   └── CuentaFondosRepository.java       ← ya existe; se usa para reservar fondos
└── model/
    └── PropuestaOrden.java               ← verificar/agregar campos firmadaEn y ordenId
```

Interfaces consumidas en esta HU:
- `IVerificacionMercado.esMercadoAbierto(String mercado)` — determina si encolar o enviar.
- `IIntegracionAlpaca.enviarOrden(CrearOrdenRequestDTO, String alpacaAccountId)` — envío al mercado.
- `IGestorParametros.obtenerPorcentajeComision()` — recálculo de comisión.
- `IAsignacionComisionista.tieneComisionistaAsignado(Long inversionistaId)` — validación de asignación.
- `IAuditLog.registrar(TipoEvento, String correo, String detalle)` — dos llamadas por firma exitosa.

### 5b. Mapeo de componentes (frontend)

```
frontend/src/app/comisionista/
└── comisionista-dashboard.component.ts   ← agrega sección "Propuestas Aprobadas"
    comisionista-dashboard.component.html ← tabla de propuestas + modal de confirmación
```

Flujo UI:
1. Sección "Propuestas Aprobadas" carga `GET /api/comisionista/propuestas/aprobadas`.
2. Tabla muestra: símbolo, tipo, lado, cantidad, precio actual recalculado, monto estimado, cliente.
3. Botón "Firmar y Enviar" abre modal con resumen (precio recalculado, monto total, comisión).
4. Confirmación envía `POST /api/comisionista/propuestas/{id}/firmar-enviar`.
5. Respuesta exitosa: propuesta desaparece del panel y se muestra toast de éxito con estado resultante.

### 5c. Modelo de datos

Campos a verificar/agregar en `propuesta_orden`:

```sql
-- propuesta — confirmar que existen estos campos (propuesta sujeta a confirmación):
ALTER TABLE propuesta_orden
    ADD COLUMN IF NOT EXISTS firmada_en  TIMESTAMP,
    ADD COLUMN IF NOT EXISTS orden_id    BIGINT REFERENCES orden(id);
```

Campos del nuevo registro en `orden` al firmar:

```sql
-- Nuevo registro creado al firmar (campos relevantes):
-- inversionista_id          BIGINT NOT NULL
-- activo_id                 BIGINT NOT NULL
-- parametro_comision_id     BIGINT NOT NULL   ← snapshot del parámetro vigente
-- monto_base                DECIMAL(15,4)     ← recalculado con precio actual
-- monto_comision            DECIMAL(15,4)
-- monto_plataforma          DECIMAL(15,4)     ← 60% de monto_comision
-- monto_comisionista        DECIMAL(15,4)     ← 40% de monto_comision
-- precio_ejecucion          DECIMAL(12,4)     ← precio del caché al momento de firma
-- alpaca_order_id           VARCHAR(255)      ← NULL si no se envió a Alpaca
-- estado                    VARCHAR(50)       ← ENVIADA | EN_COLA | EJECUTADA
```

Actualización en `cuenta_fondos` del cliente:

```sql
-- Al reservar fondos:
UPDATE cuenta_fondos
   SET fondos_reservados = fondos_reservados + :monto,
       saldo_disponible  = saldo_disponible  - :monto
 WHERE inversionista_id = :inversionistaId;
```

### 5d. Contratos de API

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| `GET` | `/api/comisionista/propuestas/aprobadas` | `COMISIONISTA` | Lista propuestas con estado `APROBADA` del comisionista autenticado |
| `POST` | `/api/comisionista/propuestas/{propuestaId}/firmar-enviar` | `COMISIONISTA` | Firma y envía al mercado la propuesta indicada |

**Respuesta GET 200:**
```json
[
  {
    "id": 42,
    "estado": "APROBADA",
    "simbolo": "AAPL",
    "tipoOrden": "MARKET",
    "lado": "BUY",
    "cantidad": 10,
    "montoBase": 925.00,
    "montoComision": 18.50,
    "clienteNombre": "Ana García"
  }
]
```

**Respuesta POST 200:**
```json
{
  "id": 42,
  "estado": "ENVIADA",
  "firmadaEn": "2026-05-24T15:30:00",
  "montoBase": 925.00,
  "montoComision": 18.50,
  "alpacaOrderId": "abc-123"
}
```

---

## 6. Grafo de dependencias entre tareas

```
T1.1 (verificar/ajustar PropuestaOrden)
    └─► T1.2 (PropuestaOrdenRepository — nuevos métodos)
            └─► T2.1 (OrdenService.firmarYEnviarPropuesta)
                    ├─► T2.2 (ComisionistaController — endpoints)
                    └─► T3.1 (tests unitarios OrdenService)
                                └─► T3.2 (tests integración endpoints)
                                            └─► T4.1 (frontend — sección propuestas aprobadas)
                                                        └─► T4.2 (frontend — modal de confirmación)
                                                                    └─► T5.1 (DoD e2e)
```

---

## 7. Estrategia de tests

| Tipo | Herramienta | Qué prueba |
|---|---|---|
| Unitario — `OrdenService` | JUnit 5 + Mockito | `firmarYEnviarPropuesta_propuestaAprobada_mercadoAbierto_creaOrdenEnviada`, `firmarYEnviarPropuesta_mercadoCerrado_creaOrdenEnCola`, `firmarYEnviarPropuesta_fondosInsuficientes_lanza402`, `firmarYEnviarPropuesta_propuestaOtroComisionista_lanza403`, `firmarYEnviarPropuesta_estadoIncorrecto_lanza400`, `firmarYEnviarPropuesta_alpacaFalla_liberaFondosYLanza502` |
| Integración — `ComisionistaController` | `@SpringBootTest` + MockMvc | `GET /api/comisionista/propuestas/aprobadas` retorna 200 con lista correcta; `POST /firmar-enviar` con JWT válido retorna 200; sin JWT retorna 401; JWT de otro rol retorna 403. |
| BD | Consulta SQL directa | Tras firma: `propuesta_orden.firmada_en IS NOT NULL`, `propuesta_orden.orden_id IS NOT NULL`, `cuenta_fondos.fondos_reservados` incrementó en el monto correcto. |

---

## 8. Trazabilidad criterios de aceptación → artefacto

| Escenario/Criterio | Test o mecanismo |
|---|---|
| Propuesta APROBADA + mercado abierto → estado ENVIADA | `firmarYEnviarPropuesta_propuestaAprobada_mercadoAbierto_creaOrdenEnviada` |
| Mercado cerrado → estado EN_COLA | `firmarYEnviarPropuesta_mercadoCerrado_creaOrdenEnCola` |
| Fondos insuficientes → 402, sin modificar la orden | `firmarYEnviarPropuesta_fondosInsuficientes_lanza402` |
| Propuesta de otro comisionista → 403 | `firmarYEnviarPropuesta_propuestaOtroComisionista_lanza403` |
| Estado ≠ APROBADA → 400 | `firmarYEnviarPropuesta_estadoIncorrecto_lanza400` |
| Propuesta no encontrada → 404 | Test de integración con `propuestaId = 9999` |
| Alpaca falla → 502, fondos liberados | `firmarYEnviarPropuesta_alpacaFalla_liberaFondosYLanza502` |
| Auditoría PROPUESTA_ORDEN_FIRMADA + evento de estado | Mock de `IAuditLog.registrar(...)` verificado con `verify(auditLog, times(2)).registrar(...)` |
| EC-12 Audit Trail | `AuditLogService` recibe al menos dos eventos distintos al firmar |

---

## 9. Preguntas abiertas

| # | Pregunta | Propuesta |
|---|---|---|
| Q1 | El SPEC usa `IAlpacaAdapter.enviarOrden(OrdenAlpacaDTO)` pero `ARQUITECTURA.md` declara `IIntegracionAlpaca.enviarOrden(CrearOrdenRequestDTO, String alpacaAccountId)`. ¿Cuál es la firma real? | Usar `IIntegracionAlpaca` con la firma de `ARQUITECTURA.md`. Si el SPEC está desactualizado, actualizar el SPEC. Requiere confirmación humana antes de implementar. |
| Q2 | El SPEC nombra los eventos de auditoría como `ORDEN_ENVIADA_ALPACA` y `ORDEN_EN_COLA`, pero `ARQUITECTURA.md` lista `ORDEN_EJECUTADA` y `ORDEN_ENCOLADA`. ¿Cuáles son los nombres canónicos en el enum `TipoEvento`? | Usar los nombres de `ARQUITECTURA.md` (`ORDEN_EJECUTADA`, `ORDEN_ENCOLADA`). Si faltan en el enum, agregarlos. Requiere confirmación humana. |
| Q3 | Los eventos `ORDEN_RECHAZADA_FONDOS` y `ORDEN_FALLO_ALPACA` no aparecen en `TipoEvento` de `ARQUITECTURA.md`. ¿Se deben agregar al enum? | Se propone agregar ambos eventos al enum `TipoEvento` en el módulo de trazabilidad. Requiere confirmación humana. |
| Q4 | El SPEC indica estado `EJECUTADA` para símbolos globales (no US) cuando el mercado está abierto. ¿La ejecución es real contra Alpaca o simulada? Si es simulada, ¿cómo se distingue de una ejecución real en el historial? | Información no disponible en SPEC — el SPEC dice "simula ejecución". Requiere aclaración sobre si hay un flag `simulada BOOLEAN` en `orden` o si el estado es suficiente. |
| Q5 | Al obtener el `alpacaAccountId` del cliente para llamar a `IIntegracionAlpaca.enviarOrden(...)`, ¿se obtiene desde `IConsultaInversionista.obtenerAlpacaAccountId(inversionistaId)` o directamente desde la entidad? | Usar `IConsultaInversionista` conforme a las reglas de comunicación entre módulos. Confirmar disponibilidad del método. |

---

## 10. Definition of Done

- [ ] `GET /api/comisionista/propuestas/aprobadas` retorna 200 con lista de propuestas en estado `APROBADA` del comisionista autenticado.
- [ ] `POST /api/comisionista/propuestas/{id}/firmar-enviar` recalcula `montoBase` con precio actual del caché.
- [ ] Fondos del cliente reservados atómicamente antes de enviar; si Alpaca falla, se liberan por rollback.
- [ ] Mercado abierto + símbolo US → estado `ENVIADA` con `alpacaOrderId` poblado.
- [ ] Mercado cerrado → estado `EN_COLA`.
- [ ] Fondos insuficientes del cliente → 402 sin modificar la propuesta ni la orden.
- [ ] Propuesta de otro comisionista → 403.
- [ ] Propuesta no en estado `APROBADA` → 400.
- [ ] Eventos de auditoría registrados: `PROPUESTA_ORDEN_FIRMADA` + evento de estado final.
- [ ] Tests unitarios de `OrdenService` pasan (mínimo 6 casos).
- [ ] Tests de integración de `ComisionistaController` pasan.
- [ ] Frontend muestra lista de propuestas aprobadas y modal de confirmación funcional.
- [ ] `docs/PROGRESO.md` marcado con ✅ para HU-32.
