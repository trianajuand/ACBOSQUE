package co.edu.unbosque.accioneselbosque.ordenes.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "holding")
@IdClass(HoldingId.class)
public class Holding {

    @Id
    @Column(name = "inversionista_id", nullable = false)
    private Long inversionistaId;

    @Id
    @Column(name = "activo_id", nullable = false)
    private Long activoId;

    @Column(name = "cantidad", nullable = false, precision = 18, scale = 6)
    private BigDecimal cantidad;

    @Column(name = "precio_promedio", nullable = false, precision = 18, scale = 4)
    private BigDecimal precioPromedio;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    public Holding() {}

    public Long getInversionistaId() { return inversionistaId; }
    public void setInversionistaId(Long inversionistaId) { this.inversionistaId = inversionistaId; }

    public Long getActivoId() { return activoId; }
    public void setActivoId(Long activoId) { this.activoId = activoId; }

    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }

    public BigDecimal getPrecioPromedio() { return precioPromedio; }
    public void setPrecioPromedio(BigDecimal precioPromedio) { this.precioPromedio = precioPromedio; }

    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(LocalDateTime actualizadoEn) { this.actualizadoEn = actualizadoEn; }
}
