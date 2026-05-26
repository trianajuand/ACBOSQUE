package co.edu.unbosque.accioneselbosque.autenticacion.model;

import jakarta.persistence.*;

@Entity
@Table(name = "especialidad")
public class Especialidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "titulo", nullable = false, length = 100)
    private String titulo;

    @Column(name = "descripcion_especialidad", length = 500)
    private String descripcionEspecialidad;

    @Column(name = "tipo_especialidad", length = 50)
    private String tipoEspecialidad;

    public Especialidad() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcionEspecialidad() { return descripcionEspecialidad; }
    public void setDescripcionEspecialidad(String descripcionEspecialidad) { this.descripcionEspecialidad = descripcionEspecialidad; }

    public String getTipoEspecialidad() { return tipoEspecialidad; }
    public void setTipoEspecialidad(String tipoEspecialidad) { this.tipoEspecialidad = tipoEspecialidad; }
}
