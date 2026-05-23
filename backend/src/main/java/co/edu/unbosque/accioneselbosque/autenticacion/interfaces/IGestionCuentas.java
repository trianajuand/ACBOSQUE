package co.edu.unbosque.accioneselbosque.autenticacion.interfaces;

import co.edu.unbosque.accioneselbosque.autenticacion.dto.UsuarioGestionDTO;

import java.util.List;

/**
 * Contrato para gestión administrativa del ciclo de vida de cuentas de usuario.
 * Consumida exclusivamente por el módulo de Administración; nunca por otros módulos.
 */
public interface IGestionCuentas {

    UsuarioGestionDTO crearComisionista(String nombreCompleto, String correo,
                                        String contrasenia, String especialidades,
                                        String adminCorreo);

    List<UsuarioGestionDTO> listarUsuarios(String rolFiltro);

    UsuarioGestionDTO cambiarEstadoUsuario(Long usuarioId, String nuevoEstado,
                                           String motivo, String adminCorreo);

    void eliminarUsuario(Long usuarioId, String adminCorreo);

    UsuarioGestionDTO asignarComisionista(Long inversionistaId, Long comisionistaId,
                                          String adminCorreo);

    boolean esAdministradorActivo(String correo);
}
