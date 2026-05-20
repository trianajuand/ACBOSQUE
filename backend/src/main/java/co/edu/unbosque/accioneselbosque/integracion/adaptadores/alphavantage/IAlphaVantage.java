package co.edu.unbosque.accioneselbosque.integracion.adaptadores.alphavantage;

import java.util.Map;

public interface IAlphaVantage {

    /** Cotizacion en tiempo real de cualquier bolsa global. */
    Map<String, Object> obtenerCotizacionGlobal(String simbolo);

    /** Serie temporal diaria. */
    Map<String, Object> obtenerSerieTemporalDiaria(String simbolo);

    /** Resumen de la empresa. */
    Map<String, Object> obtenerResumenEmpresa(String simbolo);
}
