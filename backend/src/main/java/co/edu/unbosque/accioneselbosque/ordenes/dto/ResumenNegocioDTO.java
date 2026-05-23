package co.edu.unbosque.accioneselbosque.ordenes.dto;

import java.math.BigDecimal;
import java.util.List;

public class ResumenNegocioDTO {

    private long transacciones;
    private BigDecimal volumenTransacciones = BigDecimal.ZERO;
    private BigDecimal comisionesGeneradas = BigDecimal.ZERO;
    private List<ResumenMercadoDTO> tendenciasPorMercado;

    public long getTransacciones() { return transacciones; }
    public void setTransacciones(long transacciones) { this.transacciones = transacciones; }

    public BigDecimal getVolumenTransacciones() { return volumenTransacciones; }
    public void setVolumenTransacciones(BigDecimal volumenTransacciones) { this.volumenTransacciones = volumenTransacciones; }

    public BigDecimal getComisionesGeneradas() { return comisionesGeneradas; }
    public void setComisionesGeneradas(BigDecimal comisionesGeneradas) { this.comisionesGeneradas = comisionesGeneradas; }

    public List<ResumenMercadoDTO> getTendenciasPorMercado() { return tendenciasPorMercado; }
    public void setTendenciasPorMercado(List<ResumenMercadoDTO> tendenciasPorMercado) { this.tendenciasPorMercado = tendenciasPorMercado; }
}
