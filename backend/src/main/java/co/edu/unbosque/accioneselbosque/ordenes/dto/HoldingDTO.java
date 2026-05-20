package co.edu.unbosque.accioneselbosque.ordenes.dto;

import java.math.BigDecimal;

public class HoldingDTO {

    private String simbolo;
    private BigDecimal cantidad;
    private BigDecimal precioPromedio;
    private BigDecimal precioActual;        // precio de mercado en tiempo real
    private BigDecimal valorTotal;          // cantidad × precioActual
    private BigDecimal gananciaPerdida;     // (precioActual - precioPromedio) × cantidad
    private BigDecimal gananciaPerdidaPct;  // variación porcentual

    public HoldingDTO() {}

    public String getSimbolo() { return simbolo; }
    public void setSimbolo(String simbolo) { this.simbolo = simbolo; }

    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }

    public BigDecimal getPrecioPromedio() { return precioPromedio; }
    public void setPrecioPromedio(BigDecimal precioPromedio) { this.precioPromedio = precioPromedio; }

    public BigDecimal getPrecioActual() { return precioActual; }
    public void setPrecioActual(BigDecimal precioActual) { this.precioActual = precioActual; }

    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }

    public BigDecimal getGananciaPerdida() { return gananciaPerdida; }
    public void setGananciaPerdida(BigDecimal gananciaPerdida) { this.gananciaPerdida = gananciaPerdida; }

    public BigDecimal getGananciaPerdidaPct() { return gananciaPerdidaPct; }
    public void setGananciaPerdidaPct(BigDecimal gananciaPerdidaPct) { this.gananciaPerdidaPct = gananciaPerdidaPct; }
}
