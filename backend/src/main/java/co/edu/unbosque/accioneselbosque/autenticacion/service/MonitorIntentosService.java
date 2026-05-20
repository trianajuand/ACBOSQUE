package co.edu.unbosque.accioneselbosque.autenticacion.service;

import co.edu.unbosque.accioneselbosque.autenticacion.model.IntentoFallido;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.IntentoFallidoRepository;
import co.edu.unbosque.accioneselbosque.shared.exceptions.AccountLockedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class MonitorIntentosService {

    private final IntentoFallidoRepository intentoFallidoRepository;
    private final int maxIntentos;
    private final int bloqueoMinutos;

    public MonitorIntentosService(
            IntentoFallidoRepository intentoFallidoRepository,
            @Value("${app.seguridad.max-intentos}") int maxIntentos,
            @Value("${app.seguridad.bloqueo-minutos}") int bloqueoMinutos) {
        this.intentoFallidoRepository = intentoFallidoRepository;
        this.maxIntentos = maxIntentos;
        this.bloqueoMinutos = bloqueoMinutos;
    }

    public boolean estaBloqueado(String correo) {
        Optional<IntentoFallido> opt = intentoFallidoRepository.findByCorreo(correo);
        if (opt.isEmpty()) return false;
        IntentoFallido intento = opt.get();
        if (intento.getBloqueadoHasta() == null) return false;
        return LocalDateTime.now().isBefore(intento.getBloqueadoHasta());
    }

    public void verificarBloqueo(String correo) {
        if (estaBloqueado(correo)) {
            throw new AccountLockedException("Cuenta bloqueada temporalmente. Intenta en " + bloqueoMinutos + " minutos.");
        }
    }

    @Transactional
    public boolean registrarIntentoFallido(String correo) {
        IntentoFallido intento = intentoFallidoRepository.findByCorreo(correo)
                .orElseGet(() -> {
                    IntentoFallido nuevo = new IntentoFallido();
                    nuevo.setCorreo(correo);
                    nuevo.setContador(0);
                    return nuevo;
                });

        intento.setContador(intento.getContador() + 1);
        intento.setUltimoIntento(LocalDateTime.now());

        boolean seBloqueo = false;
        if (intento.getContador() >= maxIntentos) {
            intento.setBloqueadoHasta(LocalDateTime.now().plusMinutes(bloqueoMinutos));
            seBloqueo = true;
        }

        intentoFallidoRepository.save(intento);
        return seBloqueo;
    }

    @Transactional
    public void reiniciarIntentos(String correo) {
        intentoFallidoRepository.findByCorreo(correo).ifPresent(intento -> {
            intento.setContador(0);
            intento.setBloqueadoHasta(null);
            intentoFallidoRepository.save(intento);
        });
    }
}
