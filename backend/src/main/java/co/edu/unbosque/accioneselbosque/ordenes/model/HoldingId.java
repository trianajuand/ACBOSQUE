package co.edu.unbosque.accioneselbosque.ordenes.model;

import java.io.Serializable;
import java.util.Objects;

public class HoldingId implements Serializable {

    private Long inversionistaId;
    private Long activoId;

    public HoldingId() {}

    public HoldingId(Long inversionistaId, Long activoId) {
        this.inversionistaId = inversionistaId;
        this.activoId = activoId;
    }

    public Long getInversionistaId() { return inversionistaId; }
    public void setInversionistaId(Long inversionistaId) { this.inversionistaId = inversionistaId; }

    public Long getActivoId() { return activoId; }
    public void setActivoId(Long activoId) { this.activoId = activoId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HoldingId)) return false;
        HoldingId that = (HoldingId) o;
        return Objects.equals(inversionistaId, that.inversionistaId) && Objects.equals(activoId, that.activoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inversionistaId, activoId);
    }
}
