package co.edu.unbosque.accioneselbosque.autenticacion.service;

import co.edu.unbosque.accioneselbosque.autenticacion.dto.ConfirmarRegistroDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.dto.ConfirmarRegistroResponseDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.dto.RegistroInversionistaDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.interfaces.IAsignacionComisionista;
import co.edu.unbosque.accioneselbosque.autenticacion.model.CodigoVerificacion.TipoCodigo;
import co.edu.unbosque.accioneselbosque.autenticacion.model.EstadoCuenta;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Inversionista;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Rol;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Suscripcion;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Usuario;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.InversionistaRepository;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.SuscripcionRepository;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.UsuarioRepository;
import co.edu.unbosque.accioneselbosque.integracion.notificaciones.INotificacion;
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
import java.util.List;
import java.util.Locale;

@Service
public class RegistroService {

    private final UsuarioRepository usuarioRepository;
    private final InversionistaRepository inversionistaRepository;
    private final SuscripcionRepository suscripcionRepository;
    private final MFAService mfaService;
    private final INotificacion despachador;
    private final IAuditLog auditLog;
    private final PasswordEncoder passwordEncoder;
    private final OrquestadorRegistro orquestadorRegistro;
    private final OrquestadorSuscripcion orquestadorSuscripcion;
    private final IAsignacionComisionista asignacionComisionista;

    public RegistroService(
            UsuarioRepository usuarioRepository,
            InversionistaRepository inversionistaRepository,
            SuscripcionRepository suscripcionRepository,
            MFAService mfaService,
            INotificacion despachador,
            IAuditLog auditLog,
            PasswordEncoder passwordEncoder,
            OrquestadorRegistro orquestadorRegistro,
            OrquestadorSuscripcion orquestadorSuscripcion,
            IAsignacionComisionista asignacionComisionista) {
        this.usuarioRepository = usuarioRepository;
        this.inversionistaRepository = inversionistaRepository;
        this.suscripcionRepository = suscripcionRepository;
        this.mfaService = mfaService;
        this.despachador = despachador;
        this.auditLog = auditLog;
        this.passwordEncoder = passwordEncoder;
        this.orquestadorRegistro = orquestadorRegistro;
        this.orquestadorSuscripcion = orquestadorSuscripcion;
        this.asignacionComisionista = asignacionComisionista;
    }

    @Transactional
    public void iniciarRegistro(RegistroInversionistaDTO solicitud) {
        if (usuarioRepository.existsByCorreo(solicitud.getCorreo())) {
            throw new EmailAlreadyExistsException(solicitud.getCorreo());
        }

        String plan = solicitud.getPlanSuscripcion() != null
                ? solicitud.getPlanSuscripcion().toUpperCase()
                : "BASICO";

        List<String> interesesNormalizados = normalizarIntereses(solicitud.getInteresesMercado());

        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(solicitud.getNombreCompleto());
        usuario.setCorreo(solicitud.getCorreo());
        usuario.setContrasenia(passwordEncoder.encode(solicitud.getContrasenia()));
        usuario.setRol(Rol.INVERSIONISTA);
        usuario.setEstadoCuenta(EstadoCuenta.PENDIENTE_VERIFICACION);
        usuario.setMfaHabilitado(false);
        usuario.setTelefono(solicitud.getTelefono());
        usuario.setNumeroIdentificacion(solicitud.getNumeroIdentificacion());
        if (solicitud.getFechaNacimiento() != null && !solicitud.getFechaNacimiento().isBlank()) {
            try {
                usuario.setFechaNacimiento(java.time.LocalDate.parse(solicitud.getFechaNacimiento()));
            } catch (Exception ignored) {}
        }
        usuario.setNotificacionEmail(solicitud.isNotificacionEmail());
        usuario.setNotificacionSms(solicitud.isNotificacionSms());
        usuario.setNotificacionWhatsapp(solicitud.isNotificacionWhatsapp());
        usuario.setFechaCreacion(LocalDateTime.now());
        usuarioRepository.save(usuario);

        Inversionista inversionista = new Inversionista();
        inversionista.setId(usuario.getId());
        inversionista.setNivelExperiencia(solicitud.getNivelExperiencia());
        inversionista.setTipoIdentificacion(solicitud.getTipoIdentificacion());
        inversionista.setDireccion(solicitud.getDireccion());
        inversionista.setCiudad(solicitud.getCiudad());
        inversionista.setCodigoPostal(solicitud.getCodigoPostal());
        inversionista.setPais(solicitud.getPais() != null ? solicitud.getPais() : "CO");
        inversionista.setEstiloTrading(solicitud.getEstiloTrading());
        inversionista.setTipoOrdenDefault(
                solicitud.getTipoOrdenDefault() != null ? solicitud.getTipoOrdenDefault() : "MARKET");
        inversionista.setVistaPortafolio(
                solicitud.getVistaPortafolio() != null ? solicitud.getVistaPortafolio() : "LISTA");
        inversionista.setSolicitaComisionista(solicitud.isSolicitaComisionista());
        inversionista.setInteresesMercado(String.join(",", interesesNormalizados));
        inversionista.setFechaCreacion(LocalDateTime.now());
        inversionistaRepository.save(inversionista);

        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setInversionistaId(usuario.getId());
        suscripcion.setPlanSuscripcion(plan);
        suscripcion.setEsPremium(false);
        suscripcionRepository.save(suscripcion);

        String codigo = mfaService.generarYGuardarCodigo(solicitud.getCorreo(), TipoCodigo.REGISTRO);
        despachador.enviarCodigoRegistro(solicitud.getCorreo(), solicitud.getNombreCompleto(), codigo);
        auditLog.registrar(TipoEvento.REGISTRO_INICIADO, solicitud.getCorreo(), "Plan seleccionado: " + plan);
    }

    public boolean correoDisponible(String correo) {
        return correo != null && !correo.isBlank() && !usuarioRepository.existsByCorreo(correo.trim());
    }

    @Transactional
    public ConfirmarRegistroResponseDTO confirmarRegistro(ConfirmarRegistroDTO solicitud) {
        mfaService.validarCodigo(solicitud.getCorreo(), solicitud.getCodigo(), TipoCodigo.REGISTRO);

        Usuario usuario = usuarioRepository.findByCorreo(solicitud.getCorreo())
                .orElseThrow(() -> new UsuarioNoEncontradoException(solicitud.getCorreo()));

        Inversionista inversionista = inversionistaRepository.findById(usuario.getId())
                .orElseThrow(() -> new UsuarioNoEncontradoException(solicitud.getCorreo()));
        String plan = suscripcionRepository.findById(usuario.getId())
                .map(Suscripcion::getPlanSuscripcion)
                .orElse("BASICO");
        if (!plan.equalsIgnoreCase("BASICO")) {
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
        asignacionComisionista.asignarSiSolicitado(usuario);

        auditLog.registrar(TipoEvento.REGISTRO_EXITOSO, solicitud.getCorreo(), "Cuenta activada exitosamente");
        orquestadorRegistro.crearCuentaAlpaca(usuario);

        return new ConfirmarRegistroResponseDTO("Cuenta creada exitosamente");
    }

    private List<String> normalizarIntereses(List<String> intereses) {
        if (intereses == null || intereses.isEmpty()) {
            return List.of("AAPL", "MSFT", "TSLA");
        }
        List<String> normalizados = intereses.stream()
                .filter(interes -> interes != null && !interes.isBlank())
                .map(interes -> interes.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .limit(10)
                .toList();
        return normalizados.isEmpty() ? List.of("AAPL", "MSFT", "TSLA") : normalizados;
    }
}
