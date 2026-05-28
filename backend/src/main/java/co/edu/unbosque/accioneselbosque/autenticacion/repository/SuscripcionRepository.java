package co.edu.unbosque.accioneselbosque.autenticacion.repository;

import co.edu.unbosque.accioneselbosque.autenticacion.model.Suscripcion;
import org.springframework.data.repository.CrudRepository;

public interface SuscripcionRepository extends CrudRepository<Suscripcion, Long> {
    void deleteByInversionistaId(Long inversionistaId);
}
