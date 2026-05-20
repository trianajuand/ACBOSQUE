package co.edu.unbosque.accioneselbosque.autenticacion.dto;

public class CorreoDisponibleDTO {

    private String correo;
    private boolean disponible;

    public CorreoDisponibleDTO(String correo, boolean disponible) {
        this.correo = correo;
        this.disponible = disponible;
    }

    public String getCorreo() { return correo; }
    public boolean isDisponible() { return disponible; }
}
