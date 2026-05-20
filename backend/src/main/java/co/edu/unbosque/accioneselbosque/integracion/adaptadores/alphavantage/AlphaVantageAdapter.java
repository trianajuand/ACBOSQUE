package co.edu.unbosque.accioneselbosque.integracion.adaptadores.alphavantage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

@Component
public class AlphaVantageAdapter implements IAlphaVantage {

    private static final Logger log = LoggerFactory.getLogger(AlphaVantageAdapter.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;
    private volatile LocalDateTime blockedUntil = null;

    public AlphaVantageAdapter(
            @Value("${alphavantage.base-url}") String baseUrl,
            @Value("${alphavantage.api-key}") String apiKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(6_000);
        factory.setReadTimeout(12_000);
        this.restTemplate = new RestTemplate(factory);
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> obtenerCotizacionGlobal(String simbolo) {
        if (estaRateLimitado()) return Collections.emptyMap();
        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/query")
                    .queryParam("function", "GLOBAL_QUOTE")
                    .queryParam("symbol", simbolo)
                    .queryParam("apikey", apiKey)
                    .toUriString();

            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    url, HttpMethod.GET, HttpEntity.EMPTY,
                    new ParameterizedTypeReference<Map<String, Object>>() {});

            if (resp.getBody() != null) {
                if (resp.getBody().containsKey("Global Quote")) {
                    return (Map<String, Object>) resp.getBody().get("Global Quote");
                }
                if (esRateLimitMsg(resp.getBody())) activarCircuitBreaker();
                log.warn("Alpha Vantage GLOBAL_QUOTE sin datos para '{}': {}", simbolo, extraerMensajeAV(resp.getBody()));
            }
        } catch (Exception e) {
            log.error("Error Alpha Vantage GLOBAL_QUOTE simbolo={}: {}", simbolo, e.getMessage());
        }
        return Collections.emptyMap();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> obtenerSerieTemporalDiaria(String simbolo) {
        if (estaRateLimitado()) return Collections.emptyMap();
        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/query")
                    .queryParam("function", "TIME_SERIES_DAILY")
                    .queryParam("symbol", simbolo)
                    .queryParam("outputsize", "compact")
                    .queryParam("apikey", apiKey)
                    .toUriString();

            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    url, HttpMethod.GET, HttpEntity.EMPTY,
                    new ParameterizedTypeReference<Map<String, Object>>() {});

            if (resp.getBody() != null) {
                if (resp.getBody().containsKey("Time Series (Daily)")) {
                    return (Map<String, Object>) resp.getBody().get("Time Series (Daily)");
                }
                if (esRateLimitMsg(resp.getBody())) activarCircuitBreaker();
                log.warn("Alpha Vantage TIME_SERIES_DAILY sin datos para '{}': {}", simbolo, extraerMensajeAV(resp.getBody()));
            }
        } catch (Exception e) {
            log.error("Error Alpha Vantage TIME_SERIES_DAILY simbolo={}: {}", simbolo, e.getMessage());
        }
        return Collections.emptyMap();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> obtenerResumenEmpresa(String simbolo) {
        if (estaRateLimitado()) return Collections.emptyMap();
        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/query")
                    .queryParam("function", "OVERVIEW")
                    .queryParam("symbol", simbolo)
                    .queryParam("apikey", apiKey)
                    .toUriString();

            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    url, HttpMethod.GET, HttpEntity.EMPTY,
                    new ParameterizedTypeReference<Map<String, Object>>() {});

            if (resp.getBody() != null) {
                if (esRateLimitMsg(resp.getBody())) {
                    activarCircuitBreaker();
                    return Collections.emptyMap();
                }
                if (!resp.getBody().isEmpty()) return resp.getBody();
                log.warn("Alpha Vantage OVERVIEW sin datos para '{}'", simbolo);
            }
        } catch (Exception e) {
            log.error("Error Alpha Vantage OVERVIEW simbolo={}: {}", simbolo, e.getMessage());
        }
        return Collections.emptyMap();
    }

    private boolean estaRateLimitado() {
        return blockedUntil != null && LocalDateTime.now().isBefore(blockedUntil);
    }

    private boolean esRateLimitMsg(Map<String, Object> body) {
        return body.containsKey("Note") || body.containsKey("Information");
    }

    private void activarCircuitBreaker() {
        LocalDateTime medianoche = LocalDate.now().plusDays(1).atStartOfDay();
        if (blockedUntil == null || blockedUntil.isBefore(medianoche)) {
            blockedUntil = medianoche;
            log.warn("Alpha Vantage RATE LIMIT detectado, bloqueando llamadas hasta {}", medianoche);
        }
    }

    private String extraerMensajeAV(Map<String, Object> body) {
        if (body.containsKey("Note")) return limitar(body.get("Note"));
        if (body.containsKey("Information")) return limitar(body.get("Information"));
        if (body.containsKey("Error Message")) return body.get("Error Message").toString();
        return "Respuesta inesperada: " + body.keySet();
    }

    private String limitar(Object valor) {
        String texto = valor != null ? valor.toString() : "";
        return texto.substring(0, Math.min(120, texto.length()));
    }
}
