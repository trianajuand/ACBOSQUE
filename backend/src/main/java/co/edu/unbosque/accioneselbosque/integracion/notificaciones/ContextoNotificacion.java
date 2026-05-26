package co.edu.unbosque.accioneselbosque.integracion.notificaciones;

import co.edu.unbosque.accioneselbosque.autenticacion.dto.NotificacionPreferenciasDTO;

/**
 * Agrupa los datos de contacto y preferencias del usuario necesarios
 * para que el DespachadorNotificaciones enrute al canal correcto.
 */
public class ContextoNotificacion {

    public static ContextoNotificacion desde(NotificacionPreferenciasDTO dto) {
        if (dto == null) return new ContextoNotificacion("", "", null, false, false, false, false);
        return new ContextoNotificacion(dto.getCorreo(), dto.getNombreCompleto(), dto.getTelefono(),
                dto.isNotificacionesActivas(), dto.isNotifEmail(), dto.isNotifSms(), dto.isNotifWhatsapp());
    }

    private final String correo;
    private final String nombreCompleto;
    private final String telefono;
    private final boolean notificacionesActivas;
    private final boolean notifEmail;
    private final boolean notifSms;
    private final boolean notifWhatsapp;

    public ContextoNotificacion(String correo, String nombreCompleto, String telefono,
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
    public String getNombreCompleto() { return nombreCompleto; }
    public String getTelefono() { return telefono; }
    public boolean isNotificacionesActivas() { return notificacionesActivas; }
    public boolean isNotifEmail() { return notifEmail; }
    public boolean isNotifSms() { return notifSms; }
    public boolean isNotifWhatsapp() { return notifWhatsapp; }

    /** Conveniencia: ¿tiene al menos un canal activo? */
    public boolean tieneCanal() {
        return notificacionesActivas && (notifEmail || notifSms || notifWhatsapp);
    }
}
