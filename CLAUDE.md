# Acciones ElBosque — Plataforma Web de Day Trading

> **Este archivo es la memoria persistente y el índice del proyecto.** Lee este archivo al inicio de cada sesión. Para detalles que no estén aquí, consulta los archivos en `docs/`.

---

## Índice de documentación

| Archivo | Cuándo consultarlo |
|---|---|
| `docs/REQUERIMIENTOS.md` | Antes de implementar una feature, para validar qué RF/RNF cubre. |
| `docs/ESCENARIOS_CALIDAD.md` | Para conocer los 23 escenarios de calidad (EC-01 a EC-23) y las tácticas asociadas. **Cada vez que un cambio toque seguridad, rendimiento, disponibilidad o trazabilidad, revisar acá.** |
| `docs/HISTORIAS_USUARIO.md` | Para el detalle de cada HU (criterios de aceptación, prioridad, épica). |
| `docs/ARQUITECTURA.md` | Para la estructura de los 6 servicios SOA, interfaces expuestas, despliegue, y estructura de carpetas. **Consultar antes de crear cualquier archivo nuevo.** |
| `docs/CONVENCIONES.md` | Para reglas de naming, estilo Java, Angular, manejo de errores, secretos, y mejoras vs. el proyecto Malwatcher previo. **Consultar antes de escribir código.** |
| `docs/PROGRESO.md` | Para saber qué quedó hecho, qué está en progreso, y qué sigue. **Actualizar siempre al terminar una historia.** |

---

## Contexto del proyecto en 5 líneas

- **Producto:** broker digital de day trading (compra/venta de acciones en mercados internacionales).
- **Cliente académico:** Universidad El Bosque, Ingeniería de Software 2, mayo 2026.
- **Stack:** Java 17 + Spring Boot 3 (Maven, war), PostgreSQL, JWT + BCrypt, Jakarta Mail / Angular (TypeScript) / sin Docker.
- **Arquitectura:** SOA con 6 servicios desplegados de forma consolidada en un mismo runtime Spring Boot.
- **Integraciones externas:** Alpaca (órdenes), Alpha Vantage (datos), Stripe (premium), SMTP/SMS/WhatsApp (notificaciones).

---

## Los 6 servicios

| Servicio | Responsabilidad core |
|---|---|
| **Autenticación** | Registro inversionista, login, MFA, recuperación contraseña, monitor de intentos. |
| **Órdenes** | Market/Limit/Stop Loss/Take Profit, fondos, holdings, comisiones, cola fuera de horario, flujo comisionistas. |
| **Mercado** | Dashboard, caché de precios, sincronización con APIs externas, horarios de mercados. |
| **Administración** | Parámetros (comisiones, horarios, mercados), ciclo de vida de cuentas, suscripción premium. |
| **Integración** | Adaptadores Alpaca/Stripe/Alpha Vantage, despachador notificaciones, orquestadores, manejador excepciones. |
| **Trazabilidad** | Registro inmutable y asíncrono de eventos (consumido por todos vía `IAuditLog`). |

> Ver `docs/ARQUITECTURA.md` para interfaces, dependencias y diagramas.

---

## Reglas duras (NO negociables)

1. **Organización por servicio, NO por capas.** Cada servicio replica internamente `controller/service/repository/model/dto/interfaces`. La raíz del proyecto agrupa por servicio.
2. **Comunicación entre servicios solo por interfaz `I...`.** Un servicio nunca importa clases internas de otro.
3. **Trazabilidad transversal.** Cualquier evento auditable se registra vía `IAuditLog`. No se reimplementa lógica de logs por servicio.
4. **Solo el rol `INVERSIONISTA` se auto-registra.** El resto de roles los crea un Administrador autenticado.
5. **Contraseñas con BCrypt.** Texto plano está prohibido (mejora vs. proyecto Malwatcher previo).
6. **JWT firmado, claim de rol, expiración 1h.** Tokens revocados van a tabla en BD (necesario para HU-5).
7. **Códigos de verificación van en BD con TTL, no en `ConcurrentHashMap` en memoria.** (mejora vs. Malwatcher).
8. **Secretos en `application.properties` o variables de entorno, NUNCA hardcodeados.** (mejora vs. Malwatcher).
9. **Inyección por constructor, no por campo.** (mejora vs. Malwatcher).
10. **CORS configurado en `shared/config/CorsConfig`, no por controller con `@CrossOrigin`.** (mejora vs. Malwatcher).
11. **DTOs siempre en endpoints.** Nunca exponer entidades JPA con datos sensibles al frontend.
12. **`@Transactional` va en services, no en controllers.**
13. **Sin Docker en este proyecto.** Desarrollo local con PostgreSQL nativo.
14. **Bloqueo de cuenta tras 5 intentos fallidos** por 15 minutos, notificación al titular (EC-09).
15. **MFA obligatorio para Comisionista, Administrador, Responsable Legal e Inversionista Premium. Opcional para Inversionista regular.**

> Ver `docs/CONVENCIONES.md` para todo el detalle de estilo y `docs/ESCENARIOS_CALIDAD.md` para las tácticas que sustentan estas reglas.

---

## Reglas de trabajo para Claude Code

1. **Antes de escribir código nuevo:** lee la sección relevante de `docs/ARQUITECTURA.md` y `docs/CONVENCIONES.md`. Si vas a tocar seguridad, lee también `docs/ESCENARIOS_CALIDAD.md`.
2. **Antes de implementar una historia:** lee la HU completa en `docs/HISTORIAS_USUARIO.md` (criterios de aceptación incluidos).
3. **Trabaja en pasos pequeños y verificables.** Compila después de cada bloque significativo.
4. **No hardcodees secretos jamás.** Si necesitas una API key, una contraseña o una URL externa, declárala en `application.properties` con placeholder y avisa al desarrollador.
5. **No mezcles servicios.** Si trabajas en `autenticacion/`, no toques `ordenes/`. Si necesitas algo de otro servicio, hazlo a través de su interfaz `I...`.
6. **Cada vez que termines una historia, actualiza `docs/PROGRESO.md`** marcando el checkbox y agregando una nota breve.
7. **Si introduces una decisión técnica nueva** (cambio de librería, ajuste arquitectónico, nuevo patrón), documéntala en `docs/CONVENCIONES.md` o `docs/ARQUITECTURA.md` según corresponda.
8. **Ante la duda entre dos enfoques**, prefiere el que se parezca al estilo del proyecto Malwatcher previo (descrito en `docs/CONVENCIONES.md`), excepto donde dicho proyecto tenga malas prácticas explícitamente listadas como "mejoradas".
9. **Auditar siempre eventos sensibles** vía `IAuditLog` (login exitoso/fallido, registro, cambio contraseña, ejecución de orden, cambio de parámetro administrativo, etc.).

---

## Glosario rápido

| Sigla | Significado |
|---|---|
| HU | Historia de Usuario |
| CU | Caso de Uso |
| RF | Requerimiento Funcional |
| RNF | Requerimiento No Funcional |
| EC | Escenario de Calidad |
| MVP | Producto Mínimo Viable (42 historias Must Have) |
| MFA | Multi-Factor Authentication |
| SOA | Service-Oriented Architecture |

---

*Última actualización: ver `docs/PROGRESO.md` para fecha y estado actual del sprint.*
