package co.edu.unbosque.accioneselbosque.administracion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public class MercadoConfigDTO {

    private Long id;

    @NotBlank
    private String codigo;

    @NotBlank
    private String nombre;

    @NotBlank
    private String zonaHoraria;

    @NotNull
    private LocalTime horaApertura;

    @NotNull
    private LocalTime horaCierre;

    private boolean habilitado;
    private LocalTime cierreAnticipado;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getZonaHoraria() { return zonaHoraria; }
    public void setZonaHoraria(String zonaHoraria) { this.zonaHoraria = zonaHoraria; }

    public LocalTime getHoraApertura() { return horaApertura; }
    public void setHoraApertura(LocalTime horaApertura) { this.horaApertura = horaApertura; }

    public LocalTime getHoraCierre() { return horaCierre; }
    public void setHoraCierre(LocalTime horaCierre) { this.horaCierre = horaCierre; }

    public boolean isHabilitado() { return habilitado; }
    public void setHabilitado(boolean habilitado) { this.habilitado = habilitado; }

    public LocalTime getCierreAnticipado() { return cierreAnticipado; }
    public void setCierreAnticipado(LocalTime cierreAnticipado) { this.cierreAnticipado = cierreAnticipado; }
}
