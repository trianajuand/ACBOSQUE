package co.edu.unbosque.accioneselbosque.ordenes.service;

import co.edu.unbosque.accioneselbosque.autenticacion.model.Usuario;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.UsuarioRepository;
import co.edu.unbosque.accioneselbosque.integracion.adaptadores.alpaca.IIntegracionAlpaca;
import co.edu.unbosque.accioneselbosque.mercado.dto.CotizacionDTO;
import co.edu.unbosque.accioneselbosque.mercado.service.MercadoService;
import co.edu.unbosque.accioneselbosque.ordenes.dto.*;
import co.edu.unbosque.accioneselbosque.ordenes.interfaces.IOrden;
import co.edu.unbosque.accioneselbosque.ordenes.model.*;
import co.edu.unbosque.accioneselbosque.ordenes.repository.ComisionRepository;
import co.edu.unbosque.accioneselbosque.ordenes.repository.OrdenRepository;
import co.edu.unbosque.accioneselbosque.shared.exceptions.OrdenNoEncontradaException;
import co.edu.unbosque.accioneselbosque.shared.exceptions.SimboloInvalidoException;
import co.edu.unbosque.accioneselbosque.trazabilidad.interfaces.IAuditLog;
import co.edu.unbosque.accioneselbosque.trazabilidad.model.TipoEvento;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class OrdenService implements IOrden {

    private final OrdenRepository ordenRepo;
    private final ComisionRepository comisionRepo;
    private final UsuarioRepository usuarioRepo;
    private final IIntegracionAlpaca alpaca;
    private final MercadoService mercadoService;
    private final SaldoService saldoService;
    private final PortafolioService portafolioService;
    private final IAuditLog auditLog;

    @Value("${app.comision.porcentaje:2.0}")
    private BigDecimal porcentajeComision;

    @Value("${app.comision.split-plataforma:60.0}")
    private BigDecimal splitPlataforma;

    @Value("${app.comision.split-comisionista:40.0}")
    private BigDecimal splitComisionista;

    public OrdenService(OrdenRepository ordenRepo, ComisionRepository comisionRepo,
                        UsuarioRepository usuarioRepo, IIntegracionAlpaca alpaca,
                        MercadoService mercadoService, SaldoService saldoService,
                        PortafolioService portafolioService, IAuditLog auditLog) {
        this.ordenRepo = ordenRepo;
        this.comisionRepo = comisionRepo;
        this.usuarioRepo = usuarioRepo;
        this.alpaca = alpaca;
        this.mercadoService = mercadoService;
        this.saldoService = saldoService;
        this.portafolioService = portafolioService;
        this.auditLog = auditLog;
    }

    // =========================================================
    // Previsualización: calcula comisión ANTES de confirmar (EC-13)
    // =========================================================

    @Override
    @Transactional
    public ResumenComisionDTO previsualizarOrden(Long usuarioId, CrearOrdenRequestDTO req) {
        String simbolo = req.getSimbolo().toUpperCase();
        CotizacionDTO cotizacion = mercadoService.validarSimboloOperable(simbolo);
        String mercado = cotizacion.getMercado();
        boolean abierto = cotizacion.isMercadoAbierto();
        BigDecimal precioEstimado = cotizacion.getPrecioActual();

        BigDecimal montoBase = precioEstimado.multiply(req.getCantidad()).setScale(4, RoundingMode.HALF_UP);
        BigDecimal montoComision = montoBase
                .multiply(porcentajeComision)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        Usuario usuario = usuarioRepo.findById(usuarioId).orElseThrow();
        boolean tieneComisionista = false; // se expande en Sprint 4 (flujo comisionista)

        BigDecimal montoPlatf = montoComision
                .multiply(splitPlataforma)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal montoComisionistaParte = tieneComisionista
                ? montoComision.subtract(montoPlatf)
                : BigDecimal.ZERO;
        if (!tieneComisionista) montoPlatf = montoComision;

        ResumenComisionDTO res = new ResumenComisionDTO();
        res.setSimbolo(simbolo);
        res.setTipoOrden(req.getTipoOrden());
        res.setLado(req.getLado());
        res.setCantidad(req.getCantidad());
        res.setPrecioEstimado(precioEstimado);
        res.setMontoBase(montoBase);
        res.setPorcentajeComision(porcentajeComision);
        res.setMontoComision(montoComision);
        res.setMontoPlataforma(montoPlatf);
        res.setMontoComisionista(montoComisionistaParte);
        res.setMercadoAbierto(abierto);

        if ("COMPRA".equalsIgnoreCase(req.getLado())) {
            res.setTotalADebitar(montoBase.add(montoComision));
        } else {
            res.setTotalARecibir(montoBase.subtract(montoComision));
        }
        if (!abierto) {
            res.setAdvertencia("El mercado " + mercado + " está cerrado. La orden se encolará y se ejecutará en la próxima apertura.");
        }
        return res;
    }

    // =========================================================
    // Crear orden (HU-17 a HU-20)
    // =========================================================

    @Override
    @Transactional
    public OrdenDTO crearOrden(Long usuarioId, CrearOrdenRequestDTO req, String ipOrigen) {
        String simbolo = req.getSimbolo().toUpperCase();
        TipoOrden tipoOrden = TipoOrden.valueOf(req.getTipoOrden().toUpperCase());
        TipoLado lado = TipoLado.valueOf(req.getLado().toUpperCase());
        CotizacionDTO cotizacion = mercadoService.validarSimboloOperable(simbolo);
        String mercado = cotizacion.getMercado();
        boolean mercadoAbierto = cotizacion.isMercadoAbierto();
        BigDecimal precioRef = cotizacion.getPrecioActual();

        BigDecimal precioEfectivo = (tipoOrden == TipoOrden.LIMIT || tipoOrden == TipoOrden.TAKE_PROFIT)
                ? req.getPrecioLimite() : (tipoOrden == TipoOrden.STOP_LOSS ? req.getPrecioStop() : precioRef);
        if (precioEfectivo == null || precioEfectivo.compareTo(BigDecimal.ZERO) == 0) precioEfectivo = precioRef;

        BigDecimal montoBase = precioEfectivo.multiply(req.getCantidad()).setScale(4, RoundingMode.HALF_UP);
        BigDecimal montoComision = montoBase.multiply(porcentajeComision)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        // Validaciones previas
        if (lado == TipoLado.COMPRA) {
            saldoService.reservarFondos(usuarioId, montoBase.add(montoComision));
        } else {
            portafolioService.verificarHolding(usuarioId, simbolo, req.getCantidad());
        }

        // Crear entidad Orden
        Orden orden = new Orden();
        orden.setUsuarioId(usuarioId);
        orden.setSimbolo(simbolo);
        orden.setTipoOrden(tipoOrden);
        orden.setLado(lado);
        orden.setCantidad(req.getCantidad());
        orden.setPrecioLimite(req.getPrecioLimite());
        orden.setPrecioStop(req.getPrecioStop());
        orden.setMontoTotal(montoBase);
        orden.setComision(montoComision);
        orden.setMontoNeto(lado == TipoLado.COMPRA
                ? montoBase.add(montoComision)
                : montoBase.subtract(montoComision));
        orden.setIpOrigen(ipOrigen);
        orden.setCreadaEn(LocalDateTime.now());

        if (!mercadoAbierto) {
            // HU-23: encolar para apertura
            orden.setEstado(EstadoOrden.EN_COLA);
            orden = ordenRepo.save(orden);
            auditLog.registrar(TipoEvento.ORDEN_ENCOLADA, usuarioId.toString(),
                    "Orden encolada: " + simbolo + " " + lado + " " + req.getCantidad());
            return mapearOrden(orden);
        }

        orden.setEstado(EstadoOrden.PENDIENTE);
        orden = ordenRepo.save(orden);

        Usuario usuario = usuarioRepo.findById(usuarioId).orElseThrow();

        // Si el usuario no tiene cuenta Alpaca, intentar crearla ahora
        if (usuario.getAlpacaAccountId() == null && esSimboloUs(simbolo)) {
            String nuevoId = alpaca.crearCuenta(usuario);
            if (nuevoId != null) {
                usuario.setAlpacaAccountId(nuevoId);
                usuarioRepo.save(usuario);
                auditLog.registrar(TipoEvento.ORDEN_ENVIADA_ALPACA, usuarioId.toString(), "Cuenta Alpaca creada on-demand: " + nuevoId);
            }
        }

        if (esSimboloUs(simbolo)) {
            // --- Mercado US: ejecutar vía Alpaca ---
            String alpacaOrderId = enviarAAlpaca(usuario.getAlpacaAccountId(), orden);
            if (alpacaOrderId != null) {
                orden.setAlpacaOrderId(alpacaOrderId);
                orden.setEstado(EstadoOrden.ENVIADA);
                auditLog.registrar(TipoEvento.ORDEN_ENVIADA_ALPACA, usuarioId.toString(),
                        "Orden enviada a Alpaca: " + alpacaOrderId + " | " + simbolo + " " + lado);
                confirmarSiAlpacaReportaFill(usuarioId, usuario.getAlpacaAccountId(), orden, precioRef);
            } else {
                auditLog.registrar(TipoEvento.ORDEN_FALLO_ALPACA, usuarioId.toString(),
                        "Fallo al enviar a Alpaca: " + simbolo + " " + lado);
                if (lado == TipoLado.COMPRA) {
                    saldoService.liberarFondosReservados(usuarioId, montoBase.add(montoComision));
                }
                orden.setEstado(EstadoOrden.PENDIENTE);
            }
        } else {
            // --- Mercado global (TSE, LSE, etc.): ejecucion interna con precio de Alpha Vantage ---
            // Alpaca no soporta estos simbolos; el precio real viene de Alpha Vantage (ya en precioRef).
            // Para LIMIT/STOP_LOSS/TAKE_PROFIT verificamos la condición de precio.
            boolean condicionCumplida = verificarCondicionPrecio(tipoOrden, lado, precioRef,
                    req.getPrecioLimite(), req.getPrecioStop());
            if (tipoOrden == TipoOrden.MARKET || condicionCumplida) {
                orden.setEstado(EstadoOrden.EJECUTADA);
                orden.setPrecioEjecucion(precioRef);
                orden.setEjecutadaEn(LocalDateTime.now());
                confirmarEjecucion(usuarioId, orden, precioRef);
                auditLog.registrar(TipoEvento.ORDEN_EJECUTADA, usuarioId.toString(),
                        "Orden global ejecutada internamente: " + simbolo + " precio=" + precioRef);
            } else {
                // Condición de precio no cumplida aún: queda ENVIADA esperando revisión
                orden.setEstado(EstadoOrden.ENVIADA);
                auditLog.registrar(TipoEvento.ORDEN_CREADA, usuarioId.toString(),
                        "Orden global pendiente condición de precio: " + simbolo);
            }
        }

        orden = ordenRepo.save(orden);
        auditLog.registrar(TipoEvento.ORDEN_CREADA, usuarioId.toString(),
                "Orden creada id=" + orden.getId() + " | " + simbolo + " " + tipoOrden + " " + lado);
        return mapearOrden(orden);
    }

    // =========================================================
    // Cancelar orden (HU-21)
    // =========================================================

    @Override
    @Transactional
    public boolean cancelarOrden(Long usuarioId, Long ordenId) {
        Orden orden = ordenRepo.findByIdAndUsuarioId(ordenId, usuarioId)
                .orElseThrow(() -> new OrdenNoEncontradaException("Orden no encontrada: " + ordenId));

        if (orden.getEstado() == EstadoOrden.EJECUTADA || orden.getEstado() == EstadoOrden.CANCELADA) {
            return false;
        }

        // Cancelar en Alpaca si tiene ID
        if (orden.getAlpacaOrderId() != null) {
            Usuario usuario = usuarioRepo.findById(usuarioId).orElseThrow();
            alpaca.cancelarOrden(usuario.getAlpacaAccountId(), orden.getAlpacaOrderId());
        }

        // Liberar fondos reservados para compras
        if (orden.getLado() == TipoLado.COMPRA
                && (orden.getEstado() == EstadoOrden.PENDIENTE
                || orden.getEstado() == EstadoOrden.ENVIADA
                || orden.getEstado() == EstadoOrden.EN_COLA)) {
            saldoService.liberarFondosReservados(usuarioId, orden.getMontoNeto());
        }

        orden.setEstado(EstadoOrden.CANCELADA);
        orden.setCanceladaEn(LocalDateTime.now());
        ordenRepo.save(orden);
        auditLog.registrar(TipoEvento.ORDEN_CANCELADA, usuarioId.toString(),
                "Orden cancelada id=" + ordenId);
        return true;
    }

    // =========================================================
    // Consultas (HU-22, HU-24 a HU-26)
    // =========================================================

    @Override
    @Transactional
    public List<OrdenDTO> obtenerOrdenesActivas(Long usuarioId) {
        sincronizarOrdenesEnviadasConAlpaca(usuarioId);
        List<OrdenDTO> resultado = new ArrayList<>();
        for (EstadoOrden estado : List.of(EstadoOrden.PENDIENTE, EstadoOrden.ENVIADA,
                EstadoOrden.EN_COLA, EstadoOrden.PENDIENTE_APROBACION)) {
            ordenRepo.findByUsuarioIdAndEstadoOrderByCreadaEnDesc(usuarioId, estado)
                    .forEach(o -> resultado.add(mapearOrden(o)));
        }
        return resultado;
    }

    @Override
    @Transactional
    public List<OrdenDTO> obtenerHistorialOrdenes(Long usuarioId) {
        sincronizarOrdenesEnviadasConAlpaca(usuarioId);
        List<OrdenDTO> resultado = new ArrayList<>();
        ordenRepo.findByUsuarioIdOrderByCreadaEnDesc(usuarioId)
                .forEach(o -> resultado.add(mapearOrden(o)));
        return resultado;
    }

    @Override
    @Transactional
    public PortafolioDTO obtenerPortafolio(Long usuarioId) {
        sincronizarOrdenesEnviadasConAlpaca(usuarioId);
        Usuario usuario = usuarioRepo.findById(usuarioId).orElseThrow();
        return portafolioService.obtenerPortafolio(usuarioId, usuario.getVistaPortafolio());
    }

    @Override
    @Transactional
    public SaldoDTO obtenerSaldo(Long usuarioId) {
        sincronizarOrdenesEnviadasConAlpaca(usuarioId);
        return saldoService.obtenerSaldoDTO(usuarioId);
    }

    // =========================================================
    // Helpers privados
    // =========================================================

    private String enviarAAlpaca(String alpacaAccountId, Orden orden) {
        if (alpacaAccountId == null) return null;
        String tipoAlpaca = toAlpacaTipo(orden.getTipoOrden());
        String ladoAlpaca = orden.getLado() == TipoLado.COMPRA ? "buy" : "sell";
        String precioLimite = orden.getPrecioLimite() != null ? orden.getPrecioLimite().toPlainString() : null;
        String precioStop = orden.getPrecioStop() != null ? orden.getPrecioStop().toPlainString() : null;

        return alpaca.crearOrden(alpacaAccountId, orden.getSimbolo(), tipoAlpaca,
                ladoAlpaca, orden.getCantidad().toPlainString(), precioLimite, precioStop);
    }

    private void confirmarSiAlpacaReportaFill(Long usuarioId, String alpacaAccountId, Orden orden, BigDecimal precioRef) {
        if (alpacaAccountId == null || orden.getAlpacaOrderId() == null) {
            return;
        }
        Map<String, Object> ordenAlpaca = alpaca.obtenerOrden(alpacaAccountId, orden.getAlpacaOrderId());
        String estadoAlpaca = ordenAlpaca.get("status") != null ? ordenAlpaca.get("status").toString() : "";
        if (!"filled".equalsIgnoreCase(estadoAlpaca)) {
            auditLog.registrar(TipoEvento.ORDEN_ENVIADA_ALPACA, usuarioId.toString(),
                    "Orden pendiente en Alpaca id=" + orden.getAlpacaOrderId() + " estado=" + estadoAlpaca);
            return;
        }

        BigDecimal precioEjecucion = obtenerBigDecimal(ordenAlpaca.get("filled_avg_price"), precioRef);
        orden.setEstado(EstadoOrden.EJECUTADA);
        orden.setPrecioEjecucion(precioEjecucion);
        orden.setEjecutadaEn(LocalDateTime.now());
        confirmarEjecucion(usuarioId, orden, precioEjecucion);
    }

    private void sincronizarOrdenesEnviadasConAlpaca(Long usuarioId) {
        Usuario usuario = usuarioRepo.findById(usuarioId).orElseThrow();
        if (usuario.getAlpacaAccountId() == null) {
            return;
        }

        List<Orden> enviadas = ordenRepo.findByUsuarioIdAndEstadoOrderByCreadaEnDesc(usuarioId, EstadoOrden.ENVIADA);
        for (Orden orden : enviadas) {
            if (orden.getAlpacaOrderId() == null || !esSimboloUs(orden.getSimbolo())) {
                continue;
            }

            BigDecimal precioRef = precioReferenciaOrden(orden);
            confirmarSiAlpacaReportaFill(usuarioId, usuario.getAlpacaAccountId(), orden, precioRef);
            if (orden.getEstado() == EstadoOrden.EJECUTADA) {
                ordenRepo.save(orden);
            }
        }
    }

    private BigDecimal precioReferenciaOrden(Orden orden) {
        if (orden.getPrecioEjecucion() != null && orden.getPrecioEjecucion().compareTo(BigDecimal.ZERO) > 0) {
            return orden.getPrecioEjecucion();
        }
        if (orden.getMontoTotal() != null && orden.getCantidad() != null
                && orden.getCantidad().compareTo(BigDecimal.ZERO) > 0) {
            return orden.getMontoTotal().divide(orden.getCantidad(), 4, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal obtenerBigDecimal(Object valor, BigDecimal valorPorDefecto) {
        if (valor == null) {
            return valorPorDefecto;
        }
        try {
            return new BigDecimal(valor.toString());
        } catch (NumberFormatException e) {
            return valorPorDefecto;
        }
    }

    private void confirmarEjecucion(Long usuarioId, Orden orden, BigDecimal precioEjecucion) {
        BigDecimal montoReal = precioEjecucion.multiply(orden.getCantidad()).setScale(4, RoundingMode.HALF_UP);
        BigDecimal comisionReal = montoReal.multiply(porcentajeComision)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        if (orden.getLado() == TipoLado.COMPRA) {
            saldoService.confirmarCompra(usuarioId, montoReal.add(comisionReal));
            portafolioService.registrarCompra(usuarioId, orden.getSimbolo(),
                    orden.getCantidad(), precioEjecucion);
        } else {
            BigDecimal neto = montoReal.subtract(comisionReal);
            saldoService.confirmarVenta(usuarioId, neto);
            portafolioService.registrarVenta(usuarioId, orden.getSimbolo(), orden.getCantidad());
        }

        // Registrar comisión
        BigDecimal montoPlatf = comisionReal.multiply(splitPlataforma)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        Comision comisionEnt = new Comision();
        comisionEnt.setOrdenId(orden.getId());
        comisionEnt.setUsuarioId(usuarioId);
        comisionEnt.setMontoBase(montoReal);
        comisionEnt.setPorcentajeComision(porcentajeComision);
        comisionEnt.setMontoComision(comisionReal);
        comisionEnt.setMontoPlataforma(montoPlatf);
        comisionEnt.setMontoComisionista(comisionReal.subtract(montoPlatf));
        comisionEnt.setCreadaEn(LocalDateTime.now());
        comisionRepo.save(comisionEnt);

        auditLog.registrar(TipoEvento.ORDEN_EJECUTADA, usuarioId.toString(),
                "Orden ejecutada id=" + orden.getId() + " precio=" + precioEjecucion);
    }

    private boolean esSimboloUs(String simbolo) {
        return !simbolo.contains(".");
    }

    /**
     * Para mercados globales: verifica si la condición de precio ya se cumple
     * al momento de crear la orden (precio actual vs límite/stop).
     */
    private boolean verificarCondicionPrecio(TipoOrden tipo, TipoLado lado,
                                              BigDecimal precioActual,
                                              BigDecimal precioLimite, BigDecimal precioStop) {
        if (precioActual == null || precioActual.compareTo(BigDecimal.ZERO) == 0) return false;
        return switch (tipo) {
            case LIMIT ->
                // Compra límite: ejecutar si precio actual ≤ límite
                // Venta límite (take profit): ejecutar si precio actual ≥ límite
                    lado == TipoLado.COMPRA
                            ? precioActual.compareTo(precioLimite) <= 0
                            : precioActual.compareTo(precioLimite) >= 0;
            case STOP_LOSS ->
                // Stop loss venta: ejecutar si precio actual ≤ stop
                    precioActual.compareTo(precioStop) <= 0;
            case TAKE_PROFIT ->
                // Take profit venta: ejecutar si precio actual ≥ límite
                    precioActual.compareTo(precioLimite) >= 0;
            default -> false;
        };
    }

    private String toAlpacaTipo(TipoOrden tipo) {
        return switch (tipo) {
            case MARKET -> "market";
            case LIMIT -> "limit";
            case STOP_LOSS -> "stop";
            case TAKE_PROFIT -> "limit";   // Take Profit = orden límite del lado vendedor
        };
    }

    private OrdenDTO mapearOrden(Orden o) {
        OrdenDTO dto = new OrdenDTO();
        dto.setId(o.getId());
        dto.setSimbolo(o.getSimbolo());
        dto.setTipoOrden(o.getTipoOrden());
        dto.setLado(o.getLado());
        dto.setEstado(o.getEstado());
        dto.setCantidad(o.getCantidad());
        dto.setPrecioLimite(o.getPrecioLimite());
        dto.setPrecioStop(o.getPrecioStop());
        dto.setPrecioEjecucion(o.getPrecioEjecucion());
        dto.setMontoTotal(o.getMontoTotal());
        dto.setComision(o.getComision());
        dto.setMontoNeto(o.getMontoNeto());
        dto.setAlpacaOrderId(o.getAlpacaOrderId());
        dto.setCreadaEn(o.getCreadaEn());
        dto.setEjecutadaEn(o.getEjecutadaEn());
        return dto;
    }
}
