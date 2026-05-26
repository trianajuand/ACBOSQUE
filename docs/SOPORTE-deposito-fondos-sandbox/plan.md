# plan.md — SOPORTE Depósito de Fondos Sandbox
> Derivado de `docs/SOPORTE-deposito-fondos-sandbox/SPEC.md`.
> Estado: PENDIENTE DE APROBACIÓN HUMANA.

---

## 1. Qué construye esta historia

Implementa un endpoint de soporte técnico (`POST /api/portafolio/depositar?monto=...`) que permite a cualquier usuario autenticado añadir fondos simulados a su cuenta sin integración real de pagos. Es un mecanismo sandbox para validar flujos de órdenes de compra en entornos de prueba. El SPEC lo clasifica explícitamente como soporte técnico, **no como requerimiento de producción**, y advierte que debe retirarse o restringirse antes del despliegue en producción.

---

## 2. Decisiones técnicas

| # | Decisión | Justificación |
|---|---|---|
| 1 | Endpoint disponible solo en entorno sandbox/desarrollo, no en producción | SPEC indica "debe retirarse o restringirse antes de producción". Se debe agregar guard o anotación condicional. |
| 2 | Validación `@DecimalMin("0.01")` en el parámetro `monto` | SPEC lo indica. Previene depósitos de cero o negativos. |
| 3 | La lógica de depósito en `SaldoService.depositar()` con `@Transactional` | Asegura atomicidad: si falla la actualización de `cuenta_fondos`, no se confirma el cambio. |
| 4 | `SaldoService` obtiene o crea `CuentaFondos` si no existe | Permite primera operación de depósito para usuarios nuevos sin cuenta de fondos inicializada. |
| 5 | Solo se afecta `saldoDisponible`, nunca `fondosReservados` | SPEC lo define explícitamente: el depósito sandbox no toca fondos reservados. |
| 6 | Inyección por constructor en `PortafolioController` y `SaldoService` | Convención obligatoria del proyecto (CONVENCIONES.md §1.2). |

---

## 3. Cambios de dependencias

Ningún cambio en `pom.xml` ni `package.json`.

---

## 4. Deuda técnica o hallazgos previos

| Hallazgo | Acción |
|---|---|
| El endpoint no está restringido a administrador ni a perfil sandbox en el código actual | Antes de producción, agregar: (a) un flag `app.sandbox.deposito-habilitado=true/false` en `application.properties`, o (b) restricción por rol `ADMINISTRADOR` o perfil Spring. |
| No hay auditoría de depósitos sandbox | Para un entorno de pruebas es aceptable. En producción, cualquier movimiento de fondos debería auditarse vía `IAuditLog`. Documentar como decisión diferida. |

---

## 5. Arquitectura de la solución

### 5a. Mapeo de componentes (backend)

| Capa | Componente | Módulo | Responsabilidad |
|---|---|---|---|
| Controller | `PortafolioController` | `ordenes` | Recibe `POST /api/portafolio/depositar?monto={monto}` con JWT. Valida `@DecimalMin("0.01")`. Delega a `SaldoService`. |
| Service | `SaldoService` | `ordenes` | `depositar(Long usuarioId, BigDecimal monto)`: obtiene o crea `CuentaFondos`, suma monto a `saldo_disponible`, persiste con `@Transactional`. |
| Repository | `CuentaFondosRepository` | `ordenes` | `findByInversionistaId(Long)`, `save()`. |
| Model | `CuentaFondos` | `ordenes` | `inversionistaId`, `saldoDisponible`, `fondosReservados`, `actualizadoEn`. |

### 5b. Mapeo de componentes (frontend)

| Componente | Archivo | Responsabilidad |
|---|---|---|
| `DashboardComponent` | `dashboard/dashboard.component.ts` | Formulario de depósito con campo `monto` (mínimo 0.01). Llama `POST /api/portafolio/depositar?monto=...`. Recarga saldo tras respuesta exitosa. |
| `ToastService` | `core/toast.service.ts` | Muestra resultado (éxito o error de validación). |

