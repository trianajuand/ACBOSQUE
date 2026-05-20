package co.edu.unbosque.accioneselbosque.autenticacion.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class RegistroInversionistaDTO {

    @NotBlank(message = "El nombre completo es obligatorio")
    private String nombreCompleto;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Formato de correo inválido")
    private String correo;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String contrasenia;

    private String nivelExperiencia;

    private List<String> interesesMercado;

    private String telefono;

    // BASICO (null = BASICO), PREMIUM_MENSUAL, PREMIUM_ANUAL
    private String planSuscripcion;

    public RegistroInversionistaDTO() {
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

    public String getNivelExperiencia() {
        return nivelExperiencia;
    }

    public void setNivelExperiencia(String nivelExperiencia) {
        this.nivelExperiencia = nivelExperiencia;
    }

    public List<String> getInteresesMercado() {
        return interesesMercado;
    }

    public void setInteresesMercado(List<String> interesesMercado) {
        this.interesesMercado = interesesMercado;
    }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getPlanSuscripcion() { return planSuscripcion; }
    public void setPlanSuscripcion(String planSuscripcion) { this.planSuscripcion = planSuscripcion; }
}
