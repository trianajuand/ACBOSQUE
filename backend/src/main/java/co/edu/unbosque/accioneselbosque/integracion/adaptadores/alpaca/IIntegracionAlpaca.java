package co.edu.unbosque.accioneselbosque.integracion.adaptadores.alpaca;

import co.edu.unbosque.accioneselbosque.autenticacion.model.Inversionista;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Usuario;

import java.util.List;
import java.util.Map;

public interface IIntegracionAlpaca {

    // --- Broker: gestión de cuentas ---
    String crearCuenta(Usuario usuario, Inversionista inversionista);

    // --- Trading via Broker API (opera en nombre del sub-usuario) ---
    String crearOrden(String accountId, String simbolo, String tipoOrden,
                      String lado, String cantidad, String precioLimite, String precioStop);

    boolean cancelarOrden(String accountId, String alpacaOrderId);

    List<Map<String, Object>> obtenerOrdenes(String accountId, String estado);

    Map<String, Object> obtenerOrden(String accountId, String alpacaOrderId);

    // --- Cuenta / balance ---
    Map<String, Object> obtenerCuenta(String accountId);

    List<Map<String, Object>> obtenerPosiciones(String accountId);

    // --- Market Data (US stocks) ---
    Map<String, Object> obtenerSnapshot(String simbolo);

    Map<String, Object> obtenerSnapshots(List<String> simbolos);

    List<Map<String, Object>> obtenerBarras(String simbolo, String timeframe,
                                             String inicio, String fin);
}
