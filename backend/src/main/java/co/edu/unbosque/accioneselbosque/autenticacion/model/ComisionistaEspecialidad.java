package co.edu.unbosque.accioneselbosque.autenticacion.model;

import jakarta.persistence.*;

@Entity
@Table(name = "comisionista_especialidad",
        uniqueConstraints = @UniqueConstraint(columnNames = {"comisionista_id", "especialidad_id"}))
public class ComisionistaEspecialidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "comisionista_id", nullable = false)
    private Long comisionistaId;

    @Column(name = "especialidad_id", nullable = false)
    private Long especialidadId;

    public ComisionistaEspecialidad() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getComisionistaId() { return comisionistaId; }
    public void setComisionistaId(Long comisionistaId) { this.comisionistaId = comisionistaId; }

    public Long getEspecialidadId() { return especialidadId; }
    public void setEspecialidadId(Long especialidadId) { this.especialidadId = especialidadId; }
}
