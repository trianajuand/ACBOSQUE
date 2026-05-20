package co.edu.unbosque.accioneselbosque.ordenes.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ComisionDetalleDTO {

    private Long ordenId;
    private String simbolo;
    private BigDecimal montoBase;
    private BigDecimal porcentajeComision;
    private BigDecimal montoComision;
    private LocalDateTime creadaEn;

    public ComisionDetalleDTO() {}

    public Long getOrdenId() { return ordenId; }
    public void setOrdenId(Long ordenId) { this.ordenId = ordenId; }

    public String getSimbolo() { return simbolo; }
    public void setSimbolo(String simbolo) { this.simbolo = simbolo; }

    public BigDecimal getMontoBase() { return montoBase; }
    public void setMontoBase(BigDecimal montoBase) { this.montoBase = montoBase; }

    public BigDecimal getPorcentajeComision() { return porcentajeComision; }
    public void setPorcentajeComision(BigDecimal porcentajeComision) { this.porcentajeComision = porcentajeComision; }

    public BigDecimal getMontoComision() { return montoComision; }
    public void setMontoComision(BigDecimal montoComision) { this.montoComision = montoComision; }

    public LocalDateTime getCreadaEn() { return creadaEn; }
    public void setCreadaEn(LocalDateTime creadaEn) { this.creadaEn = creadaEn; }
}
