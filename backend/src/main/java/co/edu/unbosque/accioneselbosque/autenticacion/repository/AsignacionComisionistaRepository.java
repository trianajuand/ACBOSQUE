package co.edu.unbosque.accioneselbosque.autenticacion.repository;

import co.edu.unbosque.accioneselbosque.autenticacion.model.AsignacionComisionista;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface AsignacionComisionistaRepository extends CrudRepository<AsignacionComisionista, Long> {

    Optional<AsignacionComisionista> findByInversionistaIdAndActivaTrue(Long inversionistaId);

    List<AsignacionComisionista> findByComisionistaIdAndActivaTrueOrderByFechaAsignacionDesc(Long comisionistaId);

    boolean existsByComisionistaIdAndInversionistaIdAndActivaTrue(Long comisionistaId, Long inversionistaId);

    long countByComisionistaIdAndActivaTrue(Long comisionistaId);
}
