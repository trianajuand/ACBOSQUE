package co.edu.unbosque.accioneselbosque.ordenes.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Un registro por inversionista. inversionista_id = PK (shared con inversionista.id).
 */
@Entity
@Table(name = "cuenta_fondos")
public class CuentaFondos {

    @Id
    @Column(name = "inversionista_id")
    private Long inversionistaId;

    @Column(name = "saldo_disponible", nullable = false, precision = 18, scale = 4)
    private BigDecimal saldoDisponible;

    @Column(name = "fondos_reservados", nullable = false, precision = 18, scale = 4)
    private BigDecimal fondosReservados;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    public CuentaFondos() {}

    public Long getInversionistaId() { return inversionistaId; }
    public void setInversionistaId(Long inversionistaId) { this.inversionistaId = inversionistaId; }

    public BigDecimal getSaldoDisponible() { return saldoDisponible; }
    public void setSaldoDisponible(BigDecimal saldoDisponible) { this.saldoDisponible = saldoDisponible; }

    public BigDecimal getFondosReservados() { return fondosReservados; }
    public void setFondosReservados(BigDecimal fondosReservados) { this.fondosReservados = fondosReservados; }

    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(LocalDateTime actualizadoEn) { this.actualizadoEn = actualizadoEn; }
}
