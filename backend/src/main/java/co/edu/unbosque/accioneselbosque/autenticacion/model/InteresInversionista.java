package co.edu.unbosque.accioneselbosque.autenticacion.model;

import jakarta.persistence.*;

@Entity
@Table(name = "interes_inversionista",
        uniqueConstraints = @UniqueConstraint(columnNames = {"inversionista_id", "especialidad_id"}))
public class InteresInversionista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inversionista_id", nullable = false)
    private Long inversionistaId;

    @Column(name = "especialidad_id", nullable = false)
    private Long especialidadId;

    public InteresInversionista() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getInversionistaId() { return inversionistaId; }
    public void setInversionistaId(Long inversionistaId) { this.inversionistaId = inversionistaId; }

    public Long getEspecialidadId() { return especialidadId; }
    public void setEspecialidadId(Long especialidadId) { this.especialidadId = especialidadId; }
}
