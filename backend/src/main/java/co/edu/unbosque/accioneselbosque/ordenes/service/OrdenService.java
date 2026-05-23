package co.edu.unbosque.accioneselbosque.ordenes.service;

import co.edu.unbosque.accioneselbosque.administracion.interfaces.IGestorParametros;
import co.edu.unbosque.accioneselbosque.autenticacion.interfaces.IAsignacionComisionista;
import co.edu.unbosque.accioneselbosque.autenticacion.interfaces.IConsultaInversionista;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Inversionista;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Usuario;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.InversionistaRepository;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.UsuarioRepository;
import co.edu.unbosque.accioneselbosque.integracion.adaptadores.alpaca.IIntegracionAlpaca;
import co.edu.unbosque.accioneselbosque.mercado.dto.CotizacionDTO;
import co.edu.unbosque.accioneselbosque.mercado.interfaces.IVerificacionMercado;
import co.edu.unbosque.accioneselbosque.ordenes.dto.*;
import co.edu.unbosque.accioneselbosque.ordenes.interfaces.IOrden;
import co.edu.unbosque.accioneselbosque.ordenes.model.*;
import co.edu.unbosque.accioneselbosque.ordenes.repository.ComisionRepository;
import co.edu.unbosque.accioneselbosque.ordenes.repository.OrdenRepository;
import co.edu.unbosque.accioneselbosque.shared.exceptions.OrdenNoEncontradaException;
import co.edu.unbosque.accioneselbosque.shared.exceptions.SimboloInvalidoException;
import co.edu.unbosque.accioneselbosque.trazabilidad.interfaces.IAuditLog;
import co.edu.unbosque.accioneselbosque.trazabilidad.model.TipoEvento;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrdenService implements IOrden {

    private final OrdenRepository ordenRepo;
    private final ComisionRepository comisionRepo;
    // Estos dos repos permanecen por el flujo on-demand de cuenta Alpaca
    // que requiere pasar Usuario e Inversionista a IIntegracionAlpaca.crearCuenta
    private final UsuarioRepository usuarioRepo;
    private final InversionistaRepository inversionistaRepo;
    private final IIntegracionAlpaca alpaca;
    private final IVerificacionMercado mercadoService;
    private final SaldoService saldoService;
    private final PortafolioService portafolioService;
    private final IAuditLog auditLog;
    private final IAsignacionComisionista asignacionComisionista;
    private final IGestorParametros administracion;
    private final IConsultaInversionista consultaInversionista;

    public OrdenService(OrdenRepository ordenRepo, ComisionRepository comisionRepo,
                        UsuarioRepository usuarioRepo, InversionistaRepository inversionistaRepo,
                        IIntegracionAlpaca alpaca,
                        IVerificacionMercado mercadoService, SaldoService saldoService,
                        PortafolioService portafolioService, IAuditLog auditLog,
                        IAsignacionComisionista asignacionComisionista,
                        IGestorParametros administracion,
                        IConsultaInversionista consultaInversionista) {
        this.ordenRepo = ordenRepo;
        this.comisionRepo = comisionRepo;
        this.usuarioRepo = usuarioRepo;
        this.inversionistaRepo = inversionistaRepo;
        this.alpaca = alpaca;
        this.mercadoService = mercadoService;
        this.saldoService = saldoService;
        this.portafolioService = portafolioService;
        this.auditLog = auditLog;
        this.asignacionComisionista = asignacionComisionista;
        this.administracion = administracion;
        this.consultaInversionista = consultaInversionista;
    }

    // =========================================================
    // Previsualización: calcula comisión ANTES de confirmar (EC-13)
    // =========================================================

    @Override
    @Transactional
    public ResumenComisionDTO previsualizarOrden(Long usuarioId, CrearOrdenRequestDTO req) {
        validarUsuarioPuedeOperar(usuarioId);
        String simbolo = req.getSimbolo().toUpperCase();
        TipoOrden tipoOrden = parseTipoOrden(req.getTipoOrden());
        TipoLado lado = parseTipoLado(req.getLado());
        validarParametrosOrden(tipoOrden, lado, req.getPrecioLimite(), req.getPrecioStop());
        CotizacionDTO cotizacion = mercadoService.validarSimboloOperable(simbolo);
        String mercado = cotizacion.getMercado();
        boolean abierto = cotizacion.isMercadoAbierto();
        BigDecimal precioEstimado = cotizacion.getPrecioActual();
        BigDecimal precioEfectivo = precioEfectivo(tipoOrden, req.getPrecioLimite(), req.getPrecioStop(), precioEstimado);

        BigDecimal montoBase = precioEfectivo.multiply(req.getCantidad()).setScale(4, RoundingMode.HALF_UP);
        BigDecimal montoComision = montoBase
                .multiply(porcentajeComision())
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        Usuario usuario = usuarioRepo.findById(usuarioId).orElseThrow();
        boolean tieneComisionista = asignacionComisionista.usuarioTieneComisionista(usuario.getId());

        BigDecimal montoPlatf = montoComision
                .multiply(splitPlataforma())
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal montoComisionistaParte = tieneComisionista
                ? montoComision.subtract(montoPlatf)
                : BigDecimal.ZERO;
        if (!tieneComisionista) montoPlatf = montoComision;

        ResumenComisionDTO res = new ResumenComisionDTO();
        res.setSimbolo(simbolo);
        res.setTipoOrden(tipoOrden.name());
        res.setLado(lado.name());
        res.setCantidad(req.getCantidad());
        res.setPrecioEstimado(precioEfectivo);
        res.setMontoBase(montoBase);
        res.setPorcentajeComision(porcentajeComision());
        res.setMontoComision(montoComision);
        res.setMontoPlataforma(montoPlatf);
        res.setMontoComisionista(montoComisionistaParte);
        res.setMercadoAbierto(abierto);

        if (lado == TipoLado.COMPRA) {
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
        validarUsuarioPuedeOperar(usuarioId);
        String simbolo = req.getSimbolo().toUpperCase();
        TipoOrden tipoOrden = parseTipoOrden(req.getTipoOrden());
        TipoLado lado = parseTipoLado(req.getLado());
        validarParametrosOrden(tipoOrden, lado, req.getPrecioLimite(), req.getPrecioStop());
        CotizacionDTO cotizacion = mercadoService.validarSimboloOperable(simbolo);
        String mercado = cotizacion.getMercado();
        boolean mercadoAbierto = cotizacion.isMercadoAbierto();
        BigDecimal precioRef = cotizacion.getPrecioActual();

        BigDecimal precioEfectivo = precioEfectivo(tipoOrden, req.getPrecioLimite(), req.getPrecioStop(), precioRef);

        BigDecimal montoBase = precioEfectivo.multiply(req.getCantidad()).setScale(4, RoundingMode.HALF_UP);
        BigDecimal montoComision = montoBase.multiply(porcentajeComision())
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
        orden.setComisionistaId(asignacionComisionista.obtenerComisionistaIdDeInversionista(usuarioId).orElse(null));
        orden.setCreadaEn(LocalDateTime.now());

        if (!mercadoAbierto) {
            // HU-23: encolar para apertura
            orden.setEstado(EstadoOrden.EN_COLA);
            orden = ordenRepo.save(orden);

            // Para símbolos US: enviar ya a Alpaca — registra "accepted" en el broker
            // aunque el mercado esté cerrado. El ColaOrdenesService lo sincroniza al abrir.
            if (esSimboloUs(simbolo)) {
                Usuario usuario = usuarioRepo.findById(usuarioId).orElseThrow();
                Inversionista inversionista = obtenerInversionista(usuarioId);
                if (consultaInversionista.necesitaCuentaAlpaca(usuarioId)) {
                    String nuevoId = alpaca.crearCuenta(usuario, inversionista);
                    if (nuevoId != null) {
                        consultaInversionista.actualizarAlpacaAccountId(usuarioId, nuevoId);
                    }
                }
                String alpacaAccountId = consultaInversionista.obtenerAlpacaAccountId(usuarioId);
                String alpacaOrderId = enviarAAlpaca(alpacaAccountId, orden);
                if (alpacaOrderId != null) {
                    orden.setAlpacaOrderId(alpacaOrderId);
                    orden = ordenRepo.save(orden);
                    auditLog.registrar(TipoEvento.ORDEN_ENVIADA_ALPACA, usuarioId.toString(),
                            "Orden encolada enviada a Alpaca (mercado cerrado): " + alpacaOrderId + " | " + simbolo);
                }
            }

            auditLog.registrar(TipoEvento.ORDEN_ENCOLADA, usuarioId.toString(),
                    "Orden encolada: " + simbolo + " " + lado + " " + req.getCantidad());
            return mapearOrden(orden);
        }

        orden.setEstado(EstadoOrden.PENDIENTE);
        orden = ordenRepo.save(orden);

        // Estos dos objetos solo se usan para alpaca.crearCuenta que requiere las entidades completas
        Usuario usuario = usuarioRepo.findById(usuarioId).orElseThrow();
        Inversionista inversionista = obtenerInversionista(usuarioId);

        if (consultaInversionista.necesitaCuentaAlpaca(usuarioId) && esSimboloUs(simbolo)) {
            String nuevoId = alpaca.crearCuenta(usuario, inversionista);
            if (nuevoId != null) {
                consultaInversionista.actualizarAlpacaAccountId(usuarioId, nuevoId);
                auditLog.registrar(TipoEvento.ORDEN_ENVIADA_ALPACA, usuarioId.toString(),
                        "Cuenta Alpaca creada on-demand: " + nuevoId);
            }
        }

        String alpacaAccountId = consultaInversionista.obtenerAlpacaAccountId(usuarioId);

        if (esSimboloUs(simbolo)) {
            // --- Mercado US: ejecutar vía Alpaca ---
            String alpacaOrderId = enviarAAlpaca(alpacaAccountId, orden);
            if (alpacaOrderId != null) {
                orden.setAlpacaOrderId(alpacaOrderId);
                orden.setEstado(EstadoOrden.ENVIADA);
                auditLog.registrar(TipoEvento.ORDEN_ENVIADA_ALPACA, usuarioId.toString(),
                        "Orden enviada a Alpaca: " + alpacaOrderId + " | " + simbolo + " " + lado);
                confirmarSiAlpacaReportaFill(usuarioId, alpacaAccountId, orden, precioRef);
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
            String alpacaAccountId = consultaInversionista.obtenerAlpacaAccountId(usuarioId);
            if (alpacaAccountId != null) {
                alpaca.cancelarOrden(alpacaAccountId, orden.getAlpacaOrderId());
            }
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
        return obtenerHistorialOrdenes(usuarioId, null, null, null, null, null);
    }

    @Override
    @Transactional
    public List<OrdenDTO> obtenerHistorialOrdenes(Long usuarioId, String desde, String hasta,
                                                  String tipoOrden, String simbolo, String estado) {
        sincronizarOrdenesEnviadasConAlpaca(usuarioId);
        LocalDateTime fechaDesde = parseFechaInicio(desde);
        LocalDateTime fechaHasta = parseFechaFin(hasta);
        TipoOrden filtroTipo = parseTipoOrdenOpcional(tipoOrden);
        EstadoOrden filtroEstado = parseEstadoOrdenOpcional(estado);
        String filtroSimbolo = normalizarFiltroSimbolo(simbolo);

        List<OrdenDTO> resultado = new ArrayList<>();
        ordenRepo.findByUsuarioIdOrderByCreadaEnDesc(usuarioId)
                .stream()
                .filter(o -> fechaDesde == null || !o.getCreadaEn().isBefore(fechaDesde))
                .filter(o -> fechaHasta == null || !o.getCreadaEn().isAfter(fechaHasta))
                .filter(o -> filtroTipo == null || o.getTipoOrden() == filtroTipo)
                .filter(o -> filtroEstado == null || o.getEstado() == filtroEstado)
                .filter(o -> filtroSimbolo == null || o.getSimbolo().toUpperCase(Locale.ROOT).contains(filtroSimbolo))
                .forEach(o -> resultado.add(mapearOrden(o)));
        return resultado;
    }

    @Override
    @Transactional
    public PortafolioDTO obtenerPortafolio(Long usuarioId) {
        sincronizarOrdenesEnviadasConAlpaca(usuarioId);
        return portafolioService.obtenerPortafolio(usuarioId,
                consultaInversionista.obtenerVistaPortafolio(usuarioId));
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenNegocioDTO obtenerResumenNegocio(LocalDateTime desde, LocalDateTime hasta,
                                                    String mercadoFiltro) {
        ResumenNegocioDTO resumen = new ResumenNegocioDTO();
        Map<String, ResumenMercadoDTO> porMercado = new TreeMap<>();

        for (Orden orden : ordenRepo.findAll()) {
            if (!enRango(orden.getCreadaEn(), desde, hasta)) continue;
            String mercado = mercadoService.detectarMercado(orden.getSimbolo());
            if (mercadoFiltro != null && !mercadoFiltro.isBlank()
                    && !mercado.equalsIgnoreCase(mercadoFiltro)) continue;

            resumen.setTransacciones(resumen.getTransacciones() + 1);
            resumen.setVolumenTransacciones(
                    resumen.getVolumenTransacciones().add(safe(orden.getMontoTotal())));

            ResumenMercadoDTO rm = porMercado.computeIfAbsent(mercado, k -> {
                ResumenMercadoDTO t = new ResumenMercadoDTO();
                t.setMercado(k);
                return t;
            });
            rm.setOperaciones(rm.getOperaciones() + 1);
            rm.setVolumen(rm.getVolumen().add(safe(orden.getMontoTotal())));
        }

        for (Comision comision : comisionRepo.findAll()) {
            if (!enRango(comision.getCreadaEn(), desde, hasta)) continue;
            resumen.setComisionesGeneradas(
                    resumen.getComisionesGeneradas().add(safe(comision.getMontoComision())));
            ordenRepo.findById(comision.getOrdenId()).ifPresent(orden -> {
                String mercado = mercadoService.detectarMercado(orden.getSimbolo());
                if (mercadoFiltro == null || mercadoFiltro.isBlank()
                        || mercado.equalsIgnoreCase(mercadoFiltro)) {
                    ResumenMercadoDTO rm = porMercado.computeIfAbsent(mercado, k -> {
                        ResumenMercadoDTO t = new ResumenMercadoDTO();
                        t.setMercado(k);
                        return t;
                    });
                    rm.setComisiones(rm.getComisiones().add(safe(comision.getMontoComision())));
                }
            });
        }

        resumen.setTendenciasPorMercado(new ArrayList<>(porMercado.values()));
        return resumen;
    }

    private boolean enRango(LocalDateTime fecha, LocalDateTime desde, LocalDateTime hasta) {
        if (fecha == null) return false;
        return !fecha.isBefore(desde) && !fecha.isAfter(hasta);
    }

    private BigDecimal safe(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    @Override
    @Transactional
    public SaldoDTO obtenerSaldo(Long usuarioId) {
        sincronizarOrdenesEnviadasConAlpaca(usuarioId);
        return saldoService.obtenerSaldoDTO(usuarioId);
    }

    // =========================================================
    // Flujo comisionista (HU-28 a HU-32)
    // =========================================================

    @Override
    @Transactional
    public OrdenDTO crearPropuestaOrden(Long comisionistaId, Long clienteId, CrearPropuestaOrdenDTO dto, String ipOrigen) {
        validarUsuarioPuedeOperar(clienteId);
        asignacionComisionista.validarClienteAsignado(comisionistaId, clienteId);

        TipoOrden tipoOrden = parseTipoOrden(dto.getTipoOrden());
        TipoLado lado = parseTipoLado(dto.getLado());
        validarParametrosOrden(tipoOrden, lado, dto.getPrecioLimite(), dto.getPrecioStop());
        String simbolo = dto.getSimbolo().toUpperCase();
        CotizacionDTO cotizacion = mercadoService.validarSimboloOperable(simbolo);
        BigDecimal precioRef = cotizacion.getPrecioActual();
        BigDecimal precioEfectivo = precioEfectivo(tipoOrden, dto.getPrecioLimite(), dto.getPrecioStop(), precioRef);
        BigDecimal montoBase = precioEfectivo.multiply(dto.getCantidad()).setScale(4, RoundingMode.HALF_UP);
        BigDecimal montoComision = calcularComision(montoBase);

        Orden orden = new Orden();
        orden.setUsuarioId(clienteId);
        orden.setComisionistaId(comisionistaId);
        orden.setSimbolo(simbolo);
        orden.setTipoOrden(tipoOrden);
        orden.setLado(lado);
        orden.setEstado(EstadoOrden.PENDIENTE_APROBACION);
        orden.setCantidad(dto.getCantidad());
        orden.setPrecioLimite(dto.getPrecioLimite());
        orden.setPrecioStop(dto.getPrecioStop());
        orden.setMontoTotal(montoBase);
        orden.setComision(montoComision);
        orden.setMontoNeto(lado == TipoLado.COMPRA ? montoBase.add(montoComision) : montoBase.subtract(montoComision));
        orden.setComentarioComisionista(dto.getComentarioComisionista());
        orden.setIpOrigen(ipOrigen);
        orden.setCreadaEn(LocalDateTime.now());
        orden = ordenRepo.save(orden);

        auditLog.registrar(TipoEvento.PROPUESTA_ORDEN_CREADA, comisionistaId.toString(),
                "Propuesta id=" + orden.getId() + " para inversionista=" + clienteId + " simbolo=" + simbolo);
        return mapearOrden(orden);
    }

    @Override
    @Transactional
    public List<OrdenDTO> obtenerPropuestasPendientesInversionista(Long usuarioId) {
        return ordenRepo.findByUsuarioIdAndEstadoOrderByCreadaEnDesc(usuarioId, EstadoOrden.PENDIENTE_APROBACION)
                .stream()
                .map(this::mapearOrden)
                .toList();
    }

    @Override
    @Transactional
    public List<OrdenDTO> obtenerPropuestasAprobadasComisionista(Long comisionistaId) {
        return ordenRepo.findByComisionistaIdAndEstadoOrderByCreadaEnDesc(comisionistaId, EstadoOrden.APROBADA)
                .stream()
                .map(this::mapearOrden)
                .toList();
    }

    @Override
    @Transactional
    public OrdenDTO aprobarPropuesta(Long usuarioId, Long propuestaId, String comentario) {
        Orden orden = ordenRepo.findByIdAndUsuarioId(propuestaId, usuarioId)
                .orElseThrow(() -> new OrdenNoEncontradaException("Propuesta no encontrada: " + propuestaId));
        if (orden.getEstado() != EstadoOrden.PENDIENTE_APROBACION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La propuesta ya fue decidida");
        }
        orden.setEstado(EstadoOrden.APROBADA);
        orden.setComentarioInversionista(comentario);
        orden.setAprobadaEn(LocalDateTime.now());
        ordenRepo.save(orden);
        auditLog.registrar(TipoEvento.PROPUESTA_ORDEN_APROBADA, usuarioId.toString(),
                "Propuesta aprobada id=" + propuestaId);
        return mapearOrden(orden);
    }

    @Override
    @Transactional
    public OrdenDTO rechazarPropuesta(Long usuarioId, Long propuestaId, String comentario) {
        Orden orden = ordenRepo.findByIdAndUsuarioId(propuestaId, usuarioId)
                .orElseThrow(() -> new OrdenNoEncontradaException("Propuesta no encontrada: " + propuestaId));
        if (orden.getEstado() != EstadoOrden.PENDIENTE_APROBACION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La propuesta ya fue decidida");
        }
        orden.setEstado(EstadoOrden.RECHAZADA);
        orden.setComentarioInversionista(comentario);
        orden.setRechazadaEn(LocalDateTime.now());
        ordenRepo.save(orden);
        auditLog.registrar(TipoEvento.PROPUESTA_ORDEN_RECHAZADA, usuarioId.toString(),
                "Propuesta rechazada id=" + propuestaId);
        return mapearOrden(orden);
    }

    @Override
    @Transactional
    public OrdenDTO firmarYEnviarPropuesta(Long comisionistaId, Long propuestaId, String ipOrigen) {
        Orden orden = ordenRepo.findByIdAndComisionistaId(propuestaId, comisionistaId)
                .orElseThrow(() -> new OrdenNoEncontradaException("Propuesta no encontrada: " + propuestaId));
        asignacionComisionista.validarClienteAsignado(comisionistaId, orden.getUsuarioId());
        validarUsuarioPuedeOperar(orden.getUsuarioId());
        if (orden.getEstado() != EstadoOrden.APROBADA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se pueden firmar propuestas aprobadas");
        }
        orden.setFirmadaEn(LocalDateTime.now());
        orden.setIpOrigen(ipOrigen);
        auditLog.registrar(TipoEvento.PROPUESTA_ORDEN_FIRMADA, comisionistaId.toString(),
                "Propuesta firmada id=" + propuestaId + " inversionista=" + orden.getUsuarioId());
        return enviarOrdenAprobada(orden);
    }

    // =========================================================
    // Helpers privados
    // =========================================================

    private OrdenDTO enviarOrdenAprobada(Orden orden) {
        Long usuarioId = orden.getUsuarioId();
        CotizacionDTO cotizacion = mercadoService.validarSimboloOperable(orden.getSimbolo());
        BigDecimal precioRef = cotizacion.getPrecioActual();
        BigDecimal precioEfectivo = precioEfectivo(orden.getTipoOrden(), orden.getPrecioLimite(), orden.getPrecioStop(), precioRef);
        BigDecimal montoBase = precioEfectivo.multiply(orden.getCantidad()).setScale(4, RoundingMode.HALF_UP);
        BigDecimal montoComision = calcularComision(montoBase);
        orden.setMontoTotal(montoBase);
        orden.setComision(montoComision);
        orden.setMontoNeto(orden.getLado() == TipoLado.COMPRA ? montoBase.add(montoComision) : montoBase.subtract(montoComision));

        if (orden.getLado() == TipoLado.COMPRA) {
            saldoService.reservarFondos(usuarioId, orden.getMontoNeto());
        } else {
            portafolioService.verificarHolding(usuarioId, orden.getSimbolo(), orden.getCantidad());
        }

        if (!cotizacion.isMercadoAbierto()) {
            orden.setEstado(EstadoOrden.EN_COLA);
            ordenRepo.save(orden);
            auditLog.registrar(TipoEvento.ORDEN_ENCOLADA, usuarioId.toString(),
                    "Orden asesorada encolada id=" + orden.getId());
            return mapearOrden(orden);
        }

        orden.setEstado(EstadoOrden.PENDIENTE);
        orden = ordenRepo.save(orden);
        // Solo necesarios para alpaca.crearCuenta que requiere las entidades completas
        Usuario usuario = usuarioRepo.findById(usuarioId).orElseThrow();
        Inversionista inversionista = obtenerInversionista(usuarioId);

        if (consultaInversionista.necesitaCuentaAlpaca(usuarioId) && esSimboloUs(orden.getSimbolo())) {
            String nuevoId = alpaca.crearCuenta(usuario, inversionista);
            if (nuevoId != null) {
                consultaInversionista.actualizarAlpacaAccountId(usuarioId, nuevoId);
            }
        }

        String alpacaAccountId = consultaInversionista.obtenerAlpacaAccountId(usuarioId);

        if (esSimboloUs(orden.getSimbolo())) {
            String alpacaOrderId = enviarAAlpaca(alpacaAccountId, orden);
            if (alpacaOrderId != null) {
                orden.setAlpacaOrderId(alpacaOrderId);
                orden.setEstado(EstadoOrden.ENVIADA);
                auditLog.registrar(TipoEvento.ORDEN_ENVIADA_ALPACA, usuarioId.toString(),
                        "Orden asesorada enviada a Alpaca: " + alpacaOrderId);
                confirmarSiAlpacaReportaFill(usuarioId, alpacaAccountId, orden, precioRef);
            } else {
                if (orden.getLado() == TipoLado.COMPRA) {
                    saldoService.liberarFondosReservados(usuarioId, orden.getMontoNeto());
                }
                orden.setEstado(EstadoOrden.PENDIENTE);
                auditLog.registrar(TipoEvento.ORDEN_FALLO_ALPACA, usuarioId.toString(),
                        "Fallo al enviar orden asesorada id=" + orden.getId());
            }
        } else {
            boolean condicionCumplida = verificarCondicionPrecio(orden.getTipoOrden(), orden.getLado(), precioRef,
                    orden.getPrecioLimite(), orden.getPrecioStop());
            if (orden.getTipoOrden() == TipoOrden.MARKET || condicionCumplida) {
                orden.setEstado(EstadoOrden.EJECUTADA);
                orden.setPrecioEjecucion(precioRef);
                orden.setEjecutadaEn(LocalDateTime.now());
                confirmarEjecucion(usuarioId, orden, precioRef);
            } else {
                orden.setEstado(EstadoOrden.ENVIADA);
            }
        }

        orden = ordenRepo.save(orden);
        auditLog.registrar(TipoEvento.ORDEN_CREADA, usuarioId.toString(),
                "Orden asesorada creada id=" + orden.getId());
        return mapearOrden(orden);
    }

    private BigDecimal precioEfectivo(TipoOrden tipoOrden, BigDecimal precioLimite,
                                      BigDecimal precioStop, BigDecimal precioRef) {
        BigDecimal precio = (tipoOrden == TipoOrden.LIMIT || tipoOrden == TipoOrden.TAKE_PROFIT)
                ? precioLimite : (tipoOrden == TipoOrden.STOP_LOSS ? precioStop : precioRef);
        return precio == null || precio.compareTo(BigDecimal.ZERO) == 0 ? precioRef : precio;
    }

    private TipoOrden parseTipoOrden(String tipoOrden) {
        try {
            return TipoOrden.valueOf(tipoOrden.toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de orden invalido");
        }
    }

    private TipoOrden parseTipoOrdenOpcional(String tipoOrden) {
        if (tipoOrden == null || tipoOrden.isBlank()) return null;
        return parseTipoOrden(tipoOrden.trim());
    }

    private EstadoOrden parseEstadoOrdenOpcional(String estado) {
        if (estado == null || estado.isBlank()) return null;
        try {
            return EstadoOrden.valueOf(estado.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado de orden invalido");
        }
    }

    private LocalDateTime parseFechaInicio(String fecha) {
        if (fecha == null || fecha.isBlank()) return null;
        try {
            return LocalDate.parse(fecha.trim()).atStartOfDay();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fecha inicial invalida");
        }
    }

    private LocalDateTime parseFechaFin(String fecha) {
        if (fecha == null || fecha.isBlank()) return null;
        try {
            return LocalDate.parse(fecha.trim()).atTime(23, 59, 59, 999_000_000);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fecha final invalida");
        }
    }

    private String normalizarFiltroSimbolo(String simbolo) {
        if (simbolo == null || simbolo.isBlank()) return null;
        return simbolo.trim().toUpperCase(Locale.ROOT);
    }

    private TipoLado parseTipoLado(String lado) {
        try {
            return TipoLado.valueOf(lado.toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lado de orden invalido");
        }
    }

    private void validarParametrosOrden(TipoOrden tipoOrden, TipoLado lado,
                                        BigDecimal precioLimite, BigDecimal precioStop) {
        if (tipoOrden == TipoOrden.LIMIT && precioInvalido(precioLimite)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El precio limite es obligatorio para ordenes LIMIT");
        }
        if (tipoOrden == TipoOrden.STOP_LOSS) {
            if (lado != TipoLado.VENTA) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "STOP_LOSS solo aplica para venta");
            }
            if (precioInvalido(precioStop)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El precio de activacion es obligatorio para STOP_LOSS");
            }
        }
        if (tipoOrden == TipoOrden.TAKE_PROFIT) {
            if (lado != TipoLado.VENTA) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TAKE_PROFIT solo aplica para venta");
            }
            if (precioInvalido(precioLimite)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El precio objetivo es obligatorio para TAKE_PROFIT");
            }
        }
    }

    private boolean precioInvalido(BigDecimal precio) {
        return precio == null || precio.compareTo(BigDecimal.ZERO) <= 0;
    }

    private BigDecimal calcularComision(BigDecimal montoBase) {
        return montoBase.multiply(porcentajeComision())
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal porcentajeComision() {
        return administracion.obtenerPorcentajeComision();
    }

    private BigDecimal splitPlataforma() {
        return administracion.obtenerSplitPlataforma();
    }

    private void validarUsuarioPuedeOperar(Long usuarioId) {
        consultaInversionista.validarPuedeOperar(usuarioId);
    }

    private String enviarAAlpaca(String alpacaAccountId, Orden orden) {
        if (alpacaAccountId == null) return null;
        String tipoAlpaca = toAlpacaTipo(orden.getTipoOrden());
        String ladoAlpaca = orden.getLado() == TipoLado.COMPRA ? "buy" : "sell";
        String precioLimite = orden.getPrecioLimite() != null ? orden.getPrecioLimite().toPlainString() : null;
        String precioStop = orden.getPrecioStop() != null ? orden.getPrecioStop().toPlainString() : null;

        return alpaca.crearOrden(alpacaAccountId, orden.getSimbolo(), tipoAlpaca,
                ladoAlpaca, orden.getCantidad().toPlainString(), precioLimite, precioStop);
    }

    private Inversionista obtenerInversionista(Long usuarioId) {
        return inversionistaRepo.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new IllegalStateException("Inversionista no encontrado para usuario " + usuarioId));
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
        String alpacaAccountId = consultaInversionista.obtenerAlpacaAccountId(usuarioId);
        if (alpacaAccountId == null) return;

        List<Orden> enviadas = ordenRepo.findByUsuarioIdAndEstadoOrderByCreadaEnDesc(usuarioId, EstadoOrden.ENVIADA);
        for (Orden orden : enviadas) {
            if (orden.getAlpacaOrderId() == null || !esSimboloUs(orden.getSimbolo())) continue;
            BigDecimal precioRef = precioReferenciaOrden(orden);
            confirmarSiAlpacaReportaFill(usuarioId, alpacaAccountId, orden, precioRef);
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
        BigDecimal comisionReal = montoReal.multiply(porcentajeComision())
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
        BigDecimal montoPlatf = orden.getComisionistaId() != null
                ? comisionReal.multiply(splitPlataforma()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                : comisionReal;
        Comision comisionEnt = new Comision();
        comisionEnt.setOrdenId(orden.getId());
        comisionEnt.setUsuarioId(usuarioId);
        comisionEnt.setComisionistaId(orden.getComisionistaId());
        comisionEnt.setMontoBase(montoReal);
        comisionEnt.setPorcentajeComision(porcentajeComision());
        comisionEnt.setMontoComision(comisionReal);
        comisionEnt.setMontoPlataforma(montoPlatf);
        comisionEnt.setMontoComisionista(orden.getComisionistaId() != null ? comisionReal.subtract(montoPlatf) : BigDecimal.ZERO);
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
        dto.setComisionistaId(o.getComisionistaId());
        dto.setComentarioComisionista(o.getComentarioComisionista());
        dto.setComentarioInversionista(o.getComentarioInversionista());
        dto.setCreadaEn(o.getCreadaEn());
        dto.setEjecutadaEn(o.getEjecutadaEn());
        dto.setAprobadaEn(o.getAprobadaEn());
        dto.setRechazadaEn(o.getRechazadaEn());
        dto.setFirmadaEn(o.getFirmadaEn());
        return dto;
    }
}
