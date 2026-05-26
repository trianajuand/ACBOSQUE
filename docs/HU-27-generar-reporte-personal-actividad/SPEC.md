# SPEC — Generación de reporte personal de actividad

---

## Ficha de la historia

| Campo | Valor |
|---|---|
| ID | HU-27 |
| Sprint | 4 |
| Prioridad MoSCoW | Should Have |
| Estado | Completada |
| Épica | Órdenes / Reportes |
| CU asociado | CU-27 |
| Autor | Juan Diego Triana Mejia |
| Creada | 2026-05-20 |
| Última revisión | 2026-05-24 |
| Versión de esta spec | 1.0 |

**Trazabilidad:**

| Tipo | ID | Descripción |
|---|---|---|
| Requerimiento funcional | RF-26 | Generación de reporte PDF de actividad del inversionista |
| Historia que precede a esta | HU-24..26 | Los datos del reporte son las órdenes del historial |

---

## Historia de usuario

**Como** inversionista autenticado,
**quiero** generar un reporte PDF de mi actividad en un período específico,
**para** revisar mis órdenes, ganancias, pérdidas y comisiones de manera descargable.

---

## Actores y precondiciones

### Actores

| Actor | Rol en el sistema | Participación |
|---|---|---|
| Inversionista autenticado | `INVERSIONISTA` | Iniciador — solicita el reporte |
| `ReporteService` | Módulo `ordenes` | Genera el archivo del reporte |

### Precondiciones

- JWT válido.
- Parámetros `desde` y `hasta` opcionales (default: último año hasta hoy).
- Solo formato PDF soportado en la implementación actual.

---

## Flujo principal

1. Frontend envía `GET /api/ordenes/reporte?desde=2026-05-01&hasta=2026-05-31` con JWT.
2. `ReporteService` consulta órdenes del período, holdings, comisiones y P&L.
3. Genera el archivo PDF.
4. Retorna el archivo como stream descargable con `Content-Disposition: attachment; filename="reporte_{desde}_{hasta}.pdf"`.

---

## Flujos de error

### Error 1 — No autenticado

| Campo | Valor |
|---|---|
| HTTP | 401 Unauthorized |

### Error 2 — Error técnico al generar reporte

| Campo | Valor |
|---|---|
| HTTP | 500 Internal Server Error |

---

## Contrato de API

### Endpoint — `GET /api/ordenes/reporte`

```yaml
GET /api/ordenes/reporte:
  summary: Genera y descarga un reporte personal de actividad en formato PDF
  security:
    - bearerAuth: []
  parameters:
    - name: desde
      in: query
      required: false
      schema:
        type: string
        format: date
      description: "Fecha de inicio (ISO yyyy-MM-dd). Default: un año antes de hoy."
      example: "2026-05-01"
    - name: hasta
      in: query
      required: false
      schema:
        type: string
        format: date
      description: "Fecha de fin (ISO yyyy-MM-dd). Default: hoy."
      example: "2026-05-31"
  responses:
    '200':
      description: Archivo PDF de reporte generado
      content:
        application/pdf:
          schema:
            type: string
            format: binary
      headers:
        Content-Disposition:
          schema:
            type: string
          example: "attachment; filename=\"reporte_20260501_20260531.pdf\""
    '401':
      description: No autenticado
    '500':
      description: Error al generar el reporte
```

> **Nota:** El formato Excel (XLSX) no está implementado. Solo se genera PDF. El parámetro `formato` no existe en el endpoint real.

---

## Contenido del reporte

| Sección | Descripción |
|---|---|
| Encabezado | Nombre del inversionista, correo, período del reporte, fecha de generación |
| Resumen | Total órdenes, ejecutadas, canceladas, total comisiones pagadas, P&L neto |
| Tabla de órdenes | ID, símbolo, tipo, lado, cantidad, precio ejecución, comisión, estado, fecha |
| Holdings actuales | Símbolo, cantidad, precio promedio compra, valor actual, P&L |
| Pie | Marca del sistema (Acciones ElBosque) |

---

## Módulos y arquitectura

| Módulo | Rol | Componentes |
|---|---|---|
| `ordenes` | Generación del reporte | `ReporteController`, `ReporteService` |
| `mercado` | Precios actuales para holdings | Caché de precios |

---

## Riesgos

| # | Riesgo | P | I | Mitigación |
|---|---|:-:|:-:|---|
| R1 | Período muy largo → reporte con miles de registros → timeout | Media | Bajo | Límite de 1 año por solicitud; paginación si supera X registros |

---

## Criterios de verificación

### Escenarios de aceptación (Gherkin)

```gherkin
Funcionalidad: Reporte personal de actividad

  Escenario: Generación de reporte PDF
    Cuando se envía GET /api/ordenes/reporte?desde=2026-05-01&hasta=2026-05-31 con JWT
    Entonces el sistema responde 200 OK
    Y Content-Type es application/pdf
    Y Content-Disposition contiene "attachment"

  Escenario: Sin parámetros — usa defaults (último año)
    Cuando se envía GET /api/ordenes/reporte con JWT
    Entonces el sistema responde 200 OK con PDF del último año

  Escenario: Sin JWT — 401
    Cuando se envía GET /api/ordenes/reporte sin Authorization
    Entonces el sistema responde 401 Unauthorized
```

---

## Definición de terminado

- [x] `GET /api/ordenes/reporte` genera reporte PDF con órdenes del período.
- [x] Parámetros `desde`/`hasta` opcionales; sin ellos usa el último año.
- [x] Sin JWT responde 401.
- [x] El reporte incluye resumen de comisiones y P&L.
- [ ] Formato Excel (XLSX) — pendiente como deuda técnica (DT-11).
- [x] `docs/PROGRESO.md` marcado con ✅ para HU-27.

---

## Historial de cambios

| Versión | Fecha | Descripción | Razón |
|---|---|---|---|
| 1.0 | 2026-05-24 | Refactorización a estructura SDD del proyecto. | Unificación de todos los SPEC.md bajo plantilla canónica SDD del proyecto |
| 1.1 | 2026-05-26 | Auditoría SDD: endpoint corregido de `GET /api/reportes/personal` a `GET /api/ordenes/reporte`. Parámetro `formato` eliminado (no existe). Parámetros `desde`/`hasta` son opcionales. Solo PDF implementado; Excel es deuda técnica DT-11. | Código real en `ReporteController` usa ruta `/api/ordenes/reporte` y produce solo PDF. |
