package co.edu.unbosque.accioneselbosque.administracion.repository;

import co.edu.unbosque.accioneselbosque.administracion.model.ParametroComision;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ParametroComisionRepository extends CrudRepository<ParametroComision, Long> {
    Optional<ParametroComision> findFirstByActivoTrueOrderByActualizadoEnDesc();
}
