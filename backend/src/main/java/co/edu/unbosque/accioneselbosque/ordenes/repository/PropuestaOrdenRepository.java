package co.edu.unbosque.accioneselbosque.ordenes.repository;

import co.edu.unbosque.accioneselbosque.ordenes.model.EstadoPropuesta;
import co.edu.unbosque.accioneselbosque.ordenes.model.PropuestaOrden;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface PropuestaOrdenRepository extends CrudRepository<PropuestaOrden, Long> {

    List<PropuestaOrden> findByInversionistaIdAndEstado(Long inversionistaId, EstadoPropuesta estado);

    List<PropuestaOrden> findByComisionistaIdAndEstado(Long comisionistaId, EstadoPropuesta estado);

    List<PropuestaOrden> findByInversionistaId(Long inversionistaId);

    List<PropuestaOrden> findByComisionistaId(Long comisionistaId);
}
