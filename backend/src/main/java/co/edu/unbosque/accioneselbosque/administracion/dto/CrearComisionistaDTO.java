package co.edu.unbosque.accioneselbosque.administracion.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CrearComisionistaDTO {

    @NotBlank
    private String nombreCompleto;

    @Email
    @NotBlank
    private String correo;

    @NotBlank
    @Size(min = 8)
    private String contrasenia;

    private String especialidadesMercado;

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getContrasenia() { return contrasenia; }
    public void setContrasenia(String contrasenia) { this.contrasenia = contrasenia; }

    public String getEspecialidadesMercado() { return especialidadesMercado; }
    public void setEspecialidadesMercado(String especialidadesMercado) { this.especialidadesMercado = especialidadesMercado; }
}
