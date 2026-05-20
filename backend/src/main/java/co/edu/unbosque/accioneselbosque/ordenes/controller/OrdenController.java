package co.edu.unbosque.accioneselbosque.ordenes.controller;

import co.edu.unbosque.accioneselbosque.autenticacion.model.Usuario;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.UsuarioRepository;
import co.edu.unbosque.accioneselbosque.ordenes.dto.CrearOrdenRequestDTO;
import co.edu.unbosque.accioneselbosque.ordenes.dto.OrdenDTO;
import co.edu.unbosque.accioneselbosque.ordenes.dto.ResumenComisionDTO;
import co.edu.unbosque.accioneselbosque.ordenes.interfaces.IOrden;
import co.edu.unbosque.accioneselbosque.shared.dto.RespuestaDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ordenes")
public class OrdenController {

    private final IOrden ordenService;
    private final UsuarioRepository usuarioRepo;

    public OrdenController(IOrden ordenService, UsuarioRepository usuarioRepo) {
        this.ordenService = ordenService;
        this.usuarioRepo = usuarioRepo;
    }

    /**
     * EC-13: Previsualiza comisión ANTES de confirmar la orden.
     * El frontend debe mostrar este resumen y requerir confirmación explícita.
     */
    @PostMapping("/previsualizar")
    public ResponseEntity<RespuestaDTO> previsualizar(
            @AuthenticationPrincipal String correo,
            @Valid @RequestBody CrearOrdenRequestDTO dto) {
        Usuario usuario = resolverUsuario(correo);
        ResumenComisionDTO resumen = ordenService.previsualizarOrden(usuario.getId(), dto);
        return ResponseEntity.ok(RespuestaDTO.exito(resumen));
    }

    /** HU-17 a HU-20: Crea y envía la orden (todos los tipos). */
    @PostMapping
    public ResponseEntity<RespuestaDTO> crearOrden(
            @AuthenticationPrincipal String correo,
            @Valid @RequestBody CrearOrdenRequestDTO dto,
            HttpServletRequest request) {
        Usuario usuario = resolverUsuario(correo);
        String ip = request.getRemoteAddr();
        OrdenDTO orden = ordenService.crearOrden(usuario.getId(), dto, ip);
        return ResponseEntity.ok(RespuestaDTO.exito(orden));
    }

    /** HU-21: Cancela una orden pendiente. */
    @DeleteMapping("/{ordenId}")
    public ResponseEntity<RespuestaDTO> cancelarOrden(
            @AuthenticationPrincipal String correo,
            @PathVariable Long ordenId) {
        Usuario usuario = resolverUsuario(correo);
        boolean cancelada = ordenService.cancelarOrden(usuario.getId(), ordenId);
        if (cancelada) {
            return ResponseEntity.ok(RespuestaDTO.exito("Orden cancelada correctamente"));
        }
        return ResponseEntity.badRequest().body(RespuestaDTO.error("No es posible cancelar esta orden"));
    }

    /** HU-22: Órdenes activas (PENDIENTE, ENVIADA, EN_COLA). */
    @GetMapping("/activas")
    public ResponseEntity<RespuestaDTO> ordenesActivas(
            @AuthenticationPrincipal String correo) {
        Usuario usuario = resolverUsuario(correo);
        List<OrdenDTO> ordenes = ordenService.obtenerOrdenesActivas(usuario.getId());
        return ResponseEntity.ok(RespuestaDTO.exito(ordenes));
    }

    /** HU-24 a HU-26: Historial completo de órdenes. */
    @GetMapping("/historial")
    public ResponseEntity<RespuestaDTO> historial(
            @AuthenticationPrincipal String correo) {
        Usuario usuario = resolverUsuario(correo);
        List<OrdenDTO> ordenes = ordenService.obtenerHistorialOrdenes(usuario.getId());
        return ResponseEntity.ok(RespuestaDTO.exito(ordenes));
    }

    private Usuario resolverUsuario(String correo) {
        return usuarioRepo.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }
}
