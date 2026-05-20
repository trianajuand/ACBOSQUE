package co.edu.unbosque.accioneselbosque.autenticacion.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ConfirmarRegistroDTO {

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Formato de correo inválido")
    private String correo;

    @NotBlank(message = "El código de verificación es obligatorio")
    @Size(min = 6, max = 6, message = "El código debe tener 6 dígitos")
    private String codigo;

    public ConfirmarRegistroDTO() {}

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
}
