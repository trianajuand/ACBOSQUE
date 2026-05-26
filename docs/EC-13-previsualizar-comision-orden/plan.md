# plan.md — EC-13 Previsualización de Comisión Antes de Confirmar Orden
> Derivado de `docs/EC-13-previsualizar-comision-orden/SPEC.md`.
> Estado: PENDIENTE DE APROBACIÓN HUMANA.

---

## 1. Qué construye esta historia

Implementa la táctica **Verify Message Integrity** (EC-13, RNF-15): antes de confirmar cualquier orden, el inversionista recibe un resumen completo con monto base, comisión calculada (2% configurable), split plataforma/comisionista y total a debitar o acreditar. El endpoint `POST /api/ordenes/previsualizar` **no persiste** ningún registro en BD. La confirmación de la orden real solo es posible después de que el usuario vea y acepte este resumen.

---

## 2. Decisiones técnicas

| # | Decisión | Justificación |
|---|---|---|
| 1 | El endpoint de previsualización no escribe en BD | EC-13 lo exige explícitamente. El SPEC lo confirma: "No persiste orden". |
| 2 | El porcentaje de comisión se lee de `IGestorParametros.obtenerPorcentajeComision()` en cada llamada | Parámetros de comisión deben leerse de BD en cada operación, nunca hardcodeados (ARQUITECTURA.md §14, EC-18). |
| 3 | Se detecta si hay comisionista asignado vía `IAsignacionComisionista.tieneComisionistaAsignado()` para calcular el split correcto | El SPEC indica que el split está preparado para Sprint 4. Si `tieneComisionista = false`, la comisión íntegra va a la plataforma. |
| 4 | Se verifica horario de mercado vía `IVerificacionMercado.esMercadoAbierto()` y se incluye advertencia si está cerrado | SPEC indica "mercado cerrado genera advertencia de encolamiento". No bloquea la previsualización. |
| 5 | El precio base se obtiene de la caché de precios (`PrecioCache`) vía `IVerificacionMercado.validarSimboloOperable()` | El precio usa la cotización cacheada para evitar llamadas directas a APIs externas (ARQUITECTURA.md §3.3). |
| 6 | Inyección por constructor en `OrdenService` | Convención obligatoria (CONVENCIONES.md §1.2). |

---

## 3. Cambios de dependencias

Ningún cambio en `pom.xml` ni `package.json`. Reutiliza:
- `OrdenController`, `OrdenService`, `ResumenComisionDTO`, `CrearOrdenRequestDTO`.
- `IGestorParametros` (lee parámetro de comisión de BD).
- `IAsignacionComisionista` (verifica comisionista asignado).
- `IVerificacionMercado` (cotización y horario).

---

## 4. Deuda técnica o hallazgos previos

| Hallazgo | Acción |
|---|---|
| El SPEC indica que el split comisionista está preparado pero no activo en Sprint actual | Implementar con lógica condicional: si `tieneComisionista = false`, split plataforma = 100%. Marcar como TODO para activar cuando el asignador esté funcional. |
| El SPEC dice "usa cotización real/cacheada". El precio actual viene de `PrecioCache`; si el símbolo no tiene caché reciente, puede estar desactualizado | Documentar en §9 la política de frescura del precio mostrado al usuario. |
| El SPEC no menciona auditoría de la previsualización | Correcto: una previsualización no es un evento auditable según CONVENCIONES.md §2.7 (no es una orden ejecutada). No se requiere `IAuditLog` aquí. |

---

## 5. Arquitectura de la solución

### 5a. Mapeo de componentes (backend)

| Capa | Componente | Módulo | Responsabilidad |
|---|---|---|---|
| Controller | `OrdenController` | `ordenes` | Recibe `POST /api/ordenes/previsualizar`, valida JWT, extrae `usuarioId`, delega a `OrdenService`. |
| Service | `OrdenService` | `ordenes` | `previsualizarOrden(CrearOrdenRequestDTO, Long usuarioId)`: obtiene cotización, calcula comisión, detecta mercado, retorna `ResumenComisionDTO`. |
| Interface | `IVerificacionMercado` | `mercado` | `validarSimboloOperable(simbolo)` → precio; `esMercadoAbierto(mercado)` → booleano. |
| Interface | `IGestorParametros` | `administracion` | `obtenerPorcentajeComision()`, `obtenerSplitPlataforma()`, `obtenerSplitComisionista()`. |
| Interface | `IAsignacionComisionista` | `autenticacion` | `tieneComisionistaAsignado(Long inversionistaId)`. |
| DTO entrada | `CrearOrdenRequestDTO` | `ordenes/dto` | `simbolo`, `tipo` (TipoOrden), `lado` (TipoLado), `cantidad`, `precioLimite` (opcional). |
| DTO salida | `ResumenComisionDTO` | `ordenes/dto` | `simbolo`, `precio`, `cantidad`, `montoBase`, `comision`, `splitPlataforma`, `splitComisionista`, `total`, `mercadoAbierto` (boolean), `advertencia` (String nullable). |

### 5b. Mapeo de componentes (frontend)

| Componente | Archivo | Responsabilidad |
|---|---|---|
| `DashboardComponent` | `dashboard/dashboard.component.ts` | Envía `POST /api/ordenes/previsualizar`, guarda `resumen`, muestra panel con desglose financiero, habilita botón "Confirmar Orden". |
| `ApiService` | `core/api.service.ts` | Llama el endpoint de previsualización. |

