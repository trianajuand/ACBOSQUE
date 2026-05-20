package co.edu.unbosque.accioneselbosque.autenticacion.dto;

public class ConfirmarRegistroResponseDTO {

    private String mensaje;
    private boolean requierePago;
    private String stripeCheckoutUrl;

    public ConfirmarRegistroResponseDTO(String mensaje) {
        this.mensaje = mensaje;
        this.requierePago = false;
    }

    public ConfirmarRegistroResponseDTO(String mensaje, String stripeCheckoutUrl) {
        this.mensaje = mensaje;
        this.requierePago = true;
        this.stripeCheckoutUrl = stripeCheckoutUrl;
    }

    public String getMensaje() { return mensaje; }
    public boolean isRequierePago() { return requierePago; }
    public String getStripeCheckoutUrl() { return stripeCheckoutUrl; }
}
