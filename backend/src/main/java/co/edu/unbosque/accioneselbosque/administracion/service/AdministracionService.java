package co.edu.unbosque.accioneselbosque.administracion.service;

import co.edu.unbosque.accioneselbosque.administracion.dto.*;
import co.edu.unbosque.accioneselbosque.administracion.interfaces.IAdministracion;
import co.edu.unbosque.accioneselbosque.administracion.model.FeriadoMercado;
import co.edu.unbosque.accioneselbosque.administracion.model.MercadoConfig;
import co.edu.unbosque.accioneselbosque.administracion.model.ParametroComision;
import co.edu.unbosque.accioneselbosque.administracion.repository.AdministradorRepository;
import co.edu.unbosque.accioneselbosque.administracion.repository.FeriadoMercadoRepository;
import co.edu.unbosque.accioneselbosque.administracion.repository.MercadoConfigRepository;
import co.edu.unbosque.accioneselbosque.administracion.repository.ParametroComisionRepository;
import co.edu.unbosque.accioneselbosque.autenticacion.model.*;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.AsignacionComisionistaRepository;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.ComisionistaRepository;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.InversionistaRepository;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.UsuarioRepository;
import co.edu.unbosque.accioneselbosque.ordenes.model.Comision;
import co.edu.unbosque.accioneselbosque.ordenes.model.Orden;
import co.edu.unbosque.accioneselbosque.ordenes.repository.ComisionRepository;
import co.edu.unbosque.accioneselbosque.ordenes.repository.OrdenRepository;
import co.edu.unbosque.accioneselbosque.trazabilidad.interfaces.IAuditLog;
import co.edu.unbosque.accioneselbosque.trazabilidad.model.TipoEvento;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AdministracionService implements IAdministracion {

    private static final BigDecimal COMISION_DEFAULT = BigDecimal.valueOf(2.0);
    private static final BigDecimal SPLIT_PLATAFORMA_DEFAULT = BigDecimal.valueOf(60.0);
    private static final BigDecimal SPLIT_COMISIONISTA_DEFAULT = BigDecimal.valueOf(40.0);

    private final AdministradorRepository administradorRepo;
    private final MercadoConfigRepository mercadoRepo;
    private final FeriadoMercadoRepository feriadoRepo;
    private final ParametroComisionRepository parametroRepo;
    private final UsuarioRepository usuarioRepo;
    private final InversionistaRepository inversionistaRepo;
    private final ComisionistaRepository comisionistaRepo;
    private final AsignacionComisionistaRepository asignacionRepo;
    private final OrdenRepository ordenRepo;
    private final ComisionRepository comisionRepo;
    private final PasswordEncoder passwordEncoder;
    private final IAuditLog auditLog;

    public AdministracionService(AdministradorRepository administradorRepo,
                                 MercadoConfigRepository mercadoRepo,
                                 FeriadoMercadoRepository feriadoRepo,
                                 ParametroComisionRepository parametroRepo,
                                 UsuarioRepository usuarioRepo,
                                 InversionistaRepository inversionistaRepo,
                                 ComisionistaRepository comisionistaRepo,
                                 AsignacionComisionistaRepository asignacionRepo,
                                 OrdenRepository ordenRepo,
                                 ComisionRepository comisionRepo,
                                 PasswordEncoder passwordEncoder,
                                 IAuditLog auditLog) {
        this.administradorRepo = administradorRepo;
        this.mercadoRepo = mercadoRepo;
        this.feriadoRepo = feriadoRepo;
        this.parametroRepo = parametroRepo;
        this.usuarioRepo = usuarioRepo;
        this.inversionistaRepo = inversionistaRepo;
        this.comisionistaRepo = comisionistaRepo;
        this.asignacionRepo = asignacionRepo;
        this.ordenRepo = ordenRepo;
        this.comisionRepo = comisionRepo;
        this.passwordEncoder = passwordEncoder;
        this.auditLog = auditLog;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal obtenerPorcentajeComision() {
        return parametroActivo().map(ParametroComision::getPorcentajeComision).orElse(COMISION_DEFAULT);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal obtenerSplitPlataforma() {
        return parametroActivo().map(ParametroComision::getSplitPlataforma).orElse(SPLIT_PLATAFORMA_DEFAULT);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal obtenerSplitComisionista() {
        return parametroActivo().map(ParametroComision::getSplitComisionista).orElse(SPLIT_COMISIONISTA_DEFAULT);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MercadoConfigDTO> obtenerConfiguracionMercado(String mercado) {
        return buscarMercado(mercado).map(this::mapearMercado);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean esFeriadoMercado(String mercado, LocalDate fecha) {
        return feriadoRepo.existsByMercadoCodigoIgnoreCaseAndFecha(codigoMercado(mercado), fecha);
    }

    @Transactional(readOnly = true)
    public void validarAdministrador(String correo) {
        Usuario usuario = usuarioRepo.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
        if (usuario.getRol() != Rol.ADMINISTRADOR) {
            auditLog.registrar(TipoEvento.ACCESO_DENEGADO_ADMIN, correo, "Intento de acceso al modulo administrador");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Requiere rol ADMINISTRADOR");
        }
        if (usuario.getEstadoCuenta() != EstadoCuenta.ACTIVA) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La cuenta administradora no esta activa");
        }
        if (!usuario.isMfaHabilitado()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "MFA es obligatorio para administrador");
        }
        if (administradorRepo.findByUsuarioId(usuario.getId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "El perfil administrador debe crearse manualmente en la tabla administrador");
        }
    }

    @Transactional(readOnly = true)
    public List<MercadoConfigDTO> listarMercados() {
        return mercadoRepo.findAllByOrderByCodigoAsc().stream().map(this::mapearMercado).toList();
    }

    @Transactional
    public MercadoConfigDTO guardarMercado(String codigo, MercadoConfigDTO dto, String adminCorreo) {
        MercadoConfig mercado = mercadoRepo.findByCodigoIgnoreCase(codigo).orElseGet(MercadoConfig::new);
        mercado.setCodigo(codigoMercado(codigo));
        mercado.setNombre(dto.getNombre());
        mercado.setZonaHoraria(dto.getZonaHoraria());
        mercado.setHoraApertura(dto.getHoraApertura());
        mercado.setHoraCierre(dto.getHoraCierre());
        mercado.setHabilitado(dto.isHabilitado());
        mercado.setCierreAnticipado(dto.getCierreAnticipado());
        mercado.setFechaActualizacion(LocalDateTime.now());
        mercado = mercadoRepo.save(mercado);
        auditLog.registrar(TipoEvento.PARAMETRO_ADMIN_ACTUALIZADO, adminCorreo,
                "Mercado actualizado: " + mercado.getCodigo());
        return mapearMercado(mercado);
    }

    @Transactional(readOnly = true)
    public List<FeriadoMercadoDTO> listarFeriados(String codigo) {
        return feriadoRepo.findByMercadoCodigoIgnoreCaseOrderByFechaAsc(codigoMercado(codigo))
                .stream().map(this::mapearFeriado).toList();
    }

    @Transactional
    public FeriadoMercadoDTO crearFeriado(String codigo, FeriadoMercadoDTO dto, String adminCorreo) {
        String mercadoCodigo = codigoMercado(codigo);
        if (feriadoRepo.existsByMercadoCodigoIgnoreCaseAndFecha(mercadoCodigo, dto.getFecha())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El feriado ya existe para ese mercado");
        }
        FeriadoMercado feriado = new FeriadoMercado();
        feriado.setMercadoCodigo(mercadoCodigo);
        feriado.setFecha(dto.getFecha());
        feriado.setDescripcion(dto.getDescripcion());
        feriado.setCreadoEn(LocalDateTime.now());
        feriado = feriadoRepo.save(feriado);
        auditLog.registrar(TipoEvento.PARAMETRO_ADMIN_ACTUALIZADO, adminCorreo,
                "Feriado agregado: " + mercadoCodigo + " " + dto.getFecha());
        return mapearFeriado(feriado);
    }

    @Transactional
    public void eliminarFeriado(String codigo, Long feriadoId, String adminCorreo) {
        FeriadoMercado feriado = feriadoRepo.findByIdAndMercadoCodigoIgnoreCase(feriadoId, codigoMercado(codigo))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Feriado no encontrado"));
        feriadoRepo.delete(feriado);
        auditLog.registrar(TipoEvento.PARAMETRO_ADMIN_ACTUALIZADO, adminCorreo,
                "Feriado eliminado: " + feriado.getMercadoCodigo() + " " + feriado.getFecha());
    }

    @Transactional(readOnly = true)
    public ParametroComisionDTO obtenerParametrosComision() {
        ParametroComisionDTO dto = new ParametroComisionDTO();
        dto.setPorcentajeComision(obtenerPorcentajeComision());
        dto.setSplitPlataforma(obtenerSplitPlataforma());
        dto.setSplitComisionista(obtenerSplitComisionista());
        return dto;
    }

    @Transactional
    public ParametroComisionDTO actualizarParametrosComision(ParametroComisionDTO dto, String adminCorreo) {
        BigDecimal totalSplit = dto.getSplitPlataforma().add(dto.getSplitComisionista());
        if (totalSplit.compareTo(BigDecimal.valueOf(100)) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El split debe sumar 100%");
        }
        parametroRepo.findAll().forEach(p -> {
            p.setActivo(false);
            parametroRepo.save(p);
        });
        ParametroComision nuevo = new ParametroComision();
        nuevo.setPorcentajeComision(dto.getPorcentajeComision());
        nuevo.setSplitPlataforma(dto.getSplitPlataforma());
        nuevo.setSplitComisionista(dto.getSplitComisionista());
        nuevo.setActivo(true);
        nuevo.setActualizadoEn(LocalDateTime.now());
        parametroRepo.save(nuevo);
        auditLog.registrar(TipoEvento.PARAMETRO_ADMIN_ACTUALIZADO, adminCorreo,
                "Parametros de comision actualizados");
        return obtenerParametrosComision();
    }

    @Transactional
    public UsuarioAdminDTO crearComisionista(CrearComisionistaDTO dto, String adminCorreo) {
        if (usuarioRepo.existsByCorreo(dto.getCorreo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya esta registrado");
        }
        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(dto.getNombreCompleto());
        usuario.setCorreo(dto.getCorreo().trim().toLowerCase(Locale.ROOT));
        usuario.setContrasenia(passwordEncoder.encode(dto.getContrasenia()));
        usuario.setRol(Rol.COMISIONISTA);
        usuario.setEstadoCuenta(EstadoCuenta.ACTIVA);
        usuario.setMfaHabilitado(true);
        usuario.setFechaCreacion(LocalDateTime.now());
        usuario.setFechaActualizacion(LocalDateTime.now());
        usuario = usuarioRepo.save(usuario);

        Comisionista comisionista = new Comisionista();
        comisionista.setUsuarioId(usuario.getId());
        comisionista.setEspecialidadesMercado(normalizarCsv(dto.getEspecialidadesMercado()));
        comisionista.setFechaCreacion(LocalDateTime.now());
        comisionista.setFechaActualizacion(LocalDateTime.now());
        comisionistaRepo.save(comisionista);

        auditLog.registrar(TipoEvento.USUARIO_ADMIN_GESTIONADO, adminCorreo,
                "Comisionista creado: " + usuario.getCorreo());
        return mapearUsuario(usuario);
    }

    @Transactional(readOnly = true)
    public List<UsuarioAdminDTO> listarUsuarios(String rol) {
        List<UsuarioAdminDTO> usuarios = new ArrayList<>();
        for (Usuario usuario : usuarioRepo.findAll()) {
            if (rol == null || rol.isBlank() || usuario.getRol().name().equalsIgnoreCase(rol)) {
                usuarios.add(mapearUsuario(usuario));
            }
        }
        usuarios.sort(Comparator.comparing(UsuarioAdminDTO::getFechaCreacion,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return usuarios;
    }

    @Transactional
    public UsuarioAdminDTO asignarComisionista(Long inversionistaId, Long comisionistaId, String adminCorreo) {
        Usuario inversionista = usuarioRepo.findById(inversionistaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inversionista no encontrado"));
        Usuario comisionista = usuarioRepo.findById(comisionistaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comisionista no encontrado"));
        if (inversionista.getRol() != Rol.INVERSIONISTA && inversionista.getRol() != Rol.INVERSIONISTA_PREMIUM) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuario destino debe ser inversionista");
        }
        if (comisionista.getRol() != Rol.COMISIONISTA || comisionista.getEstadoCuenta() != EstadoCuenta.ACTIVA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El comisionista debe estar activo");
        }

        asignacionRepo.findByInversionistaIdAndActivaTrue(inversionistaId).ifPresent(actual -> {
            actual.setActiva(false);
            asignacionRepo.save(actual);
        });
        AsignacionComisionista asignacion = new AsignacionComisionista();
        asignacion.setInversionistaId(inversionistaId);
        asignacion.setComisionistaId(comisionistaId);
        asignacion.setMotivo("Asignacion manual por administrador");
        asignacion.setInteresesCoincidentes("");
        asignacion.setActiva(true);
        asignacion.setFechaAsignacion(LocalDateTime.now());
        asignacionRepo.save(asignacion);

        auditLog.registrar(TipoEvento.COMISIONISTA_ASIGNADO, adminCorreo,
                "Asignacion manual: " + inversionista.getCorreo() + " -> " + comisionista.getCorreo());
        return mapearUsuario(inversionista);
    }

    @Transactional
    public UsuarioAdminDTO cambiarEstadoUsuario(Long usuarioId, CambiarEstadoCuentaDTO dto, String adminCorreo) {
        Usuario usuario = usuarioRepo.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        EstadoCuenta nuevoEstado = EstadoCuenta.valueOf(dto.getEstado().trim().toUpperCase(Locale.ROOT));
        usuario.setEstadoCuenta(nuevoEstado);
        usuario.setFechaActualizacion(LocalDateTime.now());
        usuario = usuarioRepo.save(usuario);
        auditLog.registrar(TipoEvento.CAMBIO_ESTADO_CUENTA, adminCorreo,
                "Usuario " + usuario.getCorreo() + " -> " + nuevoEstado + " | " + Objects.toString(dto.getMotivo(), ""));
        return mapearUsuario(usuario);
    }

    @Transactional
    public void eliminarUsuario(Long usuarioId, String adminCorreo) {
        Usuario usuario = usuarioRepo.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (usuario.getRol() == Rol.ADMINISTRADOR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se elimina administradores desde este modulo");
        }
        usuario.setEstadoCuenta(EstadoCuenta.INACTIVA);
        usuario.setFechaActualizacion(LocalDateTime.now());
        usuarioRepo.save(usuario);
        asignacionRepo.findByInversionistaIdAndActivaTrue(usuarioId).ifPresent(a -> {
            a.setActiva(false);
            asignacionRepo.save(a);
        });
        auditLog.registrar(TipoEvento.USUARIO_ADMIN_GESTIONADO, adminCorreo,
                "Baja logica de usuario: " + usuario.getCorreo());
    }

    @Transactional(readOnly = true)
    public DashboardEjecutivoDTO obtenerDashboard(String desde, String hasta, String mercadoFiltro) {
        LocalDateTime inicio = parseInicio(desde);
        LocalDateTime fin = parseFin(hasta);
        DashboardEjecutivoDTO dto = new DashboardEjecutivoDTO();
        Map<String, TendenciaMercadoDTO> tendencias = new TreeMap<>();

        for (Usuario usuario : usuarioRepo.findAll()) {
            if (usuario.getEstadoCuenta() == EstadoCuenta.ACTIVA) {
                dto.setUsuariosActivos(dto.getUsuariosActivos() + 1);
            }
            if (enRango(usuario.getFechaCreacion(), inicio, fin)) {
                dto.setCrecimientoUsuarios(dto.getCrecimientoUsuarios() + 1);
            }
        }

        for (Orden orden : ordenRepo.findAll()) {
            if (!enRango(orden.getCreadaEn(), inicio, fin)) {
                continue;
            }
            String mercado = detectarMercado(orden.getSimbolo());
            if (mercadoFiltro != null && !mercadoFiltro.isBlank()
                    && !mercado.equalsIgnoreCase(codigoMercado(mercadoFiltro))) {
                continue;
            }
            dto.setTransacciones(dto.getTransacciones() + 1);
            dto.setVolumenTransacciones(dto.getVolumenTransacciones().add(valor(orden.getMontoTotal())));
            TendenciaMercadoDTO tendencia = tendencias.computeIfAbsent(mercado, key -> {
                TendenciaMercadoDTO t = new TendenciaMercadoDTO();
                t.setMercado(key);
                return t;
            });
            tendencia.setOperaciones(tendencia.getOperaciones() + 1);
            tendencia.setVolumen(tendencia.getVolumen().add(valor(orden.getMontoTotal())));
        }

        for (Comision comision : comisionRepo.findAll()) {
            if (!enRango(comision.getCreadaEn(), inicio, fin)) {
                continue;
            }
            dto.setComisionesGeneradas(dto.getComisionesGeneradas().add(valor(comision.getMontoComision())));
            ordenRepo.findById(comision.getOrdenId()).ifPresent(orden -> {
                String mercado = detectarMercado(orden.getSimbolo());
                if (mercadoFiltro == null || mercadoFiltro.isBlank()
                        || mercado.equalsIgnoreCase(codigoMercado(mercadoFiltro))) {
                    tendencias.computeIfAbsent(mercado, key -> {
                        TendenciaMercadoDTO t = new TendenciaMercadoDTO();
                        t.setMercado(key);
                        return t;
                    }).setComisiones(tendencias.get(mercado).getComisiones().add(valor(comision.getMontoComision())));
                }
            });
        }
        dto.setTendenciasPorMercado(new ArrayList<>(tendencias.values()));
        return dto;
    }

    private Optional<ParametroComision> parametroActivo() {
        return parametroRepo.findFirstByActivoTrueOrderByActualizadoEnDesc();
    }

    private Optional<MercadoConfig> buscarMercado(String mercado) {
        String codigo = codigoMercado(mercado);
        Optional<MercadoConfig> directo = mercadoRepo.findByCodigoIgnoreCase(codigo);
        if (directo.isPresent()) {
            return directo;
        }
        if ("NYSE/NASDAQ".equalsIgnoreCase(codigo)) {
            return mercadoRepo.findByCodigoIgnoreCase("NYSE");
        }
        return Optional.empty();
    }

    private MercadoConfigDTO mapearMercado(MercadoConfig mercado) {
        MercadoConfigDTO dto = new MercadoConfigDTO();
        dto.setId(mercado.getId());
        dto.setCodigo(mercado.getCodigo());
        dto.setNombre(mercado.getNombre());
        dto.setZonaHoraria(mercado.getZonaHoraria());
        dto.setHoraApertura(mercado.getHoraApertura());
        dto.setHoraCierre(mercado.getHoraCierre());
        dto.setHabilitado(mercado.isHabilitado());
        dto.setCierreAnticipado(mercado.getCierreAnticipado());
        return dto;
    }

    private FeriadoMercadoDTO mapearFeriado(FeriadoMercado feriado) {
        FeriadoMercadoDTO dto = new FeriadoMercadoDTO();
        dto.setId(feriado.getId());
        dto.setMercadoCodigo(feriado.getMercadoCodigo());
        dto.setFecha(feriado.getFecha());
        dto.setDescripcion(feriado.getDescripcion());
        return dto;
    }

    private UsuarioAdminDTO mapearUsuario(Usuario usuario) {
        UsuarioAdminDTO dto = new UsuarioAdminDTO();
        dto.setId(usuario.getId());
        dto.setNombreCompleto(usuario.getNombreCompleto());
        dto.setCorreo(usuario.getCorreo());
        dto.setRol(usuario.getRol().name());
        dto.setEstadoCuenta(usuario.getEstadoCuenta().name());
        dto.setMfaHabilitado(usuario.isMfaHabilitado());
        dto.setFechaCreacion(usuario.getFechaCreacion());
        asignacionRepo.findByInversionistaIdAndActivaTrue(usuario.getId())
                .flatMap(a -> usuarioRepo.findById(a.getComisionistaId()))
                .ifPresent(c -> dto.setComisionistaAsignado(c.getNombreCompleto()));
        return dto;
    }

    private String codigoMercado(String mercado) {
        if (mercado == null || mercado.isBlank()) {
            return "NYSE";
        }
        String codigo = mercado.trim().toUpperCase(Locale.ROOT);
        if ("US".equals(codigo) || "NYSE/NASDAQ".equals(codigo)) {
            return "NYSE";
        }
        return codigo;
    }

    private String normalizarCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return "";
        }
        return String.join(",", Arrays.stream(csv.split("[,;\\s]+"))
                .map(v -> v.trim().toUpperCase(Locale.ROOT))
                .filter(v -> !v.isBlank())
                .distinct()
                .toList());
    }

    private String detectarMercado(String simbolo) {
        if (simbolo == null) return "DESCONOCIDO";
        String s = simbolo.toUpperCase(Locale.ROOT);
        if (s.endsWith(".T")) return "TSE";
        if (s.endsWith(".L")) return "LSE";
        if (s.endsWith(".AX")) return "ASX";
        return "NYSE";
    }

    private boolean enRango(LocalDateTime fecha, LocalDateTime inicio, LocalDateTime fin) {
        if (fecha == null) {
            return false;
        }
        return !fecha.isBefore(inicio) && !fecha.isAfter(fin);
    }

    private LocalDateTime parseInicio(String valor) {
        if (valor == null || valor.isBlank()) {
            return LocalDate.now().minusMonths(1).atStartOfDay();
        }
        return LocalDate.parse(valor).atStartOfDay();
    }

    private LocalDateTime parseFin(String valor) {
        if (valor == null || valor.isBlank()) {
            return LocalDate.now().atTime(23, 59, 59);
        }
        return LocalDate.parse(valor).atTime(23, 59, 59);
    }

    private BigDecimal valor(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }
}
