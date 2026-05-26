package co.edu.unbosque.accioneselbosque.mercado.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PK = activo_id (shared con Activo.id). Un registro de precio por activo.
 */
@Entity
@Table(name = "precio_cache")
public class PrecioCache {

    @Id
    @Column(name = "activo_id")
    private Long activoId;

    @Column(name = "simbolo", nullable = false, unique = true, length = 20)
    private String simbolo;

    @Column(name = "nombre_empresa", length = 200)
    private String nombreEmpresa;

    @Column(name = "mercado", length = 20)
    private String mercado;

    @Column(name = "precio_actual", precision = 18, scale = 4)
    private BigDecimal precioActual;

    @Column(name = "precio_apertura", precision = 18, scale = 4)
    private BigDecimal precioApertura;

    @Column(name = "precio_cierre_anterior", precision = 18, scale = 4)
    private BigDecimal precioCierreAnterior;

    @Column(name = "precio_maximo", precision = 18, scale = 4)
    private BigDecimal precioMaximo;

    @Column(name = "precio_minimo", precision = 18, scale = 4)
    private BigDecimal precioMinimo;

    @Column(name = "variacion_porcentual", precision = 8, scale = 4)
    private BigDecimal variacionPorcentual;

    @Column(name = "volumen")
    private Long volumen;

    @Column(name = "fuente", length = 20)
    private String fuente;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    public PrecioCache() {}

    public Long getActivoId() { return activoId; }
    public void setActivoId(Long activoId) { this.activoId = activoId; }

    public String getSimbolo() { return simbolo; }
    public void setSimbolo(String simbolo) { this.simbolo = simbolo; }

    public String getNombreEmpresa() { return nombreEmpresa; }
    public void setNombreEmpresa(String nombreEmpresa) { this.nombreEmpresa = nombreEmpresa; }

    public String getMercado() { return mercado; }
    public void setMercado(String mercado) { this.mercado = mercado; }

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

    public String getFuente() { return fuente; }
    public void setFuente(String fuente) { this.fuente = fuente; }

    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(LocalDateTime actualizadoEn) { this.actualizadoEn = actualizadoEn; }
}
