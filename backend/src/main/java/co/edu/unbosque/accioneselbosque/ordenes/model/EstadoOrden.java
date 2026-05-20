package co.edu.unbosque.accioneselbosque.ordenes.model;

public enum EstadoOrden {
    /** Creada en nuestra BD, pendiente de enviar a Alpaca. */
    PENDIENTE,
    /** Enviada a Alpaca, esperando ejecución. */
    ENVIADA,
    /** Ejecutada por el mercado. */
    EJECUTADA,
    /** Cancelada por el usuario o el sistema. */
    CANCELADA,
    /** Encolada por estar fuera del horario de mercado (HU-23). */
    EN_COLA,
    /** Propuesta por comisionista, pendiente aprobación del inversionista. */
    PENDIENTE_APROBACION,
    /** Aprobada por inversionista, pendiente firma del comisionista. */
    APROBADA,
    /** Rechazada por el inversionista. */
    RECHAZADA
}
