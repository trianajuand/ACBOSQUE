package co.edu.unbosque.accioneselbosque.autenticacion.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ClienteAsignadoDTO {

    private Long id;
    private String nombreCompleto;
    private String correo;
    private String nivelExperiencia;
    private List<String> interesesMercado;
    private LocalDateTime fechaAsignacion;
    private String interesesCoincidentes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getNivelExperiencia() { return nivelExperiencia; }
    public void setNivelExperiencia(String nivelExperiencia) { this.nivelExperiencia = nivelExperiencia; }

    public List<String> getInteresesMercado() { return interesesMercado; }
    public void setInteresesMercado(List<String> interesesMercado) { this.interesesMercado = interesesMercado; }

    public LocalDateTime getFechaAsignacion() { return fechaAsignacion; }
    public void setFechaAsignacion(LocalDateTime fechaAsignacion) { this.fechaAsignacion = fechaAsignacion; }

    public String getInteresesCoincidentes() { return interesesCoincidentes; }
    public void setInteresesCoincidentes(String interesesCoincidentes) { this.interesesCoincidentes = interesesCoincidentes; }
}
