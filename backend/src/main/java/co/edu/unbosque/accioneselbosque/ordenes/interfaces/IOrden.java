package co.edu.unbosque.accioneselbosque.ordenes.interfaces;

import co.edu.unbosque.accioneselbosque.ordenes.dto.*;

import java.util.List;

public interface IOrden {

    ResumenComisionDTO previsualizarOrden(Long usuarioId, CrearOrdenRequestDTO dto);

    OrdenDTO crearOrden(Long usuarioId, CrearOrdenRequestDTO dto, String ipOrigen);

    boolean cancelarOrden(Long usuarioId, Long ordenId);

    List<OrdenDTO> obtenerOrdenesActivas(Long usuarioId);

    List<OrdenDTO> obtenerHistorialOrdenes(Long usuarioId);

    List<OrdenDTO> obtenerHistorialOrdenes(Long usuarioId, String desde, String hasta,
                                           String tipoOrden, String simbolo, String estado);

    PortafolioDTO obtenerPortafolio(Long usuarioId);

    SaldoDTO obtenerSaldo(Long usuarioId);

    OrdenDTO crearPropuestaOrden(Long comisionistaId, Long clienteId, CrearPropuestaOrdenDTO dto, String ipOrigen);

    List<OrdenDTO> obtenerPropuestasPendientesInversionista(Long usuarioId);

    List<OrdenDTO> obtenerPropuestasAprobadasComisionista(Long comisionistaId);

    OrdenDTO aprobarPropuesta(Long usuarioId, Long propuestaId, String comentario);

    OrdenDTO rechazarPropuesta(Long usuarioId, Long propuestaId, String comentario);

    OrdenDTO firmarYEnviarPropuesta(Long comisionistaId, Long propuestaId, String ipOrigen);
}
