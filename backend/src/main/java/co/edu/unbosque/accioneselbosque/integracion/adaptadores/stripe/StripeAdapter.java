package co.edu.unbosque.accioneselbosque.integracion.adaptadores.stripe;

import co.edu.unbosque.accioneselbosque.shared.exceptions.StripeCheckoutException;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionRetrieveParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeAdapter implements IIntegracionStripe {

    private static final String MONEDA_USD = "usd";
    private static final long MONTO_MENSUAL_CENTAVOS = 1200L;
    private static final long MONTO_ANUAL_CENTAVOS = 12000L;

    private final String secretKey;
    private final String priceIdMensual;
    private final String priceIdAnual;

    public StripeAdapter(
            @Value("${stripe.secret-key}") String secretKey,
            @Value("${stripe.price-id.premium-mensual}") String priceIdMensual,
            @Value("${stripe.price-id.premium-anual}") String priceIdAnual) {
        this.secretKey = secretKey;
        this.priceIdMensual = priceIdMensual;
        this.priceIdAnual = priceIdAnual;
    }

    @Override
    public String crearSesionCheckout(String correo, String plan, Long usuarioId,
                                      String successUrl, String cancelUrl) {
        validarConfiguracion(plan);
        Stripe.apiKey = secretKey;

        String precioOProductoId = "PREMIUM_ANUAL".equalsIgnoreCase(plan) ? priceIdAnual : priceIdMensual;

        try {
            SessionCreateParams.LineItem.Builder lineItem = SessionCreateParams.LineItem.builder()
                    .setQuantity(1L);

            if (precioOProductoId.startsWith("price_")) {
                lineItem.setPrice(precioOProductoId);
            } else if (precioOProductoId.startsWith("prod_")) {
                lineItem.setPriceData(crearPrecioDesdeProducto(precioOProductoId, plan));
            } else {
                throw new StripeCheckoutException("El identificador de Stripe debe empezar por price_ o prod_");
            }

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomerEmail(correo)
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .addLineItem(lineItem.build())
                    .putMetadata("usuarioId", usuarioId.toString())
                    .putMetadata("plan", plan)
                    .build();

            Session session = Session.create(params);
            return session.getUrl();
        } catch (StripeException e) {
            throw new StripeCheckoutException("No se pudo crear la sesion de pago con Stripe: " + e.getMessage(), e);
        }
    }

    @Override
    public Session obtenerSesionCheckout(String sessionId) {
        if (noConfigurado(secretKey)) {
            throw new StripeCheckoutException("Stripe no esta configurado: falta STRIPE_SECRET_KEY en .env");
        }
        Stripe.apiKey = secretKey;
        try {
            SessionRetrieveParams params = SessionRetrieveParams.builder()
                    .addExpand("subscription")
                    .build();
            return Session.retrieve(sessionId, params, null);
        } catch (StripeException e) {
            throw new StripeCheckoutException("No se pudo consultar la sesion de pago con Stripe: " + e.getMessage(), e);
        }
    }

    @Override
    public void cancelarSuscripcion(String subscriptionId) {
        Stripe.apiKey = secretKey;
        try {
            com.stripe.model.Subscription subscription =
                    com.stripe.model.Subscription.retrieve(subscriptionId);
            com.stripe.param.SubscriptionUpdateParams params =
                    com.stripe.param.SubscriptionUpdateParams.builder()
                            .setCancelAtPeriodEnd(true)
                            .build();
            subscription.update(params);
        } catch (StripeException e) {
            throw new RuntimeException("Error al cancelar suscripcion en Stripe: " + e.getMessage(), e);
        }
    }

    private void validarConfiguracion(String plan) {
        if (noConfigurado(secretKey)) {
            throw new StripeCheckoutException("Stripe no esta configurado: falta STRIPE_SECRET_KEY en .env");
        }

        String priceId = "PREMIUM_ANUAL".equalsIgnoreCase(plan) ? priceIdAnual : priceIdMensual;
        String variable = "PREMIUM_ANUAL".equalsIgnoreCase(plan)
                ? "STRIPE_PRICE_ID_PREMIUM_ANUAL"
                : "STRIPE_PRICE_ID_PREMIUM_MENSUAL";
        if (noConfigurado(priceId)) {
            throw new StripeCheckoutException("Stripe no esta configurado: falta " + variable + " en .env");
        }
    }

    private boolean noConfigurado(String valor) {
        return valor == null || valor.isBlank() || "CAMBIAR_EN_ENV".equals(valor);
    }

    private SessionCreateParams.LineItem.PriceData crearPrecioDesdeProducto(String productId, String plan) {
        boolean anual = "PREMIUM_ANUAL".equalsIgnoreCase(plan);
        return SessionCreateParams.LineItem.PriceData.builder()
                .setCurrency(MONEDA_USD)
                .setProduct(productId)
                .setUnitAmount(anual ? MONTO_ANUAL_CENTAVOS : MONTO_MENSUAL_CENTAVOS)
                .setRecurring(SessionCreateParams.LineItem.PriceData.Recurring.builder()
                        .setInterval(anual
                                ? SessionCreateParams.LineItem.PriceData.Recurring.Interval.YEAR
                                : SessionCreateParams.LineItem.PriceData.Recurring.Interval.MONTH)
                        .build())
                .build();
    }
}
