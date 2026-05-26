package co.edu.unbosque.accioneselbosque.ordenes.repository;

import co.edu.unbosque.accioneselbosque.ordenes.model.CuentaFondos;
import org.springframework.data.repository.CrudRepository;

public interface CuentaFondosRepository extends CrudRepository<CuentaFondos, Long> {
    // PK = inversionista_id, usar findById(inversionistaId)
}
