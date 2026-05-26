package co.edu.unbosque.accioneselbosque.administracion.repository;

import co.edu.unbosque.accioneselbosque.administracion.model.Administrador;
import org.springframework.data.repository.CrudRepository;

public interface AdministradorRepository extends CrudRepository<Administrador, Long> {
    // PK = usuario.id, usar findById(usuarioId)
}
