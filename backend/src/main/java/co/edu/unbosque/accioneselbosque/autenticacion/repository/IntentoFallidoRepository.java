package co.edu.unbosque.accioneselbosque.autenticacion.repository;

import co.edu.unbosque.accioneselbosque.autenticacion.model.IntentoFallido;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface IntentoFallidoRepository extends CrudRepository<IntentoFallido, Long> {

    Optional<IntentoFallido> findByCorreo(String correo);

    void deleteByCorreo(String correo);
}
