# Historias de Usuario (HU) — Backlog del MVP

> Fuente: `Historias_de_usuario.docx` y sección 11 del Informe de Ingeniería. Las 42 historias Must Have constituyen el alcance comprometido del MVP. Las historias Won't Have (HU-43+) están listadas como referencia pero no son parte del proyecto académico actual.

Formato resumido por historia: Resumen / Descripción / Criterios de aceptación clave / Prioridad / Épica / CU asociado.

---

## Épica 1 — Autenticación

### HU-1 — Registrar Inversionista
- **Como:** usuario no registrado.
- **Quiero:** acceder al formulario de registro e ingresar mis datos personales, nivel de experiencia e intereses de mercado.
- **Para:** que el sistema valide la información y cree mi cuenta como inversionista.
- **Criterios:**
  - Form muestra campos: nombre, correo, contraseña, nivel de experiencia, intereses.
  - Si todos los campos son válidos → crea cuenta y redirige al panel de inversionista.
  - Si correo ya está registrado → mensaje *"El correo ingresado ya está en uso"* sin borrar los demás campos.
- **Prioridad:** Must Have · **Épica:** Gestión de registro · **CU-01**

### HU-2 — Integración con Alpaca al registrar inversionista
- **Como:** inversionista recién registrado.
- **Quiero:** que el sistema cree automáticamente una cuenta en Alpaca asociada a mi perfil.
- **Criterios:**
  - Tras registro exitoso, sistema invoca API Alpaca y asocia el ID retornado.
  - Si Alpaca responde OK → estado `activa` y acceso al panel sin interrupciones.
  - Si Alpaca falla → registra el fallo y notifica al admin con ID del usuario y motivo del error.
- **Prioridad:** Must Have · **Épica:** Gestión de registro · **CU-01**

### HU-3 — Inicio de sesión con credenciales
- **Como:** usuario registrado.
- **Quiero:** iniciar sesión con correo y contraseña.
- **Criterios:**
  - Credenciales válidas → continúa al flujo de autenticación según rol.
  - Cuenta suspendida → muestra mensaje informando que la cuenta está inactiva.
- **Prioridad:** Must Have · **Épica:** Gestión de autenticación · **CU-02**

### HU-4 — Autenticación multifactor (MFA)
- **Como:** usuario autenticado con credenciales válidas.
- **Quiero:** verificación MFA según mi rol.
- **Criterios:**
  - Comisionista/Admin → MFA obligatorio. Inversionista con MFA on → también se le pide.
  - Código MFA correcto → redirige al panel correspondiente (trading / clientes / administración).
- **Prioridad:** Must Have · **Épica:** Gestión de autenticación · **CU-02**

### HU-5 — Cierre de sesión
- **Como:** usuario autenticado.
- **Quiero:** cerrar sesión de forma segura.
- **Criterios:**
  - "Cerrar sesión" → invalida JWT activo y redirige a login.
  - JWT expirado → cierra sesión automáticamente y redirige a login.
- **Prioridad:** Must Have · **Épica:** Gestión de autenticación · **CU-03**

---

## Épica 2 — Perfil del Inversionista

### HU-6 — Consultar información de inversionista
- **Como:** inversionista con sesión activa.
- **Quiero:** ver mi perfil (datos personales, nivel de experiencia, intereses, comisionista asignado, preferencias).
- **Prioridad:** Must Have · **CU-04**

### HU-7 — Actualizar datos personales
- **Como:** inversionista con sesión activa.
- **Quiero:** editar nombre, correo, nivel de experiencia, intereses.
- **Criterios:**
  - Datos válidos → actualiza correctamente.
  - Confirmación visible al usuario.
- **Prioridad:** Must Have · **CU-05**

### HU-8 — Configurar preferencias de notificación
- **Como:** inversionista con sesión activa.
- **Quiero:** configurar canales (Email, SMS, WhatsApp) y tipos de notificación.
- **Prioridad:** Must Have · **CU-06**

### HU-9 — Configurar preferencias de operación
- **Como:** inversionista con sesión activa.
- **Quiero:** definir tipo de orden por defecto y vista predeterminada del portafolio (lista, gráfico).
- **Prioridad:** Must Have · **CU-06**

