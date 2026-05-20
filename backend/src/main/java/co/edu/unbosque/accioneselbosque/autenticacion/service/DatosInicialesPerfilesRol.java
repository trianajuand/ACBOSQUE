package co.edu.unbosque.accioneselbosque.autenticacion.service;

import co.edu.unbosque.accioneselbosque.autenticacion.model.Comisionista;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Inversionista;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Rol;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Usuario;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.ComisionistaRepository;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.InversionistaRepository;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DatosInicialesPerfilesRol implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final InversionistaRepository inversionistaRepository;
    private final ComisionistaRepository comisionistaRepository;

    public DatosInicialesPerfilesRol(UsuarioRepository usuarioRepository,
                                     InversionistaRepository inversionistaRepository,
                                     ComisionistaRepository comisionistaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.inversionistaRepository = inversionistaRepository;
        this.comisionistaRepository = comisionistaRepository;
    }

    @Override
    public void run(String... args) {
        for (Usuario usuario : usuarioRepository.findAll()) {
            if (usuario.getRol() == Rol.INVERSIONISTA || usuario.getRol() == Rol.INVERSIONISTA_PREMIUM) {
                crearInversionistaSiFalta(usuario);
            }
            if (usuario.getRol() == Rol.COMISIONISTA) {
                crearComisionistaSiFalta(usuario);
            }
        }
    }

    private void crearInversionistaSiFalta(Usuario usuario) {
        if (inversionistaRepository.findByUsuarioId(usuario.getId()).isPresent()) {
            return;
        }
        Inversionista inversionista = new Inversionista();
        inversionista.setUsuarioId(usuario.getId());
        inversionista.setNivelExperiencia("PRINCIPIANTE");
        inversionista.setInteresesMercado("AAPL,MSFT,TSLA");
        inversionista.setPais("CO");
        inversionista.setSolicitaComisionista(false);
        inversionista.setNotificacionEmail(true);
        inversionista.setNotificacionSms(false);
        inversionista.setNotificacionWhatsapp(false);
        inversionista.setTiposNotificacion("ORDENES,MERCADO,SEGURIDAD");
        inversionista.setTipoOrdenDefault("MARKET");
        inversionista.setVistaPortafolio("LISTA");
        inversionista.setPlanSuscripcion("BASICO");
        inversionista.setEsPremium(false);
        inversionista.setPendienteCuentaAlpaca(false);
        inversionista.setFechaCreacion(LocalDateTime.now());
        inversionistaRepository.save(inversionista);
    }

    private void crearComisionistaSiFalta(Usuario usuario) {
        if (comisionistaRepository.findByUsuarioId(usuario.getId()).isPresent()) {
            return;
        }
        Comisionista comisionista = new Comisionista();
        comisionista.setUsuarioId(usuario.getId());
        comisionista.setFechaCreacion(LocalDateTime.now());
        comisionistaRepository.save(comisionista);
    }
}
