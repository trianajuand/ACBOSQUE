package co.edu.unbosque.accioneselbosque.autenticacion.service;

import co.edu.unbosque.accioneselbosque.autenticacion.dto.ActualizarPerfilDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.dto.PerfilInversionistaDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.dto.PreferenciasNotificacionDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.dto.PreferenciasOperacionDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.interfaces.IAsignacionComisionista;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Inversionista;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Rol;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Suscripcion;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Usuario;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.InversionistaRepository;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.SuscripcionRepository;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.UsuarioRepository;
import co.edu.unbosque.accioneselbosque.integracion.adaptadores.stripe.IIntegracionStripe;
import co.edu.unbosque.accioneselbosque.integracion.orquestadores.OrquestadorSuscripcion;
import co.edu.unbosque.accioneselbosque.shared.exceptions.UsuarioNoEncontradoException;
import co.edu.unbosque.accioneselbosque.trazabilidad.interfaces.IAuditLog;
import co.edu.unbosque.accioneselbosque.trazabilidad.model.TipoEvento;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class PerfilService {

    private final UsuarioRepository usuarioRepository;
    private final InversionistaRepository inversionistaRepository;
    private final SuscripcionRepository suscripcionRepository;
    private final IAsignacionComisionista asignacionComisionista;
    private final IIntegracionStripe stripe;
    private final OrquestadorSuscripcion orquestadorSuscripcion;
    private final IAuditLog auditLog;

    public PerfilService(UsuarioRepository usuarioRepository,
                         InversionistaRepository inversionistaRepository,
                         SuscripcionRepository suscripcionRepository,
                         IAsignacionComisionista asignacionComisionista,
                         IIntegracionStripe stripe,
                         OrquestadorSuscripcion orquestadorSuscripcion,
                         IAuditLog auditLog) {
        this.usuarioRepository = usuarioRepository;
        this.inversionistaRepository = inversionistaRepository;
        this.suscripcionRepository = suscripcionRepository;
        this.asignacionComisionista = asignacionComisionista;
        this.stripe = stripe;
        this.orquestadorSuscripcion = orquestadorSuscripcion;
        this.auditLog = auditLog;
    }

    // HU-06
    public PerfilInversionistaDTO obtenerPerfil(String correo) {
        Usuario u = buscarPorCorreo(correo);
        auditLog.registrar(TipoEvento.PERFIL_CONSULTADO, correo, "Perfil consultado");
        return mapearDTO(u);
    }

    // HU-07
    @Transactional
    public void actualizarDatos(String correo, ActualizarPerfilDTO dto) {
        Usuario u = buscarPorCorreo(correo);
        Inversionista inversionista = obtenerOCrearInversionista(u);
        u.setNombreCompleto(dto.getNombreCompleto());
        if (dto.getTelefono() != null) {
            u.setTelefono(dto.getTelefono());
        }
        inversionista.setNivelExperiencia(dto.getNivelExperiencia());
        if (dto.getInteresesMercado() != null && !dto.getInteresesMercado().isEmpty()) {
            inversionista.setInteresesMercado(String.join(",", dto.getInteresesMercado()));
        }
        inversionista.setFechaActualizacion(LocalDateTime.now());
        usuarioRepository.save(u);
        inversionistaRepository.save(inversionista);
        auditLog.registrar(TipoEvento.PERFIL_ACTUALIZADO, correo, "Datos personales actualizados");
    }

    // HU-08
    @Transactional
    public void actualizarPreferenciasNotificacion(String correo, PreferenciasNotificacionDTO dto) {
        Usuario u = buscarPorCorreo(correo);
        u.setNotificacionesActivas(dto.isNotificacionesActivas());
        u.setNotificacionEmail(dto.isNotificacionEmail());
        u.setNotificacionSms(dto.isNotificacionSms());
        u.setNotificacionWhatsapp(dto.isNotificacionWhatsapp());
        usuarioRepository.save(u);

        Inversionista inversionista = obtenerOCrearInversionista(u);
        if (dto.getTiposNotificacion() != null && !dto.getTiposNotificacion().isEmpty()) {
            inversionista.setTipoNotificacion(String.join(",", dto.getTiposNotificacion()));
        }
        inversionista.setFechaActualizacion(LocalDateTime.now());
        inversionistaRepository.save(inversionista);

        auditLog.registrar(TipoEvento.PREFERENCIAS_NOTIFICACION_ACTUALIZADAS, correo,
                "Canales: email=" + dto.isNotificacionEmail() + " sms=" + dto.isNotificacionSms());
    }

    // HU-09
    @Transactional
    public void actualizarPreferenciasOperacion(String correo, PreferenciasOperacionDTO dto) {
        Usuario u = buscarPorCorreo(correo);
        Inversionista inversionista = obtenerOCrearInversionista(u);
        inversionista.setTipoOrdenDefault(dto.getTipoOrdenDefault());
        inversionista.setVistaPortafolio(dto.getVistaPortafolio());
        inversionista.setFechaActualizacion(LocalDateTime.now());
        usuarioRepository.save(u);
        inversionistaRepository.save(inversionista);
        auditLog.registrar(TipoEvento.PREFERENCIAS_OPERACION_ACTUALIZADAS, correo,
                "Orden default=" + dto.getTipoOrdenDefault() + " vista=" + dto.getVistaPortafolio());
    }

    // HU-10
    @Transactional
    public void toggleMfa(String correo, boolean activar) {
        Usuario u = buscarPorCorreo(correo);
        if (u.getRol() != Rol.INVERSIONISTA) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo los inversionistas regulares pueden modificar el MFA opcional");
        }
        u.setMfaHabilitado(activar);
        usuarioRepository.save(u);
        TipoEvento evento = activar ? TipoEvento.MFA_ACTIVADO : TipoEvento.MFA_DESACTIVADO;
        auditLog.registrar(evento, correo, "MFA " + (activar ? "activado" : "desactivado") + " por el usuario");
    }

    // HU-11 cancelacion
    @Transactional
    public void cancelarSuscripcion(String correo) {
        Usuario u = buscarPorCorreo(correo);
        Suscripcion suscripcion = suscripcionRepository.findById(u.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No tienes una suscripcion premium activa"));
        if (!suscripcion.isEsPremium()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No tienes una suscripcion premium activa");
        }
        if (suscripcion.getStripeSuscripcionId() != null) {
            try {
                stripe.cancelarSuscripcion(suscripcion.getStripeSuscripcionId());
            } catch (Exception ignored) {}
            suscripcion.setStripeSuscripcionId(null);
        }
        suscripcion.setEsPremium(false);
        suscripcion.setPlanSuscripcion("BASICO");
        suscripcion.setFechaExpiracionPremium(null);
        suscripcionRepository.save(suscripcion);
        auditLog.registrar(TipoEvento.SUSCRIPCION_PREMIUM_CANCELADA, correo,
                "Suscripcion premium cancelada por el usuario");
    }

    @Transactional
    public boolean solicitarComisionista(String correo) {
        Usuario u = buscarPorCorreo(correo);
        if (u.getRol() != Rol.INVERSIONISTA && u.getRol() != Rol.INVERSIONISTA_PREMIUM) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo los inversionistas pueden solicitar comisionista");
        }
        Inversionista inversionista = obtenerOCrearInversionista(u);
        inversionista.setSolicitaComisionista(true);
        inversionistaRepository.save(inversionista);
        boolean asignado = asignacionComisionista.asignarSiSolicitado(u).isPresent();
        auditLog.registrar(asignado ? TipoEvento.COMISIONISTA_ASIGNADO : TipoEvento.COMISIONISTA_ASIGNACION_FALLIDA, correo,
                asignado ? "Solicitud de comisionista atendida desde perfil" : "Solicitud de comisionista registrada sin asignacion inmediata");
        return asignado;
    }

    @Transactional
    public String iniciarUpgradePremium(String correo, String plan) {
        Usuario u = buscarPorCorreo(correo);
        Suscripcion suscripcion = suscripcionRepository.findById(u.getId()).orElseGet(() -> {
            Suscripcion s = new Suscripcion();
            s.setInversionistaId(u.getId());
            return s;
        });
        if (suscripcion.isEsPremium()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya tienes una suscripcion premium activa");
        }
        String planNorm = plan != null ? plan.toUpperCase() : "PREMIUM_MENSUAL";
        suscripcion.setPlanSuscripcion(planNorm);
        suscripcionRepository.save(suscripcion);
        String url = orquestadorSuscripcion.iniciarSuscripcion(u);
        auditLog.registrar(TipoEvento.SUSCRIPCION_PREMIUM_INICIADA, correo, "Upgrade iniciado: " + planNorm);
        return url;
    }

    private Usuario buscarPorCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsuarioNoEncontradoException(correo));
    }

    private PerfilInversionistaDTO mapearDTO(Usuario u) {
        Inversionista i = inversionistaRepository.findById(u.getId()).orElse(null);
        Suscripcion s = suscripcionRepository.findById(u.getId()).orElse(null);

        PerfilInversionistaDTO dto = new PerfilInversionistaDTO();
        dto.setNombreCompleto(u.getNombreCompleto());
        dto.setCorreo(u.getCorreo());
        dto.setNivelExperiencia(i != null ? i.getNivelExperiencia() : null);
        dto.setTelefono(u.getTelefono());
        dto.setTipoIdentificacion(i != null ? i.getTipoIdentificacion() : u.getTipoIdentificacion());
        dto.setNumeroIdentificacion(u.getNumeroIdentificacion());
        dto.setFechaNacimiento(u.getFechaNacimiento() != null ? u.getFechaNacimiento().toString() : null);
        dto.setDireccion(i != null ? i.getDireccion() : null);
        dto.setCiudad(i != null ? i.getCiudad() : null);
        dto.setCodigoPostal(i != null ? i.getCodigoPostal() : null);
        dto.setPais(i != null ? i.getPais() : null);
        dto.setEstiloTrading(i != null ? i.getEstiloTrading() : null);
        dto.setSolicitaComisionista(i != null && i.isSolicitaComisionista());
        dto.setComisionistaAsignado(asignacionComisionista.obtenerComisionistaAsignado(u.getId()).orElse(null));
        dto.setMfaHabilitado(u.isMfaHabilitado());
        dto.setPlanSuscripcion(s != null && s.getPlanSuscripcion() != null ? s.getPlanSuscripcion() : "BASICO");
        dto.setEsPremium(s != null && s.isEsPremium());
        dto.setNotificacionesActivas(u.isNotificacionesActivas());
        dto.setNotificacionEmail(u.isNotificacionEmail());
        dto.setNotificacionSms(u.isNotificacionSms());
        dto.setNotificacionWhatsapp(u.isNotificacionWhatsapp());
        dto.setTipoOrdenDefault(i != null ? i.getTipoOrdenDefault() : null);
        dto.setVistaPortafolio(i != null ? i.getVistaPortafolio() : null);
        String tipoNotif = i != null ? i.getTipoNotificacion() : null;
        dto.setTiposNotificacion(tipoNotif != null ? List.of(tipoNotif.split(",")) : Collections.emptyList());
        String intereses = i != null ? i.getInteresesMercado() : null;
        dto.setInteresesMercado(intereses != null && !intereses.isBlank()
                ? List.of(intereses.split(","))
                : Collections.emptyList());
        return dto;
    }

    private Inversionista obtenerOCrearInversionista(Usuario usuario) {
        return inversionistaRepository.findById(usuario.getId())
                .orElseGet(() -> {
                    Inversionista inversionista = new Inversionista();
                    inversionista.setId(usuario.getId());
                    inversionista.setNivelExperiencia("PRINCIPIANTE");
                    inversionista.setPais("CO");
                    inversionista.setSolicitaComisionista(false);
                    inversionista.setTipoOrdenDefault("MARKET");
                    inversionista.setVistaPortafolio("LISTA");
                    inversionista.setFechaCreacion(LocalDateTime.now());
                    return inversionista;
                });
    }
}
