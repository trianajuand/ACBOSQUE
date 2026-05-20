package co.edu.unbosque.accioneselbosque.integracion.adaptadores.stripe;

import com.stripe.model.checkout.Session;

public interface IIntegracionStripe {

    String crearSesionCheckout(String correo, String plan, Long usuarioId, String successUrl, String cancelUrl);

    Session obtenerSesionCheckout(String sessionId);

    void cancelarSuscripcion(String subscriptionId);
}
