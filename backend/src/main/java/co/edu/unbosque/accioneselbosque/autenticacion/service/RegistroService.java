package co.edu.unbosque.accioneselbosque.autenticacion.service;

import co.edu.unbosque.accioneselbosque.autenticacion.dto.ConfirmarRegistroDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.dto.ConfirmarRegistroResponseDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.dto.RegistroInversionistaDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.model.CodigoVerificacion.TipoCodigo;
import co.edu.unbosque.accioneselbosque.autenticacion.model.EstadoCuenta;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Rol;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Usuario;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.UsuarioRepository;
import co.edu.unbosque.accioneselbosque.integracion.notificaciones.DespachadorNotificaciones;
import co.edu.unbosque.accioneselbosque.integracion.orquestadores.OrquestadorRegistro;
import co.edu.unbosque.accioneselbosque.integracion.orquestadores.OrquestadorSuscripcion;
import co.edu.unbosque.accioneselbosque.shared.exceptions.EmailAlreadyExistsException;
import co.edu.unbosque.accioneselbosque.shared.exceptions.StripeCheckoutException;
import co.edu.unbosque.accioneselbosque.shared.exceptions.UsuarioNoEncontradoException;
import co.edu.unbosque.accioneselbosque.trazabilidad.interfaces.IAuditLog;
import co.edu.unbosque.accioneselbosque.trazabilidad.model.TipoEvento;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class RegistroService {

    private final UsuarioRepository usuarioRepository;
    private final MFAService mfaService;
    private final DespachadorNotificaciones despachador;
    private final IAuditLog auditLog;
    private final PasswordEncoder passwordEncoder;
    private final OrquestadorRegistro orquestadorRegistro;
    private final OrquestadorSuscripcion orquestadorSuscripcion;

    public RegistroService(
            UsuarioRepository usuarioRepository,
            MFAService mfaService,
            DespachadorNotificaciones despachador,
            IAuditLog auditLog,
            PasswordEncoder passwordEncoder,
            OrquestadorRegistro orquestadorRegistro,
            OrquestadorSuscripcion orquestadorSuscripcion) {
        this.usuarioRepository = usuarioRepository;
        this.mfaService = mfaService;
        this.despachador = despachador;
        this.auditLog = auditLog;
        this.passwordEncoder = passwordEncoder;
        this.orquestadorRegistro = orquestadorRegistro;
        this.orquestadorSuscripcion = orquestadorSuscripcion;
    }

    @Transactional
    public void iniciarRegistro(RegistroInversionistaDTO solicitud) {
        if (usuarioRepository.existsByCorreo(solicitud.getCorreo())) {
            throw new EmailAlreadyExistsException(solicitud.getCorreo());
        }

        String plan = solicitud.getPlanSuscripcion() != null
                ? solicitud.getPlanSuscripcion().toUpperCase()
                : "BASICO";

        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(solicitud.getNombreCompleto());
        usuario.setCorreo(solicitud.getCorreo());
        usuario.setContrasenia(passwordEncoder.encode(solicitud.getContrasenia()));
        usuario.setRol(Rol.INVERSIONISTA);
        usuario.setEstadoCuenta(EstadoCuenta.PENDIENTE_VERIFICACION);
        usuario.setNivelExperiencia(solicitud.getNivelExperiencia());
        usuario.setInteresesMercado(
                solicitud.getInteresesMercado() != null
                        ? String.join(",", solicitud.getInteresesMercado())
                        : null);
        usuario.setTelefono(solicitud.getTelefono());
        usuario.setPlanSuscripcion(plan);
        usuario.setMfaHabilitado(false);
        usuario.setNotificacionEmail(true);
        usuario.setPendienteCuentaAlpaca(false);
        usuario.setFechaCreacion(LocalDateTime.now());
        usuarioRepository.save(usuario);

        String codigo = mfaService.generarYGuardarCodigo(solicitud.getCorreo(), TipoCodigo.REGISTRO);
        despachador.enviarCodigoRegistro(solicitud.getCorreo(), solicitud.getNombreCompleto(), codigo);
        auditLog.registrar(TipoEvento.REGISTRO_INICIADO, solicitud.getCorreo(), "Plan seleccionado: " + plan);
    }

    @Transactional
    public ConfirmarRegistroResponseDTO confirmarRegistro(ConfirmarRegistroDTO solicitud) {
        mfaService.validarCodigo(solicitud.getCorreo(), solicitud.getCodigo(), TipoCodigo.REGISTRO);

        Usuario usuario = usuarioRepository.findByCorreo(solicitud.getCorreo())
                .orElseThrow(() -> new UsuarioNoEncontradoException(solicitud.getCorreo()));

        String plan = usuario.getPlanSuscripcion();
        if (plan != null && !plan.equalsIgnoreCase("BASICO")) {
            try {
                String checkoutUrl = orquestadorSuscripcion.iniciarSuscripcion(usuario);
                return new ConfirmarRegistroResponseDTO(
                        "Correo verificado. Completa el pago para activar tu cuenta premium.", checkoutUrl);
            } catch (Exception e) {
                auditLog.registrar(TipoEvento.SUSCRIPCION_PREMIUM_FALLIDA, solicitud.getCorreo(),
                        "Error al crear sesion Stripe: " + e.getMessage());
                throw new StripeCheckoutException(
                        "No se pudo iniciar el pago premium. Revisa la configuracion de Stripe e intenta de nuevo.",
                        e);
            }
        }

        usuario.setEstadoCuenta(EstadoCuenta.ACTIVA);
        usuarioRepository.save(usuario);

        auditLog.registrar(TipoEvento.REGISTRO_EXITOSO, solicitud.getCorreo(), "Cuenta activada exitosamente");
        orquestadorRegistro.crearCuentaAlpaca(usuario);

        return new ConfirmarRegistroResponseDTO("Cuenta creada exitosamente");
    }
}
