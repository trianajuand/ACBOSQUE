package co.edu.unbosque.accioneselbosque.ordenes.service;

import co.edu.unbosque.accioneselbosque.mercado.service.MercadoService;
import co.edu.unbosque.accioneselbosque.ordenes.dto.HoldingDTO;
import co.edu.unbosque.accioneselbosque.ordenes.dto.PortafolioDTO;
import co.edu.unbosque.accioneselbosque.ordenes.model.Holding;
import co.edu.unbosque.accioneselbosque.ordenes.repository.HoldingRepository;
import co.edu.unbosque.accioneselbosque.shared.exceptions.HoldingInsuficienteException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PortafolioService {

    private final HoldingRepository holdingRepo;
    private final MercadoService mercadoService;

    public PortafolioService(HoldingRepository holdingRepo, MercadoService mercadoService) {
        this.holdingRepo = holdingRepo;
        this.mercadoService = mercadoService;
    }

    @Transactional(readOnly = true)
    public PortafolioDTO obtenerPortafolio(Long usuarioId, String vistaPreferida) {
        List<Holding> holdings = holdingRepo.findByUsuarioId(usuarioId);
        List<HoldingDTO> dtos = new ArrayList<>();

        BigDecimal valorTotal = BigDecimal.ZERO;
        BigDecimal costTotal = BigDecimal.ZERO;

        for (Holding h : holdings) {
            if (h.getCantidad().compareTo(BigDecimal.ZERO) <= 0) continue;

            HoldingDTO dto = new HoldingDTO();
            dto.setSimbolo(h.getSimbolo());
            dto.setCantidad(h.getCantidad());
            dto.setPrecioPromedio(h.getPrecioPromedio());

            BigDecimal precioActual = BigDecimal.ZERO;
            try {
                var cot = mercadoService.obtenerCotizacion(h.getSimbolo());
                if (cot.getPrecioActual() != null) precioActual = cot.getPrecioActual();
            } catch (Exception ignored) {}

            dto.setPrecioActual(precioActual);

            BigDecimal valor = precioActual.multiply(h.getCantidad()).setScale(4, RoundingMode.HALF_UP);
            BigDecimal costo = h.getPrecioPromedio().multiply(h.getCantidad()).setScale(4, RoundingMode.HALF_UP);
            BigDecimal gp = valor.subtract(costo);
            BigDecimal gpPct = costo.compareTo(BigDecimal.ZERO) != 0
                    ? gp.divide(costo, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            dto.setValorTotal(valor);
            dto.setGananciaPerdida(gp);
            dto.setGananciaPerdidaPct(gpPct);

            valorTotal = valorTotal.add(valor);
            costTotal = costTotal.add(costo);
            dtos.add(dto);
        }

        BigDecimal gpTotal = valorTotal.subtract(costTotal);
        BigDecimal gpTotalPct = costTotal.compareTo(BigDecimal.ZERO) != 0
                ? gpTotal.divide(costTotal, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        PortafolioDTO portafolio = new PortafolioDTO();
        portafolio.setHoldings(dtos);
        portafolio.setValorTotalPortafolio(valorTotal);
        portafolio.setGananciaPerdidaTotal(gpTotal);
        portafolio.setGananciaPerdidaTotalPct(gpTotalPct);
        portafolio.setVistaPreferida(vistaPreferida != null ? vistaPreferida : "LISTA");
        return portafolio;
    }

    /** Agrega o actualiza un holding al ejecutar una compra. */
    @Transactional
    public void registrarCompra(Long usuarioId, String simbolo, BigDecimal cantidad, BigDecimal precioEjecucion) {
        Optional<Holding> existente = holdingRepo.findByUsuarioIdAndSimbolo(usuarioId, simbolo);
        if (existente.isPresent()) {
            Holding h = existente.get();
            BigDecimal cantidadAnterior = h.getCantidad();
            BigDecimal costoAnterior = cantidadAnterior.multiply(h.getPrecioPromedio());
            BigDecimal costoNuevo = cantidad.multiply(precioEjecucion);
            BigDecimal cantidadTotal = cantidadAnterior.add(cantidad);
            BigDecimal nuevoPrecioPromedio = costoAnterior.add(costoNuevo)
                    .divide(cantidadTotal, 4, RoundingMode.HALF_UP);
            h.setCantidad(cantidadTotal);
            h.setPrecioPromedio(nuevoPrecioPromedio);
            h.setActualizadoEn(LocalDateTime.now());
            holdingRepo.save(h);
        } else {
            Holding nuevo = new Holding();
            nuevo.setUsuarioId(usuarioId);
            nuevo.setSimbolo(simbolo);
            nuevo.setCantidad(cantidad);
            nuevo.setPrecioPromedio(precioEjecucion);
            nuevo.setActualizadoEn(LocalDateTime.now());
            holdingRepo.save(nuevo);
        }
    }

    /** Reduce el holding al ejecutar una venta. */
    @Transactional
    public void registrarVenta(Long usuarioId, String simbolo, BigDecimal cantidad) {
        Holding h = holdingRepo.findByUsuarioIdAndSimbolo(usuarioId, simbolo)
                .orElseThrow(() -> new HoldingInsuficienteException(
                        "No tienes posición en " + simbolo));
        if (h.getCantidad().compareTo(cantidad) < 0) {
            throw new HoldingInsuficienteException(
                    "Cantidad insuficiente en " + simbolo + ". Disponible: " + h.getCantidad());
        }
        BigDecimal nueva = h.getCantidad().subtract(cantidad);
        h.setCantidad(nueva);
        h.setActualizadoEn(LocalDateTime.now());
        holdingRepo.save(h);
    }

    /** Verifica si el usuario tiene suficiente cantidad del símbolo para vender. */
    public void verificarHolding(Long usuarioId, String simbolo, BigDecimal cantidad) {
        Holding h = holdingRepo.findByUsuarioIdAndSimbolo(usuarioId, simbolo)
                .orElseThrow(() -> new HoldingInsuficienteException(
                        "No tienes posición en " + simbolo));
        if (h.getCantidad().compareTo(cantidad) < 0) {
            throw new HoldingInsuficienteException(
                    "Cantidad insuficiente en " + simbolo + ". Disponible: " + h.getCantidad());
        }
    }
}
