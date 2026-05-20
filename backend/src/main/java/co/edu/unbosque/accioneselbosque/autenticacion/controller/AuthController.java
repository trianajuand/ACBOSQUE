package co.edu.unbosque.accioneselbosque.autenticacion.controller;

import co.edu.unbosque.accioneselbosque.autenticacion.dto.LoginRequestDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.dto.LoginResponseDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.dto.MFARequestDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.interfaces.IAutenticacion;
import co.edu.unbosque.accioneselbosque.shared.dto.RespuestaDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final IAutenticacion autenticacion;

    public AuthController(IAutenticacion autenticacion) {
        this.autenticacion = autenticacion;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO solicitud) {
        return ResponseEntity.ok(autenticacion.iniciarSesion(solicitud));
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<LoginResponseDTO> verificarMfa(@Valid @RequestBody MFARequestDTO solicitud) {
        return ResponseEntity.ok(autenticacion.verificarMfa(solicitud));
    }

    @PostMapping("/logout")
    public ResponseEntity<RespuestaDTO> logout(
            @RequestHeader("Authorization") String authorizationHeader) {
        autenticacion.cerrarSesion(authorizationHeader);
        return ResponseEntity.ok(new RespuestaDTO("Sesión cerrada exitosamente"));
    }
}
