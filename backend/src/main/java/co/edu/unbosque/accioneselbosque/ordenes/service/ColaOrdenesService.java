package co.edu.unbosque.accioneselbosque.ordenes.service;

import co.edu.unbosque.accioneselbosque.autenticacion.model.EstadoCuenta;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Usuario;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.UsuarioRepository;
import co.edu.unbosque.accioneselbosque.autenticacion.interfaces.IConsultaInversionista;
import co.edu.unbosque.accioneselbosque.integracion.adaptadores.alpaca.IIntegracionAlpaca;
import co.edu.unbosque.accioneselbosque.integracion.notificaciones.ContextoNotificacion;
import co.edu.unbosque.accioneselbosque.integracion.notificaciones.INotificacion;
import co.edu.unbosque.accioneselbosque.mercado.model.Activo;
import co.edu.unbosque.accioneselbosque.mercado.repository.ActivoRepository;
import co.edu.unbosque.accioneselbosque.ordenes.model.EstadoOrden;
import co.edu.unbosque.accioneselbosque.ordenes.model.Orden;
import co.edu.unbosque.accioneselbosque.ordenes.model.TipoLado;
import co.edu.unbosque.accioneselbosque.ordenes.model.TipoOrden;
import co.edu.unbosque.accioneselbosque.ordenes.repository.OrdenRepository;
import co.edu.unbosque.accioneselbosque.trazabilidad.interfaces.IAuditLog;
import co.edu.unbosque.accioneselbosque.trazabilidad.model.TipoEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Procesa las órdenes EN_COLA al abrir el mercado (HU-23, EC-04).
 */
@Service
public class ColaOrdenesService {

    private static final Logger log = LoggerFactory.getLogger(ColaOrdenesService.class);

    private final OrdenRepository ordenRepo;
    private final UsuarioRepository usuarioRepo;
    private final ActivoRepository activoRepo;
    private final IIntegracionAlpaca alpaca;
    private final SaldoService saldoService;
    private final PortafolioService portafolioService;
    private final IAuditLog auditLog;
    private final IConsultaInversionista consultaInversionista;
    private final INotificacion notificacion;

    @Value("${app.comision.porcentaje:2.0}")
    private BigDecimal porcentajeComision;

