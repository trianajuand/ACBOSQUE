package co.edu.unbosque.accioneselbosque.ordenes.controller;

import co.edu.unbosque.accioneselbosque.autenticacion.model.Inversionista;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Usuario;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.InversionistaRepository;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.UsuarioRepository;
import co.edu.unbosque.accioneselbosque.ordenes.dto.PortafolioDTO;
import co.edu.unbosque.accioneselbosque.ordenes.dto.SaldoDTO;
import co.edu.unbosque.accioneselbosque.ordenes.interfaces.IOrden;
import co.edu.unbosque.accioneselbosque.ordenes.service.SaldoService;
import co.edu.unbosque.accioneselbosque.shared.dto.RespuestaDTO;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/portafolio")
public class PortafolioController {

    private final IOrden ordenService;
    private final SaldoService saldoService;
    private final UsuarioRepository usuarioRepo;
    private final InversionistaRepository inversionistaRepo;

    public PortafolioController(IOrden ordenService, SaldoService saldoService,
                                 UsuarioRepository usuarioRepo,
                                 InversionistaRepository inversionistaRepo) {
        this.ordenService = ordenService;
        this.saldoService = saldoService;
        this.usuarioRepo = usuarioRepo;
        this.inversionistaRepo = inversionistaRepo;
    }

    /** HU-15: Portafolio con holdings y ganancia/pérdida. */
    @GetMapping
    public ResponseEntity<RespuestaDTO> portafolio(
            @AuthenticationPrincipal String correo) {
        Usuario usuario = resolverUsuario(correo);
        PortafolioDTO portafolio = ordenService.obtenerPortafolio(usuario.getId());
        return ResponseEntity.ok(RespuestaDTO.exito(portafolio));
    }

    /** HU-16: Saldo disponible, fondos reservados y comisiones. */
    @GetMapping("/saldo")
    public ResponseEntity<RespuestaDTO> saldo(
            @AuthenticationPrincipal String correo) {
        Usuario usuario = resolverUsuario(correo);
        SaldoDTO saldo = ordenService.obtenerSaldo(usuario.getId());
        return ResponseEntity.ok(RespuestaDTO.exito(saldo));
    }

    /**
     * Endpoint de prueba/admin para depositar fondos simulados.
     * En producción esto sería reemplazado por una integración real de funding.
     */
    @PostMapping("/depositar")
    public ResponseEntity<RespuestaDTO> depositar(
            @AuthenticationPrincipal String correo,
            @RequestParam @DecimalMin("0.01") BigDecimal monto) {
        Usuario usuario = resolverUsuario(correo);
        saldoService.depositar(usuario.getId(), monto);
        return ResponseEntity.ok(RespuestaDTO.exito("Fondos depositados: " + monto));
    }

    /**
     * Sincroniza el saldo con Alpaca (útil al inicio de sesión).
     */
    @PostMapping("/sincronizar")
    public ResponseEntity<RespuestaDTO> sincronizar(
            @AuthenticationPrincipal String correo) {
        Usuario usuario = resolverUsuario(correo);
        Inversionista inversionista = inversionistaRepo.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new RuntimeException("Inversionista no encontrado"));
        saldoService.sincronizarConAlpaca(usuario.getId(), inversionista.getAlpacaAccountId());
        SaldoDTO saldo = ordenService.obtenerSaldo(usuario.getId());
        return ResponseEntity.ok(RespuestaDTO.exito(saldo));
    }

    private Usuario resolverUsuario(String correo) {
        return usuarioRepo.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }
}
