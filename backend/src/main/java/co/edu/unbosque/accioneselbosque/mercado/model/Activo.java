package co.edu.unbosque.accioneselbosque.mercado.model;

import jakarta.persistence.*;

@Entity
@Table(name = "Activo")
public class Activo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "Mercado_Config_id")
    private Long mercadoConfigId;

    @Column(name = "tipo", length = 30)
    private String tipo;

    @Column(name = "ticker", length = 10, nullable = false, unique = true)
    private String ticker;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(name = "nombre_empresa", length = 200)
    private String nombreEmpresa;

    public Activo() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMercadoConfigId() { return mercadoConfigId; }
    public void setMercadoConfigId(Long mercadoConfigId) { this.mercadoConfigId = mercadoConfigId; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getNombreEmpresa() { return nombreEmpresa; }
    public void setNombreEmpresa(String nombreEmpresa) { this.nombreEmpresa = nombreEmpresa; }
}