    public ColaOrdenesService(OrdenRepository ordenRepo, UsuarioRepository usuarioRepo,
                               ActivoRepository activoRepo,
                               IIntegracionAlpaca alpaca, SaldoService saldoService,
                               PortafolioService portafolioService, IAuditLog auditLog,
                               IConsultaInversionista consultaInversionista,
                               INotificacion notificacion) {
        this.ordenRepo = ordenRepo;
        this.usuarioRepo = usuarioRepo;
        this.activoRepo = activoRepo;
        this.alpaca = alpaca;
        this.saldoService = saldoService;
        this.portafolioService = portafolioService;
        this.auditLog = auditLog;
        this.consultaInversionista = consultaInversionista;
        this.notificacion = notificacion;
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void procesarColaAlAbrirMercado() {
        if (!esMercadoUsAbierto()) return;

        List<Orden> cola = ordenRepo.findByEstadoOrderByCreadaEnAsc(EstadoOrden.EN_COLA);
        if (cola.isEmpty()) return;

        log.info("Procesando {} órdenes encoladas al abrir mercado US", cola.size());

        cola.stream()
                .map(Orden::getInversionistaId)
                .distinct()
                .forEach(uid -> {
                    try {
                        ContextoNotificacion ctx = ContextoNotificacion.desde(
                                consultaInversionista.obtenerPreferenciasNotificacion(uid));
                        notificacion.notificarAperturaMercado(ctx, "NYSE/NASDAQ");
                    } catch (Exception ignored) {}
                });

        for (Orden orden : cola) {
            try {
                procesarOrdenEncolada(orden);
            } catch (Exception e) {
                log.error("Error procesando orden encolada id={}: {}", orden.getId(), e.getMessage());
            }
        }
    }

    private void procesarOrdenEncolada(Orden orden) {
        Usuario usuario = usuarioRepo.findById(orden.getInversionistaId()).orElse(null);
        if (usuario == null || usuario.getEstadoCuenta() != EstadoCuenta.ACTIVA) {
            orden.setEstado(EstadoOrden.CANCELADA);
            orden.setCanceladaEn(LocalDateTime.now());
            ordenRepo.save(orden);
            if (orden.getLado() == TipoLado.COMPRA) {
                saldoService.liberarFondosReservados(orden.getInversionistaId(), orden.getMontoNeto());
            }
            return;
        }

        if (orden.getLado() == TipoLado.COMPRA) {
            try {
                saldoService.obtenerOCrear(orden.getInversionistaId());
            } catch (Exception e) {
                orden.setEstado(EstadoOrden.CANCELADA);
                orden.setCanceladaEn(LocalDateTime.now());
                ordenRepo.save(orden);
                auditLog.registrar(TipoEvento.ORDEN_RECHAZADA_FONDOS, orden.getInversionistaId().toString(),
                        "Orden cancelada por fondos insuficientes al abrir mercado id=" + orden.getId());
                return;
            }
        }

        Activo activo = activoRepo.findById(orden.getActivoId()).orElse(null);
        String ticker = activo != null ? activo.getTicker() : "";

        if (esSimboloUs(ticker)) {
            if (orden.getAlpacaOrderId() != null) {
                orden.setEstado(EstadoOrden.ENVIADA);
                ordenRepo.save(orden);
                auditLog.registrar(TipoEvento.ORDEN_ENVIADA_ALPACA, orden.getInversionistaId().toString(),
                        "Orden encolada promovida a ENVIADA (ya en Alpaca): " + orden.getAlpacaOrderId());
                return;
            }

            String alpacaAccountId = consultaInversionista.obtenerAlpacaAccountId(usuario.getId());
            if (alpacaAccountId == null) {
                throw new IllegalStateException("Cuenta Alpaca no encontrada para usuario " + usuario.getId());
            }
            String tipoAlpaca = toAlpacaTipo(orden.getTipoOrden());
            String ladoAlpaca = orden.getLado() == TipoLado.COMPRA ? "buy" : "sell";
            String precioLimite = orden.getPrecioLimite() != null ? orden.getPrecioLimite().toPlainString() : null;
            String precioStop = orden.getPrecioStop() != null ? orden.getPrecioStop().toPlainString() : null;

            String alpacaOrderId = alpaca.crearOrden(
                    alpacaAccountId, ticker, tipoAlpaca,
                    ladoAlpaca, orden.getCantidad().toPlainString(), precioLimite, precioStop);

            if (alpacaOrderId != null) {
                orden.setAlpacaOrderId(alpacaOrderId);
                orden.setEstado(EstadoOrden.ENVIADA);
                ordenRepo.save(orden);
                auditLog.registrar(TipoEvento.ORDEN_ENVIADA_ALPACA, orden.getInversionistaId().toString(),
                        "Orden encolada US enviada a Alpaca: " + alpacaOrderId);
            } else {
                log.warn("Fallo al enviar orden encolada id={} a Alpaca", orden.getId());
                auditLog.registrar(TipoEvento.ORDEN_FALLO_ALPACA, orden.getInversionistaId().toString(),
                        "Fallo al enviar orden encolada id=" + orden.getId());
            }
        } else {
            BigDecimal precioEjec = orden.getMontoTotal()
                    .divide(orden.getCantidad(), 4, java.math.RoundingMode.HALF_UP);
            orden.setEstado(EstadoOrden.EJECUTADA);
            orden.setPrecioEjecucion(precioEjec);
            orden.setEjecutadaEn(java.time.LocalDateTime.now());
            ordenRepo.save(orden);
            auditLog.registrar(TipoEvento.ORDEN_EJECUTADA, orden.getInversionistaId().toString(),
                    "Orden global encolada ejecutada internamente id=" + orden.getId());
            try {
                ContextoNotificacion ctxEjec = ContextoNotificacion.desde(
                        consultaInversionista.obtenerPreferenciasNotificacion(orden.getInversionistaId()));
                notificacion.notificarOrdenEjecutada(ctxEjec, ticker,
                        orden.getTipoOrden().name(), orden.getLado().name(),
                        precioEjec, orden.getCantidad(), orden.getComision());
            } catch (Exception ignored) {}
        }
    }

    private String toAlpacaTipo(TipoOrden tipo) {
        return switch (tipo) {
            case MARKET -> "market";
            case LIMIT -> "limit";
            case STOP_LOSS -> "stop";
            case TAKE_PROFIT -> "limit";
        };
    }

    private boolean esSimboloUs(String simbolo) {
        return !simbolo.contains(".");
    }

    private boolean esMercadoUsAbierto() {
        java.time.ZonedDateTime ahora = java.time.ZonedDateTime.now(
                java.time.ZoneId.of("America/New_York"));
        int dia = ahora.getDayOfWeek().getValue();
        if (dia >= 6) return false;
        int minutos = ahora.getHour() * 60 + ahora.getMinute();
        return minutos >= 9 * 60 + 30 && minutos < 16 * 60;
    }
}