### HU-10 — Activar/desactivar MFA opcional (Inversionista)
- **Como:** inversionista regular.
- **Quiero:** activar o desactivar MFA en mi cuenta.
- **Prioridad:** Must Have · **CU-04**

### HU-11 — Contratar suscripción premium (Stripe)
- **Como:** inversionista.
- **Quiero:** suscribirme al plan premium (mensual $12 o anual $120).
- **Prioridad:** Should Have · **CU-07**

### HU-12 — Cancelar suscripción premium
- **Como:** inversionista premium.
- **Quiero:** cancelar la renovación automática.
- **Criterios:** Conserva beneficios premium hasta fin del período pagado.
- **Prioridad:** Should Have · **CU-08**

---

## Épica 3 — Visualización de Mercado y Portafolio

### HU-13 — Visualización del dashboard de acciones de interés
- **Como:** inversionista.
- **Quiero:** dashboard con precios, variaciones y métricas de mis acciones de interés.
- **Criterios:**
  - Consulta APIs (Alpaca / Alpha Vantage) y muestra precios.
  - Datos se actualizan en tiempo real.
  - Si API no responde → muestra últimos datos disponibles con indicador visual de desactualización.
- **Prioridad:** Must Have · **CU-09**

### HU-14 — Consultar detalle de una acción
- **Como:** inversionista.
- **Quiero:** ver precio actual, historial, capitalización, volumen y métricas clave de una acción.
- **Prioridad:** Must Have · **CU-10**

### HU-15 — Visualización del portafolio de inversiones
- **Como:** inversionista.
- **Quiero:** ver mi portafolio con holdings, cantidad, precio promedio, precio mercado, valor total, ganancia/pérdida.
- **Criterios:**
  - Vista lista o gráfico de barras según preferencia.
  - Si no tiene posiciones → portafolio vacío con mensaje orientador.
- **Prioridad:** Must Have · **CU-11**

### HU-16 — Consultar saldo y comisiones
- **Como:** inversionista.
- **Quiero:** ver saldo disponible, fondos reservados, total y desglose de comisiones pagadas.
- **Prioridad:** Must Have · **CU-12**

---

## Épica 4 — Gestión de Órdenes

### HU-17 a HU-20 — Colocar órdenes Market / Limit / Stop Loss / Take Profit
- **Como:** inversionista.
- **Quiero:** colocar órdenes de cada uno de los 4 tipos.
- **Criterios:**
  - Verifica fondos antes de compra / holdings antes de venta.
  - Calcula y muestra comisión 2% antes de confirmar (EC-13).
  - Envía a Alpaca, registra trazabilidad, notifica.
- **Prioridad:** Must Have · **CU-13, CU-14, CU-15, CU-16**

### HU-21 — Cancelar orden pendiente
- **Como:** inversionista.
- **Quiero:** cancelar una orden que aún no se ha ejecutado.
- **Prioridad:** Must Have · **CU-17**

### HU-22 — Consultar órdenes activas
- **Como:** inversionista.
- **Quiero:** ver el estado actual de mis órdenes activas.
- **Prioridad:** Must Have · **CU-17**

### HU-23 — Encolamiento de órdenes fuera de horario
- **Como:** inversionista.
- **Quiero:** colocar órdenes fuera de horario y que se procesen al abrir el mercado.
- **Prioridad:** Must Have · **CU-13**

### HU-24, HU-25, HU-26 — Historial de órdenes con filtros
- **Como:** inversionista.
- **Quiero:** consultar mi historial de órdenes con filtros (período, tipo, activo, estado).
- **Prioridad:** Should Have · **CU-18**

### HU-27 — Generar reporte personal de actividad
- **Como:** inversionista.
- **Quiero:** generar reporte (PDF/Excel) de un período: órdenes, ganancias/pérdidas, comisiones, activos más operados.
- **Prioridad:** Could Have · **CU-19**

---

## Épica 5 — Gestión de Comisionistas

### HU-28 — Consultar portafolio de cliente asignado
- **Como:** comisionista.
- **Quiero:** ver el portafolio de mis clientes asignados (no de otros — EC-11).
- **Prioridad:** Must Have · **CU-20**

### HU-29 — Consultar órdenes activas e históricas del cliente
- **Como:** comisionista.
- **Prioridad:** Must Have · **CU-20**

