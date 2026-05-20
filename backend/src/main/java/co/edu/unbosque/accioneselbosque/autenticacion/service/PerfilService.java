package co.edu.unbosque.accioneselbosque.autenticacion.service;

import co.edu.unbosque.accioneselbosque.autenticacion.dto.ActualizarPerfilDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.dto.PerfilInversionistaDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.dto.PreferenciasNotificacionDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.dto.PreferenciasOperacionDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Rol;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Usuario;
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
    private final IAuditLog auditLog;

    public PerfilService(UsuarioRepository usuarioRepository, IAuditLog auditLog) {
        this.usuarioRepository = usuarioRepository;
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
        u.setNombreCompleto(dto.getNombreCompleto());
        u.setNivelExperiencia(dto.getNivelExperiencia());
        if (dto.getInteresesMercado() != null) {
            u.setInteresesMercado(String.join(",", dto.getInteresesMercado()));
        }
        if (dto.getTelefono() != null) {
            u.setTelefono(dto.getTelefono());
        }
        u.setFechaActualizacion(LocalDateTime.now());
        usuarioRepository.save(u);
        auditLog.registrar(TipoEvento.PERFIL_ACTUALIZADO, correo, "Datos personales actualizados");
    }

    // HU-08
    @Transactional
    public void actualizarPreferenciasNotificacion(String correo, PreferenciasNotificacionDTO dto) {
        Usuario u = buscarPorCorreo(correo);
        u.setNotificacionEmail(dto.isNotificacionEmail());
        u.setNotificacionSms(dto.isNotificacionSms());
        u.setNotificacionWhatsapp(dto.isNotificacionWhatsapp());
        if (dto.getTiposNotificacion() != null) {
            u.setTiposNotificacion(String.join(",", dto.getTiposNotificacion()));
        }
        u.setFechaActualizacion(LocalDateTime.now());
        usuarioRepository.save(u);
        auditLog.registrar(TipoEvento.PREFERENCIAS_NOTIFICACION_ACTUALIZADAS, correo,
                "Canales: email=" + dto.isNotificacionEmail() + " sms=" + dto.isNotificacionSms());
    }

    // HU-09
    @Transactional
    public void actualizarPreferenciasOperacion(String correo, PreferenciasOperacionDTO dto) {
        Usuario u = buscarPorCorreo(correo);
        u.setTipoOrdenDefault(dto.getTipoOrdenDefault());
        u.setVistaPortafolio(dto.getVistaPortafolio());
        u.setFechaActualizacion(LocalDateTime.now());
        usuarioRepository.save(u);
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

    private Usuario buscarPorCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsuarioNoEncontradoException(correo));
    }

    private PerfilInversionistaDTO mapearDTO(Usuario u) {
        PerfilInversionistaDTO dto = new PerfilInversionistaDTO();
        dto.setNombreCompleto(u.getNombreCompleto());
        dto.setCorreo(u.getCorreo());
        dto.setNivelExperiencia(u.getNivelExperiencia());
        dto.setTelefono(u.getTelefono());
        dto.setTipoIdentificacion(u.getTipoIdentificacion());
        dto.setNumeroIdentificacion(u.getNumeroIdentificacion());
        dto.setFechaNacimiento(u.getFechaNacimiento());
        dto.setDireccion(u.getDireccion());
        dto.setCiudad(u.getCiudad());
        dto.setCodigoPostal(u.getCodigoPostal());
        dto.setPais(u.getPais());
        dto.setEstiloTrading(u.getEstiloTrading());
        dto.setRangoIngresos(u.getRangoIngresos());
        dto.setSolicitaComisionista(u.isSolicitaComisionista());
        dto.setMfaHabilitado(u.isMfaHabilitado());
        dto.setPlanSuscripcion(u.getPlanSuscripcion() != null ? u.getPlanSuscripcion() : "BASICO");
        dto.setEsPremium(u.isEsPremium());
        dto.setNotificacionEmail(u.isNotificacionEmail());
        dto.setNotificacionSms(u.isNotificacionSms());
        dto.setNotificacionWhatsapp(u.isNotificacionWhatsapp());
        dto.setTipoOrdenDefault(u.getTipoOrdenDefault());
        dto.setVistaPortafolio(u.getVistaPortafolio());

        dto.setInteresesMercado(splitCsv(u.getInteresesMercado()));
        dto.setTiposNotificacion(splitCsv(u.getTiposNotificacion()));
        return dto;
    }

    private List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) return Collections.emptyList();
        return Arrays.asList(csv.split(","));
    }
}
