package co.edu.unbosque.accioneselbosque.autenticacion.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ComisionistaAsignadoDTO {

    private Long id;
    private String nombreCompleto;
    private String correo;
    private List<String> especialidadesMercado;
    private LocalDateTime fechaAsignacion;
    private String interesesCoincidentes;
    private String motivo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public List<String> getEspecialidadesMercado() { return especialidadesMercado; }
    public void setEspecialidadesMercado(List<String> especialidadesMercado) { this.especialidadesMercado = especialidadesMercado; }

    public LocalDateTime getFechaAsignacion() { return fechaAsignacion; }
    public void setFechaAsignacion(LocalDateTime fechaAsignacion) { this.fechaAsignacion = fechaAsignacion; }

    public String getInteresesCoincidentes() { return interesesCoincidentes; }
    public void setInteresesCoincidentes(String interesesCoincidentes) { this.interesesCoincidentes = interesesCoincidentes; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
