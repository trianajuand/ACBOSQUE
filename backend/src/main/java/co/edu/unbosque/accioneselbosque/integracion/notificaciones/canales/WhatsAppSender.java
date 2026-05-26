package co.edu.unbosque.accioneselbosque.integracion.notificaciones.canales;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WhatsAppSender {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppSender.class);

    private final String accountSid;
    private final String authToken;
    private final String fromNumber;
    private final boolean habilitado;

    public WhatsAppSender(
            @Value("${twilio.account-sid}") String accountSid,
            @Value("${twilio.auth-token}") String authToken,
            @Value("${twilio.whatsapp.from}") String fromNumber,
            @Value("${app.notificaciones.whatsapp.habilitado:false}") boolean habilitado) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
        this.habilitado = habilitado;
    }

    public boolean enviar(String telefono, String mensaje) {
        if (!habilitado) {
            log.info("[WHATSAPP-SIMULADO] → {}: {}", telefono, mensaje);
            return true;
        }
        if (telefono == null || telefono.isBlank()) {
            log.warn("WhatsApp no enviado: número de teléfono vacío");
            return false;
        }
        try {
            Twilio.init(accountSid, authToken);
            String dest = telefono.startsWith("whatsapp:") ? telefono : "whatsapp:" + telefono;
            Message.creator(new PhoneNumber(dest), new PhoneNumber(fromNumber), mensaje).create();
            log.info("WhatsApp enviado a {}", telefono);
            return true;
        } catch (Exception e) {
            log.error("Error enviando WhatsApp a {}: {}", telefono, e.getMessage());
            return false;
        }
    }
}
