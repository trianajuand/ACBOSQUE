package co.edu.unbosque.accioneselbosque.integracion.notificaciones;

import co.edu.unbosque.accioneselbosque.integracion.notificaciones.canales.EmailSender;
import co.edu.unbosque.accioneselbosque.integracion.notificaciones.canales.SmsSender;
import co.edu.unbosque.accioneselbosque.integracion.notificaciones.canales.WhatsAppSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class DespachadorNotificaciones implements INotificacion {

    private static final Logger log = LoggerFactory.getLogger(DespachadorNotificaciones.class);

    private final EmailSender emailSender;
    private final SmsSender smsSender;
    private final WhatsAppSender whatsAppSender;
    private final String correoAdmin;

    public DespachadorNotificaciones(EmailSender emailSender,
                                     SmsSender smsSender,
                                     WhatsAppSender whatsAppSender,
                                     @Value("${spring.mail.username}") String correoAdmin) {
        this.emailSender = emailSender;
        this.smsSender = smsSender;
        this.whatsAppSender = whatsAppSender;
        this.correoAdmin = correoAdmin;
    }

    // =========================================================
    // Autenticación — siempre por email
    // =========================================================

    @Override
    public void enviarCodigoRegistro(String correo, String nombreCompleto, String codigo) {
        log.info(">>> CODIGO REGISTRO para {}: {}", correo, codigo);
        emailSender.enviarCodigoVerificacion(correo, nombreCompleto, codigo);
    }

    @Override
    public void enviarCodigoMfa(String correo, String nombreCompleto, String codigo) {
        log.info(">>> CODIGO MFA para {}: {}", correo, codigo);
        emailSender.enviarCodigoMfa(correo, nombreCompleto, codigo);
    }

    @Override
    public void enviarTokenRecuperacion(String correo, String nombreCompleto, String token) {
        log.info(">>> TOKEN RECUPERACION para {}: {}", correo, token);
        emailSender.enviarTokenRecuperacion(correo, nombreCompleto, token);
    }

    @Override
    public void notificarBloqueo(String correo, String nombreCompleto) {
        emailSender.enviarNotificacionBloqueo(correo, nombreCompleto);
    }

    @Override
    public void notificarAdmin(String asunto, String mensaje) {
        emailSender.enviarNotificacionAdmin(correoAdmin, asunto, mensaje);
    }

    // =========================================================
    // Órdenes — multicanal
    // =========================================================

    @Override
    public void notificarOrdenCreada(ContextoNotificacion ctx,
                                      String simbolo, String tipoOrden, String lado,
                                      BigDecimal monto, BigDecimal comision) {
        if (!ctx.tieneCanal()) return;
        String asunto = "Orden creada — " + simbolo;
        String htmlMsg = emailSender.construirHtmlEvento("Orden Creada",
                ctx.getNombreCompleto(),
                "Tu orden de <strong>" + lado + "</strong> de <strong>" + simbolo + "</strong> ha sido registrada.",
                "Tipo: " + tipoOrden + " | Monto: $" + monto + " | Comisión: $" + comision);
        String smsMsg = "Acciones ElBosque: Orden " + lado + " " + simbolo
                + " (" + tipoOrden + ") creada. Monto: $" + monto + ".";
        despachar(ctx, asunto, htmlMsg, smsMsg);
    }

    @Override
    public void notificarOrdenCancelada(ContextoNotificacion ctx,
                                         String simbolo, String tipoOrden, BigDecimal montoLiberado) {
        if (!ctx.tieneCanal()) return;
        String asunto = "Orden cancelada — " + simbolo;
        String htmlMsg = emailSender.construirHtmlEvento("Orden Cancelada",
                ctx.getNombreCompleto(),
                "Tu orden de <strong>" + simbolo + "</strong> ha sido cancelada.",
                "Tipo: " + tipoOrden + " | Fondos liberados: $" + montoLiberado);
        String smsMsg = "Acciones ElBosque: Orden " + simbolo + " cancelada. Fondos liberados: $" + montoLiberado + ".";
        despachar(ctx, asunto, htmlMsg, smsMsg);
    }

    @Override
    public void notificarOrdenEjecutada(ContextoNotificacion ctx,
                                         String simbolo, String tipoOrden, String lado,
                                         BigDecimal precioEjecucion, BigDecimal cantidad, BigDecimal comision) {
        if (!ctx.tieneCanal()) return;
        String asunto = "Orden ejecutada — " + simbolo;
        String htmlMsg = emailSender.construirHtmlEvento("Orden Ejecutada ✓",
                ctx.getNombreCompleto(),
                "Tu orden de <strong>" + lado + "</strong> de <strong>" + simbolo + "</strong> fue ejecutada.",
                "Precio: $" + precioEjecucion + " | Cantidad: " + cantidad + " | Comisión: $" + comision);
        String smsMsg = "Acciones ElBosque: Orden " + lado + " " + simbolo
                + " ejecutada. Precio: $" + precioEjecucion + " x " + cantidad + ".";
        despachar(ctx, asunto, htmlMsg, smsMsg);
    }

    @Override
    public void notificarOrdenFallida(ContextoNotificacion ctx, String simbolo, String motivo) {
        if (!ctx.tieneCanal()) return;
        String asunto = "Orden fallida — " + simbolo;
        String htmlMsg = emailSender.construirHtmlEvento("Orden Fallida",
                ctx.getNombreCompleto(),
                "Tu orden para <strong>" + simbolo + "</strong> no pudo procesarse.",
                "Motivo: " + motivo);
        String smsMsg = "Acciones ElBosque: Orden " + simbolo + " fallida. Motivo: " + motivo;
        despachar(ctx, asunto, htmlMsg, smsMsg);
    }

    @Override
    public void notificarOrdenEncolada(ContextoNotificacion ctx,
                                        String simbolo, String tipoOrden, String lado) {
        if (!ctx.tieneCanal()) return;
        String asunto = "Orden encolada — " + simbolo;
        String htmlMsg = emailSender.construirHtmlEvento("Orden Encolada",
                ctx.getNombreCompleto(),
                "El mercado está cerrado. Tu orden de <strong>" + lado + "</strong> de <strong>" + simbolo
                        + "</strong> fue encolada.",
                "Tipo: " + tipoOrden + " — Se procesará en la próxima apertura del mercado.");
        String smsMsg = "Acciones ElBosque: Orden " + lado + " " + simbolo
                + " encolada. Se enviará al abrir el mercado.";
        despachar(ctx, asunto, htmlMsg, smsMsg);
    }

    // =========================================================
    // Mercado — multicanal
    // =========================================================

    @Override
    public void notificarAperturaMercado(ContextoNotificacion ctx, String mercado) {
        if (!ctx.tieneCanal()) return;
        String asunto = "Apertura de mercado — " + mercado;
        String htmlMsg = emailSender.construirHtmlEvento("Mercado Abierto",
                ctx.getNombreCompleto(),
                "El mercado <strong>" + mercado + "</strong> ha abierto.",
                "Puedes operar y tus órdenes encoladas serán procesadas.");
        String smsMsg = "Acciones ElBosque: El mercado " + mercado + " está abierto. ¡Hora de operar!";
        despachar(ctx, asunto, htmlMsg, smsMsg);
    }

    @Override
    public void notificarCierreMercado(ContextoNotificacion ctx, String mercado) {
        if (!ctx.tieneCanal()) return;
        String asunto = "Cierre de mercado — " + mercado;
        String htmlMsg = emailSender.construirHtmlEvento("Mercado Cerrado",
                ctx.getNombreCompleto(),
                "El mercado <strong>" + mercado + "</strong> ha cerrado.",
                "Las nuevas órdenes fuera de horario quedarán en cola para la próxima apertura.");
        String smsMsg = "Acciones ElBosque: El mercado " + mercado + " cerró. Tus órdenes pendientes quedan en cola.";
        despachar(ctx, asunto, htmlMsg, smsMsg);
    }

    // =========================================================
    // Resumen diario — multicanal
    // =========================================================

    @Override
    public void notificarResumenDiario(ContextoNotificacion ctx,
                                        int ordenesEjecutadas, int ordenesCanceladas,
                                        BigDecimal gananciaNetaDia) {
        if (!ctx.tieneCanal()) return;
        String gananciaStr = (gananciaNetaDia.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + "$" + gananciaNetaDia;
        String asunto = "Resumen diario — Acciones ElBosque";
        String htmlMsg = emailSender.construirHtmlEvento("Resumen del Día",
                ctx.getNombreCompleto(),
                "Aquí está tu resumen de operaciones de hoy.",
                "Órdenes ejecutadas: " + ordenesEjecutadas
                        + " | Canceladas: " + ordenesCanceladas
                        + " | Ganancia neta: " + gananciaStr);
        String smsMsg = "ElBosque resumen: " + ordenesEjecutadas + " ejecutadas, "
                + ordenesCanceladas + " canceladas. Ganancia: " + gananciaStr + ".";
        despachar(ctx, asunto, htmlMsg, smsMsg);
    }

    // =========================================================
    // Enrutamiento interno
    // =========================================================

    private void despachar(ContextoNotificacion ctx, String asunto, String htmlMsg, String smsMsg) {
        if (ctx.isNotifEmail()) {
            emailSender.enviarMensajeLibre(ctx.getCorreo(), asunto, htmlMsg);
        }
        if (ctx.isNotifSms()) {
            smsSender.enviar(ctx.getTelefono(), smsMsg);
        }
        if (ctx.isNotifWhatsapp()) {
            whatsAppSender.enviar(ctx.getTelefono(), smsMsg);
        }
    }
}
