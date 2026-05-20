package co.edu.unbosque.accioneselbosque.autenticacion.dto;

public class PreferenciasOperacionDTO {

    // MARKET, LIMIT, STOP_LOSS, TAKE_PROFIT
    private String tipoOrdenDefault;

    // LISTA, GRAFICO
    private String vistaPortafolio;

    public PreferenciasOperacionDTO() {}

    public String getTipoOrdenDefault() { return tipoOrdenDefault; }
    public void setTipoOrdenDefault(String tipoOrdenDefault) { this.tipoOrdenDefault = tipoOrdenDefault; }

    public String getVistaPortafolio() { return vistaPortafolio; }
    public void setVistaPortafolio(String vistaPortafolio) { this.vistaPortafolio = vistaPortafolio; }
}
