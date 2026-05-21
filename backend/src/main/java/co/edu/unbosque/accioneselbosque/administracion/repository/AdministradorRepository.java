package co.edu.unbosque.accioneselbosque.administracion.repository;

import co.edu.unbosque.accioneselbosque.administracion.model.Administrador;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface AdministradorRepository extends CrudRepository<Administrador, Long> {
    Optional<Administrador> findByUsuarioId(Long usuarioId);
}
