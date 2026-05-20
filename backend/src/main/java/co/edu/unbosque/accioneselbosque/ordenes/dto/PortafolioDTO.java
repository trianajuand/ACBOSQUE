package co.edu.unbosque.accioneselbosque.ordenes.dto;

import java.math.BigDecimal;
import java.util.List;

public class PortafolioDTO {

    private List<HoldingDTO> holdings;
    private BigDecimal valorTotalPortafolio;
    private BigDecimal gananciaPerdidaTotal;
    private BigDecimal gananciaPerdidaTotalPct;
    private String vistaPreferida;   // LISTA | GRAFICO

    public PortafolioDTO() {}

    public List<HoldingDTO> getHoldings() { return holdings; }
    public void setHoldings(List<HoldingDTO> holdings) { this.holdings = holdings; }

    public BigDecimal getValorTotalPortafolio() { return valorTotalPortafolio; }
    public void setValorTotalPortafolio(BigDecimal valorTotalPortafolio) { this.valorTotalPortafolio = valorTotalPortafolio; }

    public BigDecimal getGananciaPerdidaTotal() { return gananciaPerdidaTotal; }
    public void setGananciaPerdidaTotal(BigDecimal gananciaPerdidaTotal) { this.gananciaPerdidaTotal = gananciaPerdidaTotal; }

    public BigDecimal getGananciaPerdidaTotalPct() { return gananciaPerdidaTotalPct; }
    public void setGananciaPerdidaTotalPct(BigDecimal gananciaPerdidaTotalPct) { this.gananciaPerdidaTotalPct = gananciaPerdidaTotalPct; }

    public String getVistaPreferida() { return vistaPreferida; }
    public void setVistaPreferida(String vistaPreferida) { this.vistaPreferida = vistaPreferida; }
}
