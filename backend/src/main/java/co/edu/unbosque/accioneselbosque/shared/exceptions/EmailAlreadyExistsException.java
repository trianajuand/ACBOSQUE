package co.edu.unbosque.accioneselbosque.shared.exceptions;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String correo) {
        super("El correo ya está registrado: " + correo);
    }
}
