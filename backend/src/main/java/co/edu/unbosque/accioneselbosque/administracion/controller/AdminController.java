package co.edu.unbosque.accioneselbosque.administracion.controller;

import co.edu.unbosque.accioneselbosque.administracion.dto.*;
import co.edu.unbosque.accioneselbosque.administracion.service.AdministracionService;
import co.edu.unbosque.accioneselbosque.shared.dto.RespuestaDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdministracionService administracionService;

    public AdminController(AdministracionService administracionService) {
        this.administracionService = administracionService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<RespuestaDTO> dashboard(
            @AuthenticationPrincipal String correo,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(required = false) String mercado) {
        validar(correo);
        return ResponseEntity.ok(RespuestaDTO.exito(
                administracionService.obtenerDashboard(desde, hasta, mercado)));
    }

    @GetMapping("/mercados")
    public ResponseEntity<RespuestaDTO> mercados(@AuthenticationPrincipal String correo) {
        validar(correo);
        return ResponseEntity.ok(RespuestaDTO.exito(administracionService.listarMercados()));
    }

    @PutMapping("/mercados/{codigo}")
    public ResponseEntity<RespuestaDTO> guardarMercado(
            @AuthenticationPrincipal String correo,
            @PathVariable String codigo,
            @Valid @RequestBody MercadoConfigDTO dto) {
        validar(correo);
        return ResponseEntity.ok(RespuestaDTO.exito(
                administracionService.guardarMercado(codigo, dto, correo)));
    }

    @GetMapping("/mercados/{codigo}/feriados")
    public ResponseEntity<RespuestaDTO> feriados(
            @AuthenticationPrincipal String correo,
            @PathVariable String codigo) {
        validar(correo);
        return ResponseEntity.ok(RespuestaDTO.exito(administracionService.listarFeriados(codigo)));
    }

    @PostMapping("/mercados/{codigo}/feriados")
    public ResponseEntity<RespuestaDTO> crearFeriado(
            @AuthenticationPrincipal String correo,
            @PathVariable String codigo,
            @Valid @RequestBody FeriadoMercadoDTO dto) {
        validar(correo);
        return ResponseEntity.ok(RespuestaDTO.exito(
                administracionService.crearFeriado(codigo, dto, correo)));
    }

    @DeleteMapping("/mercados/{codigo}/feriados/{feriadoId}")
    public ResponseEntity<RespuestaDTO> eliminarFeriado(
            @AuthenticationPrincipal String correo,
            @PathVariable String codigo,
            @PathVariable Long feriadoId) {
        validar(correo);
        administracionService.eliminarFeriado(codigo, feriadoId, correo);
        return ResponseEntity.ok(RespuestaDTO.exito("Feriado eliminado"));
    }

    @GetMapping("/comisiones")
    public ResponseEntity<RespuestaDTO> comisiones(@AuthenticationPrincipal String correo) {
        validar(correo);
        return ResponseEntity.ok(RespuestaDTO.exito(administracionService.obtenerParametrosComision()));
    }

    @PutMapping("/comisiones")
    public ResponseEntity<RespuestaDTO> actualizarComisiones(
            @AuthenticationPrincipal String correo,
            @Valid @RequestBody ParametroComisionDTO dto) {
        validar(correo);
        return ResponseEntity.ok(RespuestaDTO.exito(
                administracionService.actualizarParametrosComision(dto, correo)));
    }

    @GetMapping("/usuarios")
    public ResponseEntity<RespuestaDTO> usuarios(
            @AuthenticationPrincipal String correo,
            @RequestParam(required = false) String rol) {
        validar(correo);
        return ResponseEntity.ok(RespuestaDTO.exito(administracionService.listarUsuarios(rol)));
    }

    @PostMapping("/comisionistas")
    public ResponseEntity<RespuestaDTO> crearComisionista(
            @AuthenticationPrincipal String correo,
            @Valid @RequestBody CrearComisionistaDTO dto) {
        validar(correo);
        return ResponseEntity.ok(RespuestaDTO.exito(administracionService.crearComisionista(dto, correo)));
    }

    @PutMapping("/inversionistas/{inversionistaId}/comisionista/{comisionistaId}")
    public ResponseEntity<RespuestaDTO> asignarComisionista(
            @AuthenticationPrincipal String correo,
            @PathVariable Long inversionistaId,
            @PathVariable Long comisionistaId) {
        validar(correo);
        return ResponseEntity.ok(RespuestaDTO.exito(
                administracionService.asignarComisionista(inversionistaId, comisionistaId, correo)));
    }

    @PutMapping("/usuarios/{usuarioId}/estado")
    public ResponseEntity<RespuestaDTO> cambiarEstado(
            @AuthenticationPrincipal String correo,
            @PathVariable Long usuarioId,
            @Valid @RequestBody CambiarEstadoCuentaDTO dto) {
        validar(correo);
        return ResponseEntity.ok(RespuestaDTO.exito(
                administracionService.cambiarEstadoUsuario(usuarioId, dto, correo)));
    }

    @DeleteMapping("/usuarios/{usuarioId}")
    public ResponseEntity<RespuestaDTO> eliminarUsuario(
            @AuthenticationPrincipal String correo,
            @PathVariable Long usuarioId) {
        validar(correo);
        administracionService.eliminarUsuario(usuarioId, correo);
        return ResponseEntity.ok(RespuestaDTO.exito("Cuenta dada de baja"));
    }

    private void validar(String correo) {
        administracionService.validarAdministrador(correo);
    }
}
