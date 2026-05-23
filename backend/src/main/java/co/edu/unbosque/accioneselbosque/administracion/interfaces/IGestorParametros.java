package co.edu.unbosque.accioneselbosque.administracion.interfaces;

import java.math.BigDecimal;

public interface IGestorParametros {
    BigDecimal obtenerPorcentajeComision();
    BigDecimal obtenerSplitPlataforma();
    BigDecimal obtenerSplitComisionista();
}
