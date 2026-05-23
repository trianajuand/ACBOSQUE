package co.edu.unbosque.accioneselbosque.ordenes.dto;

import java.math.BigDecimal;

public class ResumenMercadoDTO {

    private String mercado;
    private int operaciones;
    private BigDecimal volumen = BigDecimal.ZERO;
    private BigDecimal comisiones = BigDecimal.ZERO;

    public String getMercado() { return mercado; }
    public void setMercado(String mercado) { this.mercado = mercado; }

    public int getOperaciones() { return operaciones; }
    public void setOperaciones(int operaciones) { this.operaciones = operaciones; }

    public BigDecimal getVolumen() { return volumen; }
    public void setVolumen(BigDecimal volumen) { this.volumen = volumen; }

    public BigDecimal getComisiones() { return comisiones; }
    public void setComisiones(BigDecimal comisiones) { this.comisiones = comisiones; }
}
