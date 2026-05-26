package co.edu.unbosque.accioneselbosque.ordenes.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orden")
public class Orden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inversionista_id", nullable = false)
    private Long inversionistaId;

    @Column(name = "activo_id", nullable = false)
    private Long activoId;

    @Column(name = "propuesta_orden_id")
    private Long propuestaOrdenId;

    @Column(name = "parametro_comision_id")
    private Long parametroComisionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_orden", nullable = false, length = 20)
    private TipoOrden tipoOrden;

    @Enumerated(EnumType.STRING)
    @Column(name = "lado", nullable = false, length = 10)
    private TipoLado lado;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoOrden estado;

    @Column(name = "cantidad", nullable = false, precision = 18, scale = 6)
    private BigDecimal cantidad;

    @Column(name = "precio_limite", precision = 18, scale = 4)
    private BigDecimal precioLimite;

    @Column(name = "precio_stop", precision = 18, scale = 4)
    private BigDecimal precioStop;

    @Column(name = "precio_ejecucion", precision = 18, scale = 4)
    private BigDecimal precioEjecucion;

    @Column(name = "monto_total", precision = 18, scale = 4)
    private BigDecimal montoTotal;

    @Column(name = "comision", precision = 18, scale = 4)
    private BigDecimal comision;

    @Column(name = "monto_neto", precision = 18, scale = 4)
    private BigDecimal montoNeto;

    @Column(name = "alpaca_order_id", length = 100)
    private String alpacaOrderId;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;

    @Column(name = "ejecutada_en")
    private LocalDateTime ejecutadaEn;

    @Column(name = "cancelada_en")
    private LocalDateTime canceladaEn;

    @Column(name = "ip_origen", length = 50)
    private String ipOrigen;

    public Orden() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getInversionistaId() { return inversionistaId; }
    public void setInversionistaId(Long inversionistaId) { this.inversionistaId = inversionistaId; }

    public Long getActivoId() { return activoId; }
    public void setActivoId(Long activoId) { this.activoId = activoId; }

    public Long getPropuestaOrdenId() { return propuestaOrdenId; }
    public void setPropuestaOrdenId(Long propuestaOrdenId) { this.propuestaOrdenId = propuestaOrdenId; }

    public Long getParametroComisionId() { return parametroComisionId; }
    public void setParametroComisionId(Long parametroComisionId) { this.parametroComisionId = parametroComisionId; }

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

    public LocalDateTime getCanceladaEn() { return canceladaEn; }
    public void setCanceladaEn(LocalDateTime canceladaEn) { this.canceladaEn = canceladaEn; }

    public String getIpOrigen() { return ipOrigen; }
    public void setIpOrigen(String ipOrigen) { this.ipOrigen = ipOrigen; }
}
