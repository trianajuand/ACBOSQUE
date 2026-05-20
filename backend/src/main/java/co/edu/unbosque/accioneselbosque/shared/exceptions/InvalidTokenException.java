package co.edu.unbosque.accioneselbosque.shared.exceptions;

public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String mensaje) {
        super(mensaje);
    }
}
