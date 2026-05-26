package co.edu.unbosque.accioneselbosque.autenticacion.service;

import co.edu.unbosque.accioneselbosque.autenticacion.dto.ClienteAsignadoDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.dto.ComisionistaAsignadoDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.interfaces.IAsignacionComisionista;
import co.edu.unbosque.accioneselbosque.autenticacion.model.AsignacionComisionista;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Comisionista;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Especialidad;
import co.edu.unbosque.accioneselbosque.autenticacion.model.EstadoCuenta;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Inversionista;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Rol;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Usuario;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.AsignacionComisionistaRepository;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.ComisionistaEspecialidadRepository;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.ComisionistaRepository;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.EspecialidadRepository;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.InversionistaRepository;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.UsuarioRepository;
import co.edu.unbosque.accioneselbosque.shared.exceptions.UsuarioNoEncontradoException;
import co.edu.unbosque.accioneselbosque.trazabilidad.interfaces.IAuditLog;
import co.edu.unbosque.accioneselbosque.trazabilidad.model.TipoEvento;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class AsignacionComisionistaService implements IAsignacionComisionista {

    private final AsignacionComisionistaRepository asignacionRepo;
    private final UsuarioRepository usuarioRepo;
    private final InversionistaRepository inversionistaRepo;
    private final ComisionistaRepository comisionistaRepo;
    private final ComisionistaEspecialidadRepository comisionistaEspecialidadRepo;
    private final EspecialidadRepository especialidadRepo;
    private final IAuditLog auditLog;

    public AsignacionComisionistaService(AsignacionComisionistaRepository asignacionRepo,
                                         UsuarioRepository usuarioRepo,
                                         InversionistaRepository inversionistaRepo,
                                         ComisionistaRepository comisionistaRepo,
                                         ComisionistaEspecialidadRepository comisionistaEspecialidadRepo,
                                         EspecialidadRepository especialidadRepo,
                                         IAuditLog auditLog) {
        this.asignacionRepo = asignacionRepo;
        this.usuarioRepo = usuarioRepo;
        this.inversionistaRepo = inversionistaRepo;
        this.comisionistaRepo = comisionistaRepo;
        this.comisionistaEspecialidadRepo = comisionistaEspecialidadRepo;
        this.especialidadRepo = especialidadRepo;
        this.auditLog = auditLog;
    }

    @Override
    @Transactional
    public Optional<Long> asignarSiSolicitado(Usuario inversionista) {
        if (inversionista == null || !solicitaComisionista(inversionista)) {
            return Optional.empty();
        }
        Optional<AsignacionComisionista> existente =
                asignacionRepo.findByInversionistaIdAndActivaTrue(inversionista.getId());
        if (existente.isPresent()) {
            return Optional.of(existente.get().getComisionistaId());
        }

        List<Usuario> comisionistas = usuarioRepo.findByRolAndEstadoCuenta(Rol.COMISIONISTA, EstadoCuenta.ACTIVA);
        if (comisionistas.isEmpty()) {
            auditLog.registrar(TipoEvento.COMISIONISTA_ASIGNACION_FALLIDA, inversionista.getCorreo(),
                    "No hay comisionistas activos para asignar");
            return Optional.empty();
        }

        Set<String> intereses = normalizarCsv(interesesMercado(inversionista));
        Usuario elegido = comisionistas.stream()
                .sorted(Comparator
                        .<Usuario>comparingInt(c -> coincidencias(intereses, normalizarCsv(especialidadesMercado(c))).size())
                        .reversed()
                        .thenComparingLong(c -> asignacionRepo.countByComisionistaIdAndActivaTrue(c.getId())))
                .findFirst()
                .orElse(comisionistas.get(0));

        AsignacionComisionista asignacion = new AsignacionComisionista();
        asignacion.setInversionistaId(inversionista.getId());
        asignacion.setComisionistaId(elegido.getId());
        asignacion.setActiva(true);
        asignacion.setFechaAsignacion(LocalDateTime.now());
        asignacionRepo.save(asignacion);

        auditLog.registrar(TipoEvento.COMISIONISTA_ASIGNADO, inversionista.getCorreo(),
                "Comisionista asignado: " + elegido.getCorreo());
        return Optional.of(elegido.getId());
    }

    @Override
    public void validarClienteAsignado(Long comisionistaId, Long inversionistaId) {
        if (!esClienteAsignado(comisionistaId, inversionistaId)) {
            auditLog.registrar(TipoEvento.ACCESO_DENEGADO_CLIENTE_NO_ASIGNADO,
                    String.valueOf(comisionistaId),
                    "Intento de acceso a inversionista no asignado: " + inversionistaId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No tienes permiso para acceder a este inversionista");
        }
    }

    @Override
    public boolean esClienteAsignado(Long comisionistaId, Long inversionistaId) {
        return asignacionRepo.existsByComisionistaIdAndInversionistaIdAndActivaTrue(comisionistaId, inversionistaId);
    }

    @Override
    public boolean usuarioTieneComisionista(Long inversionistaId) {
        return asignacionRepo.findByInversionistaIdAndActivaTrue(inversionistaId).isPresent();
    }

    @Override
    public Optional<Long> obtenerComisionistaIdDeInversionista(Long inversionistaId) {
        return asignacionRepo.findByInversionistaIdAndActivaTrue(inversionistaId)
                .map(AsignacionComisionista::getComisionistaId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ComisionistaAsignadoDTO> obtenerComisionistaAsignado(Long inversionistaId) {
        Optional<AsignacionComisionista> asignacionOpt = asignacionRepo.findByInversionistaIdAndActivaTrue(inversionistaId);
        if (asignacionOpt.isEmpty()) {
            return Optional.empty();
        }

        AsignacionComisionista asignacion = asignacionOpt.get();
        Usuario comisionista = usuarioRepo.findById(asignacion.getComisionistaId())
                .orElseThrow(() -> new UsuarioNoEncontradoException("Comisionista no encontrado"));

        ComisionistaAsignadoDTO dto = new ComisionistaAsignadoDTO();
        dto.setId(comisionista.getId());
        dto.setNombreCompleto(comisionista.getNombreCompleto());
        dto.setCorreo(comisionista.getCorreo());
        dto.setEspecialidadesMercado(new ArrayList<>(normalizarCsv(especialidadesMercado(comisionista))));
        dto.setFechaAsignacion(asignacion.getFechaAsignacion());
        return Optional.of(dto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteAsignadoDTO> listarClientesAsignados(Long comisionistaId) {
        List<ClienteAsignadoDTO> clientes = new ArrayList<>();
        for (AsignacionComisionista asignacion :
                asignacionRepo.findByComisionistaIdAndActivaTrueOrderByFechaAsignacionDesc(comisionistaId)) {
            Usuario usuario = usuarioRepo.findById(asignacion.getInversionistaId())
                    .orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado"));
            ClienteAsignadoDTO dto = new ClienteAsignadoDTO();
            dto.setId(usuario.getId());
            dto.setNombreCompleto(usuario.getNombreCompleto());
            dto.setCorreo(usuario.getCorreo());
            dto.setNivelExperiencia(nivelExperiencia(usuario));
            dto.setInteresesMercado(new ArrayList<>(normalizarCsv(interesesMercado(usuario))));
            dto.setFechaAsignacion(asignacion.getFechaAsignacion());
            clientes.add(dto);
        }
        return clientes;
    }

    private boolean solicitaComisionista(Usuario usuario) {
        // InversionistaRepository now uses findById(usuarioId) — shared PK
        return inversionistaRepo.findById(usuario.getId())
                .map(Inversionista::isSolicitaComisionista)
                .orElse(false);
    }

    private String interesesMercado(Usuario usuario) {
        return inversionistaRepo.findById(usuario.getId())
                .map(Inversionista::getInteresesMercado)
                .filter(v -> v != null && !v.isBlank())
                .orElse("");
    }

    private String nivelExperiencia(Usuario usuario) {
        return inversionistaRepo.findById(usuario.getId())
                .map(Inversionista::getNivelExperiencia)
                .filter(v -> v != null && !v.isBlank())
                .orElse("");
    }

    private String especialidadesMercado(Usuario usuario) {
        List<String> titulos = comisionistaEspecialidadRepo.findByComisionistaId(usuario.getId())
                .stream()
                .map(ce -> especialidadRepo.findById(ce.getEspecialidadId())
                        .map(Especialidad::getTitulo)
                        .orElse(""))
                .filter(t -> !t.isBlank())
                .toList();
        return String.join(",", titulos);
    }

    private Set<String> normalizarCsv(String csv) {
        Set<String> valores = new HashSet<>();
        if (csv == null || csv.isBlank()) {
            return valores;
        }
        Arrays.stream(csv.split(","))
                .map(v -> v.trim().toUpperCase(Locale.ROOT))
                .filter(v -> !v.isBlank())
                .forEach(valores::add);
        return valores;
    }

    private List<String> coincidencias(Set<String> intereses, Set<String> especialidades) {
        return intereses.stream()
                .filter(especialidades::contains)
                .sorted()
                .toList();
    }
}
