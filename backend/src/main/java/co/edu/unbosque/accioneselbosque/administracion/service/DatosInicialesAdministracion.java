package co.edu.unbosque.accioneselbosque.administracion.service;

import co.edu.unbosque.accioneselbosque.administracion.model.MercadoConfig;
import co.edu.unbosque.accioneselbosque.administracion.model.ParametroComision;
import co.edu.unbosque.accioneselbosque.administracion.repository.MercadoConfigRepository;
import co.edu.unbosque.accioneselbosque.administracion.repository.ParametroComisionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
@Order(1)
public class DatosInicialesAdministracion implements CommandLineRunner {

    private final MercadoConfigRepository mercadoRepo;
    private final ParametroComisionRepository parametroRepo;
    private final BigDecimal porcentajeDefault;
    private final BigDecimal splitPlataformaDefault;
    private final BigDecimal splitComisionistaDefault;

    public DatosInicialesAdministracion(
            MercadoConfigRepository mercadoRepo,
            ParametroComisionRepository parametroRepo,
            @Value("${app.comision.porcentaje:2.0}") BigDecimal porcentajeDefault,
            @Value("${app.comision.split-plataforma:60.0}") BigDecimal splitPlataformaDefault,
            @Value("${app.comision.split-comisionista:40.0}") BigDecimal splitComisionistaDefault) {
        this.mercadoRepo = mercadoRepo;
        this.parametroRepo = parametroRepo;
        this.porcentajeDefault = porcentajeDefault;
        this.splitPlataformaDefault = splitPlataformaDefault;
        this.splitComisionistaDefault = splitComisionistaDefault;
    }

    @Override
    public void run(String... args) {
        crearMercadosBase();
        crearParametroComisionBase();
    }

    private void crearMercadosBase() {
        List<MercadoSeed> seeds = List.of(
                new MercadoSeed("NYSE", "New York Stock Exchange", "America/New_York", LocalTime.of(9, 30), LocalTime.of(16, 0)),
                new MercadoSeed("NASDAQ", "NASDAQ", "America/New_York", LocalTime.of(9, 30), LocalTime.of(16, 0)),
                new MercadoSeed("TSE", "Tokyo Stock Exchange", "Asia/Tokyo", LocalTime.of(9, 0), LocalTime.of(15, 0)),
                new MercadoSeed("LSE", "London Stock Exchange", "Europe/London", LocalTime.of(8, 0), LocalTime.of(16, 30)),
                new MercadoSeed("ASX", "Australian Securities Exchange", "Australia/Sydney", LocalTime.of(10, 0), LocalTime.of(16, 0))
        );

        for (MercadoSeed seed : seeds) {
            mercadoRepo.findByCodigoIgnoreCase(seed.codigo()).orElseGet(() -> {
                MercadoConfig mercado = new MercadoConfig();
                mercado.setCodigo(seed.codigo());
                mercado.setNombre(seed.nombre());
                mercado.setZonaHoraria(seed.zonaHoraria());
                mercado.setHoraApertura(seed.horaApertura());
                mercado.setHoraCierre(seed.horaCierre());
                mercado.setHabilitado(true);
                mercado.setFechaActualizacion(LocalDateTime.now());
                return mercadoRepo.save(mercado);
            });
        }
    }

    private void crearParametroComisionBase() {
        if (parametroRepo.findParametroActivo(java.time.LocalDate.now()).isPresent()) {
            return;
        }
        ParametroComision parametro = new ParametroComision();
        parametro.setPorcentajeComision(porcentajeDefault);
        parametro.setSplitPlataforma(splitPlataformaDefault);
        parametro.setSplitComisionista(splitComisionistaDefault);
        parametro.setFechaInicio(java.time.LocalDate.now());
        parametro.setFechaFin(null);
        parametroRepo.save(parametro);
    }

    private record MercadoSeed(String codigo, String nombre, String zonaHoraria,
                               LocalTime horaApertura, LocalTime horaCierre) {}
}
