package co.edu.unbosque.accioneselbosque.autenticacion.repository;

import co.edu.unbosque.accioneselbosque.autenticacion.model.Inversionista;
import org.springframework.data.repository.CrudRepository;

public interface InversionistaRepository extends CrudRepository<Inversionista, Long> {
    // PK = usuario.id, usar findById(usuarioId)
}
