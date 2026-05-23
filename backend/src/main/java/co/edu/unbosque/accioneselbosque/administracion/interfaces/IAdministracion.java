package co.edu.unbosque.accioneselbosque.administracion.interfaces;

import co.edu.unbosque.accioneselbosque.administracion.dto.MercadoConfigDTO;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Contrato de lectura del módulo de Administración para consumo externo.
 * Consumida por el módulo de Mercado para verificar horarios y feriados.
 * Los parámetros de comisión se exponen por separado en IGestorParametros.
 */
public interface IAdministracion {

    Optional<MercadoConfigDTO> obtenerConfiguracionMercado(String mercado);

    boolean esFeriadoMercado(String mercado, LocalDate fecha);
}
