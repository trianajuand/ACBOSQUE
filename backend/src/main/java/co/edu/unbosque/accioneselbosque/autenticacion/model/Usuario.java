package co.edu.unbosque.accioneselbosque.autenticacion.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
public class Usuario implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_completo", nullable = false)
    private String nombreCompleto;

    @Column(name = "correo", nullable = false, unique = true)
    private String correo;

    @Column(name = "contrasenia", nullable = false)
    private String contrasenia;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false)
    private Rol rol;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_cuenta", nullable = false)
    private EstadoCuenta estadoCuenta;

    @Column(name = "nivel_experiencia")
    private String nivelExperiencia;

    @Column(name = "intereses_mercado")
    private String interesesMercado;

    @Column(name = "mfa_habilitado", nullable = false)
    private boolean mfaHabilitado;

    @Column(name = "alpaca_account_id")
    private String alpacaAccountId;

    @Column(name = "pendiente_cuenta_alpaca", nullable = false)
    private boolean pendienteCuentaAlpaca;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "telefono")
    private String telefono;

    // Suscripción
    @Column(name = "plan_suscripcion")
    private String planSuscripcion; // BASICO, PREMIUM_MENSUAL, PREMIUM_ANUAL

    @Column(name = "es_premium")
    private Boolean esPremium;

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    @Column(name = "stripe_suscripcion_id")
    private String stripeSuscripcionId;

    @Column(name = "fecha_expiracion_premium")
    private LocalDate fechaExpiracionPremium;

    // Preferencias de notificación
    @Column(name = "notificacion_email")
    private Boolean notificacionEmail;

    @Column(name = "notificacion_sms")
    private Boolean notificacionSms;

    @Column(name = "notificacion_whatsapp")
    private Boolean notificacionWhatsapp;

    @Column(name = "tipos_notificacion")
    private String tiposNotificacion;

    // Preferencias de operación
    @Column(name = "tipo_orden_default")
    private String tipoOrdenDefault;

    @Column(name = "vista_portafolio")
    private String vistaPortafolio;

    public Usuario() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getContrasenia() {
        return contrasenia;
    }

    public void setContrasenia(String contrasenia) {
        this.contrasenia = contrasenia;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public EstadoCuenta getEstadoCuenta() {
        return estadoCuenta;
    }

    public void setEstadoCuenta(EstadoCuenta estadoCuenta) {
        this.estadoCuenta = estadoCuenta;
    }

    public String getNivelExperiencia() {
        return nivelExperiencia;
    }

    public void setNivelExperiencia(String nivelExperiencia) {
        this.nivelExperiencia = nivelExperiencia;
    }

    public String getInteresesMercado() {
        return interesesMercado;
    }

    public void setInteresesMercado(String interesesMercado) {
        this.interesesMercado = interesesMercado;
    }

    public boolean isMfaHabilitado() {
        return mfaHabilitado;
    }

    public void setMfaHabilitado(boolean mfaHabilitado) {
        this.mfaHabilitado = mfaHabilitado;
    }

    public String getAlpacaAccountId() {
        return alpacaAccountId;
    }

    public void setAlpacaAccountId(String alpacaAccountId) {
        this.alpacaAccountId = alpacaAccountId;
    }

    public boolean isPendienteCuentaAlpaca() {
        return pendienteCuentaAlpaca;
    }

    public void setPendienteCuentaAlpaca(boolean pendienteCuentaAlpaca) {
        this.pendienteCuentaAlpaca = pendienteCuentaAlpaca;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getPlanSuscripcion() { return planSuscripcion; }
    public void setPlanSuscripcion(String planSuscripcion) { this.planSuscripcion = planSuscripcion; }

    public boolean isEsPremium() { return Boolean.TRUE.equals(esPremium); }
    public void setEsPremium(boolean esPremium) { this.esPremium = esPremium; }

    public String getStripeCustomerId() { return stripeCustomerId; }
    public void setStripeCustomerId(String stripeCustomerId) { this.stripeCustomerId = stripeCustomerId; }

    public String getStripeSuscripcionId() { return stripeSuscripcionId; }
    public void setStripeSuscripcionId(String stripeSuscripcionId) { this.stripeSuscripcionId = stripeSuscripcionId; }

    public LocalDate getFechaExpiracionPremium() { return fechaExpiracionPremium; }
    public void setFechaExpiracionPremium(LocalDate fechaExpiracionPremium) { this.fechaExpiracionPremium = fechaExpiracionPremium; }

    public boolean isNotificacionEmail() { return !Boolean.FALSE.equals(notificacionEmail); } // default true
    public void setNotificacionEmail(boolean notificacionEmail) { this.notificacionEmail = notificacionEmail; }

    public boolean isNotificacionSms() { return Boolean.TRUE.equals(notificacionSms); }
    public void setNotificacionSms(boolean notificacionSms) { this.notificacionSms = notificacionSms; }

    public boolean isNotificacionWhatsapp() { return Boolean.TRUE.equals(notificacionWhatsapp); }
    public void setNotificacionWhatsapp(boolean notificacionWhatsapp) { this.notificacionWhatsapp = notificacionWhatsapp; }

    public String getTiposNotificacion() { return tiposNotificacion; }
    public void setTiposNotificacion(String tiposNotificacion) { this.tiposNotificacion = tiposNotificacion; }

    public String getTipoOrdenDefault() { return tipoOrdenDefault; }
    public void setTipoOrdenDefault(String tipoOrdenDefault) { this.tipoOrdenDefault = tipoOrdenDefault; }

    public String getVistaPortafolio() { return vistaPortafolio; }
    public void setVistaPortafolio(String vistaPortafolio) { this.vistaPortafolio = vistaPortafolio; }

    @Override
    public String toString() {
        return "Usuario [id=" + id + ", correo=" + correo + ", rol=" + rol + ", estado=" + estadoCuenta + "]";
    }
}
