# SPEC — Consulta de saldo, fondos reservados y comisiones

---

## Ficha de la historia

| Campo | Valor |
|---|---|
| ID | HU-16 |
| Sprint | 2 |
| Prioridad MoSCoW | Must Have |
| Estado | Completada |
| Épica | Órdenes / Portafolio |
| CU asociado | CU-16 |
| Autor | Juan Diego Triana Mejia |
| Creada | 2026-05-20 |
| Última revisión | 2026-05-24 |
| Versión de esta spec | 1.0 |

**Trazabilidad:**

| Tipo | ID | Descripción |
|---|---|---|
| Requerimiento funcional | RF-15 | Consulta de saldo disponible, fondos reservados y comisiones pagadas |
| Historia que precede a esta | HU-17..20 | Las órdenes de compra reservan y consumen fondos |

---

## Historia de usuario

**Como** inversionista autenticado,
**quiero** consultar mi saldo disponible, los fondos que tengo reservados en órdenes y el historial de comisiones pagadas,
**para** saber con cuánto capital puedo operar y cuánto he pagado en comisiones.

---

## Actores y precondiciones

### Actores

| Actor | Rol en el sistema | Participación en este flujo |
|---|---|---|
| Inversionista autenticado | `INVERSIONISTA` | Iniciador |
| `PortafolioService` | Módulo `ordenes` | Consulta `cuenta_fondos` y comisiones pagadas |

### Precondiciones

- JWT válido en cabecera `Authorization: Bearer`.
- Existe `cuenta_fondos` para el usuario (creada al activar la cuenta).

---

## Flujo principal

1. Frontend llama `GET /api/portafolio/saldo` con JWT.

**Backend — `PortafolioService.saldo(correo)`:**

2. Carga `cuenta_fondos` del usuario (saldo disponible, fondos reservados).
3. Calcula o consulta total de comisiones pagadas (sum de `orden.comision` para órdenes EJECUTADAS — columna real en entidad `Orden`).
4. Responde `200 OK` con `SaldoDTO`.

---

## Contrato de API

### Endpoint 1 — `GET /api/portafolio/saldo`

```yaml
GET /api/portafolio/saldo:
  summary: Retorna saldo disponible, fondos reservados y comisiones del inversionista
  security:
    - bearerAuth: []
  responses:
    '200':
      description: Estado financiero del inversionista
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/SaldoDTO'
          example:
            saldoDisponible: 5000.00
            fondosReservados: 1800.00
            totalComisionesPagadas: 36.00
    '401':
      description: No autenticado
    '500':
      description: Error interno del servidor

components:
  schemas:
    SaldoDTO:
      type: object
      properties:
        saldoDisponible:
          type: number
          format: double
          description: "Fondos listos para usar en nuevas órdenes"
        fondosReservados:
          type: number
          format: double
          description: "Fondos bloqueados por órdenes pendientes/enviadas"
        totalComisionesPagadas:
          type: number
          format: double
          description: "Suma de comisiones pagadas en órdenes ejecutadas"
```

### Endpoint 2 — `POST /api/portafolio/depositar` (sandbox)

```yaml
POST /api/portafolio/depositar:
  summary: Deposita fondos simulados (solo para entorno de desarrollo/sandbox)
  security:
    - bearerAuth: []
  parameters:
    - name: monto
      in: query
      required: true
      schema:
        type: number
        format: double
      example: 10000.00
  responses:
    '200':
      description: Fondos depositados exitosamente
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'
          example:
            mensaje: "Depósito exitoso: $10000.00"
    '401':
      description: No autenticado
```

> **Nota:** `POST /api/portafolio/depositar` es un endpoint de sandbox para pruebas. No debe estar expuesto en producción.

### Endpoint 3 — `POST /api/portafolio/sincronizar`

```yaml
POST /api/portafolio/sincronizar:
  summary: Sincroniza el saldo local con la cuenta Alpaca del inversionista
  security:
    - bearerAuth: []
  responses:
    '200':
      description: Saldo sincronizado con Alpaca
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/RespuestaDTO'
    '401':
      description: No autenticado
```

---

## Modelo de datos

### Tabla `cuenta_fondos` (módulo `ordenes`)

```sql
CREATE TABLE cuenta_fondos (
    inversionista_id  BIGINT PRIMARY KEY REFERENCES inversionista(id),  -- PK compartida, no auto-generada
    saldo_disponible  DECIMAL(15,4) NOT NULL DEFAULT 0,
    fondos_reservados DECIMAL(15,4) NOT NULL DEFAULT 0
);
-- Nota: no hay columna id auto-increment ni usuario_id separada.
-- inversionista_id ES la PK (shared PK con inversionista.id = usuario.id).
```

---

## Módulos y arquitectura

### Módulos involucrados

| Módulo | Rol | Componentes específicos |
|---|---|---|
| `ordenes` | Coordinador del flujo | `PortafolioController`, `PortafolioService` |
| `integracion` | Sincronización con Alpaca | `AlpacaAdapter` (en `sincronizar`) |

---

## Criterios de verificación

### Escenarios de aceptación (Gherkin)

```gherkin
Funcionalidad: Consulta de saldo y comisiones

  Escenario: Consulta de saldo con fondos disponibles
    Dado que "ana@test.com" tiene saldo_disponible=5000 y fondos_reservados=0
    Cuando se envía GET /api/portafolio/saldo con JWT
    Entonces el sistema responde 200 OK
    Y saldoDisponible es 5000
    Y fondosReservados es 0

  Escenario: Depósito sandbox
    Cuando se envía POST /api/portafolio/depositar?monto=1000 con JWT
    Entonces el sistema responde 200 OK
    Y cuenta_fondos.saldo_disponible aumenta en 1000

  Escenario: Sin JWT — 401
    Cuando se envía GET /api/portafolio/saldo sin Authorization
    Entonces el sistema responde 401 Unauthorized
```

---

## Riesgos

| # | Riesgo | P | I | Mitigación | Test que lo cubre |
|---|---|:-:|:-:|---|---|
| R1 | Endpoint `depositar` expuesto en producción permite manipular saldos arbitrariamente | Alta (si se despliega sin control) | Crítico | Proteger con flag de ambiente (`spring.profiles.active=prod`) que deshabilite el endpoint | Manual: verificar que `depositar` no funciona en perfil de producción |

---

## Definición de terminado

- [x] `GET /api/portafolio/saldo` retorna saldo, fondos reservados y comisiones.
- [x] `POST /api/portafolio/depositar` funciona en entorno de sandbox.
- [x] Sin JWT responde 401.
- [x] `docs/PROGRESO.md` marcado con ✅ para HU-16.

---

## Historial de cambios

| Versión | Fecha | Descripción | Razón |
|---|---|---|---|
| 1.0 | 2026-05-24 | Refactorización a estructura SDD del proyecto. | Unificación de todos los SPEC.md bajo plantilla canónica SDD del proyecto |
| 1.1 | 2026-05-26 | Auditoría SDD: `orden.monto_comision` → `orden.comision` (nombre real de la columna/campo en entidad `Orden`). | Columna real se llama `comision`, no `monto_comision`. |
