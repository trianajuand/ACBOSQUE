package co.edu.unbosque.accioneselbosque.autenticacion.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ResetPasswordDTO {

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Formato de correo inválido")
    private String correo;

    @NotBlank(message = "El token de recuperación es obligatorio")
    private String token;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String nuevaContrasenia;

    public ResetPasswordDTO() {}

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getNuevaContrasenia() { return nuevaContrasenia; }
    public void setNuevaContrasenia(String nuevaContrasenia) { this.nuevaContrasenia = nuevaContrasenia; }
}
