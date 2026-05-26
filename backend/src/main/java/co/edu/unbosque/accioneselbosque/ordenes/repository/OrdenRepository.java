package co.edu.unbosque.accioneselbosque.ordenes.repository;

import co.edu.unbosque.accioneselbosque.ordenes.model.EstadoOrden;
import co.edu.unbosque.accioneselbosque.ordenes.model.Orden;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface OrdenRepository extends CrudRepository<Orden, Long> {

    List<Orden> findByInversionistaIdOrderByCreadaEnDesc(Long inversionistaId);

    List<Orden> findByInversionistaIdAndEstadoOrderByCreadaEnDesc(Long inversionistaId, EstadoOrden estado);

    List<Orden> findByEstadoOrderByCreadaEnAsc(EstadoOrden estado);

    Optional<Orden> findByIdAndInversionistaId(Long id, Long inversionistaId);
}
