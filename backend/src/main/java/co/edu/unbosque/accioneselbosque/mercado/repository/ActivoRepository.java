package co.edu.unbosque.accioneselbosque.mercado.repository;

import co.edu.unbosque.accioneselbosque.mercado.model.Activo;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ActivoRepository extends CrudRepository<Activo, Long> {

    Optional<Activo> findByTicker(String ticker);
}
