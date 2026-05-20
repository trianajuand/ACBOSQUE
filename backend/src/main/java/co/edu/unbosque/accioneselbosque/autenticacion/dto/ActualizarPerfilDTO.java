package co.edu.unbosque.accioneselbosque.autenticacion.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class ActualizarPerfilDTO {

    @NotBlank(message = "El nombre completo es obligatorio")
    private String nombreCompleto;

    private String nivelExperiencia;
    private List<String> interesesMercado;
    private String telefono;

    public ActualizarPerfilDTO() {}

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public String getNivelExperiencia() { return nivelExperiencia; }
    public void setNivelExperiencia(String nivelExperiencia) { this.nivelExperiencia = nivelExperiencia; }

    public List<String> getInteresesMercado() { return interesesMercado; }
    public void setInteresesMercado(List<String> interesesMercado) { this.interesesMercado = interesesMercado; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
}
