package co.edu.unbosque.accioneselbosque.integracion.notificaciones;

import co.edu.unbosque.accioneselbosque.integracion.notificaciones.canales.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DespachadorNotificaciones {

    private static final Logger log = LoggerFactory.getLogger(DespachadorNotificaciones.class);
    private final EmailSender emailSender;
    private final String correoAdmin;

    public DespachadorNotificaciones(EmailSender emailSender,
                                     @Value("${spring.mail.username}") String correoAdmin) {
        this.emailSender = emailSender;
        this.correoAdmin = correoAdmin;
    }

    public void enviarCodigoRegistro(String correo, String nombreCompleto, String codigo) {
        log.info(">>> CODIGO REGISTRO para {}: {}", correo, codigo);
        emailSender.enviarCodigoVerificacion(correo, nombreCompleto, codigo);
    }

    public void enviarCodigoMfa(String correo, String nombreCompleto, String codigo) {
        log.info(">>> CODIGO MFA para {}: {}", correo, codigo);
        emailSender.enviarCodigoMfa(correo, nombreCompleto, codigo);
    }

    public void enviarTokenRecuperacion(String correo, String nombreCompleto, String token) {
        log.info(">>> TOKEN RECUPERACION para {}: {}", correo, token);
        emailSender.enviarTokenRecuperacion(correo, nombreCompleto, token);
    }

    public void notificarBloqueo(String correo, String nombreCompleto) {
        emailSender.enviarNotificacionBloqueo(correo, nombreCompleto);
    }

    public void notificarAdmin(String asunto, String mensaje) {
        emailSender.enviarNotificacionAdmin(correoAdmin, asunto, mensaje);
    }
}
