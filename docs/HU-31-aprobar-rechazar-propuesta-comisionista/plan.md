# Plan de implementación — HU-31: Aprobación o rechazo de propuesta del comisionista

## Contexto

HU-31 es el segundo paso del flujo de propuesta. Tras la creación por el comisionista (HU-30), el inversionista recibe las propuestas y decide aprobarlas o rechazarlas. Esta historia completa el loop de consentimiento: ninguna operación se ejecuta en nombre del inversionista sin su aprobación explícita. El siguiente paso (HU-32) es la firma y envío al mercado por parte del comisionista.

Estado actual: **Completada**.

---

## Objetivo

Exponer tres endpoints bajo `/api/propuestas` para que el inversionista:
1. Consulte sus propuestas pendientes.
2. Apruebe una propuesta específica.
3. Rechace una propuesta específica.

---

## Decisiones de diseño

| Decisión | Elección | Motivo |
|---|---|---|
| Controlador separado `PropuestaController` | Distinto de `ComisionistaController` | El actor es el inversionista, no el comisionista; separar mejora la claridad y el control de acceso por rol |
| Estado como guarda de idempotencia | Solo se puede aprobar/rechazar si `estado == PENDIENTE_APROBACION` | Evita doble aprobación por clic accidental o condición de carrera |
| `comentarioInversionista` opcional | El body del request es `DecisionPropuestaDTO{comentario?}` | El inversionista puede dejar comentario, pero no es obligatorio |
| Sin reserva de fondos | Aprobar no toca `CuentaFondos` | La reserva ocurre en HU-32 cuando el comisionista firma y crea la orden real |
| Sin interacción con Alpaca | Este paso es solo administrativo | Consistente con la filosofía de dos etapas: decisión (HU-31) vs. ejecución (HU-32) |
| Auditoría | `PROPUESTA_ORDEN_APROBADA` / `PROPUESTA_ORDEN_RECHAZADA` | EC-12: cada decisión del inversionista queda trazada |

---

## Componentes involucrados

| Capa | Clase | Responsabilidad |
|---|---|---|
| Controller | `PropuestaController` | Valida rol INVERSIONISTA, resuelve ID, delega a `IOrden` |
| Interface | `IOrden.obtenerPropuestasPendientesInversionista()` | Lista propuestas con estado PENDIENTE_APROBACION |
| Interface | `IOrden.aprobarPropuesta()` | Cambia estado a APROBADA |
| Interface | `IOrden.rechazarPropuesta()` | Cambia estado a RECHAZADA |
| Service | `OrdenService` | Implementa la lógica de cambio de estado con validaciones |
| Trazabilidad | `IAuditLog` | Registra la decisión del inversionista |

---

## Flujo de datos — aprobación

```
Inversionista
  → POST /api/propuestas/{propuestaId}/aprobar
    Body: DecisionPropuestaDTO{comentario?} (opcional)
    JWT: INVERSIONISTA
      → PropuestaController.aprobar(correo, propuestaId, dto)
          → resolverInversionista(correo)
          → IOrden.aprobarPropuesta(propuestaId, inversionistaId, comentario)
              → Buscar PropuestaOrden por propuestaId  [404 si no existe]
              → Validar propuesta.inversionistaId == inversionistaId  [403 si no]
              → Validar propuesta.estado == PENDIENTE_APROBACION  [400 si no]
              → Actualizar: estado=APROBADA, aprobadaEn=now(), comentarioInversionista
              → IAuditLog.registrar(PROPUESTA_ORDEN_APROBADA, correo, detalle)
              ← OrdenDTO
      ← 200 OK con RespuestaDTO{data: OrdenDTO}
```

---

## Escenarios de calidad materializados

| EC | Táctica | Materialización |
|---|---|---|
| EC-12 | Audit Trail | PROPUESTA_ORDEN_APROBADA / RECHAZADA con correo del inversionista y ID de propuesta |
| EC-18 | Encapsulate | Validación `propuesta.inversionistaId == inversionistaId` garantiza que solo el dueño puede decidir |

---

## Consideraciones de calidad

- La guarda `estado == PENDIENTE_APROBACION` es la defensa contra condiciones de carrera y clics dobles; retorna 400 en lugar de silenciar el error para informar al cliente.
- El `comentarioInversionista` se persiste aunque sea null, para no requerir el campo en la respuesta si el usuario no lo proporcionó.
- La lista `/api/propuestas` solo muestra propuestas con estado `PENDIENTE_APROBACION`; las históricas (APROBADA/RECHAZADA) se pueden consultar a través del historial de órdenes.
