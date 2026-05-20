package co.edu.unbosque.accioneselbosque.autenticacion.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "codigo_verificacion")
public class CodigoVerificacion implements Serializable {

    public enum TipoCodigo {
        REGISTRO,
        MFA,
        RECUPERACION_PASSWORD
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "correo", nullable = false)
    private String correo;

    @Column(name = "codigo", nullable = false)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoCodigo tipo;

    @Column(name = "expiracion", nullable = false)
    private LocalDateTime expiracion;

    @Column(name = "usado", nullable = false)
    private boolean usado;

    public CodigoVerificacion() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public TipoCodigo getTipo() {
        return tipo;
    }

    public void setTipo(TipoCodigo tipo) {
        this.tipo = tipo;
    }

    public LocalDateTime getExpiracion() {
        return expiracion;
    }

    public void setExpiracion(LocalDateTime expiracion) {
        this.expiracion = expiracion;
    }

    public boolean isUsado() {
        return usado;
    }

    public void setUsado(boolean usado) {
        this.usado = usado;
    }
}
