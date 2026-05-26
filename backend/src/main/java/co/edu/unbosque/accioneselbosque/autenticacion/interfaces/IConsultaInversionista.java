package co.edu.unbosque.accioneselbosque.autenticacion.interfaces;

import co.edu.unbosque.accioneselbosque.autenticacion.dto.NotificacionPreferenciasDTO;

/**
 * Contrato para que módulos externos (Órdenes) consulten datos del inversionista
 * sin acceder directamente a los repositorios del módulo de Autenticación.
 */
public interface IConsultaInversionista {

    /** Valida que el usuario pueda operar; lanza ResponseStatusException si no puede. */
    void validarPuedeOperar(Long usuarioId);

    /** Retorna el Alpaca account ID del inversionista, o null si no tiene cuenta aún. */
    String obtenerAlpacaAccountId(Long usuarioId);

    /** Retorna true si el inversionista aún no tiene cuenta Alpaca creada. */
    boolean necesitaCuentaAlpaca(Long usuarioId);

    /** Actualiza el Alpaca account ID tras la creación exitosa de la cuenta. */
    void actualizarAlpacaAccountId(Long usuarioId, String alpacaAccountId);

    /** Retorna la preferencia de vista del portafolio (RESUMEN o DETALLE). */
    String obtenerVistaPortafolio(Long usuarioId);

    /** Retorna datos de contacto y preferencias de notificación del usuario. */
    NotificacionPreferenciasDTO obtenerPreferenciasNotificacion(Long usuarioId);
}
