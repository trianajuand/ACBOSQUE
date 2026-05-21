package co.edu.unbosque.accioneselbosque.administracion.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class DashboardEjecutivoDTO {

    private long usuariosActivos;
    private long crecimientoUsuarios;
    private long transacciones;
    private BigDecimal volumenTransacciones = BigDecimal.ZERO;
    private BigDecimal comisionesGeneradas = BigDecimal.ZERO;
    private List<TendenciaMercadoDTO> tendenciasPorMercado = new ArrayList<>();

    public long getUsuariosActivos() { return usuariosActivos; }
    public void setUsuariosActivos(long usuariosActivos) { this.usuariosActivos = usuariosActivos; }

    public long getCrecimientoUsuarios() { return crecimientoUsuarios; }
    public void setCrecimientoUsuarios(long crecimientoUsuarios) { this.crecimientoUsuarios = crecimientoUsuarios; }

    public long getTransacciones() { return transacciones; }
    public void setTransacciones(long transacciones) { this.transacciones = transacciones; }

    public BigDecimal getVolumenTransacciones() { return volumenTransacciones; }
    public void setVolumenTransacciones(BigDecimal volumenTransacciones) { this.volumenTransacciones = volumenTransacciones; }

    public BigDecimal getComisionesGeneradas() { return comisionesGeneradas; }
    public void setComisionesGeneradas(BigDecimal comisionesGeneradas) { this.comisionesGeneradas = comisionesGeneradas; }

    public List<TendenciaMercadoDTO> getTendenciasPorMercado() { return tendenciasPorMercado; }
    public void setTendenciasPorMercado(List<TendenciaMercadoDTO> tendenciasPorMercado) { this.tendenciasPorMercado = tendenciasPorMercado; }
}
