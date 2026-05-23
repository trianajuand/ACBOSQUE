package co.edu.unbosque.accioneselbosque.administracion.service;

import co.edu.unbosque.accioneselbosque.administracion.dto.*;
import co.edu.unbosque.accioneselbosque.administracion.interfaces.IAdministracion;
import co.edu.unbosque.accioneselbosque.administracion.interfaces.IGestorParametros;
import co.edu.unbosque.accioneselbosque.administracion.model.FeriadoMercado;
import co.edu.unbosque.accioneselbosque.administracion.model.MercadoConfig;
import co.edu.unbosque.accioneselbosque.administracion.model.ParametroComision;
import co.edu.unbosque.accioneselbosque.administracion.repository.AdministradorRepository;
import co.edu.unbosque.accioneselbosque.administracion.repository.FeriadoMercadoRepository;
import co.edu.unbosque.accioneselbosque.administracion.repository.MercadoConfigRepository;
import co.edu.unbosque.accioneselbosque.administracion.repository.ParametroComisionRepository;
import co.edu.unbosque.accioneselbosque.autenticacion.dto.UsuarioGestionDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.interfaces.IGestionCuentas;
import co.edu.unbosque.accioneselbosque.ordenes.dto.ResumenNegocioDTO;
import co.edu.unbosque.accioneselbosque.ordenes.interfaces.IOrden;
import co.edu.unbosque.accioneselbosque.trazabilidad.interfaces.IAuditLog;
import co.edu.unbosque.accioneselbosque.trazabilidad.model.TipoEvento;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AdministracionService implements IAdministracion, IGestorParametros {

    private static final BigDecimal COMISION_DEFAULT = BigDecimal.valueOf(2.0);
    private static final BigDecimal SPLIT_PLATAFORMA_DEFAULT = BigDecimal.valueOf(60.0);
    private static final BigDecimal SPLIT_COMISIONISTA_DEFAULT = BigDecimal.valueOf(40.0);

    private final AdministradorRepository administradorRepo;
    private final MercadoConfigRepository mercadoRepo;
    private final FeriadoMercadoRepository feriadoRepo;
    private final ParametroComisionRepository parametroRepo;
    private final IGestionCuentas gestionCuentas;
    private final IOrden ordenService;
    private final IAuditLog auditLog;

    public AdministracionService(AdministradorRepository administradorRepo,
                                  MercadoConfigRepository mercadoRepo,
                                  FeriadoMercadoRepository feriadoRepo,
                                  ParametroComisionRepository parametroRepo,
                                  IGestionCuentas gestionCuentas,
                                  @Lazy IOrden ordenService,
                                  IAuditLog auditLog) {
        this.administradorRepo = administradorRepo;
        this.mercadoRepo = mercadoRepo;
        this.feriadoRepo = feriadoRepo;
        this.parametroRepo = parametroRepo;
        this.gestionCuentas = gestionCuentas;
        this.ordenService = ordenService;
        this.auditLog = auditLog;
    }

    // =========================================================
    // IGestorParametros — lecturas de parámetros de comisión
    // =========================================================

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

    // =========================================================
    // IAdministracion — configuración de mercados y feriados
    // =========================================================

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

    // =========================================================
    // Validación de administrador
    // =========================================================

    @Transactional(readOnly = true)
    public void validarAdministrador(String correo) {
        if (!gestionCuentas.esAdministradorActivo(correo)) {
            auditLog.registrar(TipoEvento.ACCESO_DENEGADO_ADMIN, correo,
                    "Acceso denegado al modulo administrador");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Requiere rol ADMINISTRADOR activo con MFA habilitado");
        }
    }

    // =========================================================
    // Gestión de mercados
    // =========================================================

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

    // =========================================================
    // Gestión de parámetros de comisión
    // =========================================================

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

    // =========================================================
    // Gestión de usuarios — delegado a IGestionCuentas
    // =========================================================

    @Transactional
    public UsuarioAdminDTO crearComisionista(CrearComisionistaDTO dto, String adminCorreo) {
        UsuarioGestionDTO result = gestionCuentas.crearComisionista(
                dto.getNombreCompleto(), dto.getCorreo(),
                dto.getContrasenia(), dto.getEspecialidadesMercado(), adminCorreo);
        return mapearDesdeGestion(result);
    }

    @Transactional(readOnly = true)
    public List<UsuarioAdminDTO> listarUsuarios(String rol) {
        return gestionCuentas.listarUsuarios(rol)
                .stream()
                .map(this::mapearDesdeGestion)
                .toList();
    }

    @Transactional
    public UsuarioAdminDTO asignarComisionista(Long inversionistaId, Long comisionistaId,
                                               String adminCorreo) {
        UsuarioGestionDTO result = gestionCuentas.asignarComisionista(
                inversionistaId, comisionistaId, adminCorreo);
        return mapearDesdeGestion(result);
    }

    @Transactional
    public UsuarioAdminDTO cambiarEstadoUsuario(Long usuarioId, CambiarEstadoCuentaDTO dto,
                                                String adminCorreo) {
        UsuarioGestionDTO result = gestionCuentas.cambiarEstadoUsuario(
                usuarioId, dto.getEstado(), dto.getMotivo(), adminCorreo);
        return mapearDesdeGestion(result);
    }

    @Transactional
    public void eliminarUsuario(Long usuarioId, String adminCorreo) {
        gestionCuentas.eliminarUsuario(usuarioId, adminCorreo);
    }

    // =========================================================
    // Dashboard ejecutivo — agrega datos de usuarios + ordenes
    // =========================================================

    @Transactional(readOnly = true)
    public DashboardEjecutivoDTO obtenerDashboard(String desde, String hasta, String mercadoFiltro) {
        LocalDateTime inicio = parseInicio(desde);
        LocalDateTime fin = parseFin(hasta);

        DashboardEjecutivoDTO dto = new DashboardEjecutivoDTO();
        List<UsuarioGestionDTO> usuarios = gestionCuentas.listarUsuarios(null);
        for (UsuarioGestionDTO u : usuarios) {
            if ("ACTIVA".equals(u.getEstadoCuenta())) {
                dto.setUsuariosActivos(dto.getUsuariosActivos() + 1);
            }
            if (u.getFechaCreacion() != null && enRango(u.getFechaCreacion(), inicio, fin)) {
                dto.setCrecimientoUsuarios(dto.getCrecimientoUsuarios() + 1);
            }
        }

        ResumenNegocioDTO resumen = ordenService.obtenerResumenNegocio(inicio, fin, mercadoFiltro);
        dto.setTransacciones((int) resumen.getTransacciones());
        dto.setVolumenTransacciones(resumen.getVolumenTransacciones());
        dto.setComisionesGeneradas(resumen.getComisionesGeneradas());

        List<TendenciaMercadoDTO> tendencias = resumen.getTendenciasPorMercado().stream()
                .map(rm -> {
                    TendenciaMercadoDTO t = new TendenciaMercadoDTO();
                    t.setMercado(rm.getMercado());
                    t.setOperaciones(rm.getOperaciones());
                    t.setVolumen(rm.getVolumen());
                    t.setComisiones(rm.getComisiones());
                    return t;
                })
                .toList();
        dto.setTendenciasPorMercado(tendencias);
        return dto;
    }

    // =========================================================
    // Helpers privados
    // =========================================================

    private Optional<ParametroComision> parametroActivo() {
        return parametroRepo.findFirstByActivoTrueOrderByActualizadoEnDesc();
    }

    private Optional<MercadoConfig> buscarMercado(String mercado) {
        String codigo = codigoMercado(mercado);
        Optional<MercadoConfig> directo = mercadoRepo.findByCodigoIgnoreCase(codigo);
        if (directo.isPresent()) return directo;
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

    private UsuarioAdminDTO mapearDesdeGestion(UsuarioGestionDTO g) {
        UsuarioAdminDTO dto = new UsuarioAdminDTO();
        dto.setId(g.getId());
        dto.setNombreCompleto(g.getNombreCompleto());
        dto.setCorreo(g.getCorreo());
        dto.setRol(g.getRol());
        dto.setEstadoCuenta(g.getEstadoCuenta());
        dto.setMfaHabilitado(g.isMfaHabilitado());
        dto.setFechaCreacion(g.getFechaCreacion());
        dto.setComisionistaAsignado(g.getComisionistaAsignado());
        return dto;
    }

    private String codigoMercado(String mercado) {
        if (mercado == null || mercado.isBlank()) return "NYSE";
        String codigo = mercado.trim().toUpperCase(Locale.ROOT);
        if ("US".equals(codigo) || "NYSE/NASDAQ".equals(codigo)) return "NYSE";
        return codigo;
    }

    private boolean enRango(LocalDateTime fecha, LocalDateTime inicio, LocalDateTime fin) {
        if (fecha == null) return false;
        return !fecha.isBefore(inicio) && !fecha.isAfter(fin);
    }

    private LocalDateTime parseInicio(String valor) {
        if (valor == null || valor.isBlank()) return LocalDate.now().minusMonths(1).atStartOfDay();
        return LocalDate.parse(valor).atStartOfDay();
    }

    private LocalDateTime parseFin(String valor) {
        if (valor == null || valor.isBlank()) return LocalDate.now().atTime(23, 59, 59);
        return LocalDate.parse(valor).atTime(23, 59, 59);
    }
}
