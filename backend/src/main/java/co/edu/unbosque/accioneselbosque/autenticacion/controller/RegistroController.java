package co.edu.unbosque.accioneselbosque.autenticacion.controller;

import co.edu.unbosque.accioneselbosque.autenticacion.dto.ConfirmarRegistroDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.dto.ConfirmarRegistroResponseDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.dto.CorreoDisponibleDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.dto.RegistroInversionistaDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.service.RegistroService;
import co.edu.unbosque.accioneselbosque.shared.dto.RespuestaDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/auth")
public class RegistroController {

    private final RegistroService registroService;

    public RegistroController(RegistroService registroService) {
        this.registroService = registroService;
    }

    @GetMapping("/register/email-disponible")
    public ResponseEntity<CorreoDisponibleDTO> verificarCorreoDisponible(
            @RequestParam @NotBlank @Email String correo) {
        return ResponseEntity.ok(new CorreoDisponibleDTO(
                correo,
                registroService.correoDisponible(correo)));
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
