package co.edu.unbosque.accioneselbosque.integracion.adaptadores.alpaca;

import co.edu.unbosque.accioneselbosque.autenticacion.model.Inversionista;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

@Component
public class AlpacaAdapter implements IIntegracionAlpaca {

    private static final Logger log = LoggerFactory.getLogger(AlpacaAdapter.class);

    private final RestTemplate restTemplate;
    private final String brokerBaseUrl;
    private final String dataBaseUrl;
    private final String brokerApiKey;
    private final String brokerApiSecret;
    private final String dataApiKey;
    private final String dataApiSecret;
    private final String authBasic;

    public AlpacaAdapter(
            @Value("${alpaca.broker.base-url}") String brokerBaseUrl,
            @Value("${alpaca.data.base-url}") String dataBaseUrl,
            @Value("${alpaca.broker.api-key}") String brokerApiKey,
            @Value("${alpaca.broker.api-secret}") String brokerApiSecret,
            @Value("${alpaca.data.api-key}") String dataApiKey,
            @Value("${alpaca.data.api-secret}") String dataApiSecret) {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(10_000);
        this.restTemplate = new RestTemplate(factory);
        this.brokerBaseUrl = brokerBaseUrl;
        this.dataBaseUrl = dataBaseUrl;
        this.brokerApiKey = brokerApiKey;
        this.brokerApiSecret = brokerApiSecret;
        this.dataApiKey = dataApiKey;
        this.dataApiSecret = dataApiSecret;
        this.authBasic = "Basic " + Base64.getEncoder()
                .encodeToString((brokerApiKey + ":" + brokerApiSecret).getBytes());
    }

    // =========================================================
    // Helpers de headers
    // =========================================================

