package co.edu.unbosque.accioneselbosque.autenticacion.service;

import co.edu.unbosque.accioneselbosque.autenticacion.dto.ActualizarPerfilDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.dto.PerfilInversionistaDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.dto.PreferenciasNotificacionDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.dto.PreferenciasOperacionDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.interfaces.IAsignacionComisionista;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Inversionista;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Rol;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Usuario;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.InversionistaRepository;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.UsuarioRepository;
import co.edu.unbosque.accioneselbosque.shared.exceptions.UsuarioNoEncontradoException;
import co.edu.unbosque.accioneselbosque.trazabilidad.interfaces.IAuditLog;
import co.edu.unbosque.accioneselbosque.trazabilidad.model.TipoEvento;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class PerfilService {

    private final UsuarioRepository usuarioRepository;
    private final InversionistaRepository inversionistaRepository;
    private final IAsignacionComisionista asignacionComisionista;
    private final IAuditLog auditLog;

    public PerfilService(UsuarioRepository usuarioRepository,
                         InversionistaRepository inversionistaRepository,
                         IAsignacionComisionista asignacionComisionista,
                         IAuditLog auditLog) {
        this.usuarioRepository = usuarioRepository;
        this.inversionistaRepository = inversionistaRepository;
        this.asignacionComisionista = asignacionComisionista;
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
        inversionista.setNivelExperiencia(dto.getNivelExperiencia());
        if (dto.getInteresesMercado() != null) {
            String intereses = String.join(",", dto.getInteresesMercado());
            inversionista.setInteresesMercado(intereses);
        }
        if (dto.getTelefono() != null) {
            inversionista.setTelefono(dto.getTelefono());
        }
        u.setFechaActualizacion(LocalDateTime.now());
        inversionista.setFechaActualizacion(LocalDateTime.now());
        usuarioRepository.save(u);
        inversionistaRepository.save(inversionista);
        auditLog.registrar(TipoEvento.PERFIL_ACTUALIZADO, correo, "Datos personales actualizados");
    }

    // HU-08
    @Transactional
    public void actualizarPreferenciasNotificacion(String correo, PreferenciasNotificacionDTO dto) {
        Usuario u = buscarPorCorreo(correo);
        Inversionista inversionista = obtenerOCrearInversionista(u);
        inversionista.setNotificacionEmail(dto.isNotificacionEmail());
        inversionista.setNotificacionSms(dto.isNotificacionSms());
        inversionista.setNotificacionWhatsapp(dto.isNotificacionWhatsapp());
        if (dto.getTiposNotificacion() != null) {
            String tipos = String.join(",", dto.getTiposNotificacion());
            inversionista.setTiposNotificacion(tipos);
        }
        u.setFechaActualizacion(LocalDateTime.now());
        inversionista.setFechaActualizacion(LocalDateTime.now());
        usuarioRepository.save(u);
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
        u.setFechaActualizacion(LocalDateTime.now());
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
        u.setFechaActualizacion(LocalDateTime.now());
        usuarioRepository.save(u);
        TipoEvento evento = activar ? TipoEvento.MFA_ACTIVADO : TipoEvento.MFA_DESACTIVADO;
        auditLog.registrar(evento, correo, "MFA " + (activar ? "activado" : "desactivado") + " por el usuario");
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
        inversionista.setFechaActualizacion(LocalDateTime.now());
        inversionistaRepository.save(inversionista);
        boolean asignado = asignacionComisionista.asignarSiSolicitado(u).isPresent();
        auditLog.registrar(asignado ? TipoEvento.COMISIONISTA_ASIGNADO : TipoEvento.COMISIONISTA_ASIGNACION_FALLIDA, correo,
                asignado ? "Solicitud de comisionista atendida desde perfil" : "Solicitud de comisionista registrada sin asignacion inmediata");
        return asignado;
    }

    private Usuario buscarPorCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsuarioNoEncontradoException(correo));
    }

    private PerfilInversionistaDTO mapearDTO(Usuario u) {
        Inversionista i = inversionistaRepository.findByUsuarioId(u.getId()).orElse(null);
        PerfilInversionistaDTO dto = new PerfilInversionistaDTO();
        dto.setNombreCompleto(u.getNombreCompleto());
        dto.setCorreo(u.getCorreo());
        dto.setNivelExperiencia(i != null ? i.getNivelExperiencia() : null);
        dto.setTelefono(i != null ? i.getTelefono() : null);
        dto.setTipoIdentificacion(i != null ? i.getTipoIdentificacion() : null);
        dto.setNumeroIdentificacion(i != null ? i.getNumeroIdentificacion() : null);
        dto.setFechaNacimiento(i != null ? i.getFechaNacimiento() : null);
        dto.setDireccion(i != null ? i.getDireccion() : null);
        dto.setCiudad(i != null ? i.getCiudad() : null);
        dto.setCodigoPostal(i != null ? i.getCodigoPostal() : null);
        dto.setPais(i != null ? i.getPais() : null);
        dto.setEstiloTrading(i != null ? i.getEstiloTrading() : null);
        dto.setRangoIngresos(i != null ? i.getRangoIngresos() : null);
        dto.setSolicitaComisionista(i != null && i.isSolicitaComisionista());
        dto.setComisionistaAsignado(asignacionComisionista.obtenerComisionistaAsignado(u.getId()).orElse(null));
        dto.setMfaHabilitado(u.isMfaHabilitado());
        dto.setPlanSuscripcion(i != null && i.getPlanSuscripcion() != null ? i.getPlanSuscripcion() : "BASICO");
        dto.setEsPremium(i != null && i.isEsPremium());
        dto.setNotificacionEmail(i == null || i.isNotificacionEmail());
        dto.setNotificacionSms(i != null && i.isNotificacionSms());
        dto.setNotificacionWhatsapp(i != null && i.isNotificacionWhatsapp());
        dto.setTipoOrdenDefault(i != null ? i.getTipoOrdenDefault() : null);
        dto.setVistaPortafolio(i != null ? i.getVistaPortafolio() : null);

        dto.setInteresesMercado(splitCsv(i != null ? i.getInteresesMercado() : null));
        dto.setTiposNotificacion(splitCsv(i != null ? i.getTiposNotificacion() : null));
        return dto;
    }

    private Inversionista obtenerOCrearInversionista(Usuario usuario) {
        return inversionistaRepository.findByUsuarioId(usuario.getId())
                .orElseGet(() -> {
                    Inversionista inversionista = new Inversionista();
                    inversionista.setUsuarioId(usuario.getId());
                    inversionista.setNivelExperiencia("PRINCIPIANTE");
                    inversionista.setInteresesMercado("AAPL,MSFT,TSLA");
                    inversionista.setPais("CO");
                    inversionista.setSolicitaComisionista(false);
                    inversionista.setNotificacionEmail(true);
                    inversionista.setNotificacionSms(false);
                    inversionista.setNotificacionWhatsapp(false);
                    inversionista.setTiposNotificacion("ORDENES,MERCADO,SEGURIDAD");
                    inversionista.setTipoOrdenDefault("MARKET");
                    inversionista.setVistaPortafolio("LISTA");
                    inversionista.setPlanSuscripcion("BASICO");
                    inversionista.setEsPremium(false);
                    inversionista.setPendienteCuentaAlpaca(false);
                    inversionista.setFechaCreacion(LocalDateTime.now());
                    return inversionista;
                });
    }

    private List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) return Collections.emptyList();
        return Arrays.asList(csv.split(","));
    }
}
