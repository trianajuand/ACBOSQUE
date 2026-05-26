# Tareas de implementación — HU-27: Generación de reporte personal de actividad

## Estado general: COMPLETADA

---

## Tareas backend

- [x] **T1 — Agregar dependencia OpenPDF al `pom.xml`**
  - Dependencia: `com.github.librepdf:openpdf:1.3.43`
  - Criterio: el proyecto compila sin errores después de agregar la dependencia.

- [x] **T2 — Crear `ReporteService` en `ordenes/service/`**
  - Método principal: `generarReporte(Long usuarioId, LocalDate desde, LocalDate hasta) → byte[]`
  - Lógica:
    1. Consultar órdenes ejecutadas del período para el usuario.
    2. Calcular totales: número de órdenes, comisiones sumadas, P&L neto.
    3. Identificar top activos por número de órdenes.
    4. Construir el PDF con OpenPDF (encabezado, tabla de órdenes, resumen, top activos).
  - Criterio: genera un PDF descargable con los datos reales.

- [x] **T3 — Crear `ReporteController` en `ordenes/controller/`**
  - Endpoint: `GET /api/ordenes/reporte`
  - Params: `desde` (opcional, default hoy-1mes), `hasta` (opcional, default hoy)
  - Respuesta: `ResponseEntity<byte[]>` con:
    - `Content-Type: application/pdf`
    - `Content-Disposition: attachment; filename="reporte_{desde}_{hasta}.pdf"`
  - Criterio: la descarga comienza correctamente en el navegador / Postman.

- [x] **T4 — Registrar el endpoint en `SecurityConfig`**
  - Criterio: el endpoint requiere JWT válido; sin token devuelve 401.

---

## Tareas frontend (Angular)

- [x] **T5 — Agregar método `getReporte(desde, hasta)` en `ApiService`**
  - Usa `HttpClient.get(..., { responseType: 'blob' })` para manejar la respuesta binaria.

- [x] **T6 — Crear panel "Reporte" en el dashboard**
  - Controles: selector de fecha `desde`, selector de fecha `hasta`, botón "Descargar PDF".
  - Al hacer clic: llama a `ApiService.getReporte()` y dispara la descarga en el navegador usando una URL de objeto Blob.
  - Criterio: el archivo descargado se abre correctamente como PDF.

- [x] **T7 — Manejar errores en la descarga**
  - Si el backend devuelve error → mostrar mensaje en español al usuario.

---

## Tareas de verificación

- [x] **T8 — Probar `GET /api/ordenes/reporte` en Postman**
  - Resultado esperado: 200 OK, archivo PDF descargado con datos del período.

- [x] **T9 — Probar sin JWT**
  - Resultado esperado: 401 Unauthorized.

- [x] **T10 — Verificar que el PDF incluye resumen de comisiones y P&L**
  - Abrir el PDF descargado y confirmar presencia de sección de resumen financiero.

- [x] **T11 — Actualizar `docs/PROGRESO.md`**
  - Marcar HU-27 con ✅ en la tabla del Sprint 4.
