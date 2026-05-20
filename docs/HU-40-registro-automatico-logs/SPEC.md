# Historia de Usuario

## Título
Registro automático de eventos auditables.

## Descripción
Como sistema
Quiero registrar eventos críticos de autenticación, perfil, mercado y órdenes
Para mantener trazabilidad operativa y soporte a auditoría.

## Contexto
Implementa parcialmente HU-40 y EC-12. La trazabilidad es un servicio transversal consumido por otros servicios vía `IAuditLog`.

## Flujo funcional
1. Un servicio ejecuta una operación crítica.
2. Invoca `auditLog.registrar(tipo, correoUsuario, detalle)`.
3. `AuditLogService` crea `EventoAuditoria`.
4. Guarda evento en BD de forma asíncrona.
5. Escribe línea técnica en logs de aplicación.

## Reglas de negocio
- Eventos auditables pasan por `IAuditLog`.
- Registro es asíncrono con `@Async`.
- Se persiste tipo, usuario/correo, detalle y timestamp.
- No se deben registrar contraseñas ni tokens.

## Componentes involucrados
- `backend/.../trazabilidad/interfaces/IAuditLog.java`
- `backend/.../trazabilidad/service/AuditLogService.java`
- `backend/.../trazabilidad/model/EventoAuditoria.java`
- `backend/.../trazabilidad/model/TipoEvento.java`
- Servicios de autenticación, perfil, mercado y órdenes.

## Backend
`AuditLogService` implementa `IAuditLog`, usa `AuditLogRepository` y `@Async`. Los servicios lo reciben por constructor.

## Frontend
No hay UI de auditoría. Los eventos se generan por acciones de usuario desde Angular.

## Base de datos
Tabla `evento_auditoria`: `tipo_evento`, `correo_usuario`, `detalle`, `timestamp`.

## API / Endpoints
No expone endpoints de consulta. Se activa indirectamente desde:
- `/api/auth/*`
- `/api/perfil/*`
- `/api/ordenes/*`
- tareas de caché/cola.

## Validaciones
- Tipo de evento es enum `TipoEvento`.
- Detalle limitado por columna `length=500`.

## Seguridad
Evita logging de secretos en los servicios revisados. La auditoría no es manipulable desde frontend.

## Consideraciones técnicas
Cumple la táctica Audit Trail. No integra Splunk/Elasticsearch; usa BD local y SLF4J.

## Dependencias
Dependencia transversal de autenticación, perfil, mercado, órdenes, registro Alpaca y Stripe.

## Criterios de aceptación
- [ ] Login exitoso/fallido se audita.
- [ ] Registro y verificación se auditan.
- [ ] Órdenes se auditan.
- [ ] Cambios de perfil/preferencias se auditan.

## Notas
No hay endpoint para Responsable Legal o administrador que consulte auditoría.
