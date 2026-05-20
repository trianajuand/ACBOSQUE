package co.edu.unbosque.accioneselbosque.ordenes.interfaces;

import co.edu.unbosque.accioneselbosque.ordenes.dto.*;

import java.util.List;

public interface IOrden {

    ResumenComisionDTO previsualizarOrden(Long usuarioId, CrearOrdenRequestDTO dto);

    OrdenDTO crearOrden(Long usuarioId, CrearOrdenRequestDTO dto, String ipOrigen);

    boolean cancelarOrden(Long usuarioId, Long ordenId);

    List<OrdenDTO> obtenerOrdenesActivas(Long usuarioId);

    List<OrdenDTO> obtenerHistorialOrdenes(Long usuarioId);

    PortafolioDTO obtenerPortafolio(Long usuarioId);

    SaldoDTO obtenerSaldo(Long usuarioId);
}
