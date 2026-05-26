# plan.md — RF-08 Verificación de Horario de Mercado
> Derivado de `docs/RF-08-verificar-horario-mercado/SPEC.md`.
> Estado: PENDIENTE DE APROBACIÓN HUMANA.

---

## 1. Qué construye esta historia

Implementa la función transversal `IVerificacionMercado.esMercadoAbierto()` del **módulo Mercado**, consumida por el módulo de Órdenes antes de enviar o encolar cualquier orden. La lógica evalúa la zona horaria y la ventana horaria de apertura/cierre de cada mercado (NYSE/NASDAQ, TSE, LSE, ASX). Cuando el flag `app.mercado.sandbox-siempre-abierto=true` está activo, retorna siempre abierto (para pruebas). También expone el diagnóstico vía `GET /api/mercado/horario/{mercado}`.

---

## 2. Decisiones técnicas

| # | Decisión | Justificación |
|---|---|---|
| 1 | Las zonas horarias se usan con `ZoneId` de Java y `ZonedDateTime` | Garantiza corrección ante horario de verano/invierno y simplifica comparaciones de ventana horaria. |
| 2 | La lógica de horarios está en `MercadoService`, expuesta por `IVerificacionMercado` | El módulo de Órdenes consume la interfaz, no importa `MercadoService` directamente (ARQUITECTURA.md §5). |
| 3 | La propiedad `app.mercado.sandbox-siempre-abierto` en `application.properties` fuerza el retorno `true` | Permite pruebas sin depender del horario real. Consistente con la táctica de Sandbox del proyecto (EC-22). |
| 4 | Mercado desconocido o nulo retorna `false` (cerrado por defecto) | Política de seguridad: ante la duda, no ejecutar la orden. |
| 5 | La implementación actual **no consulta `feriado_mercado` ni `mercado_config` en BD** | El SPEC lo documenta explícitamente como limitación. Ver §4 deuda técnica y §9. |
| 6 | El endpoint diagnóstico `GET /api/mercado/horario/{mercado}` está protegido por JWT | SPEC indica "protegido por JWT según configuración general". |

---

## 3. Cambios de dependencias

Ningún cambio en `pom.xml`. La propiedad `app.mercado.sandbox-siempre-abierto` debe estar declarada en `application.properties`.

---

## 4. Deuda técnica o hallazgos previos

| Hallazgo | Acción |
|---|---|
| La implementación actual no consulta feriados de mercado (`feriado_mercado`) | El SPEC lo documenta como limitación conocida. Para cumplir EC-19 completamente, se debe agregar consulta a `IAdministracion` o `FeriadoMercadoRepository` (a través de la interfaz correcta). Exponer en §9. |
| La implementación actual usa horarios hardcodeados en código, no en `mercado_config` | Incumple EC-18/EC-19 (configuración sin redespliegue). Es deuda técnica explícita del SPEC. La solución completa requeriría leer horarios desde BD. Documentado en §9. |

---

## 5. Arquitectura de la solución

### 5a. Mapeo de componentes (backend)

| Capa | Componente | Módulo | Responsabilidad |
|---|---|---|---|
| Interface | `IVerificacionMercado` | `mercado` | `esMercadoAbierto(String mercado)`, `detectarMercado(String simbolo)`. Consumida por `OrdenService`. |
| Service | `MercadoService` | `mercado` | Implementa `IVerificacionMercado`. Contiene la lógica de zonas horarias y ventanas de apertura. |
| Controller | `MercadoController` | `mercado` | Expone `GET /api/mercado/horario/{mercado}` con respuesta `{ "mercado": "NYSE", "abierto": true }`. |

**Lógica de `esMercadoAbierto`:**

```
Dado un String "mercado":
  NYSE / NASDAQ / US:
    ZoneId = "America/New_York"
    Lun-Vie, 09:30 - 16:00
  TSE:
    ZoneId = "Asia/Tokyo"
    Lun-Vie, 09:00 - 15:00
  LSE:
    ZoneId = "Europe/London"
    Lun-Vie, 08:00 - 16:30
  ASX:
    ZoneId = "Australia/Sydney"
    Lun-Vie, 10:00 - 16:00
  Otro / null: retorna false
  Si sandbox-siempre-abierto=true: retorna true sin evaluar horario
```

**Lógica de `detectarMercado`:**
```
Detecta mercado por sufijo del símbolo:
  ".T" → TSE
  ".L" → LSE
  ".AX" → ASX
  Sin sufijo → NYSE/NASDAQ (US)
```

### 5b. Mapeo de componentes (frontend)

| Componente | Archivo | Responsabilidad |
|---|---|---|
| `DashboardComponent` | `dashboard/dashboard.component.ts` | Muestra `mercadoAbierto` en cotizaciones y en el resumen de previsualización de orden. |

