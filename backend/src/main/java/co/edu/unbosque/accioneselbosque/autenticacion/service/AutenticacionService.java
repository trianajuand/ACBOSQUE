package co.edu.unbosque.accioneselbosque.autenticacion.service;

import co.edu.unbosque.accioneselbosque.autenticacion.dto.LoginRequestDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.dto.LoginResponseDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.dto.MFARequestDTO;
import co.edu.unbosque.accioneselbosque.autenticacion.interfaces.IAutenticacion;
import co.edu.unbosque.accioneselbosque.autenticacion.model.CodigoVerificacion.TipoCodigo;
import co.edu.unbosque.accioneselbosque.autenticacion.model.EstadoCuenta;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Rol;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Usuario;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.UsuarioRepository;
import co.edu.unbosque.accioneselbosque.autenticacion.security.JwtUtil;
import co.edu.unbosque.accioneselbosque.integracion.notificaciones.DespachadorNotificaciones;
import co.edu.unbosque.accioneselbosque.shared.exceptions.AccountLockedException;
import co.edu.unbosque.accioneselbosque.shared.exceptions.UsuarioNoEncontradoException;
import co.edu.unbosque.accioneselbosque.trazabilidad.interfaces.IAuditLog;
import co.edu.unbosque.accioneselbosque.trazabilidad.model.TipoEvento;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AutenticacionService implements IAutenticacion {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final MonitorIntentosService monitorIntentos;
    private final MFAService mfaService;
    private final DespachadorNotificaciones despachador;
    private final IAuditLog auditLog;

    public AutenticacionService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            MonitorIntentosService monitorIntentos,
            MFAService mfaService,
            DespachadorNotificaciones despachador,
            IAuditLog auditLog) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.monitorIntentos = monitorIntentos;
        this.mfaService = mfaService;
        this.despachador = despachador;
        this.auditLog = auditLog;
    }

    @Override
    public LoginResponseDTO iniciarSesion(LoginRequestDTO solicitud) {
        monitorIntentos.verificarBloqueo(solicitud.getCorreo());

        Usuario usuario = usuarioRepository.findByCorreo(solicitud.getCorreo())
                .orElseThrow(() -> {
                    registrarIntentoFallido(solicitud.getCorreo(), null);
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
                });

        if (usuario.getEstadoCuenta() == EstadoCuenta.BLOQUEADA) {
            throw new AccountLockedException("Cuenta bloqueada. Contacta al administrador.");
        }

        if (usuario.getEstadoCuenta() != EstadoCuenta.ACTIVA) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "La cuenta aun no esta activa. Verifica tu correo y completa el pago si elegiste premium.");
        }

        if (!passwordEncoder.matches(solicitud.getContrasenia(), usuario.getContrasenia())) {
            boolean seBloqueo = monitorIntentos.registrarIntentoFallido(solicitud.getCorreo());
            auditLog.registrar(TipoEvento.LOGIN_FALLIDO, solicitud.getCorreo(), "Contraseña incorrecta");
            if (seBloqueo) {
                auditLog.registrar(TipoEvento.CUENTA_BLOQUEADA, solicitud.getCorreo(), "Bloqueada tras 5 intentos");
                despachador.notificarBloqueo(usuario.getCorreo(), usuario.getNombreCompleto());
            }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }

        monitorIntentos.reiniciarIntentos(solicitud.getCorreo());

        boolean requiereMfa = usuario.isMfaHabilitado()
                || usuario.getRol() == Rol.COMISIONISTA
                || usuario.getRol() == Rol.ADMINISTRADOR
                || usuario.getRol() == Rol.RESPONSABLE_LEGAL;

        if (requiereMfa) {
            String codigo = mfaService.generarYGuardarCodigo(usuario.getCorreo(), TipoCodigo.MFA);
            despachador.enviarCodigoMfa(usuario.getCorreo(), usuario.getNombreCompleto(), codigo);
            String mfaToken = jwtUtil.generarTokenMfa(usuario.getCorreo());
            auditLog.registrar(TipoEvento.MFA_ENVIADO, usuario.getCorreo(), "Código MFA enviado");
            return LoginResponseDTO.requiereMfa(mfaToken);
        }

        String jwt = jwtUtil.generarToken(usuario.getCorreo(), usuario.getRol().name());
        auditLog.registrar(TipoEvento.LOGIN_EXITOSO, usuario.getCorreo(), "Login sin MFA");
        return LoginResponseDTO.conJwt(jwt, usuario.getRol().name());
    }

    @Override
    public LoginResponseDTO verificarMfa(MFARequestDTO solicitud) {
        if (!jwtUtil.esValido(solicitud.getMfaToken())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token MFA inválido o expirado");
        }

        String correo = jwtUtil.obtenerCorreo(solicitud.getMfaToken());
        mfaService.validarCodigo(correo, solicitud.getCodigo(), TipoCodigo.MFA);

        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsuarioNoEncontradoException(correo));

        String jwt = jwtUtil.generarToken(correo, usuario.getRol().name());
        auditLog.registrar(TipoEvento.MFA_VERIFICADO, correo, "MFA verificado correctamente");
        auditLog.registrar(TipoEvento.LOGIN_EXITOSO, correo, "Login completado con MFA");
        return LoginResponseDTO.conJwt(jwt, usuario.getRol().name());
    }

    @Override
    public void cerrarSesion(String token) {
        String correo = jwtUtil.obtenerCorreo(token.replace("Bearer ", ""));
        auditLog.registrar(TipoEvento.LOGOUT, correo, "Cierre de sesión");
    }

    private void registrarIntentoFallido(String correo, Usuario usuario) {
        boolean seBloqueo = monitorIntentos.registrarIntentoFallido(correo);
        auditLog.registrar(TipoEvento.LOGIN_FALLIDO, correo, "Usuario no encontrado");
        if (seBloqueo && usuario != null) {
            despachador.notificarBloqueo(usuario.getCorreo(), usuario.getNombreCompleto());
        }
    }
}
