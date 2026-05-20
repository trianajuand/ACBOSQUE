package co.edu.unbosque.accioneselbosque.ordenes.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class CrearPropuestaOrdenDTO {

    @NotBlank(message = "El simbolo es obligatorio")
    private String simbolo;

    @NotNull(message = "El tipo de orden es obligatorio")
    private String tipoOrden;

    @NotNull(message = "El lado es obligatorio")
    private String lado;

    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.000001", message = "La cantidad debe ser mayor a 0")
    private BigDecimal cantidad;

    private BigDecimal precioLimite;
    private BigDecimal precioStop;
    private String comentarioComisionista;

    public String getSimbolo() { return simbolo; }
    public void setSimbolo(String simbolo) { this.simbolo = simbolo; }

    public String getTipoOrden() { return tipoOrden; }
    public void setTipoOrden(String tipoOrden) { this.tipoOrden = tipoOrden; }

    public String getLado() { return lado; }
    public void setLado(String lado) { this.lado = lado; }

    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }

    public BigDecimal getPrecioLimite() { return precioLimite; }
    public void setPrecioLimite(BigDecimal precioLimite) { this.precioLimite = precioLimite; }

    public BigDecimal getPrecioStop() { return precioStop; }
    public void setPrecioStop(BigDecimal precioStop) { this.precioStop = precioStop; }

    public String getComentarioComisionista() { return comentarioComisionista; }
    public void setComentarioComisionista(String comentarioComisionista) { this.comentarioComisionista = comentarioComisionista; }
}
