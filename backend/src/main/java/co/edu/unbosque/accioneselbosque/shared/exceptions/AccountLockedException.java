package co.edu.unbosque.accioneselbosque.shared.exceptions;

public class AccountLockedException extends RuntimeException {

    public AccountLockedException(String mensaje) {
        super(mensaje);
    }
}