    private HttpHeaders headersBroker() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("Authorization", authBasic);
        return h;
    }

    private HttpHeaders headersData() {
        HttpHeaders h = new HttpHeaders();
        h.set("APCA-API-KEY-ID", dataApiKey);
        h.set("APCA-API-SECRET-KEY", dataApiSecret);
        return h;
    }

    // =========================================================
    // Broker: gestión de cuentas
    // =========================================================

    @Override
    public String crearCuenta(Usuario usuario, Inversionista inversionista) {
        try {
            String[] partes = usuario.getNombreCompleto().split(" ", 2);
            String nombre = partes[0];
            String apellido = partes.length > 1 ? partes[1] : nombre;

            Map<String, Object> body = new HashMap<>();
            body.put("account_type", "trading");
            body.put("contact", Map.of(
                    "email_address", usuario.getCorreo(),
                    "phone_number", normalizarTelefono(usuario.getTelefono()),
                    "street_address", List.of(valor(inversionista.getDireccion(), "123 Main St")),
                    "city", valor(inversionista.getCiudad(), "Bogota"),
                    "state", "CO",
                    "postal_code", valor(inversionista.getCodigoPostal(), "110111"),
                    "country", normalizarPais(inversionista.getPais())
            ));
            body.put("identity", Map.of(
                    "given_name", nombre,
                    "family_name", apellido,
                    "date_of_birth", usuario.getFechaNacimiento() != null ? usuario.getFechaNacimiento().toString() : "1990-01-01",
                    "tax_id", generarTaxIdSandbox(usuario),
                    "tax_id_type", "OTHER",
                    "country_of_citizenship", normalizarPais(inversionista.getPais()),
                    "country_of_birth", normalizarPais(inversionista.getPais()),
                    "country_of_tax_residence", normalizarPais(inversionista.getPais()),
                    "funding_source", List.of("employment_income")
            ));
            body.put("disclosures", Map.of(
                    "is_control_person", false,
                    "is_affiliated_exchange_or_finra", false,
                    "is_politically_exposed", false,
                    "immediate_family_exposed", false
            ));
            body.put("agreements", List.of(
                    Map.of("agreement", "margin_agreement", "signed_at", "2024-01-01T00:00:00Z", "ip_address", "127.0.0.1"),
                    Map.of("agreement", "account_agreement", "signed_at", "2024-01-01T00:00:00Z", "ip_address", "127.0.0.1"),
                    Map.of("agreement", "customer_agreement", "signed_at", "2024-01-01T00:00:00Z", "ip_address", "127.0.0.1")
            ));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headersBroker());
            Map respuesta = restTemplate.postForObject(brokerBaseUrl + "/v1/accounts", request, Map.class);

            if (respuesta != null && respuesta.get("id") != null) {
                String accountId = respuesta.get("id").toString();
                autoFondearCuentaSandbox(accountId, usuario.getCorreo());
                return accountId;
            }
        } catch (Exception e) {
            log.error("Error al crear cuenta Alpaca para {}: {}", usuario.getCorreo(), e.getMessage());
        }
        return null;
    }

    private void autoFondearCuentaSandbox(String accountId, String correo) {
        try {
            Map<String, Object> ach = new HashMap<>();
            ach.put("account_owner_name", "Sandbox User");
            ach.put("bank_account_type", "CHECKING");
            ach.put("bank_account_number", "sandbox" + Math.abs(accountId.hashCode()));
            ach.put("bank_routing_number", "121000358");
            ach.put("nickname", "sandbox-funding");

            HttpEntity<Map<String, Object>> achReq = new HttpEntity<>(ach, headersBroker());
            Map achResp = restTemplate.postForObject(
                    brokerBaseUrl + "/v1/accounts/" + accountId + "/ach_relationships", achReq, Map.class);

            if (achResp == null || achResp.get("id") == null) return;
            String achId = achResp.get("id").toString();

            Map<String, Object> transfer = new HashMap<>();
            transfer.put("transfer_type", "ach");
            transfer.put("relationship_id", achId);
            transfer.put("amount", "50000");
            transfer.put("direction", "INCOMING");
            transfer.put("instant_amount", "50000");

            HttpEntity<Map<String, Object>> transferReq = new HttpEntity<>(transfer, headersBroker());
            restTemplate.postForObject(
                    brokerBaseUrl + "/v1/accounts/" + accountId + "/transfers", transferReq, Map.class);

            log.info("Auto-fondeo sandbox iniciado para cuenta {} ({})", accountId, correo);
        } catch (Exception e) {
            log.warn("No se pudo auto-fondear cuenta sandbox {}: {}", accountId, e.getMessage());
        }
    }

    private String valor(String valor, String respaldo) {
        return valor == null || valor.isBlank() ? respaldo : valor;
    }

    private String normalizarPais(String pais) {
        if (pais == null || pais.isBlank() || "CO".equalsIgnoreCase(pais)) {
            return "COL";
        }
        return pais.length() == 2 ? pais.toUpperCase(Locale.ROOT) : pais.toUpperCase(Locale.ROOT);
    }

    private String normalizarTelefono(String telefono) {
        if (telefono == null || telefono.isBlank()) {
            return "+573001234567";
        }
        String limpio = telefono.replaceAll("[^0-9+]", "");
        if (limpio.startsWith("+")) {
            return limpio;
        }
        if (limpio.startsWith("57")) {
            return "+" + limpio;
        }
        return "+57" + limpio;
    }

    private String generarTaxIdSandbox(Usuario usuario) {
        String base = usuario.getId() != null ? usuario.getId().toString() : usuario.getCorreo();
        int hash = Math.abs(Objects.hash(base, usuario.getCorreo()));
        String taxId = String.valueOf(100000000 + (hash % 899999999));
        return todosCaracteresIguales(taxId) ? "123456789" : taxId;
    }

    private boolean todosCaracteresIguales(String valor) {
        if (valor == null || valor.isEmpty()) {
            return false;
        }
        char primero = valor.charAt(0);
        for (int i = 1; i < valor.length(); i++) {
            if (valor.charAt(i) != primero) {
                return false;
            }
        }
        return true;
    }

    // =========================================================
    // Trading via Broker API
    // =========================================================

    @Override
    public String crearOrden(String accountId, String simbolo, String tipoOrden,
                              String lado, String cantidad, String precioLimite, String precioStop) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("symbol", simbolo);
            body.put("qty", cantidad);
            body.put("side", lado);             // "buy" | "sell"
            body.put("type", tipoOrden);        // "market" | "limit" | "stop" | "stop_limit"
            body.put("time_in_force", "day");

            if (precioLimite != null && !precioLimite.isEmpty()) {
                body.put("limit_price", precioLimite);
            }
            if (precioStop != null && !precioStop.isEmpty()) {
                body.put("stop_price", precioStop);
            }

            String url = brokerBaseUrl + "/v1/trading/accounts/" + accountId + "/orders";
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headersBroker());
            Map respuesta = restTemplate.postForObject(url, request, Map.class);

            if (respuesta != null && respuesta.get("id") != null) {
                return respuesta.get("id").toString();
            }
        } catch (Exception e) {
            log.error("Error al crear orden Alpaca accountId={}, simbolo={}: {}", accountId, simbolo, e.getMessage());
        }
        return null;
    }

    @Override
    public boolean cancelarOrden(String accountId, String alpacaOrderId) {
        try {
            String url = brokerBaseUrl + "/v1/trading/accounts/" + accountId + "/orders/" + alpacaOrderId;
            HttpEntity<Void> request = new HttpEntity<>(headersBroker());
            restTemplate.exchange(url, HttpMethod.DELETE, request, Void.class);
            return true;
        } catch (Exception e) {
            log.error("Error al cancelar orden Alpaca orderId={}: {}", alpacaOrderId, e.getMessage());
            return false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> obtenerOrdenes(String accountId, String estado) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(brokerBaseUrl + "/v1/trading/accounts/" + accountId + "/orders")
                    .queryParam("status", estado != null ? estado : "all")
                    .queryParam("limit", 100)
                    .toUriString();

            HttpEntity<Void> request = new HttpEntity<>(headersBroker());
            ResponseEntity<List<Map<String, Object>>> resp = restTemplate.exchange(
                    url, HttpMethod.GET, request,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {});

            return resp.getBody() != null ? resp.getBody() : Collections.emptyList();
        } catch (Exception e) {
            log.error("Error al obtener órdenes Alpaca accountId={}: {}", accountId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> obtenerOrden(String accountId, String alpacaOrderId) {
        try {
            String url = brokerBaseUrl + "/v1/trading/accounts/" + accountId + "/orders/" + alpacaOrderId;
            HttpEntity<Void> request = new HttpEntity<>(headersBroker());
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    url, HttpMethod.GET, request,
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            return resp.getBody() != null ? resp.getBody() : Collections.emptyMap();
        } catch (Exception e) {
            log.error("Error al obtener orden Alpaca orderId={}: {}", alpacaOrderId, e.getMessage());
            return Collections.emptyMap();
        }
    }

    // =========================================================
    // Cuenta / balance
    // =========================================================

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> obtenerCuenta(String accountId) {
        try {
            String url = brokerBaseUrl + "/v1/trading/accounts/" + accountId + "/account";
            HttpEntity<Void> request = new HttpEntity<>(headersBroker());
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    url, HttpMethod.GET, request,
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            return resp.getBody() != null ? resp.getBody() : Collections.emptyMap();
        } catch (Exception e) {
            log.error("Error al obtener cuenta Alpaca accountId={}: {}", accountId, e.getMessage());
            return Collections.emptyMap();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> obtenerPosiciones(String accountId) {
        try {
            String url = brokerBaseUrl + "/v1/trading/accounts/" + accountId + "/positions";
            HttpEntity<Void> request = new HttpEntity<>(headersBroker());
            ResponseEntity<List<Map<String, Object>>> resp = restTemplate.exchange(
                    url, HttpMethod.GET, request,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            return resp.getBody() != null ? resp.getBody() : Collections.emptyList();
        } catch (Exception e) {
            log.error("Error al obtener posiciones Alpaca accountId={}: {}", accountId, e.getMessage());
            return Collections.emptyList();
        }
    }

    // =========================================================
    // Sandbox: fondear cuenta via Journals API
    // =========================================================

    @Override
    public boolean fondearCuentaSandbox(String accountId, String monto) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("from_account", "FIRM");
            body.put("entry_type", "JNLC");
            body.put("to_account", accountId);
            body.put("amount", monto);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headersBroker());
            restTemplate.postForObject(brokerBaseUrl + "/v1/journals", request, Map.class);
            log.info("Cuenta sandbox fondeada: accountId={}, monto={}", accountId, monto);
            return true;
        } catch (Exception e) {
            log.error("Error al fondear cuenta sandbox accountId={}: {}", accountId, e.getMessage());
            return false;
        }
    }

    // =========================================================
    // Market Data API (US stocks)
    // =========================================================

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> obtenerSnapshot(String simbolo) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(dataBaseUrl + "/v2/stocks/" + simbolo + "/snapshot")
                    .queryParam("feed", "iex")
                    .toUriString();
            HttpEntity<Void> request = new HttpEntity<>(headersData());
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    url, HttpMethod.GET, request,
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            return resp.getBody() != null ? resp.getBody() : Collections.emptyMap();
        } catch (Exception e) {
            log.error("Error al obtener snapshot Alpaca simbolo={}: {}", simbolo, e.getMessage());
            return Collections.emptyMap();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> obtenerSnapshots(List<String> simbolos) {
        try {
            String symbolsParam = String.join(",", simbolos);
            String url = UriComponentsBuilder
                    .fromHttpUrl(dataBaseUrl + "/v2/stocks/snapshots")
                    .queryParam("symbols", symbolsParam)
                    .queryParam("feed", "iex")
                    .toUriString();

            HttpEntity<Void> request = new HttpEntity<>(headersData());
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    url, HttpMethod.GET, request,
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            return resp.getBody() != null ? resp.getBody() : Collections.emptyMap();
        } catch (Exception e) {
            log.error("Error al obtener snapshots Alpaca: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> obtenerBarras(String simbolo, String timeframe,
                                                    String inicio, String fin) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(dataBaseUrl + "/v2/stocks/" + simbolo + "/bars")
                    .queryParam("timeframe", timeframe != null ? timeframe : "1Day")
                    .queryParam("start", inicio)
                    .queryParam("end", fin)
                    .queryParam("limit", 365)
                    .queryParam("feed", "iex")
                    .toUriString();

            HttpEntity<Void> request = new HttpEntity<>(headersData());
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    url, HttpMethod.GET, request,
                    new ParameterizedTypeReference<Map<String, Object>>() {});

            if (resp.getBody() != null && resp.getBody().get("bars") instanceof List) {
                return (List<Map<String, Object>>) resp.getBody().get("bars");
            }
        } catch (Exception e) {
            log.error("Error al obtener barras Alpaca simbolo={}: {}", simbolo, e.getMessage());
        }
        return Collections.emptyList();
    }
}
