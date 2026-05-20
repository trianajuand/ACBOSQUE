package co.edu.unbosque.accioneselbosque.autenticacion.service;

import co.edu.unbosque.accioneselbosque.autenticacion.model.CodigoVerificacion.TipoCodigo;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Usuario;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.CodigoVerificacionRepository;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.UsuarioRepository;
import co.edu.unbosque.accioneselbosque.integracion.notificaciones.DespachadorNotificaciones;
import co.edu.unbosque.accioneselbosque.shared.exceptions.InvalidTokenException;
import co.edu.unbosque.accioneselbosque.shared.exceptions.UsuarioNoEncontradoException;
import co.edu.unbosque.accioneselbosque.trazabilidad.interfaces.IAuditLog;
import co.edu.unbosque.accioneselbosque.trazabilidad.model.TipoEvento;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unbosque.accioneselbosque.autenticacion.model.CodigoVerificacion;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class RecuperacionPasswordService {

    private final UsuarioRepository usuarioRepository;
    private final CodigoVerificacionRepository codigoVerificacionRepository;
    private final DespachadorNotificaciones despachador;
    private final IAuditLog auditLog;
    private final PasswordEncoder passwordEncoder;
    private final int expiracionMinutos;
    private static final SecureRandom random = new SecureRandom();

    public RecuperacionPasswordService(
            UsuarioRepository usuarioRepository,
            CodigoVerificacionRepository codigoVerificacionRepository,
            DespachadorNotificaciones despachador,
            IAuditLog auditLog,
            PasswordEncoder passwordEncoder,
            @Value("${app.seguridad.recuperacion-expiracion-minutos}") int expiracionMinutos) {
        this.usuarioRepository = usuarioRepository;
        this.codigoVerificacionRepository = codigoVerificacionRepository;
        this.despachador = despachador;
        this.auditLog = auditLog;
        this.passwordEncoder = passwordEncoder;
        this.expiracionMinutos = expiracionMinutos;
    }

    @Transactional
    public void solicitarRecuperacion(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsuarioNoEncontradoException(correo));

        codigoVerificacionRepository.deleteByCorreoAndTipo(correo, TipoCodigo.RECUPERACION_PASSWORD);

        String token = String.format("%06d", random.nextInt(1_000_000));

        CodigoVerificacion cv = new CodigoVerificacion();
        cv.setCorreo(correo);
        cv.setCodigo(token);
        cv.setTipo(TipoCodigo.RECUPERACION_PASSWORD);
        cv.setExpiracion(LocalDateTime.now().plusMinutes(expiracionMinutos));
        cv.setUsado(false);
        codigoVerificacionRepository.save(cv);

        despachador.enviarTokenRecuperacion(correo, usuario.getNombreCompleto(), token);
        auditLog.registrar(TipoEvento.RECUPERACION_PASSWORD_SOLICITADA, correo, "Token de recuperación enviado");
    }

    @Transactional
    public void resetearPassword(String correo, String token, String nuevaContrasenia) {
        Optional<CodigoVerificacion> opt = codigoVerificacionRepository
                .findByCorreoAndTipoAndUsadoFalse(correo, TipoCodigo.RECUPERACION_PASSWORD);

        if (opt.isEmpty()) {
            throw new InvalidTokenException("Token no encontrado o ya utilizado.");
        }

        CodigoVerificacion cv = opt.get();

        if (LocalDateTime.now().isAfter(cv.getExpiracion())) {
            throw new InvalidTokenException("El token de recuperación ha expirado.");
        }

        if (!cv.getCodigo().equals(token)) {
            throw new InvalidTokenException("Token de recuperación incorrecto.");
        }

        cv.setUsado(true);
        codigoVerificacionRepository.save(cv);

        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsuarioNoEncontradoException(correo));

        usuario.setContrasenia(passwordEncoder.encode(nuevaContrasenia));
        usuario.setFechaActualizacion(LocalDateTime.now());
        usuarioRepository.save(usuario);

        auditLog.registrar(TipoEvento.PASSWORD_RESETEADA, correo, "Contraseña restablecida exitosamente");
    }
}
