package co.edu.unbosque.accioneselbosque.ordenes.controller;

import co.edu.unbosque.accioneselbosque.autenticacion.model.Usuario;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.UsuarioRepository;
import co.edu.unbosque.accioneselbosque.ordenes.service.ReporteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/ordenes/reporte")
public class ReporteController {

    private final ReporteService reporteService;
    private final UsuarioRepository usuarioRepo;

    public ReporteController(ReporteService reporteService, UsuarioRepository usuarioRepo) {
        this.reporteService = reporteService;
        this.usuarioRepo = usuarioRepo;
    }

    @GetMapping(produces = "application/pdf")
    public ResponseEntity<byte[]> generarReporte(
            @AuthenticationPrincipal String correo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {

        if (desde == null) desde = LocalDate.now().minusMonths(1);
        if (hasta == null) hasta = LocalDate.now();

        Usuario usuario = usuarioRepo.findByCorreo(correo)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        byte[] pdf = reporteService.generarReporte(usuario.getId(), desde, hasta);

        String nombreArchivo = "reporte_" + desde.format(DateTimeFormatter.BASIC_ISO_DATE)
                + "_" + hasta.format(DateTimeFormatter.BASIC_ISO_DATE) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
