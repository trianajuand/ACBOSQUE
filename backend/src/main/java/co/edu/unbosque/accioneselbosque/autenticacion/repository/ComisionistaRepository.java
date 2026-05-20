package co.edu.unbosque.accioneselbosque.autenticacion.repository;

import co.edu.unbosque.accioneselbosque.autenticacion.model.Comisionista;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ComisionistaRepository extends CrudRepository<Comisionista, Long> {

    Optional<Comisionista> findByUsuarioId(Long usuarioId);
}
