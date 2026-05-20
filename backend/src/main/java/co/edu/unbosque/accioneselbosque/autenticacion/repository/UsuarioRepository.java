package co.edu.unbosque.accioneselbosque.autenticacion.repository;

import co.edu.unbosque.accioneselbosque.autenticacion.model.Usuario;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface UsuarioRepository extends CrudRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);
}
