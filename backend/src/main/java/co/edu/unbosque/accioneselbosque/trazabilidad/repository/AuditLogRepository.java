package co.edu.unbosque.accioneselbosque.trazabilidad.repository;

import co.edu.unbosque.accioneselbosque.trazabilidad.model.EventoAuditoria;
import org.springframework.data.repository.CrudRepository;

public interface AuditLogRepository extends CrudRepository<EventoAuditoria, Long> {
}
