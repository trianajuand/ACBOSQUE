package co.edu.unbosque.accioneselbosque.administracion.interfaces;

import co.edu.unbosque.accioneselbosque.administracion.dto.MercadoConfigDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface IAdministracion {

    BigDecimal obtenerPorcentajeComision();

    BigDecimal obtenerSplitPlataforma();

    BigDecimal obtenerSplitComisionista();

    Optional<MercadoConfigDTO> obtenerConfiguracionMercado(String mercado);

    boolean esFeriadoMercado(String mercado, LocalDate fecha);
}
