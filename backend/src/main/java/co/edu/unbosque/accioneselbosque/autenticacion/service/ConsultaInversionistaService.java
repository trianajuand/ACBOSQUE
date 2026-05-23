package co.edu.unbosque.accioneselbosque.autenticacion.service;

import co.edu.unbosque.accioneselbosque.autenticacion.interfaces.IConsultaInversionista;
import co.edu.unbosque.accioneselbosque.autenticacion.model.EstadoCuenta;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Inversionista;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Usuario;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.InversionistaRepository;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.UsuarioRepository;
import co.edu.unbosque.accioneselbosque.trazabilidad.interfaces.IAuditLog;
import co.edu.unbosque.accioneselbosque.trazabilidad.model.TipoEvento;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ConsultaInversionistaService implements IConsultaInversionista {

    private final UsuarioRepository usuarioRepo;
    private final InversionistaRepository inversionistaRepo;
    private final IAuditLog auditLog;

    public ConsultaInversionistaService(UsuarioRepository usuarioRepo,
                                         InversionistaRepository inversionistaRepo,
                                         IAuditLog auditLog) {
        this.usuarioRepo = usuarioRepo;
        this.inversionistaRepo = inversionistaRepo;
        this.auditLog = auditLog;
    }

    @Override
    @Transactional(readOnly = true)
    public void validarPuedeOperar(Long usuarioId) {
        Usuario usuario = usuarioRepo.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (usuario.getEstadoCuenta() == EstadoCuenta.OPERACIONES_RESTRINGIDAS) {
            auditLog.registrar(TipoEvento.OPERACION_RESTRINGIDA_BLOQUEADA, usuario.getCorreo(),
                    "Intento de crear o enviar una nueva orden con operaciones restringidas");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "La cuenta tiene operaciones restringidas y no puede colocar nuevas ordenes");
        }
        if (usuario.getEstadoCuenta() != EstadoCuenta.ACTIVA) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "La cuenta no esta activa para operar");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String obtenerAlpacaAccountId(Long usuarioId) {
        return inversionistaRepo.findByUsuarioId(usuarioId)
                .map(Inversionista::getAlpacaAccountId)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean necesitaCuentaAlpaca(Long usuarioId) {
        return inversionistaRepo.findByUsuarioId(usuarioId)
                .map(i -> i.getAlpacaAccountId() == null)
                .orElse(true);
    }

    @Override
    @Transactional
    public void actualizarAlpacaAccountId(Long usuarioId, String alpacaAccountId) {
        inversionistaRepo.findByUsuarioId(usuarioId).ifPresent(inversionista -> {
            inversionista.setAlpacaAccountId(alpacaAccountId);
            inversionista.setPendienteCuentaAlpaca(false);
            inversionistaRepo.save(inversionista);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public String obtenerVistaPortafolio(Long usuarioId) {
        return inversionistaRepo.findByUsuarioId(usuarioId)
                .map(Inversionista::getVistaPortafolio)
                .orElse("RESUMEN");
    }
}
