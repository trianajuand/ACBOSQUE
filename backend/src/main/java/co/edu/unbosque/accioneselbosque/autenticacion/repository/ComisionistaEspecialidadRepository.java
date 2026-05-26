package co.edu.unbosque.accioneselbosque.autenticacion.repository;

import co.edu.unbosque.accioneselbosque.autenticacion.model.ComisionistaEspecialidad;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ComisionistaEspecialidadRepository extends CrudRepository<ComisionistaEspecialidad, Long> {
    List<ComisionistaEspecialidad> findByComisionistaId(Long comisionistaId);
    boolean existsByComisionistaIdAndEspecialidadId(Long comisionistaId, Long especialidadId);
}
