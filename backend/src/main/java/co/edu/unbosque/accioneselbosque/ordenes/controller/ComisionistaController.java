package co.edu.unbosque.accioneselbosque.ordenes.controller;

import co.edu.unbosque.accioneselbosque.autenticacion.dto.ClienteAsignadoDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.interfaces.IAsignacionComisionista;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Rol;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Usuario;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.UsuarioRepository;
import co.edu.unbosque.accioneselbosque.ordenes.dto.CrearPropuestaOrdenDTO;
import co.edu.unbosque.accioneselbosque.ordenes.dto.OrdenDTO;
import co.edu.unbosque.accioneselbosque.ordenes.dto.PortafolioDTO;
import co.edu.unbosque.accioneselbosque.ordenes.interfaces.IOrden;
import co.edu.unbosque.accioneselbosque.shared.dto.RespuestaDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/comisionista")
public class ComisionistaController {

    private final UsuarioRepository usuarioRepo;
    private final IAsignacionComisionista asignacionComisionista;
    private final IOrden ordenService;

    public ComisionistaController(UsuarioRepository usuarioRepo,
                                  IAsignacionComisionista asignacionComisionista,
                                  IOrden ordenService) {
        this.usuarioRepo = usuarioRepo;
        this.asignacionComisionista = asignacionComisionista;
        this.ordenService = ordenService;
    }

    @GetMapping("/clientes")
    public ResponseEntity<RespuestaDTO> clientes(@AuthenticationPrincipal String correo) {
        Usuario comisionista = resolverComisionista(correo);
        List<ClienteAsignadoDTO> clientes = asignacionComisionista.listarClientesAsignados(comisionista.getId());
        return ResponseEntity.ok(RespuestaDTO.exito(clientes));
    }

    @GetMapping("/clientes/{clienteId}/portafolio")
    public ResponseEntity<RespuestaDTO> portafolioCliente(@AuthenticationPrincipal String correo,
                                                          @PathVariable Long clienteId) {
        Usuario comisionista = resolverComisionista(correo);
        asignacionComisionista.validarClienteAsignado(comisionista.getId(), clienteId);
        PortafolioDTO portafolio = ordenService.obtenerPortafolio(clienteId);
        return ResponseEntity.ok(RespuestaDTO.exito(portafolio));
    }

    @GetMapping("/clientes/{clienteId}/ordenes/activas")
    public ResponseEntity<RespuestaDTO> ordenesActivasCliente(@AuthenticationPrincipal String correo,
                                                              @PathVariable Long clienteId) {
        Usuario comisionista = resolverComisionista(correo);
        asignacionComisionista.validarClienteAsignado(comisionista.getId(), clienteId);
        return ResponseEntity.ok(RespuestaDTO.exito(ordenService.obtenerOrdenesActivas(clienteId)));
    }

    @GetMapping("/clientes/{clienteId}/ordenes/historial")
    public ResponseEntity<RespuestaDTO> historialCliente(@AuthenticationPrincipal String correo,
                                                         @PathVariable Long clienteId) {
        Usuario comisionista = resolverComisionista(correo);
        asignacionComisionista.validarClienteAsignado(comisionista.getId(), clienteId);
        return ResponseEntity.ok(RespuestaDTO.exito(ordenService.obtenerHistorialOrdenes(clienteId)));
    }

    @PostMapping("/clientes/{clienteId}/propuestas")
    public ResponseEntity<RespuestaDTO> proponerOrden(@AuthenticationPrincipal String correo,
                                                      @PathVariable Long clienteId,
                                                      @Valid @RequestBody CrearPropuestaOrdenDTO dto,
                                                      HttpServletRequest request) {
        Usuario comisionista = resolverComisionista(correo);
        OrdenDTO propuesta = ordenService.crearPropuestaOrden(comisionista.getId(), clienteId, dto, request.getRemoteAddr());
        return ResponseEntity.ok(RespuestaDTO.exito(propuesta));
    }

    @GetMapping("/propuestas/aprobadas")
    public ResponseEntity<RespuestaDTO> propuestasAprobadas(@AuthenticationPrincipal String correo) {
        Usuario comisionista = resolverComisionista(correo);
        return ResponseEntity.ok(RespuestaDTO.exito(ordenService.obtenerPropuestasAprobadasComisionista(comisionista.getId())));
    }

    @PostMapping("/propuestas/{propuestaId}/firmar-enviar")
    public ResponseEntity<RespuestaDTO> firmarEnviar(@AuthenticationPrincipal String correo,
                                                     @PathVariable Long propuestaId,
                                                     HttpServletRequest request) {
        Usuario comisionista = resolverComisionista(correo);
        OrdenDTO orden = ordenService.firmarYEnviarPropuesta(comisionista.getId(), propuestaId, request.getRemoteAddr());
        return ResponseEntity.ok(RespuestaDTO.exito(orden));
    }

    private Usuario resolverComisionista(String correo) {
        Usuario usuario = usuarioRepo.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (usuario.getRol() != Rol.COMISIONISTA) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo comisionistas pueden usar este modulo");
        }
        return usuario;
    }
}
