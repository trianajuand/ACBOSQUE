package co.edu.unbosque.accioneselbosque.ordenes.repository;

import co.edu.unbosque.accioneselbosque.ordenes.model.CuentaFondos;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface CuentaFondosRepository extends CrudRepository<CuentaFondos, Long> {

    Optional<CuentaFondos> findByUsuarioId(Long usuarioId);
}
