package co.edu.unbosque.accioneselbosque.integracion.notificaciones.canales;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SmsSender {

    private static final Logger log = LoggerFactory.getLogger(SmsSender.class);

    private final String accountSid;
    private final String authToken;
    private final String fromNumber;
    private final boolean habilitado;

    public SmsSender(
            @Value("${twilio.account-sid}") String accountSid,
            @Value("${twilio.auth-token}") String authToken,
            @Value("${twilio.sms.from}") String fromNumber,
            @Value("${app.notificaciones.sms.habilitado:false}") boolean habilitado) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
        this.habilitado = habilitado;
    }

    public boolean enviar(String telefono, String mensaje) {
        if (!habilitado) {
            log.info("[SMS-SIMULADO] → {}: {}", telefono, mensaje);
            return true;
        }
        if (telefono == null || telefono.isBlank()) {
            log.warn("SMS no enviado: número de teléfono vacío");
            return false;
        }
        try {
            Twilio.init(accountSid, authToken);
            Message.creator(new PhoneNumber(telefono), new PhoneNumber(fromNumber), mensaje).create();
            log.info("SMS enviado a {}", telefono);
            return true;
        } catch (Exception e) {
            log.error("Error enviando SMS a {}: {}", telefono, e.getMessage());
            return false;
        }
    }
}
