# Plan de implementación — HU-30: Propuesta de orden para cliente (comisionista)

## Contexto

HU-30 es el primer paso del flujo de propuesta de dos etapas: el comisionista propone una orden para un cliente asignado, y el inversionista la aprueba o rechaza (HU-31). Esta historia NO reserva fondos ni envía nada a Alpaca; solo persiste la intención con estado `PENDIENTE_APROBACION` en la tabla `propuesta_orden`.

Estado actual: **Completada**.

---

## Objetivo

Permitir que el comisionista cree una propuesta de orden con recomendación escrita. La propuesta queda registrada en `propuesta_orden` y espera la decisión del inversionista.

---

## Decisiones de diseño

| Decisión | Elección | Motivo |
|---|---|---|
| Tabla separada `propuesta_orden` | No mezclar propuesta con `orden` | Normalización 3NF: la propuesta tiene su propio ciclo de vida distinto al de la orden efectiva |
| Sin reserva de fondos | La propuesta no toca `CuentaFondos` | El inversionista no ha tomado ninguna decisión aún; reservar fondos sin su consentimiento sería incorrecto |
| Estimación de comisión | Se calcula `montoBase × %comision` como valor informativo en la propuesta | El inversionista puede ver el costo estimado antes de decidir |
| `activoId` en lugar de `simbolo` | Usa `activo_id` (FK a tabla `activo`) | Consistente con la normalización 3NF del modelo; el símbolo se puede resolver a través del activo |
| Auditoría | `PROPUESTA_ORDEN_CREADA` vía `IAuditLog` | Toda propuesta queda trazada con el correo del cliente y el comisionista que la creó |

---

## Componentes involucrados

| Capa | Clase | Responsabilidad |
|---|---|---|
| Controller | `ComisionistaController.proponerOrden()` | Recibe la solicitud, valida rol, delega a `IOrden` |
| Interface | `IOrden.crearPropuestaOrden()` | Crea la propuesta en `propuesta_orden` |
| Service | `OrdenService.crearPropuestaOrden()` | Valida asignación, calcula estimado, persiste la propuesta |
| Model | `PropuestaOrden` (tabla `propuesta_orden`) | Entidad JPA con ciclo de vida propio |
| Interface | `IAsignacionComisionista` | Valida que el cliente está asignado antes de crear la propuesta |
| Trazabilidad | `IAuditLog` | Registra `PROPUESTA_ORDEN_CREADA` |

---

## Ciclo de vida de la propuesta

```
[Comisionista crea] → PENDIENTE_APROBACION  (HU-30)
                          ↓              ↓
                  [Inversionista aprueba]  [Inversionista rechaza]  (HU-31)
                       APROBADA              RECHAZADA
                          ↓
              [Comisionista firma y envía]  (HU-32)
                  ENVIADA / EN_COLA / EJECUTADA
```

---

## Flujo de datos

```
Comisionista
  → POST /api/comisionista/clientes/{clienteId}/propuestas
    Body: CrearPropuestaOrdenDTO {activoId, tipoOrden, lado, cantidad, precioLimite?, comentario?}
    JWT: COMISIONISTA
      → ComisionistaController.proponerOrden(correo, clienteId, dto, request)
          → resolverComisionista(correo)
          → IOrden.crearPropuestaOrden(comisionistaId, clienteId, dto, ipOrigen)
              → IAsignacionComisionista.validarClienteAsignado(comisionistaId, clienteId)  [403 si no asignado]
              → Calcular montoBase y comisión estimada
              → Persistir PropuestaOrden{estado=PENDIENTE_APROBACION}
              → IAuditLog.registrar(PROPUESTA_ORDEN_CREADA, correo_cliente, detalle)
              ← OrdenDTO
      ← 200 OK con RespuestaDTO{data: OrdenDTO}
```

---

## Modelo de datos clave

La tabla `propuesta_orden` almacena la propuesta con su ciclo de vida propio. El campo `orden_id` se popula en HU-32 cuando el comisionista firma y crea la orden real. Los campos `aprobada_en`, `rechazada_en`, `firmada_en` rastrean los timestamps de cada transición de estado.

---

## Consideraciones de calidad

- El `comentarioComisionista` es el mecanismo de recomendación; se persiste como `TEXT` para soportar comentarios largos.
- EC-12 (Audit Trail): la propuesta es un evento auditable; el log incluye el correo del cliente para facilitar trazabilidad cruzada.
- EC-18 (Encapsulate): la validación de asignación garantiza que el comisionista solo puede proponer para sus propios clientes.
