package co.edu.unbosque.accioneselbosque.administracion.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "mercado_config")
public class MercadoConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "zona_horaria", nullable = false)
    private String zonaHoraria;

    @Column(name = "hora_apertura", nullable = false)
    private LocalTime horaApertura;

    @Column(name = "hora_cierre", nullable = false)
    private LocalTime horaCierre;

    @Column(name = "habilitado", nullable = false)
    private boolean habilitado;

    @Column(name = "cierre_anticipado")
    private LocalTime cierreAnticipado;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getZonaHoraria() { return zonaHoraria; }
    public void setZonaHoraria(String zonaHoraria) { this.zonaHoraria = zonaHoraria; }

    public LocalTime getHoraApertura() { return horaApertura; }
    public void setHoraApertura(LocalTime horaApertura) { this.horaApertura = horaApertura; }

    public LocalTime getHoraCierre() { return horaCierre; }
    public void setHoraCierre(LocalTime horaCierre) { this.horaCierre = horaCierre; }

    public boolean isHabilitado() { return habilitado; }
    public void setHabilitado(boolean habilitado) { this.habilitado = habilitado; }

    public LocalTime getCierreAnticipado() { return cierreAnticipado; }
    public void setCierreAnticipado(LocalTime cierreAnticipado) { this.cierreAnticipado = cierreAnticipado; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
}
