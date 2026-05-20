package co.edu.unbosque.accioneselbosque.autenticacion.dto;

public class LoginResponseDTO {

    private String token;
    private boolean requiereMfa;
    private String mfaToken;
    private String rol;
    private String mensaje;

    public LoginResponseDTO() {
    }

    public static LoginResponseDTO conJwt(String token, String rol) {
        LoginResponseDTO dto = new LoginResponseDTO();
        dto.token = token;
        dto.requiereMfa = false;
        dto.rol = rol;
        return dto;
    }

    public static LoginResponseDTO requiereMfa(String mfaToken) {
        LoginResponseDTO dto = new LoginResponseDTO();
        dto.requiereMfa = true;
        dto.mfaToken = mfaToken;
        return dto;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isRequiereMfa() {
        return requiereMfa;
    }

    public void setRequiereMfa(boolean requiereMfa) {
        this.requiereMfa = requiereMfa;
    }

    public String getMfaToken() {
        return mfaToken;
    }

    public void setMfaToken(String mfaToken) {
        this.mfaToken = mfaToken;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
