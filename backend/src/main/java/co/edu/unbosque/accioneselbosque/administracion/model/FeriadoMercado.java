package co.edu.unbosque.accioneselbosque.administracion.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "feriado_mercado",
        uniqueConstraints = @UniqueConstraint(name = "uk_feriado_mercado_fecha", columnNames = {"mercado_codigo", "fecha"})
)
public class FeriadoMercado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mercado_codigo", nullable = false, length = 30)
    private String mercadoCodigo;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "descripcion", nullable = false)
    private String descripcion;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMercadoCodigo() { return mercadoCodigo; }
    public void setMercadoCodigo(String mercadoCodigo) { this.mercadoCodigo = mercadoCodigo; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }
}
