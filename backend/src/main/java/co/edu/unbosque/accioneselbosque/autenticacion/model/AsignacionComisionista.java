package co.edu.unbosque.accioneselbosque.autenticacion.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "asignacion_comisionista",
        uniqueConstraints = @UniqueConstraint(name = "uk_asignacion_inversionista", columnNames = "inversionista_id")
)
public class AsignacionComisionista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inversionista_id", nullable = false)
    private Long inversionistaId;

    @Column(name = "comisionista_id", nullable = false)
    private Long comisionistaId;

    @Column(name = "intereses_coincidentes", length = 500)
    private String interesesCoincidentes;

    @Column(name = "motivo", length = 500)
    private String motivo;

    @Column(name = "activa", nullable = false)
    private boolean activa;

    @Column(name = "fecha_asignacion", nullable = false)
    private LocalDateTime fechaAsignacion;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getInversionistaId() { return inversionistaId; }
    public void setInversionistaId(Long inversionistaId) { this.inversionistaId = inversionistaId; }

    public Long getComisionistaId() { return comisionistaId; }
    public void setComisionistaId(Long comisionistaId) { this.comisionistaId = comisionistaId; }

    public String getInteresesCoincidentes() { return interesesCoincidentes; }
    public void setInteresesCoincidentes(String interesesCoincidentes) { this.interesesCoincidentes = interesesCoincidentes; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }

    public LocalDateTime getFechaAsignacion() { return fechaAsignacion; }
    public void setFechaAsignacion(LocalDateTime fechaAsignacion) { this.fechaAsignacion = fechaAsignacion; }
}