### 5c. Modelo de datos

No se lee ni escribe en BD en la implementación actual. La lógica es por código más propiedad sandbox.

Tablas que se deberían consultar en la versión completa (deuda técnica):
- `mercado_config` (horarios configurados por admin).
- `feriado_mercado` (días no operativos por mercado).

### 5d. Contratos de API

```
GET /api/mercado/horario/{mercado}
Authorization: Bearer <JWT>
Path param: mercado = NYSE | NASDAQ | TSE | LSE | ASX | US

Response 200:
{
  "mercado": "NYSE",
  "abierto": true,
  "zonaHoraria": "America/New_York",
  "horaActualMercado": "2026-05-25T10:30:00-04:00"
}

Response 400: mercado desconocido o nulo.
Response 401: sin JWT.
```

---

## 6. Grafo de dependencias entre tareas

```
T1.1 (verificar application.properties sandbox)
    └─► T1.2 (verificar/implementar MercadoService.esMercadoAbierto)
            └─► T1.3 (verificar/implementar MercadoService.detectarMercado)
                    └─► T2.1 (test unitario MercadoService)
                            └─► T2.2 (verificar OrdenService consume IVerificacionMercado)
                                    └─► T3.1 (test integración endpoint horario)
                                            └─► T4.1 (DoD + deuda técnica documentada)
```

---

## 7. Estrategia de tests

- **Unitario `MercadoService`:**
  - `esMercadoAbierto_nyseEnHorario_retornaTrue`: simular `ZonedDateTime` dentro del horario.
  - `esMercadoAbierto_nyseFueraHorario_retornaFalse`.
  - `esMercadoAbierto_mercadoDesconocido_retornaFalse`.
  - `esMercadoAbierto_sandboxActivo_retornaSiempreTrue`.
  - `detectarMercado_simboloConSufixoT_retornaTSE`.
  - `detectarMercado_simboloSinSufijo_retornaUS`.
- **Integración `MockMvc`:** `GET /api/mercado/horario/NYSE` → 200 con JSON correcto; `GET /api/mercado/horario/DESCONOCIDO` → 400/false.

---

## 8. Trazabilidad criterios de aceptación → artefacto

| Criterio (SPEC) | Test o mecanismo |
|---|---|
| Detecta horario US (NYSE/NASDAQ) | `esMercadoAbierto_nyseEnHorario_retornaTrue` y `esMercadoAbierto_nyseEnHorario_retornaFalse` |
| Detecta horario TSE/LSE/ASX | Tests con cada zona horaria. |
| Permite sandbox siempre abierto | `esMercadoAbierto_sandboxActivo_retornaSiempreTrue` |
| Órdenes fuera de horario se encolan | Test de `OrdenService` que verifica encolamiento cuando `IVerificacionMercado.esMercadoAbierto` retorna false. |

---

## 9. Preguntas abiertas

| # | Pregunta | Propuesta |
|---|---|---|
| 1 | ¿Cuándo se implementará la consulta de `feriado_mercado` en BD para que `esMercadoAbierto` retorne false en feriados? | Propuesta: sprint siguiente. Requiere agregar llamada a `IAdministracion.obtenerFeriados(mercado, fecha)` o similar en `MercadoService`. |
| 2 | ¿Cuándo se migrarán los horarios hardcodeados a `mercado_config` en BD (cumplimiento total de EC-19)? | Propuesta: sprint siguiente. Requiere `IAdministracion.listarMercados()` y leer horarios desde las entidades `MercadoConfig`. |
| 3 | ¿El endpoint `GET /api/mercado/horario/{mercado}` debe ser público o requerir JWT? | El SPEC dice "protegido por JWT según configuración general". Confirmar con el equipo si el frontend lo necesita antes del login. |

---

## 10. Definition of Done

- [ ] `MercadoService.esMercadoAbierto()` evalúa correctamente NYSE/NASDAQ, TSE, LSE y ASX con sus zonas horarias.
- [ ] `app.mercado.sandbox-siempre-abierto=true` en `application.properties` fuerza retorno `true`.
- [ ] Mercado desconocido o nulo retorna `false`.
- [ ] `OrdenService` consume `IVerificacionMercado.esMercadoAbierto()` para decidir si encolar.
- [ ] `GET /api/mercado/horario/{mercado}` retorna estado actual del mercado solicitado.
- [ ] Tests unitarios de `MercadoService` en verde.
- [ ] Test de integración MockMvc en verde.
- [ ] Deuda técnica de feriados y horarios en BD documentada en §9 y en `docs/PROGRESO.md`.
- [ ] `docs/PROGRESO.md` actualizado.
