package co.edu.unbosque.accioneselbosque.mercado.repository;

import co.edu.unbosque.accioneselbosque.mercado.model.PrecioCache;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface PrecioCacheRepository extends CrudRepository<PrecioCache, Long> {

    Optional<PrecioCache> findBySimbolo(String simbolo);

    List<PrecioCache> findBySimboloIn(List<String> simbolos);
}
