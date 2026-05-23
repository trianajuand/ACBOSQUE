package co.edu.unbosque.accioneselbosque.mercado.interfaces;

import co.edu.unbosque.accioneselbosque.mercado.dto.CotizacionDTO;
import co.edu.unbosque.accioneselbosque.mercado.dto.DetalleAccionDTO;

import java.util.List;

public interface IVerificacionMercado {

    /** Retorna true si el mercado especificado está abierto en este momento. */
    boolean esMercadoAbierto(String mercado);

    /** Retorna el nombre del mercado dado un símbolo bursátil. */
    String detectarMercado(String simbolo);

    /** Valida que el símbolo esté operable y retorna su cotización actual. */
    CotizacionDTO validarSimboloOperable(String simbolo);

    /** Retorna la lista de cotizaciones para los mercados de interés del usuario. */
    List<CotizacionDTO> obtenerDashboard(String interesesMercado);

    /** Retorna el detalle completo de una acción dado su símbolo. */
    DetalleAccionDTO obtenerDetalle(String simbolo);
}
