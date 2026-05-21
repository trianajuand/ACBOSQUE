package co.edu.unbosque.accioneselbosque.administracion.dto;

import java.math.BigDecimal;

public class TendenciaMercadoDTO {

    private String mercado;
    private long operaciones;
    private BigDecimal volumen = BigDecimal.ZERO;
    private BigDecimal comisiones = BigDecimal.ZERO;

    public String getMercado() { return mercado; }
    public void setMercado(String mercado) { this.mercado = mercado; }

    public long getOperaciones() { return operaciones; }
    public void setOperaciones(long operaciones) { this.operaciones = operaciones; }

    public BigDecimal getVolumen() { return volumen; }
    public void setVolumen(BigDecimal volumen) { this.volumen = volumen; }

    public BigDecimal getComisiones() { return comisiones; }
    public void setComisiones(BigDecimal comisiones) { this.comisiones = comisiones; }
}
