package co.edu.unbosque.accioneselbosque.autenticacion.controller;

import co.edu.unbosque.accioneselbosque.autenticacion.dto.ConfirmarRegistroDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.dto.ConfirmarRegistroResponseDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.dto.RegistroInversionistaDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.service.RegistroService;
import co.edu.unbosque.accioneselbosque.shared.dto.RespuestaDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class RegistroController {

    private final RegistroService registroService;

    public RegistroController(RegistroService registroService) {
        this.registroService = registroService;
    }

    @PostMapping("/register/investor")
    public ResponseEntity<RespuestaDTO> registrarInversionista(
            @Valid @RequestBody RegistroInversionistaDTO solicitud) {
        registroService.iniciarRegistro(solicitud);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RespuestaDTO("Código de verificación enviado a " + solicitud.getCorreo()));
    }

    @PostMapping("/register/confirm")
    public ResponseEntity<ConfirmarRegistroResponseDTO> confirmarRegistro(
            @Valid @RequestBody ConfirmarRegistroDTO solicitud) {
        return ResponseEntity.ok(registroService.confirmarRegistro(solicitud));
    }
}
