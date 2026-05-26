package co.edu.unbosque.accioneselbosque.autenticacion.service;

import co.edu.unbosque.accioneselbosque.autenticacion.model.Comisionista;
import co.edu.unbosque.accioneselbosque.autenticacion.model.ComisionistaEspecialidad;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Especialidad;
import co.edu.unbosque.accioneselbosque.autenticacion.model.EstadoCuenta;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Rol;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Usuario;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.ComisionistaEspecialidadRepository;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.ComisionistaRepository;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.EspecialidadRepository;
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
    private final EspecialidadRepository especialidadRepository;
    private final ComisionistaEspecialidadRepository comisionistaEspecialidadRepository;
    private final PasswordEncoder passwordEncoder;
    private final String passwordDefault;

    public DatosInicialesComisionistas(UsuarioRepository usuarioRepository,
                                       ComisionistaRepository comisionistaRepository,
                                       EspecialidadRepository especialidadRepository,
                                       ComisionistaEspecialidadRepository comisionistaEspecialidadRepository,
                                       PasswordEncoder passwordEncoder,
                                       @Value("${app.comisionistas.password-default:Comisionista123!}") String passwordDefault) {
        this.usuarioRepository = usuarioRepository;
        this.comisionistaRepository = comisionistaRepository;
        this.especialidadRepository = especialidadRepository;
        this.comisionistaEspecialidadRepository = comisionistaEspecialidadRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordDefault = passwordDefault;
    }

    @Override
    public void run(String... args) {
        List<SeedComisionista> seeds = List.of(
                new SeedComisionista("Ana Mercado Tech", "ana.tech@accioneselbosque.local",
                        List.of("AAPL", "MSFT", "TSLA", "GOOGL", "NVDA", "META", "AMZN")),
                new SeedComisionista("Carlos Renta Financiera", "carlos.finanzas@accioneselbosque.local",
                        List.of("JPM", "AMZN", "SONY")),
                new SeedComisionista("Lucia Mercados Globales", "lucia.global@accioneselbosque.local",
                        List.of("RIO.LON", "SONY", "TSLA", "AMZN"))
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
            usuario = usuarioRepository.save(usuario);

            // Comisionista uses shared PK: id = usuario.id
            Comisionista comisionista = comisionistaRepository.findById(usuario.getId()).orElseGet(Comisionista::new);
            boolean perfilNuevo = comisionista.getId() == null;
            comisionista.setId(usuario.getId());
            if (perfilNuevo) {
                comisionista.setFechaCreacion(LocalDateTime.now());
            }
            comisionista.setFechaActualizacion(LocalDateTime.now());
            comisionistaRepository.save(comisionista);

            // Seed specialties and link them to this comisionista
            for (String ticker : seed.tickers()) {
                Especialidad especialidad = especialidadRepository.findByTitulo(ticker).orElseGet(() -> {
                    Especialidad e = new Especialidad();
                    e.setTitulo(ticker);
                    return especialidadRepository.save(e);
                });
                if (!comisionistaEspecialidadRepository.existsByComisionistaIdAndEspecialidadId(
                        usuario.getId(), especialidad.getId())) {
                    ComisionistaEspecialidad link = new ComisionistaEspecialidad();
                    link.setComisionistaId(usuario.getId());
                    link.setEspecialidadId(especialidad.getId());
                    comisionistaEspecialidadRepository.save(link);
                }
            }
        }
    }

    private record SeedComisionista(String nombre, String correo, List<String> tickers) {}
}
