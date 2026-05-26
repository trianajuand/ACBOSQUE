package co.edu.unbosque.accioneselbosque.ordenes.service;

import co.edu.unbosque.accioneselbosque.autenticacion.model.Usuario;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.UsuarioRepository;
import co.edu.unbosque.accioneselbosque.mercado.model.Activo;
import co.edu.unbosque.accioneselbosque.mercado.repository.ActivoRepository;
import co.edu.unbosque.accioneselbosque.ordenes.model.Comision;
import co.edu.unbosque.accioneselbosque.ordenes.model.EstadoOrden;
import co.edu.unbosque.accioneselbosque.ordenes.model.Orden;
import co.edu.unbosque.accioneselbosque.ordenes.model.TipoLado;
import co.edu.unbosque.accioneselbosque.ordenes.repository.ComisionRepository;
import co.edu.unbosque.accioneselbosque.ordenes.repository.OrdenRepository;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

@Service
public class ReporteService {

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final OrdenRepository ordenRepo;
    private final ComisionRepository comisionRepo;
    private final UsuarioRepository usuarioRepo;
    private final ActivoRepository activoRepo;

    public ReporteService(OrdenRepository ordenRepo,
                          ComisionRepository comisionRepo,
                          UsuarioRepository usuarioRepo,
                          ActivoRepository activoRepo) {
        this.ordenRepo = ordenRepo;
        this.comisionRepo = comisionRepo;
        this.usuarioRepo = usuarioRepo;
        this.activoRepo = activoRepo;
    }

