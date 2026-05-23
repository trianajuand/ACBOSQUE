package co.edu.unbosque.accioneselbosque.integracion.notificaciones;

/**
 * Contrato del despachador de notificaciones multicanal.
 * Consumida por Autenticación y Órdenes para enviar comunicaciones al usuario
 * sin acoplarse a la implementación de canales (Email, SMS, WhatsApp).
 */
public interface INotificacion {

    void enviarCodigoRegistro(String correo, String nombreCompleto, String codigo);

    void enviarCodigoMfa(String correo, String nombreCompleto, String codigo);

    void enviarTokenRecuperacion(String correo, String nombreCompleto, String token);

    void notificarBloqueo(String correo, String nombreCompleto);

    void notificarAdmin(String asunto, String mensaje);
}
