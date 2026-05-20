package co.edu.unbosque.accioneselbosque.mercado.interfaces;

public interface IVerificacionMercado {

    /** Retorna true si el mercado especificado está abierto en este momento. */
    boolean esMercadoAbierto(String mercado);

    /** Retorna el nombre del mercado dado un símbolo bursátil. */
    String detectarMercado(String simbolo);
}
