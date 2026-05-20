package co.edu.unbosque.accioneselbosque.integracion.notificaciones.canales;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class EmailSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private final JavaMailSender mailSender;
    private final String remitenteNombre;
    private final String remitenteCorreo;

    public EmailSender(JavaMailSender mailSender,
                       @Value("${app.mail.remitente.nombre}") String remitenteNombre,
                       @Value("${spring.mail.username}") String remitenteCorreo) {
        this.mailSender = mailSender;
        this.remitenteNombre = remitenteNombre;
        this.remitenteCorreo = remitenteCorreo;
    }

    public boolean enviarCodigoVerificacion(String destinatario, String nombreCompleto, String codigo) {
        String asunto = "Acciones ElBosque — Código de verificación";
        String contenido = construirHtmlCodigo(nombreCompleto, codigo,
                "Confirma tu registro", "Este código expira en 10 minutos.");
        return enviar(destinatario, asunto, contenido);
    }

    public boolean enviarCodigoMfa(String destinatario, String nombreCompleto, String codigo) {
        String asunto = "Acciones ElBosque — Código de acceso MFA";
        String contenido = construirHtmlCodigo(nombreCompleto, codigo,
                "Segundo factor de autenticación", "Este código expira en 10 minutos.");
        return enviar(destinatario, asunto, contenido);
    }

    public boolean enviarTokenRecuperacion(String destinatario, String nombreCompleto, String token) {
        String asunto = "Acciones ElBosque — Recuperación de contraseña";
        String contenido = construirHtmlCodigo(nombreCompleto, token,
                "Recuperación de contraseña", "Este código expira en 30 minutos. Úsalo una sola vez.");
        return enviar(destinatario, asunto, contenido);
    }

    public boolean enviarNotificacionBloqueo(String destinatario, String nombreCompleto) {
        String asunto = "Acciones ElBosque — Cuenta temporalmente bloqueada";
        String contenido = construirHtmlMensaje(nombreCompleto,
                "Tu cuenta ha sido bloqueada temporalmente por 15 minutos "
                + "tras múltiples intentos fallidos de inicio de sesión. "
                + "Si no fuiste tú, te recomendamos cambiar tu contraseña.");
        return enviar(destinatario, asunto, contenido);
    }

    public boolean enviarNotificacionAdmin(String destinatario, String asunto, String mensaje) {
        String contenido = construirHtmlMensaje("Administrador", mensaje);
        return enviar(destinatario, asunto, contenido);
    }

    private boolean enviar(String destinatario, String asunto, String contenidoHtml) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(remitenteNombre + " <" + remitenteCorreo + ">");
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(contenidoHtml, true);
            mailSender.send(mensaje);
            return true;
        } catch (MessagingException e) {
            log.error("Error enviando correo a {}: {}", destinatario, e.getMessage());
            return false;
        }
    }

    private String construirHtmlCodigo(String nombre, String codigo, String titulo, String nota) {
        return "<!DOCTYPE html><html lang='es'><head><meta charset='UTF-8'>"
                + "<style>body{font-family:Arial,sans-serif;background:#F3F3F3;margin:0;padding:0;}"
                + ".container{width:100%;max-width:600px;margin:0 auto;background:#13173d;padding:20px;border-radius:10px;text-align:center;}"
                + ".header{background:#232c42;color:white;padding:10px;border-radius:5px;}"
                + ".content{background:#232c42;color:white;padding:30px;margin:20px 0;border-radius:10px;}"
                + ".content p{font-size:18px;margin:10px 0;}"
                + ".codigo{font-size:32px;font-weight:bold;color:#FFD700;letter-spacing:8px;margin:20px 0;}"
                + ".nota{font-size:13px;color:#bbbec7;margin-top:10px;}"
                + ".footer{background:#232c42;color:white;padding:10px;border-radius:5px;font-size:12px;}"
                + "</style></head><body>"
                + "<div class='container'>"
                + "<div class='header'><h2>" + titulo + "</h2></div>"
                + "<div class='content'>"
                + "<p>Hola, <strong>" + nombre + "</strong></p>"
                + "<p>Tu código es:</p>"
                + "<div class='codigo'>" + codigo + "</div>"
                + "<p class='nota'>" + nota + "</p>"
                + "</div>"
                + "<div class='footer'><p>Acciones ElBosque &mdash; No respondas a este correo.</p></div>"
                + "</div></body></html>";
    }

    private String construirHtmlMensaje(String nombre, String mensaje) {
        return "<!DOCTYPE html><html lang='es'><head><meta charset='UTF-8'>"
                + "<style>body{font-family:Arial,sans-serif;background:#F3F3F3;}"
                + ".container{max-width:600px;margin:0 auto;background:#13173d;padding:20px;border-radius:10px;text-align:center;}"
                + ".header{background:#232c42;color:white;padding:10px;border-radius:5px;}"
                + ".content{background:#232c42;color:white;padding:30px;margin:20px 0;border-radius:10px;}"
                + ".content p{font-size:16px;}"
                + ".footer{background:#232c42;color:white;padding:10px;border-radius:5px;font-size:12px;}"
                + "</style></head><body>"
                + "<div class='container'>"
                + "<div class='header'><h2>Acciones ElBosque</h2></div>"
                + "<div class='content'>"
                + "<p>Hola, <strong>" + nombre + "</strong></p>"
                + "<p>" + mensaje + "</p>"
                + "</div>"
                + "<div class='footer'><p>No respondas a este correo.</p></div>"
                + "</div></body></html>";
    }
}
