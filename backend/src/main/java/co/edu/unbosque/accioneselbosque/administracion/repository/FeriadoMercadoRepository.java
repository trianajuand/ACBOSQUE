package co.edu.unbosque.accioneselbosque.administracion.repository;

import co.edu.unbosque.accioneselbosque.administracion.model.FeriadoMercado;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FeriadoMercadoRepository extends CrudRepository<FeriadoMercado, Long> {
    boolean existsByMercadoCodigoIgnoreCaseAndFecha(String mercadoCodigo, LocalDate fecha);
    List<FeriadoMercado> findByMercadoCodigoIgnoreCaseOrderByFechaAsc(String mercadoCodigo);
    Optional<FeriadoMercado> findByIdAndMercadoCodigoIgnoreCase(Long id, String mercadoCodigo);
}
