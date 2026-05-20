package co.edu.unbosque.accioneselbosque.integracion.orquestadores;

import co.edu.unbosque.accioneselbosque.autenticacion.model.Usuario;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.UsuarioRepository;
import co.edu.unbosque.accioneselbosque.integracion.adaptadores.alpaca.IIntegracionAlpaca;
import co.edu.unbosque.accioneselbosque.trazabilidad.interfaces.IAuditLog;
import co.edu.unbosque.accioneselbosque.trazabilidad.model.TipoEvento;
import org.springframework.stereotype.Service;

@Service
public class OrquestadorRegistro {

    private final IIntegracionAlpaca alpaca;
    private final UsuarioRepository usuarioRepository;
    private final IAuditLog auditLog;

    public OrquestadorRegistro(IIntegracionAlpaca alpaca, UsuarioRepository usuarioRepository, IAuditLog auditLog) {
        this.alpaca = alpaca;
        this.usuarioRepository = usuarioRepository;
        this.auditLog = auditLog;
    }

    public void crearCuentaAlpaca(Usuario usuario) {
        String alpacaId = alpaca.crearCuenta(usuario);
        if (alpacaId != null) {
            usuario.setAlpacaAccountId(alpacaId);
            usuario.setPendienteCuentaAlpaca(false);
            usuarioRepository.save(usuario);
            auditLog.registrar(TipoEvento.REGISTRO_EXITOSO, usuario.getCorreo(), "Cuenta Alpaca creada: " + alpacaId);
        } else {
            usuario.setPendienteCuentaAlpaca(true);
            usuarioRepository.save(usuario);
            auditLog.registrar(TipoEvento.REGISTRO_FALLO_ALPACA, usuario.getCorreo(), "Fallo al crear cuenta Alpaca");
        }
    }
}
