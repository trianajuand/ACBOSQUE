package co.edu.unbosque.accioneselbosque.ordenes.repository;

import co.edu.unbosque.accioneselbosque.ordenes.model.Holding;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface HoldingRepository extends CrudRepository<Holding, Long> {

    List<Holding> findByUsuarioId(Long usuarioId);

    Optional<Holding> findByUsuarioIdAndSimbolo(Long usuarioId, String simbolo);
}
