package co.edu.unbosque.accioneselbosque.mercado.controller;

import co.edu.unbosque.accioneselbosque.autenticacion.model.Usuario;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.UsuarioRepository;
import co.edu.unbosque.accioneselbosque.mercado.dto.CotizacionDTO;
import co.edu.unbosque.accioneselbosque.mercado.dto.DetalleAccionDTO;
import co.edu.unbosque.accioneselbosque.mercado.service.MercadoService;
import co.edu.unbosque.accioneselbosque.shared.dto.RespuestaDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mercado")
public class MercadoController {

    private static final Logger log = LoggerFactory.getLogger(MercadoController.class);

    private final MercadoService mercadoService;
    private final UsuarioRepository usuarioRepo;

    public MercadoController(MercadoService mercadoService,
                             UsuarioRepository usuarioRepo) {
        this.mercadoService = mercadoService;
        this.usuarioRepo = usuarioRepo;
    }

    /** HU-13: Dashboard con acciones de interés del usuario. */
    @GetMapping("/dashboard")
    public ResponseEntity<RespuestaDTO> dashboard(
            @AuthenticationPrincipal String correo) {
        log.info("GET /api/mercado/dashboard - usuario={}", correo);
        List<CotizacionDTO> cotizaciones = mercadoService.obtenerDashboard("");
        log.info("Dashboard devolviendo {} cotizaciones", cotizaciones.size());
        return ResponseEntity.ok(RespuestaDTO.exito(cotizaciones));
    }

    /** Cotización de un símbolo específico. */
    @GetMapping("/cotizacion/{simbolo}")
    public ResponseEntity<RespuestaDTO> cotizacion(@PathVariable String simbolo) {
        CotizacionDTO cot = mercadoService.obtenerCotizacion(simbolo.toUpperCase());
        return ResponseEntity.ok(RespuestaDTO.exito(cot));
    }

    /** HU-14: Detalle completo de una acción. */
    @GetMapping("/detalle/{simbolo}")
    public ResponseEntity<RespuestaDTO> detalle(@PathVariable String simbolo) {
        DetalleAccionDTO detalle = mercadoService.obtenerDetalle(simbolo.toUpperCase());
        return ResponseEntity.ok(RespuestaDTO.exito(detalle));
    }

    /**
     * Catálogo de símbolos disponibles agrupados por sector.
     * Público — se usa en el formulario de registro y en perfil.
     */
    @GetMapping("/simbolos")
    public ResponseEntity<RespuestaDTO> simbolosDisponibles() {
        return ResponseEntity.ok(RespuestaDTO.exito(mercadoService.obtenerSimbolosDisponibles()));
    }

    /** Verifica si un mercado está abierto ahora. */
    @GetMapping("/horario/{mercado}")
    public ResponseEntity<RespuestaDTO> horario(@PathVariable String mercado) {
        boolean abierto = mercadoService.esMercadoAbierto(mercado);
        return ResponseEntity.ok(RespuestaDTO.exito(java.util.Map.of(
                "mercado", mercado,
                "abierto", abierto
        )));
    }
}
