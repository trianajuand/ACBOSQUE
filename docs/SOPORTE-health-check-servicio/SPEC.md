# Historia de Usuario

## Título
Health check del backend.

## Descripción
Como componente de monitoreo o desarrollador
Quiero consultar un endpoint simple de salud
Para verificar que el backend está respondiendo.

## Contexto
Soporta EC-06 de forma mínima. No implementa monitoreo completo de los seis servicios ni notificación al administrador.

## Flujo funcional
1. Cliente o monitor llama `GET /api/health`.
2. Backend responde `OK`.
3. El endpoint está permitido sin autenticación.

## Reglas de negocio
- El endpoint no realiza validaciones profundas.
- Debe estar disponible públicamente para diagnóstico local.
- No expone datos sensibles.

## Componentes involucrados
- `backend/.../shared/config/HealthController.java`
- `backend/.../autenticacion/security/SecurityConfig.java`

## Backend
`HealthController` expone una respuesta simple. `SecurityConfig` permite `/api/health`.

## Frontend
No hay consumo directo desde Angular.

## Base de datos
No usa base de datos.

## API / Endpoints
- `GET /api/health`

## Validaciones
No aplica.

## Seguridad
Endpoint público sin datos sensibles.

## Consideraciones técnicas
Es un ping/echo básico, no un actuator ni heartbeat con estado de dependencias.

## Dependencias
Puede ser usado por scripts, navegador o monitores externos.

## Criterios de aceptación
- [ ] Responde sin JWT.
- [ ] Retorna texto simple.
- [ ] No consulta servicios externos.

## Notas
Para cumplir EC-06 completo haría falta monitoreo de servicios y alerta automática.