### 5c. Modelo de datos

Tabla afectada: `cuenta_fondos` (módulo `ordenes`).

```
cuenta_fondos
  inversionista_id   BIGINT PK (FK → inversionista.id)
  saldo_disponible   NUMERIC(15,2) NOT NULL DEFAULT 0
  fondos_reservados  NUMERIC(15,2) NOT NULL DEFAULT 0
  actualizado_en     TIMESTAMPTZ
```

Operación: `saldo_disponible = saldo_disponible + :monto` donde `:monto >= 0.01`.

### 5d. Contratos de API

```
POST /api/portafolio/depositar?monto=100.00
Authorization: Bearer <JWT>

Response 200:
{
  "mensaje": "Depósito exitoso",
  "saldoDisponible": 250.00
}

Response 400: monto < 0.01 o monto no numérico.
Response 401: sin JWT.
```

---

## 6. Grafo de dependencias entre tareas

```
T1.1 (verificar CuentaFondos modelo)
    └─► T1.2 (verificar SaldoService.depositar)
            └─► T1.3 (verificar guard sandbox)
                    └─► T2.1 (test unitario SaldoService)
                            └─► T2.2 (test integración endpoint)
                                    └─► T3.1 (validación frontend)
                                            └─► T3.2 (DoD + documentación sandbox)
```

---

## 7. Estrategia de tests

- **Unitario `SaldoService`:**
  - `depositar_montoValido_incrementaSaldoDisponible`.
  - `depositar_cuentaNoExiste_creaYDepositaFondos`.
  - `depositar_montoInvalido_lanzaValidationException`.
- **Integración `MockMvc`:**
  - `POST /api/portafolio/depositar?monto=100` con JWT → 200 con saldo actualizado.
  - Sin JWT → 401.
  - `monto=0` → 400.
  - `monto=-5` → 400.

---

## 8. Trazabilidad criterios de aceptación → artefacto

| Criterio (SPEC) | Test o mecanismo |
|---|---|
| Monto válido aumenta saldo disponible | `depositar_montoValido_incrementaSaldoDisponible` + test integración |
| Monto inválido es rechazado | `depositar_montoInvalido_lanzaValidationException` |
| La UI refresca saldo | Verificación manual en DashboardComponent tras depósito. |

---

## 9. Preguntas abiertas

| # | Pregunta | Propuesta |
|---|---|---|
| 1 | ¿Cómo se restringirá el endpoint antes de producción? | Propuesta A: flag `app.sandbox.deposito-habilitado=false` en producción, con check al inicio del método. Propuesta B: solo accesible con rol `ADMINISTRADOR`. Decidir antes del Sprint final. |
| 2 | ¿Se debe auditar el depósito sandbox vía `IAuditLog`? | En sandbox no es estrictamente necesario. En producción (si se habilita un flujo real), sí sería obligatorio. |
| 3 | ¿El depósito sandbox debe tener un límite máximo por operación? | No definido en SPEC. Para sandbox podría omitirse; documentar como decisión explícita. |

---

## 10. Definition of Done

- [ ] `POST /api/portafolio/depositar?monto=...` incrementa `saldo_disponible` en `cuenta_fondos`.
- [ ] Monto < 0.01 retorna 400.
- [ ] Sin JWT retorna 401.
- [ ] `CuentaFondos` se crea si no existe.
- [ ] `fondosReservados` no se modifica.
- [ ] Tests unitarios del service en verde.
- [ ] Test de integración MockMvc en verde.
- [ ] `DashboardComponent` refresca saldo tras depósito exitoso.
- [ ] Estrategia de restricción para producción documentada en `application.properties` o en código.
- [ ] `docs/PROGRESO.md` actualizado con nota de soporte sandbox.
