package co.edu.unbosque.accioneselbosque.autenticacion.service;

import co.edu.unbosque.accioneselbosque.autenticacion.model.Comisionista;
import co.edu.unbosque.accioneselbosque.autenticacion.model.EstadoCuenta;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Rol;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Usuario;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.ComisionistaRepository;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DatosInicialesComisionistas implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final ComisionistaRepository comisionistaRepository;
    private final PasswordEncoder passwordEncoder;
    private final String passwordDefault;

    public DatosInicialesComisionistas(UsuarioRepository usuarioRepository,
                                       ComisionistaRepository comisionistaRepository,
                                       PasswordEncoder passwordEncoder,
                                       @Value("${app.comisionistas.password-default:Comisionista123!}") String passwordDefault) {
        this.usuarioRepository = usuarioRepository;
        this.comisionistaRepository = comisionistaRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordDefault = passwordDefault;
    }

    @Override
    public void run(String... args) {
        List<SeedComisionista> seeds = List.of(
                new SeedComisionista("Ana Mercado Tech", "ana.tech@accioneselbosque.local",
                        "AAPL,MSFT,NVDA,GOOGL,AMZN,META,TSLA,NFLX,AMD,INTC"),
                new SeedComisionista("Carlos Renta Financiera", "carlos.finanzas@accioneselbosque.local",
                        "JPM,BAC,GS,MS,V,MA,AXP,WFC,C,BLK"),
                new SeedComisionista("Lucia Mercados Globales", "lucia.global@accioneselbosque.local",
                        "HSBA.L,BP.L,SHEL.L,AZN.L,7203.T,6758.T,9984.T,BHP.AX,CBA.AX,CSL.AX")
        );

        for (SeedComisionista seed : seeds) {
            Usuario usuario = usuarioRepository.findByCorreo(seed.correo()).orElseGet(Usuario::new);
            boolean nuevo = usuario.getId() == null;
            usuario.setNombreCompleto(seed.nombre());
            usuario.setCorreo(seed.correo());
            if (nuevo) {
                usuario.setContrasenia(passwordEncoder.encode(passwordDefault));
                usuario.setFechaCreacion(LocalDateTime.now());
            }
            usuario.setRol(Rol.COMISIONISTA);
            usuario.setEstadoCuenta(EstadoCuenta.ACTIVA);
            usuario.setMfaHabilitado(true);
            usuario.setFechaActualizacion(LocalDateTime.now());
            usuario = usuarioRepository.save(usuario);

            Comisionista comisionista = comisionistaRepository.findByUsuarioId(usuario.getId()).orElseGet(Comisionista::new);
            boolean perfilNuevo = comisionista.getId() == null;
            comisionista.setUsuarioId(usuario.getId());
            comisionista.setEspecialidadesMercado(seed.especialidades());
            if (perfilNuevo) {
                comisionista.setFechaCreacion(LocalDateTime.now());
            }
            comisionista.setFechaActualizacion(LocalDateTime.now());
            comisionistaRepository.save(comisionista);
        }
    }

    private record SeedComisionista(String nombre, String correo, String especialidades) {}
}