### 5c. Modelo de datos

No se escribe en ninguna tabla durante la previsualización. Se leen:
- `precio_cache` (vía `IVerificacionMercado`): precio actual del activo.
- `parametro_comision` (vía `IGestorParametros`): porcentaje vigente.
- `asignacion_comisionista` (vía `IAsignacionComisionista`): presencia de comisionista.

### 5d. Contratos de API

```
POST /api/ordenes/previsualizar
Authorization: Bearer <JWT_INVERSIONISTA>
Content-Type: application/json

Request (CrearOrdenRequestDTO):
{
  "simbolo": "AAPL",
  "tipo": "MARKET",
  "lado": "COMPRA",
  "cantidad": 5,
  "precioLimite": null
}

Response 200 (ResumenComisionDTO):
{
  "simbolo": "AAPL",
  "precio": 175.50,
  "cantidad": 5,
  "montoBase": 877.50,
  "comision": 17.55,
  "splitPlataforma": 17.55,
  "splitComisionista": 0.00,
  "totalADebitar": 895.05,
  "totalARecibir": null,
  "mercadoAbierto": true,
  "advertencia": null
}

Response 400 → símbolo inválido, cantidad <= 0, tipo/lado faltante.
Response 401/403 → sin JWT o rol inválido.
Response 404 → símbolo sin precio en caché.
```

---

## 6. Grafo de dependencias entre tareas

```
T1.1 (verificar ResumenComisionDTO)
    └─► T1.2 (verificar CrearOrdenRequestDTO)
            └─► T2.1 (implementar/verificar OrdenService.previsualizarOrden)
                    └─► T2.2 (test unitario OrdenService)
                            └─► T3.1 (test integración endpoint)
                                    └─► T3.2 (validación frontend — panel previsualización)
                                            └─► T4.1 (DoD + PROGRESO.md)
```

---

## 7. Estrategia de tests

- **Unitario `OrdenService`:**
  - `previsualizarOrden_compraMarket_retornaResumenConComision2Pct`: verifica cálculo con precio mockeado.
  - `previsualizarOrden_sinComisionista_splitPlataforma100Pct`.
  - `previsualizarOrden_mercadoCerrado_retornaAdvertencia`.
  - `previsualizarOrden_simboloInvalido_lanzaSimboloInvalidoException`.
  - `previsualizarOrden_cantidadCero_retorna400`.
- **Integración `MockMvc`:** `POST /api/ordenes/previsualizar` con JWT válido → 200 con `ResumenComisionDTO`; sin JWT → 401; símbolo inválido → 400/404.
- **Naming:** `previsualizarOrden_compraMarket_retornaResumenConComision2Pct`.

---

## 8. Trazabilidad criterios de aceptación → artefacto

| Criterio (SPEC) | Test o mecanismo |
|---|---|
| Calcula monto base (precio × cantidad) | `previsualizarOrden_compraMarket_retornaResumenConComision2Pct` |
| Calcula comisión (2% configurable) | Test unitario con `IGestorParametros` mockeado retornando 2%. |
| Informa total a debitar (compra) o recibir (venta) | Test unitario para lado COMPRA y VENTA. |
| Advierte si mercado está cerrado | `previsualizarOrden_mercadoCerrado_retornaAdvertencia` |
| 99% de órdenes muestran info antes de confirmar (EC-13 métrica) | El flujo de UI obliga a llamar previsualizar antes de habilitar el botón confirmar. |
| No crea registros en BD | Verificar que ningún `save()` se invoca en `OrdenService.previsualizarOrden` (test unitario con repositorios mockeados). |

---

## 9. Preguntas abiertas

| # | Pregunta | Propuesta |
|---|---|---|
| 1 | ¿Cuál es la política de frescura del precio mostrado? Si `PrecioCache` tiene un precio de hace 10 segundos, ¿se muestra con advertencia? | Propuesta: mostrar `precio` junto con `actualizadoEn` en el `ResumenComisionDTO` para que el usuario sepa cuán reciente es. |
| 2 | Para órdenes Limit/Stop Loss/Take Profit, ¿el monto base se calcula con el `precioLimite` ingresado o con el precio de mercado actual? | Propuesta: para LIMIT usar `precioLimite`; para MARKET usar precio cacheado. Requiere clarificación para STOP_LOSS y TAKE_PROFIT. |
| 3 | ¿El endpoint de previsualización debe respetar el límite de fondos del inversionista (mostrar advertencia si no alcanza)? | El SPEC actual no lo menciona. Podría ser útil para UX (EC-20), pero añadiría una dependencia a `SaldoService`. |

---

## 10. Definition of Done

- [ ] `POST /api/ordenes/previsualizar` retorna `ResumenComisionDTO` con monto, comisión, split y total.
- [ ] El endpoint no persiste ningún dato en BD.
- [ ] Comisión calculada usando `IGestorParametros` (no constante hardcodeada).
- [ ] Si mercado cerrado → campo `mercadoAbierto: false` y `advertencia` descriptiva.
- [ ] Símbolo inválido retorna 400/404.
- [ ] Sin JWT → 401; rol incorrecto → 403.
- [ ] Tests unitarios del service en verde con cobertura ≥ 80%.
- [ ] Test de integración MockMvc en verde.
- [ ] `DashboardComponent` muestra resumen antes del botón "Confirmar Orden".
- [ ] `docs/PROGRESO.md` actualizado.
