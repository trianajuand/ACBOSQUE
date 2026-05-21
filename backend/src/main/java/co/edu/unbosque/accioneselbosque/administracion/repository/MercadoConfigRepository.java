package co.edu.unbosque.accioneselbosque.administracion.repository;

import co.edu.unbosque.accioneselbosque.administracion.model.MercadoConfig;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface MercadoConfigRepository extends CrudRepository<MercadoConfig, Long> {
    Optional<MercadoConfig> findByCodigoIgnoreCase(String codigo);
    List<MercadoConfig> findAllByOrderByCodigoAsc();
}
