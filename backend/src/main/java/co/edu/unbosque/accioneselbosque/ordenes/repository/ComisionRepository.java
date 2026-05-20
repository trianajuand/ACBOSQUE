package co.edu.unbosque.accioneselbosque.ordenes.repository;

import co.edu.unbosque.accioneselbosque.ordenes.model.Comision;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ComisionRepository extends CrudRepository<Comision, Long> {

    List<Comision> findByUsuarioIdOrderByCreadaEnDesc(Long usuarioId);

    List<Comision> findByOrdenId(Long ordenId);
}
