package co.edu.unbosque.accioneselbosque.administracion.repository;

import co.edu.unbosque.accioneselbosque.administracion.model.ParametroComision;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Optional;

public interface ParametroComisionRepository extends CrudRepository<ParametroComision, Long> {

    @Query("SELECT p FROM ParametroComision p WHERE p.fechaInicio <= :hoy AND (p.fechaFin IS NULL OR p.fechaFin >= :hoy) ORDER BY p.fechaInicio DESC")
    Optional<ParametroComision> findParametroActivo(LocalDate hoy);
}
