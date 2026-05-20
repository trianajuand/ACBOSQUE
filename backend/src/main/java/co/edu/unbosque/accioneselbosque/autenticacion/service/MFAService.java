package co.edu.unbosque.accioneselbosque.autenticacion.service;

import co.edu.unbosque.accioneselbosque.autenticacion.model.CodigoVerificacion;
import co.edu.unbosque.accioneselbosque.autenticacion.model.CodigoVerificacion.TipoCodigo;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.CodigoVerificacionRepository;
import co.edu.unbosque.accioneselbosque.shared.exceptions.InvalidMfaException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class MFAService {

    private final CodigoVerificacionRepository codigoVerificacionRepository;
    private final int expiracionMinutos;
    private static final SecureRandom random = new SecureRandom();

    public MFAService(
            CodigoVerificacionRepository codigoVerificacionRepository,
            @Value("${app.seguridad.codigo-expiracion-minutos}") int expiracionMinutos) {
        this.codigoVerificacionRepository = codigoVerificacionRepository;
        this.expiracionMinutos = expiracionMinutos;
    }

    @Transactional
    public String generarYGuardarCodigo(String correo, TipoCodigo tipo) {
        codigoVerificacionRepository.deleteByCorreoAndTipo(correo, tipo);

        String codigo = String.format("%06d", random.nextInt(1_000_000));

        CodigoVerificacion cv = new CodigoVerificacion();
        cv.setCorreo(correo);
        cv.setCodigo(codigo);
        cv.setTipo(tipo);
        cv.setExpiracion(LocalDateTime.now().plusMinutes(expiracionMinutos));
        cv.setUsado(false);
        codigoVerificacionRepository.save(cv);

        return codigo;
    }

    @Transactional
    public void validarCodigo(String correo, String codigo, TipoCodigo tipo) {
        Optional<CodigoVerificacion> opt = codigoVerificacionRepository
                .findByCorreoAndTipoAndUsadoFalse(correo, tipo);

        if (opt.isEmpty()) {
            throw new InvalidMfaException("Código no encontrado o ya utilizado.");
        }

        CodigoVerificacion cv = opt.get();

        if (LocalDateTime.now().isAfter(cv.getExpiracion())) {
            throw new InvalidMfaException("El código ha expirado.");
        }

        if (!cv.getCodigo().equals(codigo)) {
            throw new InvalidMfaException("Código incorrecto.");
        }

        cv.setUsado(true);
        codigoVerificacionRepository.save(cv);
    }
}