### HU-30 — Proponer orden para cliente
- **Como:** comisionista.
- **Quiero:** crear una propuesta de orden (estado "Pendiente de Aprobación") para un cliente asignado.
- **Prioridad:** Should Have · **CU-21**

### HU-31 — Aprobar/rechazar propuesta del comisionista
- **Como:** inversionista.
- **Quiero:** revisar y aprobar/rechazar las propuestas que me envíe mi comisionista.
- **Prioridad:** Should Have · **CU-21**

### HU-32 — Firmar y enviar orden aprobada al mercado
- **Como:** comisionista.
- **Quiero:** firmar las órdenes que el cliente aprobó y enviarlas a Alpaca.
- **Prioridad:** Should Have · **CU-21**

---

## Épica 6 — Gestión de Configuración (Administración)

### HU-33 — Configurar mercados habilitados
- **Como:** administrador.
- **Quiero:** activar/desactivar mercados (NYSE, NASDAQ, TSE, etc.) y configurar sus horarios.
- **Prioridad:** Must Have · **CU-22**

### HU-34 — Configurar feriados de mercado
- **Como:** administrador.
- **Quiero:** agregar feriados al calendario de cada mercado.
- **Prioridad:** Must Have · **CU-23** · **EC-19**

### HU-35 — Configurar parámetros de comisión
- **Como:** administrador.
- **Quiero:** modificar el % de comisión y el split plataforma/comisionista.
- **Prioridad:** Must Have · **CU-24** · **EC-18**

---

## Épica 7 — Gestión de Usuarios (Administración)

### HU-36 — Crear cuenta de Comisionista
- **Como:** administrador.
- **Quiero:** crear cuentas de comisionista (no se auto-registran).
- **Prioridad:** Must Have · **CU-25**

### HU-37 — Asignar comisionista a inversionista
- **Como:** administrador.
- **Prioridad:** Must Have · **CU-26**

### HU-38 — Suspender / reactivar cuenta de inversionista
- **Como:** administrador.
- **Prioridad:** Must Have · **CU-26**

### HU-39 — Eliminar cuenta de inversionista
- **Como:** administrador.
- **Prioridad:** Must Have · **CU-26**

---

## Épica 8 — Trazabilidad y Notificaciones

### HU-40 — Registro automático de logs
- **Como:** sistema.
- **Quiero:** generar registros inmutables de toda operación crítica (autenticación, órdenes, cambios admin, accesos denegados).
- **Prioridad:** Must Have · **CU-27** · **EC-12**

### HU-41 — Despacho de notificaciones multicanal
- **Como:** sistema.
- **Quiero:** enviar notificaciones por los canales configurados (Email, SMS, WhatsApp) ante eventos relevantes.
- **Prioridad:** Should Have · **CU-28** · **EC-17**

### HU-42 — Detección de fallo de servicio y notificación al admin
- **Como:** sistema.
- **Quiero:** detectar cuando un servicio del backend deja de responder y notificar al administrador.
- **Prioridad:** Must Have · **CU-27** · **EC-06**

---

## Historias fuera del MVP (Won't Have)

Estas se documentan pero **no se implementan** en el proyecto académico:

| HU | Descripción | Razón aplazamiento |
|---|---|---|
| HU-43 | Resumen diario de operaciones automático al cierre del mercado | Funcionalidad de valor agregado, no crítica para MVP |
| HU-44 | Agregar acción a Watchlist (premium) | Feature premium adicional |
| HU-45 | Eliminar acción de Watchlist (premium) | Feature premium adicional |
| HU-46 | Configurar alertas de precio (premium) | Feature premium adicional |
| HU-47 | Módulo de auditoría de Responsable Legal | Complejidad alta, fuera del MVP |
| HU-48 | Dashboard ejecutivo de métricas (admin) | Feature analítica, fuera del MVP |
| HU-49 | (otra historia aplazada) | — |

---

## Resumen del backlog

- **Total comprometido (Must + Should + Could):** 42 historias = MVP.
- **Distribución por prioridad:**
  - Must Have: ~30 historias
  - Should Have: ~10 historias
  - Could Have: ~2 historias
- **Won't Have:** 7 historias documentadas pero no implementadas.

> Para criterios de aceptación completos de cada historia, consulta el documento original `Historias_de_usuario.docx` o el tablero Jira del proyecto.
