package co.edu.unbosque.accioneselbosque.integracion.orquestadores;

import co.edu.unbosque.accioneselbosque.autenticacion.model.Inversionista;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Usuario;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.InversionistaRepository;
import co.edu.unbosque.accioneselbosque.integracion.adaptadores.alpaca.IIntegracionAlpaca;
import co.edu.unbosque.accioneselbosque.trazabilidad.interfaces.IAuditLog;
import co.edu.unbosque.accioneselbosque.trazabilidad.model.TipoEvento;
import org.springframework.stereotype.Service;

@Service
public class OrquestadorRegistro {

    private final IIntegracionAlpaca alpaca;
    private final InversionistaRepository inversionistaRepository;
    private final IAuditLog auditLog;

    public OrquestadorRegistro(IIntegracionAlpaca alpaca,
                               InversionistaRepository inversionistaRepository,
                               IAuditLog auditLog) {
        this.alpaca = alpaca;
        this.inversionistaRepository = inversionistaRepository;
        this.auditLog = auditLog;
    }

    public void crearCuentaAlpaca(Usuario usuario) {
        Inversionista inversionista = inversionistaRepository.findById(usuario.getId())
                .orElseThrow(() -> new IllegalStateException("Inversionista no encontrado para usuario " + usuario.getId()));
        String alpacaId = alpaca.crearCuenta(usuario, inversionista);

        if (alpacaId != null) {
            inversionista.setAlpacaAccountId(alpacaId);
            inversionista.setPendienteCuentaAlpaca(false);
            inversionistaRepository.save(inversionista);
            auditLog.registrar(TipoEvento.REGISTRO_EXITOSO, usuario.getCorreo(), "Cuenta Alpaca creada: " + alpacaId);
        } else {
            inversionista.setPendienteCuentaAlpaca(true);
            inversionistaRepository.save(inversionista);
            auditLog.registrar(TipoEvento.REGISTRO_FALLO_ALPACA, usuario.getCorreo(), "Fallo al crear cuenta Alpaca");
        }
    }
}
