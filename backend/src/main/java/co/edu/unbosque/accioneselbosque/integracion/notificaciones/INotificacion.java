package co.edu.unbosque.accioneselbosque.integracion.notificaciones;

import java.math.BigDecimal;

/**
 * Contrato del despachador de notificaciones multicanal.
 * Consumida por Autenticación y Órdenes.
 *
 * Los métodos de autenticación (registro, MFA, recuperación) van siempre por email
 * porque son los únicos canales verificados en ese momento del flujo.
 *
 * Los métodos de eventos de negocio (órdenes, mercado) reciben las preferencias
 * del usuario para enrutar al canal correcto.
 */
public interface INotificacion {

    // =========================================================
    // Autenticación — siempre por email
    // =========================================================

    void enviarCodigoRegistro(String correo, String nombreCompleto, String codigo);

    void enviarCodigoMfa(String correo, String nombreCompleto, String codigo);

    void enviarTokenRecuperacion(String correo, String nombreCompleto, String token);

    void notificarBloqueo(String correo, String nombreCompleto);

    void notificarAdmin(String asunto, String mensaje);

    // =========================================================
    // Órdenes — multicanal según preferencias del usuario
    // =========================================================

    void notificarOrdenCreada(ContextoNotificacion ctx,
                               String simbolo, String tipoOrden, String lado,
                               BigDecimal monto, BigDecimal comision);

    void notificarOrdenCancelada(ContextoNotificacion ctx,
                                  String simbolo, String tipoOrden, BigDecimal montoLiberado);

    void notificarOrdenEjecutada(ContextoNotificacion ctx,
                                  String simbolo, String tipoOrden, String lado,
                                  BigDecimal precioEjecucion, BigDecimal cantidad, BigDecimal comision);

    void notificarOrdenFallida(ContextoNotificacion ctx,
                                String simbolo, String motivo);

    void notificarOrdenEncolada(ContextoNotificacion ctx,
                                 String simbolo, String tipoOrden, String lado);

    // =========================================================
    // Mercado — multicanal según preferencias del usuario
    // =========================================================

    void notificarAperturaMercado(ContextoNotificacion ctx, String mercado);

    void notificarCierreMercado(ContextoNotificacion ctx, String mercado);

    // =========================================================
    // Resumen diario
    // =========================================================

    void notificarResumenDiario(ContextoNotificacion ctx,
                                 int ordenesEjecutadas, int ordenesCanceladas,
                                 BigDecimal gananciaNetaDia);
}
