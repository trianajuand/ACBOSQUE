package co.edu.unbosque.accioneselbosque.administracion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class FeriadoMercadoDTO {

    private Long id;
    private String mercadoCodigo;

    @NotNull
    private LocalDate fecha;

    @NotBlank
    private String descripcion;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMercadoCodigo() { return mercadoCodigo; }
    public void setMercadoCodigo(String mercadoCodigo) { this.mercadoCodigo = mercadoCodigo; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
