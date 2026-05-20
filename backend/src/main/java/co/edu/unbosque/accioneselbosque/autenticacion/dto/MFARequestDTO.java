package co.edu.unbosque.accioneselbosque.autenticacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MFARequestDTO {

    @NotBlank(message = "El mfaToken es obligatorio")
    private String mfaToken;

    @NotBlank(message = "El código MFA es obligatorio")
    @Size(min = 6, max = 6, message = "El código debe tener 6 dígitos")
    private String codigo;

    public MFARequestDTO() {
    }

    public String getMfaToken() {
        return mfaToken;
    }

    public void setMfaToken(String mfaToken) {
        this.mfaToken = mfaToken;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }
}
