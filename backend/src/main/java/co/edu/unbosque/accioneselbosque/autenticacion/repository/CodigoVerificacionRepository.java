package co.edu.unbosque.accioneselbosque.autenticacion.repository;

import co.edu.unbosque.accioneselbosque.autenticacion.model.CodigoVerificacion;
import co.edu.unbosque.accioneselbosque.autenticacion.model.CodigoVerificacion.TipoCodigo;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface CodigoVerificacionRepository extends CrudRepository<CodigoVerificacion, Long> {

    Optional<CodigoVerificacion> findByCorreoAndTipoAndUsadoFalse(String correo, TipoCodigo tipo);

    void deleteByCorreoAndTipo(String correo, TipoCodigo tipo);

    void deleteByCorreo(String correo);
}
