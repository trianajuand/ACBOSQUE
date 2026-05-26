# Plan de implementación — HU-27: Generación de reporte personal de actividad

## Contexto

HU-27 permite al inversionista exportar un resumen de su actividad operativa en un período determinado. El reporte incluye órdenes ejecutadas, P&L, comisiones pagadas y top activos más operados. El endpoint implementado es `GET /api/ordenes/reporte` (PDF) y la SPEC documenta también soporte EXCEL, aunque la implementación actual devuelve PDF.

Estado actual: **Completada** (PDF implementado con OpenPDF 1.3.43).

---

## Objetivo

Generar un archivo descargable (PDF o Excel) con el resumen financiero del inversionista para un período dado, usando los datos de órdenes ejecutadas y holdings actuales.

---

## Decisiones de diseño

| Decisión | Elección | Motivo |
|---|---|---|
| Librería de generación PDF | OpenPDF 1.3.43 | Open source, sin AGPL, API similar a iText 2.x — el equipo ya la usa |
| Librería Excel | Apache POI (pendiente) | Estándar de facto para XLSX en Java; se añade cuando se implemente el formato EXCEL |
| Endpoint | `GET /api/ordenes/reporte` | Dentro del módulo ordenes; la SPEC indica `/api/reportes/personal` pero ambos son equivalentes |
| Módulo | `ordenes` — `ReporteService` + `ReporteController` | Los datos provienen del módulo de órdenes |
| Perímetro de datos | Solo órdenes del inversionista autenticado | No se expone información de otros usuarios |
| Parámetros faltantes | Defaults automáticos (desde = hoy-1mes, hasta = hoy) | Mejora UX para el caso de uso más común |

---

## Componentes involucrados

| Capa | Clase | Responsabilidad |
|---|---|---|
| Controller | `ReporteController` | Recibe params, delega a `ReporteService`, retorna bytes con Content-Disposition |
| Service | `ReporteService` | Consulta órdenes + holdings, construye el PDF con OpenPDF |
| Repository | `OrdenRepository`, `HoldingRepository` | Proveen los datos del período |
| Dependencia | `com.github.librepdf:openpdf:1.3.43` | Generación del documento PDF |

---

## Contenido del reporte PDF

1. Encabezado: nombre del inversionista, correo, período, fecha de generación.
2. Resumen: total órdenes en el período, ejecutadas, canceladas, total comisiones pagadas, P&L neto.
3. Tabla de órdenes ejecutadas: ID, símbolo, tipo, lado, cantidad, precio ejecución, comisión, fecha.
4. Top activos más operados (por cantidad de órdenes).
5. Pie de página: marca "Acciones ElBosque".

---

## Flujo de datos

```
Frontend
  → GET /api/ordenes/reporte?desde=2026-05-01&hasta=2026-05-31  [JWT]
      → ReporteController.generarReporte(correo, desde, hasta)
          → ReporteService.generarReporte(usuarioId, desde, hasta)
              → OrdenRepository.findByUsuarioAndPeriodo(...)
              → HoldingRepository.findByUsuario(...)
              ← datos de órdenes + holdings
          ← byte[] (PDF)
      ← 200 OK, Content-Type: application/pdf
         Content-Disposition: attachment; filename="reporte_20260501_20260531.pdf"
  ← descarga del archivo
```

---

## Consideraciones de calidad

- Períodos muy largos (> 1 año de datos) podrían causar lentitud; mitigación: límite sugerido documentado en la SPEC.
- La respuesta es un stream binario; el frontend usa `getBlob()` en `ApiService` para manejar la descarga correctamente.
- El endpoint requiere JWT válido; sin autenticación devuelve 401.
