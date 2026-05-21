package co.edu.unbosque.accioneselbosque.administracion.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class ParametroComisionDTO {

    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    private BigDecimal porcentajeComision;

    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    private BigDecimal splitPlataforma;

    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    private BigDecimal splitComisionista;

    public BigDecimal getPorcentajeComision() { return porcentajeComision; }
    public void setPorcentajeComision(BigDecimal porcentajeComision) { this.porcentajeComision = porcentajeComision; }

    public BigDecimal getSplitPlataforma() { return splitPlataforma; }
    public void setSplitPlataforma(BigDecimal splitPlataforma) { this.splitPlataforma = splitPlataforma; }

    public BigDecimal getSplitComisionista() { return splitComisionista; }
    public void setSplitComisionista(BigDecimal splitComisionista) { this.splitComisionista = splitComisionista; }
}
