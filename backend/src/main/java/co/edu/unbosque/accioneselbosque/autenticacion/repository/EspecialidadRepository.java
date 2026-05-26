package co.edu.unbosque.accioneselbosque.autenticacion.repository;

import co.edu.unbosque.accioneselbosque.autenticacion.model.Especialidad;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface EspecialidadRepository extends CrudRepository<Especialidad, Long> {
    Optional<Especialidad> findByTitulo(String titulo);
}
