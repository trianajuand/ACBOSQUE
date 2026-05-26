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

    @Column(name = "mfa_habilitado", nullable = false)
    private boolean mfaHabilitado;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "tipo_identificacion", length = 30)
    private String tipoIdentificacion;

    @Column(name = "numero_identificacion", length = 50)
    private String numeroIdentificacion;

    @Column(name = "fecha_nacimiento")
    private java.time.LocalDate fechaNacimiento;

    @Column(name = "notificaciones_activas", nullable = false,
            columnDefinition = "boolean NOT NULL DEFAULT true")
    private boolean notificacionesActivas = true;

    @Column(name = "notificacion_email", nullable = false,
            columnDefinition = "boolean NOT NULL DEFAULT true")
    private boolean notificacionEmail = true;

    @Column(name = "notificacion_sms", nullable = false,
            columnDefinition = "boolean NOT NULL DEFAULT false")
    private boolean notificacionSms;

    @Column(name = "notificacion_whatsapp", nullable = false,
            columnDefinition = "boolean NOT NULL DEFAULT false")
    private boolean notificacionWhatsapp;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

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

    public boolean isMfaHabilitado() {
        return mfaHabilitado;
    }

    public void setMfaHabilitado(boolean mfaHabilitado) {
        this.mfaHabilitado = mfaHabilitado;
    }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getTipoIdentificacion() { return tipoIdentificacion; }
    public void setTipoIdentificacion(String tipoIdentificacion) { this.tipoIdentificacion = tipoIdentificacion; }

    public String getNumeroIdentificacion() { return numeroIdentificacion; }
    public void setNumeroIdentificacion(String numeroIdentificacion) { this.numeroIdentificacion = numeroIdentificacion; }

    public java.time.LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(java.time.LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

    public boolean isNotificacionesActivas() { return notificacionesActivas; }
    public void setNotificacionesActivas(boolean notificacionesActivas) { this.notificacionesActivas = notificacionesActivas; }

    public boolean isNotificacionEmail() { return notificacionEmail; }
    public void setNotificacionEmail(boolean notificacionEmail) { this.notificacionEmail = notificacionEmail; }

    public boolean isNotificacionSms() { return notificacionSms; }
    public void setNotificacionSms(boolean notificacionSms) { this.notificacionSms = notificacionSms; }

    public boolean isNotificacionWhatsapp() { return notificacionWhatsapp; }
    public void setNotificacionWhatsapp(boolean notificacionWhatsapp) { this.notificacionWhatsapp = notificacionWhatsapp; }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    @Override
    public String toString() {
        return "Usuario [id=" + id + ", correo=" + correo + ", rol=" + rol + ", estado=" + estadoCuenta + "]";
    }
}
