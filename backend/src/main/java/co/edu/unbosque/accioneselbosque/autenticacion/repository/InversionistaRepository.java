package co.edu.unbosque.accioneselbosque.autenticacion.repository;

import co.edu.unbosque.accioneselbosque.autenticacion.model.Inversionista;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface InversionistaRepository extends CrudRepository<Inversionista, Long> {

    Optional<Inversionista> findByUsuarioId(Long usuarioId);
}
