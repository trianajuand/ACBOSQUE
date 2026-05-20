package co.edu.unbosque.accioneselbosque.mercado.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CotizacionDTO {

    private String simbolo;
    private String nombreEmpresa;
    private BigDecimal precioActual;
    private BigDecimal precioApertura;
    private BigDecimal precioCierreAnterior;
    private BigDecimal precioMaximo;
    private BigDecimal precioMinimo;
    private BigDecimal variacionPorcentual;
    private Long volumen;
    private String mercado;
    private boolean mercadoAbierto;
    private LocalDateTime actualizadoEn;

    public CotizacionDTO() {}

    public String getSimbolo() { return simbolo; }
    public void setSimbolo(String simbolo) { this.simbolo = simbolo; }

    public String getNombreEmpresa() { return nombreEmpresa; }
    public void setNombreEmpresa(String nombreEmpresa) { this.nombreEmpresa = nombreEmpresa; }

    public BigDecimal getPrecioActual() { return precioActual; }
    public void setPrecioActual(BigDecimal precioActual) { this.precioActual = precioActual; }

    public BigDecimal getPrecioApertura() { return precioApertura; }
    public void setPrecioApertura(BigDecimal precioApertura) { this.precioApertura = precioApertura; }

    public BigDecimal getPrecioCierreAnterior() { return precioCierreAnterior; }
    public void setPrecioCierreAnterior(BigDecimal precioCierreAnterior) { this.precioCierreAnterior = precioCierreAnterior; }

    public BigDecimal getPrecioMaximo() { return precioMaximo; }
    public void setPrecioMaximo(BigDecimal precioMaximo) { this.precioMaximo = precioMaximo; }

    public BigDecimal getPrecioMinimo() { return precioMinimo; }
    public void setPrecioMinimo(BigDecimal precioMinimo) { this.precioMinimo = precioMinimo; }

    public BigDecimal getVariacionPorcentual() { return variacionPorcentual; }
    public void setVariacionPorcentual(BigDecimal variacionPorcentual) { this.variacionPorcentual = variacionPorcentual; }

    public Long getVolumen() { return volumen; }
    public void setVolumen(Long volumen) { this.volumen = volumen; }

    public String getMercado() { return mercado; }
    public void setMercado(String mercado) { this.mercado = mercado; }

    public boolean isMercadoAbierto() { return mercadoAbierto; }
    public void setMercadoAbierto(boolean mercadoAbierto) { this.mercadoAbierto = mercadoAbierto; }

    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(LocalDateTime actualizadoEn) { this.actualizadoEn = actualizadoEn; }
}
