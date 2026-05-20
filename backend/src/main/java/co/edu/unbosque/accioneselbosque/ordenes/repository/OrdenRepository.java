package co.edu.unbosque.accioneselbosque.ordenes.repository;

import co.edu.unbosque.accioneselbosque.ordenes.model.EstadoOrden;
import co.edu.unbosque.accioneselbosque.ordenes.model.Orden;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface OrdenRepository extends CrudRepository<Orden, Long> {

    List<Orden> findByUsuarioIdOrderByCreadaEnDesc(Long usuarioId);

    List<Orden> findByUsuarioIdAndEstadoOrderByCreadaEnDesc(Long usuarioId, EstadoOrden estado);

    List<Orden> findByComisionistaIdAndEstadoOrderByCreadaEnDesc(Long comisionistaId, EstadoOrden estado);

    List<Orden> findByEstadoOrderByCreadaEnAsc(EstadoOrden estado);

    Optional<Orden> findByIdAndUsuarioId(Long id, Long usuarioId);

    Optional<Orden> findByIdAndComisionistaId(Long id, Long comisionistaId);
}
