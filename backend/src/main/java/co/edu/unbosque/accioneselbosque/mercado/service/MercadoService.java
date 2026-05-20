package co.edu.unbosque.accioneselbosque.mercado.service;

import co.edu.unbosque.accioneselbosque.integracion.adaptadores.alpaca.IIntegracionAlpaca;
import co.edu.unbosque.accioneselbosque.integracion.adaptadores.alphavantage.IAlphaVantage;
import co.edu.unbosque.accioneselbosque.mercado.dto.CotizacionDTO;
import co.edu.unbosque.accioneselbosque.mercado.dto.DetalleAccionDTO;
import co.edu.unbosque.accioneselbosque.mercado.interfaces.IVerificacionMercado;
import co.edu.unbosque.accioneselbosque.mercado.model.PrecioCache;
import co.edu.unbosque.accioneselbosque.mercado.repository.PrecioCacheRepository;
import co.edu.unbosque.accioneselbosque.shared.exceptions.SimboloInvalidoException;
import co.edu.unbosque.accioneselbosque.trazabilidad.interfaces.IAuditLog;
import co.edu.unbosque.accioneselbosque.trazabilidad.model.TipoEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class MercadoService implements IVerificacionMercado {

    private static final Logger log = LoggerFactory.getLogger(MercadoService.class);

    private static final List<String> SIMBOLOS_DEFAULT_US =
            List.of("AAPL", "MSFT", "GOOGL", "AMZN", "TSLA", "META", "NVDA", "JPM");

    public static final Map<String, List<String>> SIMBOLOS_DISPONIBLES = Map.of(
            "Tecnologia", List.of("AAPL", "MSFT", "GOOGL", "NVDA", "META", "AMZN", "NFLX", "AMD", "INTC", "CRM"),
            "Finanzas", List.of("JPM", "BAC", "GS", "MS", "V", "MA", "AXP", "WFC", "C", "BLK"),
            "Salud", List.of("JNJ", "UNH", "PFE", "ABBV", "MRK", "LLY", "TMO", "ABT", "BMY", "AMGN"),
            "Energia", List.of("XOM", "CVX", "COP", "SLB", "EOG", "PXD", "MPC", "VLO", "PSX", "HAL"),
            "Consumo", List.of("TSLA", "NKE", "MCD", "SBUX", "TGT", "HD", "LOW", "COST", "WMT", "DIS"),
            "Londres (LSE)", List.of("HSBA.L", "BP.L", "SHEL.L", "AZN.L", "GSK.L", "ULVR.L", "RIO.L", "BT.L"),
            "Tokio (TSE)", List.of("7203.T", "6758.T", "9984.T", "8306.T", "6861.T", "4502.T", "6501.T"),
            "Sidney (ASX)", List.of("BHP.AX", "CBA.AX", "CSL.AX", "NAB.AX", "WBC.AX", "ANZ.AX", "RIO.AX")
    );

    private static final Pattern PATRON_SIMBOLO =
            Pattern.compile("^[A-Z0-9]{1,6}(\\.(T|L|AX|TO))?$");

    @org.springframework.beans.factory.annotation.Value("${app.mercado.sandbox-siempre-abierto:false}")
    private boolean sandboxSiempreAbierto;

    private record CachedResponse(Map<String, Object> data, LocalDateTime cachedAt) {}

    private final Map<String, CachedResponse> overviewCache = new ConcurrentHashMap<>();
    private final Map<String, CachedResponse> serieCache = new ConcurrentHashMap<>();

    private final IIntegracionAlpaca alpaca;
    private final IAlphaVantage alphaVantage;
    private final PrecioCacheRepository cacheRepo;
    private final IAuditLog auditLog;

    public MercadoService(IIntegracionAlpaca alpaca, IAlphaVantage alphaVantage,
                          PrecioCacheRepository cacheRepo, IAuditLog auditLog) {
        this.alpaca = alpaca;
        this.alphaVantage = alphaVantage;
        this.cacheRepo = cacheRepo;
        this.auditLog = auditLog;
    }

    @Override
    public boolean esMercadoAbierto(String mercado) {
        if (sandboxSiempreAbierto) return true;
        if (mercado == null) return false;
        return switch (mercado.toUpperCase()) {
            case "NYSE", "NASDAQ", "AMEX", "US", "NYSE/NASDAQ" -> esMercadoUsAbierto();
            case "TSE", "TOKYO" -> esMercadoTokioAbierto();
            case "LSE", "LONDON" -> esMercadoLondresAbierto();
            case "ASX", "SYDNEY" -> esMercadoSidneyAbierto();
            default -> false;
        };
    }

    @Override
    public String detectarMercado(String simbolo) {
        if (simbolo == null) return "DESCONOCIDO";
        String s = simbolo.toUpperCase();
        if (s.endsWith(".T")) return "TSE";
        if (s.endsWith(".L")) return "LSE";
        if (s.endsWith(".AX")) return "ASX";
        if (s.endsWith(".TO")) return "TSX";
        return "NYSE/NASDAQ";
    }

    @Transactional
    public List<CotizacionDTO> obtenerDashboard(String interesesMercado) {
        List<String> simbolos = parsearIntereses(interesesMercado);
        if (simbolos.isEmpty()) simbolos = SIMBOLOS_DEFAULT_US;

        List<CotizacionDTO> resultado = new ArrayList<>();
        for (String simbolo : simbolos) {
            try {
                resultado.add(obtenerCotizacion(simbolo));
            } catch (SimboloInvalidoException e) {
                log.warn("Simbolo omitido del dashboard: {}", e.getMessage());
            }
        }
        return resultado;
    }

    @Transactional
    public CotizacionDTO obtenerCotizacion(String simbolo) {
        String sim = normalizarSimbolo(simbolo != null ? simbolo.toUpperCase() : null);
        validarFormato(sim, simbolo);
        Optional<PrecioCache> cacheOpt = cacheRepo.findBySimbolo(sim);

        if (cacheOpt.isPresent()) {
            PrecioCache cached = cacheOpt.get();
            int ttlMinutos = esSimboloUs(sim) ? 3 : 60;
            if (cached.getActualizadoEn().isAfter(LocalDateTime.now().minusMinutes(ttlMinutos))) {
                return mapearCache(cached);
            }
            if (!esMercadoAbierto(detectarMercado(sim))) {
                return mapearCache(cached);
            }
        }

        return refrescarYRetornar(sim, cacheOpt.orElse(null));
    }

    @Transactional
    public CotizacionDTO validarSimboloOperable(String simbolo) {
        CotizacionDTO cotizacion = obtenerCotizacion(simbolo);
        if (!cotizacionTienePrecio(cotizacion)) {
            cacheRepo.findBySimbolo(cotizacion.getSimbolo()).ifPresent(cacheRepo::delete);
            throw new SimboloInvalidoException(
                    "El ticker '" + cotizacion.getSimbolo() + "' no existe o no tiene cotizacion disponible.");
        }
        return cotizacion;
    }

    @Transactional
    public DetalleAccionDTO obtenerDetalle(String simbolo) {
        simbolo = normalizarSimbolo(simbolo != null ? simbolo.toUpperCase() : null);
        validarFormato(simbolo, simbolo);
        String mercado = detectarMercado(simbolo);
        DetalleAccionDTO dto = new DetalleAccionDTO();
        dto.setSimbolo(simbolo);
        dto.setMercado(mercado);
        dto.setMercadoAbierto(esMercadoAbierto(mercado));

        if (esSimboloUs(simbolo)) {
            rellenarDetalleDesdeAlpaca(dto, simbolo);
        } else {
            boolean precioDesdeCache = false;
            Optional<PrecioCache> cacheOpt = cacheRepo.findBySimbolo(simbolo);
            if (cacheOpt.isPresent()
                    && cacheOpt.get().getActualizadoEn().isAfter(LocalDateTime.now().minusMinutes(60))) {
                copiarCacheADetalle(dto, cacheOpt.get());
                precioDesdeCache = true;
            }
            rellenarDetalleDesdeAlphaVantage(dto, simbolo, precioDesdeCache);
        }
        return dto;
    }

    @Scheduled(fixedDelay = 180_000)
    @Transactional
    public void refrescarCacheSimbolosActivos() {
        Iterable<PrecioCache> todos = cacheRepo.findAll();
        List<PrecioCache> lista = new ArrayList<>();
        todos.forEach(lista::add);

        for (PrecioCache pc : lista) {
            String sim = pc.getSimbolo();
            if (sim == null || sim.replaceAll("[^A-Z]", "").isEmpty()) {
                log.warn("Eliminando entrada invalida de cache: '{}'", sim);
                cacheRepo.delete(pc);
                continue;
            }
            if (!esSimboloUs(sim)) continue;
            refrescarYRetornar(sim, pc);
        }
        if (!lista.isEmpty()) {
            auditLog.registrar(TipoEvento.CACHE_REFRESCADO, "sistema", "Cache de precios refrescado");
        }
    }

    private CotizacionDTO refrescarYRetornar(String simbolo, PrecioCache existente) {
        PrecioCache cache = existente != null ? existente : new PrecioCache();
        cache.setSimbolo(simbolo);
        cache.setMercado(detectarMercado(simbolo));
        cache.setActualizadoEn(LocalDateTime.now());

        try {
            if (esSimboloUs(simbolo)) {
                rellenarCacheDesdeAlpaca(cache, simbolo);
                cache.setFuente("ALPACA");
            } else {
                rellenarCacheDesdeAlphaVantage(cache, simbolo);
                cache.setFuente("ALPHAVANTAGE");
            }
        } catch (Exception e) {
            log.warn("No se pudo refrescar precio de {}: {}", simbolo, e.getMessage());
        }

        cacheRepo.save(cache);
        return mapearCache(cache);
    }

    @SuppressWarnings("unchecked")
    private void rellenarCacheDesdeAlpaca(PrecioCache cache, String simbolo) {
        Map<String, Object> snap = alpaca.obtenerSnapshot(simbolo);
        if (snap.isEmpty()) return;

        Map<String, Object> latestTrade = (Map<String, Object>) snap.get("latestTrade");
        Map<String, Object> dailyBar = (Map<String, Object>) snap.get("dailyBar");
        Map<String, Object> prevDailyBar = (Map<String, Object>) snap.get("prevDailyBar");

        if (latestTrade != null && latestTrade.get("p") != null) {
            cache.setPrecioActual(new BigDecimal(latestTrade.get("p").toString()));
        }
        rellenarOHLC(cache, dailyBar, prevDailyBar);
    }

    private void rellenarCacheDesdeAlphaVantage(PrecioCache cache, String simbolo) {
        Map<String, Object> quote = alphaVantage.obtenerCotizacionGlobal(simboloParaAlphaVantage(simbolo));
        if (quote.isEmpty()) return;

        if (quote.get("05. price") != null) cache.setPrecioActual(new BigDecimal(quote.get("05. price").toString()));
        if (quote.get("02. open") != null) cache.setPrecioApertura(new BigDecimal(quote.get("02. open").toString()));
        if (quote.get("03. high") != null) cache.setPrecioMaximo(new BigDecimal(quote.get("03. high").toString()));
        if (quote.get("04. low") != null) cache.setPrecioMinimo(new BigDecimal(quote.get("04. low").toString()));
        if (quote.get("08. previous close") != null) {
            cache.setPrecioCierreAnterior(new BigDecimal(quote.get("08. previous close").toString()));
        }
        if (quote.get("06. volume") != null) cache.setVolumen(Long.parseLong(quote.get("06. volume").toString()));
        if (quote.get("10. change percent") != null) {
            String pct = quote.get("10. change percent").toString().replace("%", "");
            cache.setVariacionPorcentual(new BigDecimal(pct).setScale(2, RoundingMode.HALF_UP));
        }
    }

    private void rellenarOHLC(PrecioCache cache, Map<String, Object> dailyBar, Map<String, Object> prevDailyBar) {
        if (dailyBar != null) {
            if (dailyBar.get("o") != null) cache.setPrecioApertura(new BigDecimal(dailyBar.get("o").toString()));
            if (dailyBar.get("h") != null) cache.setPrecioMaximo(new BigDecimal(dailyBar.get("h").toString()));
            if (dailyBar.get("l") != null) cache.setPrecioMinimo(new BigDecimal(dailyBar.get("l").toString()));
            if (dailyBar.get("v") != null) cache.setVolumen(Long.parseLong(dailyBar.get("v").toString()));
        }
        if (prevDailyBar != null && prevDailyBar.get("c") != null) {
            BigDecimal cierre = new BigDecimal(prevDailyBar.get("c").toString());
            cache.setPrecioCierreAnterior(cierre);
            if (cache.getPrecioActual() != null && cierre.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal variacion = cache.getPrecioActual()
                        .subtract(cierre)
                        .divide(cierre, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                cache.setVariacionPorcentual(variacion.setScale(2, RoundingMode.HALF_UP));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void rellenarDetalleDesdeAlpaca(DetalleAccionDTO dto, String simbolo) {
        Map<String, Object> snap = alpaca.obtenerSnapshot(simbolo);
        if (!snap.isEmpty()) {
            Map<String, Object> latestTrade = (Map<String, Object>) snap.get("latestTrade");
            Map<String, Object> dailyBar = (Map<String, Object>) snap.get("dailyBar");
            Map<String, Object> prevDailyBar = (Map<String, Object>) snap.get("prevDailyBar");

            if (latestTrade != null && latestTrade.get("p") != null) {
                dto.setPrecioActual(new BigDecimal(latestTrade.get("p").toString()));
            }
            copiarOHLCADetalle(dto, dailyBar, prevDailyBar);
        }

        Map<String, Object> overview = overviewConCache(simbolo);
        copiarOverview(dto, overview);

        String fin = java.time.LocalDate.now().toString();
        String inicio = java.time.LocalDate.now().minusDays(30).toString();
        dto.setHistoricoPrecios(alpaca.obtenerBarras(simbolo, "1Day", inicio, fin));
    }

    @SuppressWarnings("unchecked")
    private void rellenarDetalleDesdeAlphaVantage(DetalleAccionDTO dto, String simbolo, boolean saltarCotizacion) {
        String simAV = simboloParaAlphaVantage(simbolo);

        if (!saltarCotizacion) {
            Map<String, Object> quote = alphaVantage.obtenerCotizacionGlobal(simAV);
            if (!quote.isEmpty()) {
                if (quote.get("05. price") != null) dto.setPrecioActual(new BigDecimal(quote.get("05. price").toString()));
                if (quote.get("02. open") != null) dto.setPrecioApertura(new BigDecimal(quote.get("02. open").toString()));
                if (quote.get("03. high") != null) dto.setPrecioMaximo(new BigDecimal(quote.get("03. high").toString()));
                if (quote.get("04. low") != null) dto.setPrecioMinimo(new BigDecimal(quote.get("04. low").toString()));
                if (quote.get("08. previous close") != null) {
                    dto.setPrecioCierreAnterior(new BigDecimal(quote.get("08. previous close").toString()));
                }
                if (quote.get("06. volume") != null) dto.setVolumen(Long.parseLong(quote.get("06. volume").toString()));
                if (quote.get("10. change percent") != null) {
                    String pct = quote.get("10. change percent").toString().replace("%", "");
                    dto.setVariacionPorcentual(new BigDecimal(pct).setScale(2, RoundingMode.HALF_UP));
                }
            }
        }

        copiarOverview(dto, overviewConCache(simAV));

        Map<String, Object> serie = serieConCache(simAV);
        if (!serie.isEmpty()) {
            List<Map<String, Object>> historico = new ArrayList<>();
            serie.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
                    .limit(30)
                    .forEach(e -> {
                        Map<String, Object> punto = new LinkedHashMap<>();
                        punto.put("fecha", e.getKey());
                        if (e.getValue() instanceof Map) {
                            Map<String, Object> vals = (Map<String, Object>) e.getValue();
                            punto.put("apertura", vals.get("1. open"));
                            punto.put("maximo", vals.get("2. high"));
                            punto.put("minimo", vals.get("3. low"));
                            punto.put("cierre", vals.get("4. close"));
                            punto.put("volumen", vals.get("5. volume"));
                        }
                        historico.add(punto);
                    });
            dto.setHistoricoPrecios(historico);
        }
    }

    private void copiarCacheADetalle(DetalleAccionDTO dto, PrecioCache cached) {
        dto.setPrecioActual(cached.getPrecioActual());
        dto.setPrecioApertura(cached.getPrecioApertura());
        dto.setPrecioMaximo(cached.getPrecioMaximo());
        dto.setPrecioMinimo(cached.getPrecioMinimo());
        dto.setPrecioCierreAnterior(cached.getPrecioCierreAnterior());
        dto.setVariacionPorcentual(cached.getVariacionPorcentual());
        dto.setVolumen(cached.getVolumen());
    }

    private void copiarOHLCADetalle(DetalleAccionDTO dto, Map<String, Object> dailyBar, Map<String, Object> prevDailyBar) {
        if (dailyBar != null) {
            if (dailyBar.get("o") != null) dto.setPrecioApertura(new BigDecimal(dailyBar.get("o").toString()));
            if (dailyBar.get("h") != null) dto.setPrecioMaximo(new BigDecimal(dailyBar.get("h").toString()));
            if (dailyBar.get("l") != null) dto.setPrecioMinimo(new BigDecimal(dailyBar.get("l").toString()));
            if (dailyBar.get("v") != null) dto.setVolumen(Long.parseLong(dailyBar.get("v").toString()));
        }
        if (prevDailyBar != null && prevDailyBar.get("c") != null) {
            BigDecimal cierre = new BigDecimal(prevDailyBar.get("c").toString());
            dto.setPrecioCierreAnterior(cierre);
            if (dto.getPrecioActual() != null && cierre.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal var = dto.getPrecioActual().subtract(cierre)
                        .divide(cierre, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
                dto.setVariacionPorcentual(var);
            }
        }
    }

    private void copiarOverview(DetalleAccionDTO dto, Map<String, Object> overview) {
        if (overview.isEmpty()) return;
        dto.setNombreEmpresa(str(overview, "Name"));
        dto.setSector(str(overview, "Sector"));
        dto.setIndustria(str(overview, "Industry"));
        dto.setDescripcion(str(overview, "Description"));
        if (overview.get("MarketCapitalization") != null && !"None".equals(overview.get("MarketCapitalization"))) {
            try {
                dto.setCapitalizacionMercado(new BigDecimal(str(overview, "MarketCapitalization")));
            } catch (Exception ignored) {}
        }
    }

    private Map<String, Object> overviewConCache(String simAV) {
        CachedResponse cached = overviewCache.get(simAV);
        if (cached != null && cached.cachedAt().isAfter(LocalDateTime.now().minusHours(24))) {
            return cached.data();
        }
        Map<String, Object> data = alphaVantage.obtenerResumenEmpresa(simAV);
        if (!data.isEmpty()) overviewCache.put(simAV, new CachedResponse(data, LocalDateTime.now()));
        return data;
    }

    private Map<String, Object> serieConCache(String simAV) {
        CachedResponse cached = serieCache.get(simAV);
        if (cached != null && cached.cachedAt().isAfter(LocalDateTime.now().minusHours(8))) {
            return cached.data();
        }
        Map<String, Object> data = alphaVantage.obtenerSerieTemporalDiaria(simAV);
        if (!data.isEmpty()) serieCache.put(simAV, new CachedResponse(data, LocalDateTime.now()));
        return data;
    }

    private CotizacionDTO mapearCache(PrecioCache c) {
        CotizacionDTO dto = new CotizacionDTO();
        dto.setSimbolo(c.getSimbolo());
        dto.setNombreEmpresa(c.getNombreEmpresa());
        dto.setPrecioActual(c.getPrecioActual());
        dto.setPrecioApertura(c.getPrecioApertura());
        dto.setPrecioCierreAnterior(c.getPrecioCierreAnterior());
        dto.setPrecioMaximo(c.getPrecioMaximo());
        dto.setPrecioMinimo(c.getPrecioMinimo());
        dto.setVariacionPorcentual(c.getVariacionPorcentual());
        dto.setVolumen(c.getVolumen());
        dto.setMercado(c.getMercado());
        dto.setMercadoAbierto(esMercadoAbierto(c.getMercado()));
        dto.setActualizadoEn(c.getActualizadoEn());
        return dto;
    }

    public Map<String, List<String>> obtenerSimbolosDisponibles() {
        return SIMBOLOS_DISPONIBLES;
    }

    private void validarFormato(String sim, String original) {
        if (sim == null || !PATRON_SIMBOLO.matcher(sim).matches()) {
            throw new SimboloInvalidoException("El ticker '" + original + "' no tiene un formato valido.");
        }
    }

    private boolean cotizacionTienePrecio(CotizacionDTO cotizacion) {
        return cotizacion != null
                && cotizacion.getPrecioActual() != null
                && cotizacion.getPrecioActual().compareTo(BigDecimal.ZERO) > 0;
    }

    private String normalizarSimbolo(String simbolo) {
        if (simbolo == null) return null;
        String s = simbolo.toUpperCase().trim();
        if (s.endsWith(".LON")) return s.substring(0, s.length() - 4) + ".L";
        if (s.endsWith(".AUS")) return s.substring(0, s.length() - 4) + ".AX";
        if (s.endsWith(".TYO") || s.endsWith(".TSE")) return s.substring(0, s.length() - 4) + ".T";
        return s;
    }

    private String simboloParaAlphaVantage(String simbolo) {
        if (simbolo == null) return simbolo;
        if (simbolo.endsWith(".L")) return simbolo.substring(0, simbolo.length() - 2) + ".LON";
        if (simbolo.endsWith(".T")) return simbolo.substring(0, simbolo.length() - 2) + ".TYO";
        return simbolo;
    }

    private List<String> parsearIntereses(String interesesMercado) {
        if (interesesMercado == null || interesesMercado.isBlank()) return Collections.emptyList();
        return Arrays.stream(interesesMercado.split("[,;\\s]+"))
                .map(s -> normalizarSimbolo(s.trim()))
                .filter(s -> s != null && !s.isEmpty() && PATRON_SIMBOLO.matcher(s).matches())
                .toList();
    }

    private boolean esSimboloUs(String simbolo) {
        return simbolo != null && !simbolo.contains(".");
    }

    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString() : null;
    }

    private boolean esMercadoUsAbierto() {
        java.time.ZonedDateTime ahora = java.time.ZonedDateTime.now(
                java.time.ZoneId.of("America/New_York"));
        int diaSemana = ahora.getDayOfWeek().getValue();
        if (diaSemana >= 6) return false;
        int totalMinutos = ahora.getHour() * 60 + ahora.getMinute();
        return totalMinutos >= 9 * 60 + 30 && totalMinutos < 16 * 60;
    }

    private boolean esMercadoTokioAbierto() {
        java.time.ZonedDateTime ahora = java.time.ZonedDateTime.now(
                java.time.ZoneId.of("Asia/Tokyo"));
        int diaSemana = ahora.getDayOfWeek().getValue();
        if (diaSemana >= 6) return false;
        int totalMinutos = ahora.getHour() * 60 + ahora.getMinute();
        return totalMinutos >= 9 * 60 && totalMinutos < 15 * 60;
    }

    private boolean esMercadoLondresAbierto() {
        java.time.ZonedDateTime ahora = java.time.ZonedDateTime.now(
                java.time.ZoneId.of("Europe/London"));
        int diaSemana = ahora.getDayOfWeek().getValue();
        if (diaSemana >= 6) return false;
        int totalMinutos = ahora.getHour() * 60 + ahora.getMinute();
        return totalMinutos >= 8 * 60 && totalMinutos < 16 * 60 + 30;
    }

    private boolean esMercadoSidneyAbierto() {
        java.time.ZonedDateTime ahora = java.time.ZonedDateTime.now(
                java.time.ZoneId.of("Australia/Sydney"));
        int diaSemana = ahora.getDayOfWeek().getValue();
        if (diaSemana >= 6) return false;
        int totalMinutos = ahora.getHour() * 60 + ahora.getMinute();
        return totalMinutos >= 10 * 60 && totalMinutos < 16 * 60;
    }
}
