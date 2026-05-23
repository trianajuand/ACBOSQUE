package co.edu.unbosque.accioneselbosque.autenticacion.service;

import co.edu.unbosque.accioneselbosque.autenticacion.dto.UsuarioGestionDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.interfaces.IGestionCuentas;
import co.edu.unbosque.accioneselbosque.autenticacion.model.*;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.*;
import co.edu.unbosque.accioneselbosque.trazabilidad.interfaces.IAuditLog;
import co.edu.unbosque.accioneselbosque.trazabilidad.model.TipoEvento;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class GestionCuentasService implements IGestionCuentas {

    private final UsuarioRepository usuarioRepo;
    private final ComisionistaRepository comisionistaRepo;
    private final AsignacionComisionistaRepository asignacionRepo;
    private final PasswordEncoder passwordEncoder;
    private final IAuditLog auditLog;

    public GestionCuentasService(UsuarioRepository usuarioRepo,
                                  ComisionistaRepository comisionistaRepo,
                                  AsignacionComisionistaRepository asignacionRepo,
                                  PasswordEncoder passwordEncoder,
                                  IAuditLog auditLog) {
        this.usuarioRepo = usuarioRepo;
        this.comisionistaRepo = comisionistaRepo;
        this.asignacionRepo = asignacionRepo;
        this.passwordEncoder = passwordEncoder;
        this.auditLog = auditLog;
    }

    @Override
    @Transactional
    public UsuarioGestionDTO crearComisionista(String nombreCompleto, String correo,
                                               String contrasenia, String especialidades,
                                               String adminCorreo) {
        if (usuarioRepo.existsByCorreo(correo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya esta registrado");
        }
        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(nombreCompleto);
        usuario.setCorreo(correo.trim().toLowerCase(Locale.ROOT));
        usuario.setContrasenia(passwordEncoder.encode(contrasenia));
        usuario.setRol(Rol.COMISIONISTA);
        usuario.setEstadoCuenta(EstadoCuenta.ACTIVA);
        usuario.setMfaHabilitado(true);
        usuario.setFechaCreacion(LocalDateTime.now());
        usuario.setFechaActualizacion(LocalDateTime.now());
        usuario = usuarioRepo.save(usuario);

        Comisionista comisionista = new Comisionista();
        comisionista.setUsuarioId(usuario.getId());
        comisionista.setEspecialidadesMercado(normalizarCsv(especialidades));
        comisionista.setFechaCreacion(LocalDateTime.now());
        comisionista.setFechaActualizacion(LocalDateTime.now());
        comisionistaRepo.save(comisionista);

        auditLog.registrar(TipoEvento.USUARIO_ADMIN_GESTIONADO, adminCorreo,
                "Comisionista creado: " + usuario.getCorreo());
        return mapearUsuario(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioGestionDTO> listarUsuarios(String rolFiltro) {
        List<UsuarioGestionDTO> resultado = new ArrayList<>();
        for (Usuario usuario : usuarioRepo.findAll()) {
            if (rolFiltro == null || rolFiltro.isBlank()
                    || usuario.getRol().name().equalsIgnoreCase(rolFiltro)) {
                resultado.add(mapearUsuario(usuario));
            }
        }
        resultado.sort(Comparator.comparing(UsuarioGestionDTO::getFechaCreacion,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return resultado;
    }

    @Override
    @Transactional
    public UsuarioGestionDTO cambiarEstadoUsuario(Long usuarioId, String nuevoEstado,
                                                   String motivo, String adminCorreo) {
        Usuario usuario = usuarioRepo.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        EstadoCuenta estado = EstadoCuenta.valueOf(nuevoEstado.trim().toUpperCase(Locale.ROOT));
        usuario.setEstadoCuenta(estado);
        usuario.setFechaActualizacion(LocalDateTime.now());
        usuario = usuarioRepo.save(usuario);
        auditLog.registrar(TipoEvento.CAMBIO_ESTADO_CUENTA, adminCorreo,
                "Usuario " + usuario.getCorreo() + " -> " + estado + " | " + Objects.toString(motivo, ""));
        return mapearUsuario(usuario);
    }

    @Override
    @Transactional
    public void eliminarUsuario(Long usuarioId, String adminCorreo) {
        Usuario usuario = usuarioRepo.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (usuario.getRol() == Rol.ADMINISTRADOR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se elimina administradores desde este modulo");
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

    @Override
    @Transactional
    public UsuarioGestionDTO asignarComisionista(Long inversionistaId, Long comisionistaId,
                                                  String adminCorreo) {
        Usuario inversionista = usuarioRepo.findById(inversionistaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inversionista no encontrado"));
        Usuario comisionista = usuarioRepo.findById(comisionistaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comisionista no encontrado"));

        if (inversionista.getRol() != Rol.INVERSIONISTA
                && inversionista.getRol() != Rol.INVERSIONISTA_PREMIUM) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El usuario destino debe ser inversionista");
        }
        if (comisionista.getRol() != Rol.COMISIONISTA
                || comisionista.getEstadoCuenta() != EstadoCuenta.ACTIVA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El comisionista debe estar activo");
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

    @Override
    @Transactional(readOnly = true)
    public boolean esAdministradorActivo(String correo) {
        return usuarioRepo.findByCorreo(correo)
                .map(u -> u.getRol() == Rol.ADMINISTRADOR
                        && u.getEstadoCuenta() == EstadoCuenta.ACTIVA
                        && u.isMfaHabilitado())
                .orElse(false);
    }

    private UsuarioGestionDTO mapearUsuario(Usuario usuario) {
        UsuarioGestionDTO dto = new UsuarioGestionDTO();
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

    private String normalizarCsv(String csv) {
        if (csv == null || csv.isBlank()) return "";
        return String.join(",", Arrays.stream(csv.split("[,;\\s]+"))
                .map(v -> v.trim().toUpperCase(Locale.ROOT))
                .filter(v -> !v.isBlank())
                .distinct()
                .toList());
    }
}
