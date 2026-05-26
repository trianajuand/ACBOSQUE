package co.edu.unbosque.accioneselbosque.autenticacion.repository;

import co.edu.unbosque.accioneselbosque.autenticacion.model.Comisionista;
import org.springframework.data.repository.CrudRepository;

public interface ComisionistaRepository extends CrudRepository<Comisionista, Long> {
    // PK = usuario.id, usar findById(usuarioId)
}
