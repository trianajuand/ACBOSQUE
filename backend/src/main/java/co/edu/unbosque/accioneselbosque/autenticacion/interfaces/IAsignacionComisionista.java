package co.edu.unbosque.accioneselbosque.autenticacion.interfaces;

import co.edu.unbosque.accioneselbosque.autenticacion.dto.ClienteAsignadoDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.dto.ComisionistaAsignadoDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Usuario;

import java.util.List;
import java.util.Optional;

public interface IAsignacionComisionista {

    Optional<Long> asignarSiSolicitado(Usuario inversionista);

    void validarClienteAsignado(Long comisionistaId, Long inversionistaId);

    boolean esClienteAsignado(Long comisionistaId, Long inversionistaId);

    boolean usuarioTieneComisionista(Long inversionistaId);

    Optional<Long> obtenerComisionistaIdDeInversionista(Long inversionistaId);

    Optional<ComisionistaAsignadoDTO> obtenerComisionistaAsignado(Long inversionistaId);

    List<ClienteAsignadoDTO> listarClientesAsignados(Long comisionistaId);
}
