package co.edu.unbosque.accioneselbosque.trazabilidad.service;

import co.edu.unbosque.accioneselbosque.trazabilidad.interfaces.IAuditLog;
import co.edu.unbosque.accioneselbosque.trazabilidad.model.EventoAuditoria;
import co.edu.unbosque.accioneselbosque.trazabilidad.model.TipoEvento;
import co.edu.unbosque.accioneselbosque.trazabilidad.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditLogService implements IAuditLog {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Async
    public void registrar(TipoEvento tipo, String correoUsuario, String detalle) {
        EventoAuditoria evento = new EventoAuditoria();
        evento.setTipoEvento(tipo);
        evento.setCorreoUsuario(correoUsuario);
        evento.setDetalle(detalle);
        evento.setTimestamp(LocalDateTime.now());

        auditLogRepository.save(evento);
        log.info("[AUDIT] {} | {} | {}", tipo, correoUsuario, detalle);
    }
}