    @Transactional(readOnly = true)
    public byte[] generarReporte(Long usuarioId, LocalDate desde, LocalDate hasta) {
        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.atTime(23, 59, 59);

        Usuario usuario = usuarioRepo.findById(usuarioId).orElseThrow();

        List<Orden> ordenes = ordenRepo.findByInversionistaIdOrderByCreadaEnDesc(usuarioId)
                .stream()
                .filter(o -> o.getEstado() == EstadoOrden.EJECUTADA)
                .filter(o -> o.getCreadaEn() != null
                        && !o.getCreadaEn().isBefore(inicio)
                        && !o.getCreadaEn().isAfter(fin))
                .toList();

        List<Comision> comisiones = new java.util.ArrayList<>();
        comisionRepo.findAll().forEach(comisiones::add);
        comisiones = comisiones.stream()
                .filter(c -> c.getUsuarioId().equals(usuarioId))
                .filter(c -> c.getCreadaEn() != null
                        && !c.getCreadaEn().isBefore(inicio)
                        && !c.getCreadaEn().isAfter(fin))
                .toList();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 40, 40, 50, 40);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            agregarEncabezado(doc, usuario.getNombreCompleto(), usuario.getCorreo(), desde, hasta);
            agregarResumen(doc, ordenes, comisiones);
            agregarTablaOrdenes(doc, ordenes);
            agregarActivosMasOperados(doc, ordenes);

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF", e);
        }
    }

    private void agregarEncabezado(Document doc, String nombre, String correo,
                                    LocalDate desde, LocalDate hasta) throws DocumentException {
        Font fTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(30, 30, 80));
        Font fSub = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);

        Paragraph titulo = new Paragraph("Acciones El Bosque — Reporte de Actividad", fTitulo);
        titulo.setAlignment(Element.ALIGN_CENTER);
        doc.add(titulo);

        Paragraph sub = new Paragraph(
                nombre + " <" + correo + ">  |  Periodo: "
                        + desde.format(FMT_FECHA) + " — " + hasta.format(FMT_FECHA),
                fSub);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingBefore(4);
        sub.setSpacingAfter(16);
        doc.add(sub);

        Paragraph generado = new Paragraph(
                "Generado el " + LocalDateTime.now().format(FMT_DT), fSub);
        generado.setAlignment(Element.ALIGN_RIGHT);
        generado.setSpacingAfter(20);
        doc.add(generado);
    }

    private void agregarResumen(Document doc, List<Orden> ordenes,
                                 List<Comision> comisiones) throws DocumentException {
        Font fSeccion = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(30, 30, 80));
        Font fLabel = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
        Font fValor = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);

        doc.add(new Paragraph("Resumen del periodo", fSeccion));
        doc.add(new Paragraph(" "));

        BigDecimal totalCompras = ordenes.stream()
                .filter(o -> o.getLado() == TipoLado.COMPRA)
                .map(o -> safe(o.getMontoTotal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalVentas = ordenes.stream()
                .filter(o -> o.getLado() == TipoLado.VENTA)
                .map(o -> safe(o.getMontoTotal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal flujNeto = totalVentas.subtract(totalCompras);

        BigDecimal totalComisiones = comisiones.stream()
                .map(c -> safe(c.getMontoComision()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(60);
        tabla.setHorizontalAlignment(Element.ALIGN_LEFT);
        tabla.setSpacingAfter(20);

        filaResumen(tabla, "Ordenes ejecutadas", String.valueOf(ordenes.size()), fLabel, fValor);
        filaResumen(tabla, "Total compras", "$" + fmt(totalCompras), fLabel, fValor);
        filaResumen(tabla, "Total ventas", "$" + fmt(totalVentas), fLabel, fValor);
        filaResumen(tabla, "Flujo neto (ventas - compras)", "$" + fmt(flujNeto), fLabel,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11,
                        flujNeto.signum() >= 0 ? new Color(0, 120, 60) : Color.RED));
        filaResumen(tabla, "Total comisiones pagadas", "$" + fmt(totalComisiones), fLabel, fValor);

        doc.add(tabla);
    }

    private void agregarTablaOrdenes(Document doc, List<Orden> ordenes) throws DocumentException {
        Font fSeccion = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(30, 30, 80));
        Font fHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        Font fCelda = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);

        doc.add(new Paragraph("Ordenes ejecutadas", fSeccion));
        doc.add(new Paragraph(" "));

        if (ordenes.isEmpty()) {
            doc.add(new Paragraph("No hay ordenes ejecutadas en el periodo.", fCelda));
            doc.add(new Paragraph(" "));
            return;
        }

        PdfPTable tabla = new PdfPTable(new float[]{14, 7, 7, 8, 12, 10, 10, 12});
        tabla.setWidthPercentage(100);
        tabla.setSpacingAfter(20);

        String[] headers = {"Fecha", "Simbolo", "Lado", "Tipo", "Cantidad", "Precio", "Monto", "Comision"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, fHeader));
            cell.setBackgroundColor(new Color(30, 30, 80));
            cell.setPadding(5);
            cell.setBorder(Rectangle.NO_BORDER);
            tabla.addCell(cell);
        }

        boolean par = false;
        for (Orden o : ordenes) {
            Color bg = par ? new Color(245, 245, 255) : Color.WHITE;
            par = !par;
            addCell(tabla, o.getEjecutadaEn() != null ? o.getEjecutadaEn().format(FMT_DT) : "-", fCelda, bg);
            addCell(tabla, activoRepo.findById(o.getActivoId()).map(Activo::getTicker).orElse("?"), fCelda, bg);
            addCell(tabla, o.getLado().name(), fCelda, bg);
            addCell(tabla, o.getTipoOrden().name(), fCelda, bg);
            addCell(tabla, fmt(o.getCantidad()), fCelda, bg);
            addCell(tabla, "$" + fmt(o.getPrecioEjecucion()), fCelda, bg);
            addCell(tabla, "$" + fmt(o.getMontoTotal()), fCelda, bg);
            addCell(tabla, "$" + fmt(o.getComision()), fCelda, bg);
        }
        doc.add(tabla);
    }

    private void agregarActivosMasOperados(Document doc, List<Orden> ordenes) throws DocumentException {
        Font fSeccion = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(30, 30, 80));
        Font fHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        Font fCelda = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);

        doc.add(new Paragraph("Activos mas operados", fSeccion));
        doc.add(new Paragraph(" "));

        Map<String, long[]> stats = new LinkedHashMap<>();
        for (Orden o : ordenes) {
            String ticker = activoRepo.findById(o.getActivoId()).map(Activo::getTicker).orElse("?");
            stats.computeIfAbsent(ticker, k -> new long[2]);
            stats.get(ticker)[0]++;
            stats.get(ticker)[1] += safe(o.getMontoTotal()).longValue();
        }

        List<Map.Entry<String, long[]>> top = stats.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]))
                .limit(10)
                .toList();

        if (top.isEmpty()) {
            doc.add(new Paragraph("Sin datos.", fCelda));
            return;
        }

        PdfPTable tabla = new PdfPTable(new float[]{20, 15, 25});
        tabla.setWidthPercentage(60);
        tabla.setHorizontalAlignment(Element.ALIGN_LEFT);

        for (String h : new String[]{"Simbolo", "Operaciones", "Volumen total (USD)"}) {
            PdfPCell cell = new PdfPCell(new Phrase(h, fHeader));
            cell.setBackgroundColor(new Color(30, 30, 80));
            cell.setPadding(5);
            cell.setBorder(Rectangle.NO_BORDER);
            tabla.addCell(cell);
        }

        boolean par = false;
        for (Map.Entry<String, long[]> e : top) {
            Color bg = par ? new Color(245, 245, 255) : Color.WHITE;
            par = !par;
            addCell(tabla, e.getKey(), fCelda, bg);
            addCell(tabla, String.valueOf(e.getValue()[0]), fCelda, bg);
            addCell(tabla, "$" + e.getValue()[1], fCelda, bg);
        }
        doc.add(tabla);
    }

    private void filaResumen(PdfPTable t, String label, String valor, Font fL, Font fV) {
        PdfPCell cL = new PdfPCell(new Phrase(label, fL));
        cL.setBorder(Rectangle.BOTTOM);
        cL.setPadding(5);
        t.addCell(cL);
        PdfPCell cV = new PdfPCell(new Phrase(valor, fV));
        cV.setBorder(Rectangle.BOTTOM);
        cV.setPadding(5);
        t.addCell(cV);
    }

    private void addCell(PdfPTable t, String text, Font f, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, f));
        cell.setBackgroundColor(bg);
        cell.setPadding(4);
        cell.setBorder(Rectangle.BOTTOM);
        t.addCell(cell);
    }

    private String fmt(BigDecimal v) {
        if (v == null) return "0.00";
        return v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private BigDecimal safe(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
