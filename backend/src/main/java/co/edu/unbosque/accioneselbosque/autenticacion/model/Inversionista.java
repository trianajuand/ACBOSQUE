package co.edu.unbosque.accioneselbosque.autenticacion.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
@Entity
@Table(name = "inversionista")
public class Inversionista {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "nivel_experiencia")
    private String nivelExperiencia;

    @Column(name = "tipo_identificacion", length = 30)
    private String tipoIdentificacion;

    @Column(name = "direccion")
    private String direccion;

    @Column(name = "ciudad")
    private String ciudad;

    @Column(name = "codigo_postal")
    private String codigoPostal;

    @Column(name = "pais")
    private String pais;

    @Column(name = "estilo_trading")
    private String estiloTrading;

    @Column(name = "intereses_mercado", length = 500)
    private String interesesMercado;

    @Column(name = "alpaca_account_id", length = 100)
    private String alpacaAccountId;

    @Column(name = "pendiente_cuenta_alpaca", nullable = false,
            columnDefinition = "boolean NOT NULL DEFAULT false")
    private boolean pendienteCuentaAlpaca;

    @Column(name = "solicita_comisionista")
    private Boolean solicitaComisionista;

    @Column(name = "tipo_orden_default", length = 20)
    private String tipoOrdenDefault;

    @Column(name = "tipo_notificacion", length = 20)
    private String tipoNotificacion;

    @Column(name = "vista_portafolio", length = 20)
    private String vistaPortafolio;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNivelExperiencia() { return nivelExperiencia; }
    public void setNivelExperiencia(String nivelExperiencia) { this.nivelExperiencia = nivelExperiencia; }

    public String getTipoIdentificacion() { return tipoIdentificacion; }
    public void setTipoIdentificacion(String tipoIdentificacion) { this.tipoIdentificacion = tipoIdentificacion; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }

    public String getCodigoPostal() { return codigoPostal; }
    public void setCodigoPostal(String codigoPostal) { this.codigoPostal = codigoPostal; }

    public String getPais() { return pais; }
    public void setPais(String pais) { this.pais = pais; }

    public String getEstiloTrading() { return estiloTrading; }
    public void setEstiloTrading(String estiloTrading) { this.estiloTrading = estiloTrading; }

    public String getInteresesMercado() { return interesesMercado; }
    public void setInteresesMercado(String interesesMercado) { this.interesesMercado = interesesMercado; }

    public String getAlpacaAccountId() { return alpacaAccountId; }
    public void setAlpacaAccountId(String alpacaAccountId) { this.alpacaAccountId = alpacaAccountId; }

    public boolean isPendienteCuentaAlpaca() { return pendienteCuentaAlpaca; }
    public void setPendienteCuentaAlpaca(boolean pendienteCuentaAlpaca) { this.pendienteCuentaAlpaca = pendienteCuentaAlpaca; }

    public boolean isSolicitaComisionista() { return Boolean.TRUE.equals(solicitaComisionista); }
    public void setSolicitaComisionista(boolean solicitaComisionista) { this.solicitaComisionista = solicitaComisionista; }

    public String getTipoOrdenDefault() { return tipoOrdenDefault; }
    public void setTipoOrdenDefault(String tipoOrdenDefault) { this.tipoOrdenDefault = tipoOrdenDefault; }

    public String getTipoNotificacion() { return tipoNotificacion; }
    public void setTipoNotificacion(String tipoNotificacion) { this.tipoNotificacion = tipoNotificacion; }

    public String getVistaPortafolio() { return vistaPortafolio; }
    public void setVistaPortafolio(String vistaPortafolio) { this.vistaPortafolio = vistaPortafolio; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
}
