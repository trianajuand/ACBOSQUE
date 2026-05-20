package co.edu.unbosque.accioneselbosque.autenticacion.controller;

import co.edu.unbosque.accioneselbosque.autenticacion.dto.ActualizarPerfilDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.dto.PerfilInversionistaDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.dto.PreferenciasNotificacionDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.dto.PreferenciasOperacionDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.service.PerfilService;
import co.edu.unbosque.accioneselbosque.shared.dto.RespuestaDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/perfil")
public class PerfilController {

    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    @GetMapping
    public ResponseEntity<PerfilInversionistaDTO> obtenerPerfil() {
        return ResponseEntity.ok(perfilService.obtenerPerfil(correoAutenticado()));
    }

    @PutMapping
    public ResponseEntity<RespuestaDTO> actualizarPerfil(@Valid @RequestBody ActualizarPerfilDTO dto) {
        perfilService.actualizarDatos(correoAutenticado(), dto);
        return ResponseEntity.ok(new RespuestaDTO("Datos actualizados exitosamente"));
    }

    @PutMapping("/preferencias/notificaciones")
    public ResponseEntity<RespuestaDTO> actualizarNotificaciones(
            @RequestBody PreferenciasNotificacionDTO dto) {
        perfilService.actualizarPreferenciasNotificacion(correoAutenticado(), dto);
        return ResponseEntity.ok(new RespuestaDTO("Preferencias de notificación actualizadas"));
    }

    @PutMapping("/preferencias/operacion")
    public ResponseEntity<RespuestaDTO> actualizarOperacion(
            @RequestBody PreferenciasOperacionDTO dto) {
        perfilService.actualizarPreferenciasOperacion(correoAutenticado(), dto);
        return ResponseEntity.ok(new RespuestaDTO("Preferencias de operación actualizadas"));
    }

    @PutMapping("/mfa")
    public ResponseEntity<RespuestaDTO> toggleMfa(@RequestParam boolean activar) {
        perfilService.toggleMfa(correoAutenticado(), activar);
        String msg = activar ? "MFA activado exitosamente" : "MFA desactivado exitosamente";
        return ResponseEntity.ok(new RespuestaDTO(msg));
    }

    private String correoAutenticado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
