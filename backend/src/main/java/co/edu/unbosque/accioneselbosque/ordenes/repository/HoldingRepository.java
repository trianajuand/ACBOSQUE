package co.edu.unbosque.accioneselbosque.ordenes.repository;

import co.edu.unbosque.accioneselbosque.ordenes.model.Holding;
import co.edu.unbosque.accioneselbosque.ordenes.model.HoldingId;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface HoldingRepository extends CrudRepository<Holding, HoldingId> {

    List<Holding> findByInversionistaId(Long inversionistaId);

    Optional<Holding> findByInversionistaIdAndActivoId(Long inversionistaId, Long activoId);
}
