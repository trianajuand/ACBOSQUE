package co.edu.unbosque.accioneselbosque.shared.exceptions;

public class UsuarioNoEncontradoException extends RuntimeException {

    public UsuarioNoEncontradoException(String correo) {
        super("Usuario no encontrado: " + correo);
    }
}
