package co.edu.unbosque.accioneselbosque.autenticacion.controller;

import co.edu.unbosque.accioneselbosque.autenticacion.dto.RecuperarPasswordDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.dto.ResetPasswordDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.service.RecuperacionPasswordService;
import co.edu.unbosque.accioneselbosque.shared.dto.RespuestaDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class RecuperacionController {

    private final RecuperacionPasswordService recuperacionService;

    public RecuperacionController(RecuperacionPasswordService recuperacionService) {
        this.recuperacionService = recuperacionService;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<RespuestaDTO> solicitarRecuperacion(
            @Valid @RequestBody RecuperarPasswordDTO solicitud) {
        recuperacionService.solicitarRecuperacion(solicitud.getCorreo());
        return ResponseEntity.ok(new RespuestaDTO("Código de recuperación enviado a " + solicitud.getCorreo()));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<RespuestaDTO> resetearPassword(
            @Valid @RequestBody ResetPasswordDTO solicitud) {
        recuperacionService.resetearPassword(solicitud.getCorreo(), solicitud.getToken(), solicitud.getNuevaContrasenia());
        return ResponseEntity.ok(new RespuestaDTO("Contraseña restablecida exitosamente"));
    }
}
