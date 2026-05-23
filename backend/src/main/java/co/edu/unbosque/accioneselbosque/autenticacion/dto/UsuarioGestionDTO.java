package co.edu.unbosque.accioneselbosque.autenticacion.dto;

import java.time.LocalDateTime;

public class UsuarioGestionDTO {

    private Long id;
    private String nombreCompleto;
    private String correo;
    private String rol;
    private String estadoCuenta;
    private boolean mfaHabilitado;
    private LocalDateTime fechaCreacion;
    private String comisionistaAsignado;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public String getEstadoCuenta() { return estadoCuenta; }
    public void setEstadoCuenta(String estadoCuenta) { this.estadoCuenta = estadoCuenta; }

    public boolean isMfaHabilitado() { return mfaHabilitado; }
    public void setMfaHabilitado(boolean mfaHabilitado) { this.mfaHabilitado = mfaHabilitado; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getComisionistaAsignado() { return comisionistaAsignado; }
    public void setComisionistaAsignado(String comisionistaAsignado) { this.comisionistaAsignado = comisionistaAsignado; }
}
