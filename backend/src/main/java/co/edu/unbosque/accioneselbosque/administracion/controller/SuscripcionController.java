package co.edu.unbosque.accioneselbosque.administracion.controller;

import co.edu.unbosque.accioneselbosque.autenticacion.model.Usuario;
import co.edu.unbosque.accioneselbosque.integracion.orquestadores.OrquestadorSuscripcion;
import co.edu.unbosque.accioneselbosque.shared.dto.RespuestaDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/suscripciones")
public class SuscripcionController {

    private final OrquestadorSuscripcion orquestadorSuscripcion;

    public SuscripcionController(OrquestadorSuscripcion orquestadorSuscripcion) {
        this.orquestadorSuscripcion = orquestadorSuscripcion;
    }

    @GetMapping("/confirmar-checkout")
    public ResponseEntity<RespuestaDTO> confirmarCheckout(@RequestParam("session_id") String sessionId) {
        Usuario usuario = orquestadorSuscripcion.confirmarPagoCheckout(sessionId);
        return ResponseEntity.ok(RespuestaDTO.exito(
                "Suscripcion premium activada para " + usuario.getCorreo()
        ));
    }
}
