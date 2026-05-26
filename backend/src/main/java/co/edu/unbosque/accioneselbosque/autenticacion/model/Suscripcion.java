package co.edu.unbosque.accioneselbosque.autenticacion.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "suscripcion")
public class Suscripcion {

    @Id
    @Column(name = "inversionista_id")
    private Long inversionistaId;

    @Column(name = "codigo_suscripcion", length = 100)
    private String codigoSuscripcion;

    @Column(name = "es_premium", nullable = false)
    private boolean esPremium;

    @Column(name = "fecha_expiracion_premium")
    private LocalDate fechaExpiracionPremium;

    @Column(name = "plan_suscripcion", length = 50)
    private String planSuscripcion;

    @Column(name = "stripe_customer_id", length = 100)
    private String stripeCustomerId;

    @Column(name = "stripe_suscripcion_id", length = 100)
    private String stripeSuscripcionId;

    public Suscripcion() {}

    public Long getInversionistaId() { return inversionistaId; }
    public void setInversionistaId(Long inversionistaId) { this.inversionistaId = inversionistaId; }

    public String getCodigoSuscripcion() { return codigoSuscripcion; }
    public void setCodigoSuscripcion(String codigoSuscripcion) { this.codigoSuscripcion = codigoSuscripcion; }

    public boolean isEsPremium() { return esPremium; }
    public void setEsPremium(boolean esPremium) { this.esPremium = esPremium; }

    public LocalDate getFechaExpiracionPremium() { return fechaExpiracionPremium; }
    public void setFechaExpiracionPremium(LocalDate fechaExpiracionPremium) { this.fechaExpiracionPremium = fechaExpiracionPremium; }

    public String getPlanSuscripcion() { return planSuscripcion; }
    public void setPlanSuscripcion(String planSuscripcion) { this.planSuscripcion = planSuscripcion; }

    public String getStripeCustomerId() { return stripeCustomerId; }
    public void setStripeCustomerId(String stripeCustomerId) { this.stripeCustomerId = stripeCustomerId; }

    public String getStripeSuscripcionId() { return stripeSuscripcionId; }
    public void setStripeSuscripcionId(String stripeSuscripcionId) { this.stripeSuscripcionId = stripeSuscripcionId; }
}
