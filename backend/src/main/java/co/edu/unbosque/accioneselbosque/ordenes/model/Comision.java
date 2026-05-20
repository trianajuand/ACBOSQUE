package co.edu.unbosque.accioneselbosque.ordenes.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "comision")
public class Comision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "orden_id", nullable = false)
    private Long ordenId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "comisionista_id")
    private Long comisionistaId;

    /** Monto de la orden base. */
    @Column(name = "monto_base", nullable = false, precision = 18, scale = 4)
    private BigDecimal montoBase;

    /** Porcentaje de comisión aplicado (ej. 2.0). */
    @Column(name = "porcentaje_comision", nullable = false, precision = 6, scale = 2)
    private BigDecimal porcentajeComision;

    /** Monto total de comisión. */
    @Column(name = "monto_comision", nullable = false, precision = 18, scale = 4)
    private BigDecimal montoComision;

    /** Parte de la comisión que va a la plataforma (ej. 60%). */
    @Column(name = "monto_plataforma", nullable = false, precision = 18, scale = 4)
    private BigDecimal montoPlataforma;

    /** Parte de la comisión que va al comisionista (ej. 40%), 0 si no hay comisionista. */
    @Column(name = "monto_comisionista", nullable = false, precision = 18, scale = 4)
    private BigDecimal montoComisionista;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;

    public Comision() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOrdenId() { return ordenId; }
    public void setOrdenId(Long ordenId) { this.ordenId = ordenId; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public Long getComisionistaId() { return comisionistaId; }
    public void setComisionistaId(Long comisionistaId) { this.comisionistaId = comisionistaId; }

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

    public LocalDateTime getCreadaEn() { return creadaEn; }
    public void setCreadaEn(LocalDateTime creadaEn) { this.creadaEn = creadaEn; }
}
