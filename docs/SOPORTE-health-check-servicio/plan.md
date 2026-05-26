# plan.md — SOPORTE Health Check del Servicio
> Derivado de `docs/SOPORTE-health-check-servicio/SPEC.md`.
> Estado: PENDIENTE DE APROBACIÓN HUMANA.

---

## 1. Qué construye esta historia

Implementa un endpoint mínimo de salud `GET /api/health` en `shared/config/HealthController.java` que responde con texto simple sin requerir autenticación. El objetivo es permitir que scripts, monitores externos o el propio equipo de desarrollo verifiquen rápidamente si el backend Spring Boot está respondiendo. El SPEC documenta explícitamente que **no** es un actuator de Spring Boot ni implementa heartbeat con estado de dependencias; es un ping/echo básico. Soporta parcialmente EC-06 (detección de fallo de servicio).

---

## 2. Decisiones técnicas

| # | Decisión | Justificación |
|---|---|---|
| 1 | Endpoint público (sin autenticación) | SPEC lo indica: "disponible públicamente para diagnóstico local". `SecurityConfig` debe incluir `/api/health` en la lista de rutas permitidas sin JWT. |
| 2 | Respuesta de texto simple, sin JSON complejo | SPEC dice "retorna texto simple". Evita dependencias de BD u otros servicios. No expone datos sensibles. |
| 3 | Implementado en `shared/config/HealthController.java` | ARQUITECTURA.md §9.8 lo registra ahí. Es infraestructura transversal, no pertenece a ningún módulo de dominio. |
| 4 | No usa Spring Actuator | El SPEC descarta esta opción explícitamente. Se mantiene liviano sin agregar dependencias de actuator al `pom.xml`. |
| 5 | No consulta BD ni APIs externas | Un health check que depende de BD puede bloquear en momentos de fallo, que es exactamente cuando más se necesita responder. |

---

## 3. Cambios de dependencias

Ningún cambio en `pom.xml` ni `package.json`. Solo verificar que `SecurityConfig` permite `/api/health` sin autenticación.

---

## 4. Deuda técnica o hallazgos previos

| Hallazgo | Acción |
|---|---|
| El ping/echo actual no cumple EC-06 completo (detección de fallo de servicios individuales y notificación al admin) | Documentar en §9 qué se necesitaría para cumplir EC-06: monitoreo de 6 servicios + heartbeat + alerta automática. Puede quedar como deuda técnica. |
| No hay verificación de conectividad a BD, Alpaca ni SMTP | Podría ser útil un endpoint `/api/health/detail` con checks opcionales. Exponer en §9. |

---

## 5. Arquitectura de la solución

### 5a. Mapeo de componentes (backend)

| Capa | Componente | Módulo | Responsabilidad |
|---|---|---|---|
| Controller | `HealthController` | `shared/config` | `GET /api/health` → retorna texto `"OK"` o JSON `{"status": "UP"}`. |
| Config | `SecurityConfig` | `autenticacion/security` | Permite `/api/health` sin JWT en la lista de rutas públicas. |

**Respuesta del endpoint:**

El SPEC dice "retorna texto simple". Se pueden contemplar dos formas:
- Texto plano: `"OK"` con `Content-Type: text/plain`.
- JSON mínimo: `{"status": "UP", "timestamp": "2026-05-25T10:00:00Z"}` con `Content-Type: application/json`.

Se recomienda el JSON mínimo para mayor utilidad en scripts.

### 5b. Mapeo de componentes (frontend)

No hay consumo directo desde Angular. El frontend no llama a `/api/health`.

### 5c. Modelo de datos

No hay consultas a BD.

### 5d. Contratos de API

```
GET /api/health
(sin Authorization header)

Response 200:
{
  "status": "UP",
  "timestamp": "2026-05-25T10:00:00Z"
}

O alternativamente:
"OK"

No hay casos de error definidos: si el backend no responde, la conexión simplemente falla.
```

**Checks incluidos en este endpoint:** ninguno. Es ping/echo básico.

**Checks NO incluidos (deuda técnica):**
- Conectividad a PostgreSQL.
- Conectividad a Alpaca API.
- Conectividad a SMTP.
- Estado de los 6 módulos de dominio.

---

## 6. Grafo de dependencias entre tareas

```
T1.1 (verificar HealthController existente)
    └─► T1.2 (verificar SecurityConfig permite /api/health sin JWT)
            └─► T2.1 (test integración endpoint)
                    └─► T3.1 (documentar deuda técnica EC-06)
                            └─► T3.2 (DoD + PROGRESO.md)
```

---

## 7. Estrategia de tests

- **Integración `MockMvc`:**
  - `GET /api/health` sin JWT → 200 con cuerpo `"OK"` o JSON `{"status": "UP"}`.
  - `GET /api/health` con JWT inválido → 200 (no bloquea con 401 por ser público).
- **Prueba manual:** `curl http://localhost:8080/api/health` retorna 200.

---

## 8. Trazabilidad criterios de aceptación → artefacto

| Criterio (SPEC) | Test o mecanismo |
|---|---|
| Responde sin JWT | Test integración sin `Authorization` header → 200. |
| Retorna texto simple | Verificar cuerpo de respuesta en test. |
| No consulta servicios externos | Inspección del código: sin llamadas a BD, Alpaca ni SMTP en `HealthController`. |

---

## 9. Preguntas abiertas

| # | Pregunta | Propuesta |
|---|---|---|
| 1 | ¿Qué se necesita para cumplir EC-06 completo (detección de fallo de cada servicio y alerta al admin en ≤ 30 seg)? | Propuesta: implementar un `HealthMonitorService` en el módulo de Trazabilidad que ejecute `@Scheduled` cada 10 s, invoque un método de "ping" en cada módulo vía interfaz, y llame `INotificacion.notificarAdmin()` si 3 ciclos consecutivos fallan. Esto es HU-42. |
| 2 | ¿Se debe agregar un endpoint `/api/health/detail` con checks de BD, SMTP, Alpaca? | Podría usar Spring Actuator (`/actuator/health`) si se agrega la dependencia. Decisión del equipo: simple vs. completo. |
| 3 | ¿La respuesta del endpoint debe ser JSON o texto plano? | SPEC dice "texto simple", pero JSON es más útil para monitores automatizados. Propuesta: `{"status": "UP"}`. |

---

## 10. Definition of Done

- [ ] `GET /api/health` responde 200 sin requerir JWT.
- [ ] Respuesta es texto simple o JSON mínimo (`{"status": "UP"}`).
- [ ] No expone datos sensibles.
- [ ] No consulta BD ni servicios externos.
- [ ] `SecurityConfig` incluye `/api/health` en rutas públicas.
- [ ] Test de integración MockMvc en verde.
- [ ] Deuda técnica de EC-06 completo documentada en `docs/PROGRESO.md`.
- [ ] `docs/PROGRESO.md` actualizado.
