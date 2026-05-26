package co.edu.unbosque.accioneselbosque.autenticacion.dto;

/**
 * DTO que cruza la frontera autenticacion → ordenes/integracion
 * con los datos de contacto y preferencias de notificación del usuario.
 */
public class NotificacionPreferenciasDTO {

    private String correo;
    private String nombreCompleto;
    private String telefono;
    private boolean notificacionesActivas;
    private boolean notifEmail;
    private boolean notifSms;
    private boolean notifWhatsapp;

    public NotificacionPreferenciasDTO() {}

    public NotificacionPreferenciasDTO(String correo, String nombreCompleto, String telefono,
                                        boolean notificacionesActivas,
                                        boolean notifEmail, boolean notifSms, boolean notifWhatsapp) {
        this.correo = correo;
        this.nombreCompleto = nombreCompleto;
        this.telefono = telefono;
        this.notificacionesActivas = notificacionesActivas;
        this.notifEmail = notifEmail;
        this.notifSms = notifSms;
        this.notifWhatsapp = notifWhatsapp;
    }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public boolean isNotificacionesActivas() { return notificacionesActivas; }
    public void setNotificacionesActivas(boolean notificacionesActivas) { this.notificacionesActivas = notificacionesActivas; }

    public boolean isNotifEmail() { return notifEmail; }
    public void setNotifEmail(boolean notifEmail) { this.notifEmail = notifEmail; }

    public boolean isNotifSms() { return notifSms; }
    public void setNotifSms(boolean notifSms) { this.notifSms = notifSms; }

    public boolean isNotifWhatsapp() { return notifWhatsapp; }
    public void setNotifWhatsapp(boolean notifWhatsapp) { this.notifWhatsapp = notifWhatsapp; }
}
