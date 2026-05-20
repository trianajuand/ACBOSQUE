package co.edu.unbosque.accioneselbosque.mercado.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class DetalleAccionDTO {

    private String simbolo;
    private String nombreEmpresa;
    private String sector;
    private String industria;
    private String descripcion;
    private BigDecimal precioActual;
    private BigDecimal precioCierreAnterior;
    private BigDecimal precioApertura;
    private BigDecimal precioMaximo;
    private BigDecimal precioMinimo;
    private BigDecimal variacionPorcentual;
    private Long volumen;
    private BigDecimal capitalizacionMercado;
    private String mercado;
    private boolean mercadoAbierto;
    // Lista de {fecha, apertura, maximo, minimo, cierre, volumen}
    private List<Map<String, Object>> historicoPrecios;

    public DetalleAccionDTO() {}

    public String getSimbolo() { return simbolo; }
    public void setSimbolo(String simbolo) { this.simbolo = simbolo; }

    public String getNombreEmpresa() { return nombreEmpresa; }
    public void setNombreEmpresa(String nombreEmpresa) { this.nombreEmpresa = nombreEmpresa; }

    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }

    public String getIndustria() { return industria; }
    public void setIndustria(String industria) { this.industria = industria; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getPrecioActual() { return precioActual; }
    public void setPrecioActual(BigDecimal precioActual) { this.precioActual = precioActual; }

    public BigDecimal getPrecioCierreAnterior() { return precioCierreAnterior; }
    public void setPrecioCierreAnterior(BigDecimal precioCierreAnterior) { this.precioCierreAnterior = precioCierreAnterior; }

    public BigDecimal getPrecioApertura() { return precioApertura; }
    public void setPrecioApertura(BigDecimal precioApertura) { this.precioApertura = precioApertura; }

    public BigDecimal getPrecioMaximo() { return precioMaximo; }
    public void setPrecioMaximo(BigDecimal precioMaximo) { this.precioMaximo = precioMaximo; }

    public BigDecimal getPrecioMinimo() { return precioMinimo; }
    public void setPrecioMinimo(BigDecimal precioMinimo) { this.precioMinimo = precioMinimo; }

    public BigDecimal getVariacionPorcentual() { return variacionPorcentual; }
    public void setVariacionPorcentual(BigDecimal variacionPorcentual) { this.variacionPorcentual = variacionPorcentual; }

    public Long getVolumen() { return volumen; }
    public void setVolumen(Long volumen) { this.volumen = volumen; }

    public BigDecimal getCapitalizacionMercado() { return capitalizacionMercado; }
    public void setCapitalizacionMercado(BigDecimal capitalizacionMercado) { this.capitalizacionMercado = capitalizacionMercado; }

    public String getMercado() { return mercado; }
    public void setMercado(String mercado) { this.mercado = mercado; }

    public boolean isMercadoAbierto() { return mercadoAbierto; }
    public void setMercadoAbierto(boolean mercadoAbierto) { this.mercadoAbierto = mercadoAbierto; }

    public List<Map<String, Object>> getHistoricoPrecios() { return historicoPrecios; }
    public void setHistoricoPrecios(List<Map<String, Object>> historicoPrecios) { this.historicoPrecios = historicoPrecios; }
}
