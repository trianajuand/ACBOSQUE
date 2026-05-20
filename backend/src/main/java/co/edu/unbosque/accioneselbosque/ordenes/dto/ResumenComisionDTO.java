package co.edu.unbosque.accioneselbosque.ordenes.dto;

import java.math.BigDecimal;

/** Desglose de comisión mostrado ANTES de confirmar la orden (EC-13). */
public class ResumenComisionDTO {

    private String simbolo;
    private String tipoOrden;
    private String lado;
    private BigDecimal cantidad;
    private BigDecimal precioEstimado;
    private BigDecimal montoBase;
    private BigDecimal porcentajeComision;
    private BigDecimal montoComision;
    private BigDecimal montoPlataforma;
    private BigDecimal montoComisionista;
    private BigDecimal totalADebitar;   // compra: montoBase + comision
    private BigDecimal totalARecibir;   // venta: montoBase - comision
    private boolean mercadoAbierto;
    private String advertencia;         // mensaje si el mercado está cerrado (orden se encolará)

    public ResumenComisionDTO() {}

    public String getSimbolo() { return simbolo; }
    public void setSimbolo(String simbolo) { this.simbolo = simbolo; }

    public String getTipoOrden() { return tipoOrden; }
    public void setTipoOrden(String tipoOrden) { this.tipoOrden = tipoOrden; }

    public String getLado() { return lado; }
    public void setLado(String lado) { this.lado = lado; }

    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }

    public BigDecimal getPrecioEstimado() { return precioEstimado; }
    public void setPrecioEstimado(BigDecimal precioEstimado) { this.precioEstimado = precioEstimado; }

    public BigDecimal getMontoBase() { return montoBase; }
    public void setMontoBase(BigDecimal montoBase) { this.montoBase = montoBase; }

    public BigDecimal getPorcentajeComision() { return porcentajeComision; }
    public void setPorcentajeComision(BigDecimal porcentajeComision) { this.porcentajeComision = porcentajeComision; }

    public BigDecimal getMontoComision() { return montoComision; }
    public void setMontoComision(BigDecimal montoComision) { this.montoComision = montoComision; }

    public BigDecimal getMontoPlataforma() { return montoPlataforma; }
    public void setMontoPlataforma(BigDecimal montoPlataforma) { this.montoPlataforma = montoPlataforma; }

    public BigDecimal getMontoComisionista() { return montoComisionista; }
    public void setMontoComisionista(BigDecimal montoComisionista) { this.montoComisionista = montoComisionista; }

    public BigDecimal getTotalADebitar() { return totalADebitar; }
    public void setTotalADebitar(BigDecimal totalADebitar) { this.totalADebitar = totalADebitar; }

    public BigDecimal getTotalARecibir() { return totalARecibir; }
    public void setTotalARecibir(BigDecimal totalARecibir) { this.totalARecibir = totalARecibir; }

    public boolean isMercadoAbierto() { return mercadoAbierto; }
    public void setMercadoAbierto(boolean mercadoAbierto) { this.mercadoAbierto = mercadoAbierto; }

    public String getAdvertencia() { return advertencia; }
    public void setAdvertencia(String advertencia) { this.advertencia = advertencia; }
}
