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
import java.util.List;
import java.util.Locale;

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
        usuario.setInteresesMercado(String.join(",", normalizarIntereses(solicitud.getInteresesMercado())));
        usuario.setTelefono(solicitud.getTelefono());
        usuario.setTipoIdentificacion(solicitud.getTipoIdentificacion());
        usuario.setNumeroIdentificacion(solicitud.getNumeroIdentificacion());
        usuario.setFechaNacimiento(solicitud.getFechaNacimiento());
        usuario.setDireccion(solicitud.getDireccion());
        usuario.setCiudad(solicitud.getCiudad());
        usuario.setCodigoPostal(solicitud.getCodigoPostal());
        usuario.setPais(solicitud.getPais() != null ? solicitud.getPais() : "CO");
        usuario.setEstiloTrading(solicitud.getEstiloTrading());
        usuario.setRangoIngresos(solicitud.getRangoIngresos());
        usuario.setTipoOrdenDefault(
                solicitud.getTipoOrdenDefault() != null ? solicitud.getTipoOrdenDefault() : "MARKET");
        usuario.setVistaPortafolio(
                solicitud.getVistaPortafolio() != null ? solicitud.getVistaPortafolio() : "LISTA");
        usuario.setNotificacionEmail(solicitud.isNotificacionEmail());
        usuario.setNotificacionSms(solicitud.isNotificacionSms());
        usuario.setNotificacionWhatsapp(solicitud.isNotificacionWhatsapp());
        usuario.setSolicitaComisionista(solicitud.isSolicitaComisionista());
        usuario.setTiposNotificacion("ORDENES,MERCADO,SEGURIDAD");
        usuario.setPlanSuscripcion(plan);
        usuario.setMfaHabilitado(false);
        usuario.setPendienteCuentaAlpaca(false);
        usuario.setFechaCreacion(LocalDateTime.now());
        usuarioRepository.save(usuario);

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
