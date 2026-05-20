package co.edu.unbosque.accioneselbosque.ordenes.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "holding",
        uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "simbolo"}))
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "simbolo", nullable = false, length = 20)
    private String simbolo;

    /** Cantidad total de acciones en posesión. */
    @Column(name = "cantidad", nullable = false, precision = 18, scale = 6)
    private BigDecimal cantidad;

    /** Precio promedio de compra (costo base). */
    @Column(name = "precio_promedio", nullable = false, precision = 18, scale = 4)
    private BigDecimal precioPromedio;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    public Holding() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getSimbolo() { return simbolo; }
    public void setSimbolo(String simbolo) { this.simbolo = simbolo; }

    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }

    public BigDecimal getPrecioPromedio() { return precioPromedio; }
    public void setPrecioPromedio(BigDecimal precioPromedio) { this.precioPromedio = precioPromedio; }

    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(LocalDateTime actualizadoEn) { this.actualizadoEn = actualizadoEn; }
}
