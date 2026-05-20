package co.edu.unbosque.accioneselbosque.ordenes.dto;

import co.edu.unbosque.accioneselbosque.ordenes.model.EstadoOrden;
import co.edu.unbosque.accioneselbosque.ordenes.model.TipoLado;
import co.edu.unbosque.accioneselbosque.ordenes.model.TipoOrden;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrdenDTO {

    private Long id;
    private String simbolo;
    private TipoOrden tipoOrden;
    private TipoLado lado;
    private EstadoOrden estado;
    private BigDecimal cantidad;
    private BigDecimal precioLimite;
    private BigDecimal precioStop;
    private BigDecimal precioEjecucion;
    private BigDecimal montoTotal;
    private BigDecimal comision;
    private BigDecimal montoNeto;
    private String alpacaOrderId;
    private LocalDateTime creadaEn;
    private LocalDateTime ejecutadaEn;

    public OrdenDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSimbolo() { return simbolo; }
    public void setSimbolo(String simbolo) { this.simbolo = simbolo; }

    public TipoOrden getTipoOrden() { return tipoOrden; }
    public void setTipoOrden(TipoOrden tipoOrden) { this.tipoOrden = tipoOrden; }

    public TipoLado getLado() { return lado; }
    public void setLado(TipoLado lado) { this.lado = lado; }

    public EstadoOrden getEstado() { return estado; }
    public void setEstado(EstadoOrden estado) { this.estado = estado; }

    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }

    public BigDecimal getPrecioLimite() { return precioLimite; }
    public void setPrecioLimite(BigDecimal precioLimite) { this.precioLimite = precioLimite; }

    public BigDecimal getPrecioStop() { return precioStop; }
    public void setPrecioStop(BigDecimal precioStop) { this.precioStop = precioStop; }

    public BigDecimal getPrecioEjecucion() { return precioEjecucion; }
    public void setPrecioEjecucion(BigDecimal precioEjecucion) { this.precioEjecucion = precioEjecucion; }

    public BigDecimal getMontoTotal() { return montoTotal; }
    public void setMontoTotal(BigDecimal montoTotal) { this.montoTotal = montoTotal; }

    public BigDecimal getComision() { return comision; }
    public void setComision(BigDecimal comision) { this.comision = comision; }

    public BigDecimal getMontoNeto() { return montoNeto; }
    public void setMontoNeto(BigDecimal montoNeto) { this.montoNeto = montoNeto; }

    public String getAlpacaOrderId() { return alpacaOrderId; }
    public void setAlpacaOrderId(String alpacaOrderId) { this.alpacaOrderId = alpacaOrderId; }

    public LocalDateTime getCreadaEn() { return creadaEn; }
    public void setCreadaEn(LocalDateTime creadaEn) { this.creadaEn = creadaEn; }

    public LocalDateTime getEjecutadaEn() { return ejecutadaEn; }
    public void setEjecutadaEn(LocalDateTime ejecutadaEn) { this.ejecutadaEn = ejecutadaEn; }
}
