package co.edu.unbosque.accioneselbosque.autenticacion.dto;

import java.util.List;

public class PreferenciasNotificacionDTO {

    /** Interruptor maestro: false deshabilita todas las notificaciones de negocio */
    private boolean notificacionesActivas = true;
    private boolean notificacionEmail;
    private boolean notificacionSms;
    private boolean notificacionWhatsapp;
    private List<String> tiposNotificacion;

    public PreferenciasNotificacionDTO() {}

    public boolean isNotificacionesActivas() { return notificacionesActivas; }
    public void setNotificacionesActivas(boolean notificacionesActivas) { this.notificacionesActivas = notificacionesActivas; }

    public boolean isNotificacionEmail() { return notificacionEmail; }
    public void setNotificacionEmail(boolean notificacionEmail) { this.notificacionEmail = notificacionEmail; }

    public boolean isNotificacionSms() { return notificacionSms; }
    public void setNotificacionSms(boolean notificacionSms) { this.notificacionSms = notificacionSms; }

    public boolean isNotificacionWhatsapp() { return notificacionWhatsapp; }
    public void setNotificacionWhatsapp(boolean notificacionWhatsapp) { this.notificacionWhatsapp = notificacionWhatsapp; }

    public List<String> getTiposNotificacion() { return tiposNotificacion; }
    public void setTiposNotificacion(List<String> tiposNotificacion) { this.tiposNotificacion = tiposNotificacion; }
}
