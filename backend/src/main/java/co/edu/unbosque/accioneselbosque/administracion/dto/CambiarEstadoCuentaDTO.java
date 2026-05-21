package co.edu.unbosque.accioneselbosque.administracion.dto;

import jakarta.validation.constraints.NotBlank;

public class CambiarEstadoCuentaDTO {

    @NotBlank
    private String estado;

    private String motivo;

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
