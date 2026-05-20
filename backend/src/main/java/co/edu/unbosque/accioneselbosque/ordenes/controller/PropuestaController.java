package co.edu.unbosque.accioneselbosque.ordenes.controller;

import co.edu.unbosque.accioneselbosque.autenticacion.model.Rol;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Usuario;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.UsuarioRepository;
import co.edu.unbosque.accioneselbosque.ordenes.dto.DecisionPropuestaDTO;
import co.edu.unbosque.accioneselbosque.ordenes.dto.OrdenDTO;
import co.edu.unbosque.accioneselbosque.ordenes.interfaces.IOrden;
import co.edu.unbosque.accioneselbosque.shared.dto.RespuestaDTO;
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

@RestController
@RequestMapping("/api/propuestas")
public class PropuestaController {

    private final UsuarioRepository usuarioRepo;
    private final IOrden ordenService;

    public PropuestaController(UsuarioRepository usuarioRepo, IOrden ordenService) {
        this.usuarioRepo = usuarioRepo;
        this.ordenService = ordenService;
    }

    @GetMapping
    public ResponseEntity<RespuestaDTO> propuestasPendientes(@AuthenticationPrincipal String correo) {
        Usuario inversionista = resolverInversionista(correo);
        return ResponseEntity.ok(RespuestaDTO.exito(
                ordenService.obtenerPropuestasPendientesInversionista(inversionista.getId())));
    }

    @PostMapping("/{propuestaId}/aprobar")
    public ResponseEntity<RespuestaDTO> aprobar(@AuthenticationPrincipal String correo,
                                                @PathVariable Long propuestaId,
                                                @RequestBody(required = false) DecisionPropuestaDTO dto) {
        Usuario inversionista = resolverInversionista(correo);
        String comentario = dto != null ? dto.getComentario() : null;
        OrdenDTO propuesta = ordenService.aprobarPropuesta(inversionista.getId(), propuestaId, comentario);
        return ResponseEntity.ok(RespuestaDTO.exito(propuesta));
    }

    @PostMapping("/{propuestaId}/rechazar")
    public ResponseEntity<RespuestaDTO> rechazar(@AuthenticationPrincipal String correo,
                                                 @PathVariable Long propuestaId,
                                                 @RequestBody(required = false) DecisionPropuestaDTO dto) {
        Usuario inversionista = resolverInversionista(correo);
        String comentario = dto != null ? dto.getComentario() : null;
        OrdenDTO propuesta = ordenService.rechazarPropuesta(inversionista.getId(), propuestaId, comentario);
        return ResponseEntity.ok(RespuestaDTO.exito(propuesta));
    }

    private Usuario resolverInversionista(String correo) {
        Usuario usuario = usuarioRepo.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (usuario.getRol() != Rol.INVERSIONISTA && usuario.getRol() != Rol.INVERSIONISTA_PREMIUM) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo inversionistas pueden decidir propuestas");
        }
        return usuario;
    }
}
