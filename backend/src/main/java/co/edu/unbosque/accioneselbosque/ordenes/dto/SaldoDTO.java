package co.edu.unbosque.accioneselbosque.ordenes.dto;

import java.math.BigDecimal;
import java.util.List;

public class SaldoDTO {

    private BigDecimal saldoDisponible;
    private BigDecimal fondosReservados;
    private BigDecimal totalComisionesPagadas;
    private List<ComisionDetalleDTO> historialComisiones;

    public SaldoDTO() {}

    public BigDecimal getSaldoDisponible() { return saldoDisponible; }
    public void setSaldoDisponible(BigDecimal saldoDisponible) { this.saldoDisponible = saldoDisponible; }

    public BigDecimal getFondosReservados() { return fondosReservados; }
    public void setFondosReservados(BigDecimal fondosReservados) { this.fondosReservados = fondosReservados; }

    public BigDecimal getTotalComisionesPagadas() { return totalComisionesPagadas; }
    public void setTotalComisionesPagadas(BigDecimal totalComisionesPagadas) { this.totalComisionesPagadas = totalComisionesPagadas; }

    public List<ComisionDetalleDTO> getHistorialComisiones() { return historialComisiones; }
    public void setHistorialComisiones(List<ComisionDetalleDTO> historialComisiones) { this.historialComisiones = historialComisiones; }
}
