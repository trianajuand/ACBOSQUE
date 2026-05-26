package co.edu.unbosque.accioneselbosque.administracion.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "parametro_comision")
public class ParametroComision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "porcentaje_comision", nullable = false, precision = 6, scale = 2)
    private BigDecimal porcentajeComision;

    @Column(name = "split_plataforma", nullable = false, precision = 6, scale = 2)
    private BigDecimal splitPlataforma;

    @Column(name = "split_comisionista", nullable = false, precision = 6, scale = 2)
    private BigDecimal splitComisionista;

    @Column(name = "fecha_inicio", nullable = false,
            columnDefinition = "date NOT NULL DEFAULT CURRENT_DATE")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BigDecimal getPorcentajeComision() { return porcentajeComision; }
    public void setPorcentajeComision(BigDecimal porcentajeComision) { this.porcentajeComision = porcentajeComision; }

    public BigDecimal getSplitPlataforma() { return splitPlataforma; }
    public void setSplitPlataforma(BigDecimal splitPlataforma) { this.splitPlataforma = splitPlataforma; }

    public BigDecimal getSplitComisionista() { return splitComisionista; }
    public void setSplitComisionista(BigDecimal splitComisionista) { this.splitComisionista = splitComisionista; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public boolean isActivo() {
        LocalDate hoy = LocalDate.now();
        return !hoy.isBefore(fechaInicio) && (fechaFin == null || !hoy.isAfter(fechaFin));
    }
}
