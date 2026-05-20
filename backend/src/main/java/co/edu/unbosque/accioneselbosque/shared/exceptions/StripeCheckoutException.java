package co.edu.unbosque.accioneselbosque.shared.exceptions;

public class StripeCheckoutException extends RuntimeException {

    public StripeCheckoutException(String mensaje) {
        super(mensaje);
    }

    public StripeCheckoutException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
