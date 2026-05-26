package co.edu.unbosque.accioneselbosque.ordenes.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "propuesta_orden")
public class PropuestaOrden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inversionista_id", nullable = false)
    private Long inversionistaId;

    @Column(name = "comisionista_id", nullable = false)
    private Long comisionistaId;

    @Column(name = "activo_id", nullable = false)
    private Long activoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_orden", nullable = false, length = 20)
    private TipoOrden tipoOrden;

    @Enumerated(EnumType.STRING)
    @Column(name = "lado", nullable = false, length = 10)
    private TipoLado lado;

    @Column(name = "cantidad", nullable = false, precision = 18, scale = 6)
    private BigDecimal cantidad;

    @Column(name = "precio_limite", precision = 18, scale = 4)
    private BigDecimal precioLimite;

    @Column(name = "precio_stop", precision = 18, scale = 4)
    private BigDecimal precioStop;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoPropuesta estado;

    @Column(name = "comentario_comisionista", length = 500)
    private String comentarioComisionista;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;

    @Column(name = "aprobada_en")
    private LocalDateTime aprobadaEn;

    @Column(name = "rechazada_en")
    private LocalDateTime rechazadaEn;

    @Column(name = "firmada_en")
    private LocalDateTime firmadaEn;

    public PropuestaOrden() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getInversionistaId() { return inversionistaId; }
    public void setInversionistaId(Long inversionistaId) { this.inversionistaId = inversionistaId; }

    public Long getComisionistaId() { return comisionistaId; }
    public void setComisionistaId(Long comisionistaId) { this.comisionistaId = comisionistaId; }

    public Long getActivoId() { return activoId; }
    public void setActivoId(Long activoId) { this.activoId = activoId; }

    public TipoOrden getTipoOrden() { return tipoOrden; }
    public void setTipoOrden(TipoOrden tipoOrden) { this.tipoOrden = tipoOrden; }

    public TipoLado getLado() { return lado; }
    public void setLado(TipoLado lado) { this.lado = lado; }

    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }

    public BigDecimal getPrecioLimite() { return precioLimite; }
    public void setPrecioLimite(BigDecimal precioLimite) { this.precioLimite = precioLimite; }

    public BigDecimal getPrecioStop() { return precioStop; }
    public void setPrecioStop(BigDecimal precioStop) { this.precioStop = precioStop; }

    public EstadoPropuesta getEstado() { return estado; }
    public void setEstado(EstadoPropuesta estado) { this.estado = estado; }

    public String getComentarioComisionista() { return comentarioComisionista; }
    public void setComentarioComisionista(String comentarioComisionista) { this.comentarioComisionista = comentarioComisionista; }

    public LocalDateTime getCreadaEn() { return creadaEn; }
    public void setCreadaEn(LocalDateTime creadaEn) { this.creadaEn = creadaEn; }

    public LocalDateTime getAprobadaEn() { return aprobadaEn; }
    public void setAprobadaEn(LocalDateTime aprobadaEn) { this.aprobadaEn = aprobadaEn; }

    public LocalDateTime getRechazadaEn() { return rechazadaEn; }
    public void setRechazadaEn(LocalDateTime rechazadaEn) { this.rechazadaEn = rechazadaEn; }

    public LocalDateTime getFirmadaEn() { return firmadaEn; }
    public void setFirmadaEn(LocalDateTime firmadaEn) { this.firmadaEn = firmadaEn; }
}
