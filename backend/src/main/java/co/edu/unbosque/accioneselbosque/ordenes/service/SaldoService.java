package co.edu.unbosque.accioneselbosque.ordenes.service;

import co.edu.unbosque.accioneselbosque.integracion.adaptadores.alpaca.IIntegracionAlpaca;
import co.edu.unbosque.accioneselbosque.ordenes.dto.ComisionDetalleDTO;
import co.edu.unbosque.accioneselbosque.ordenes.dto.SaldoDTO;
import co.edu.unbosque.accioneselbosque.ordenes.model.Comision;
import co.edu.unbosque.accioneselbosque.ordenes.model.CuentaFondos;
import co.edu.unbosque.accioneselbosque.ordenes.repository.ComisionRepository;
import co.edu.unbosque.accioneselbosque.ordenes.repository.CuentaFondosRepository;
import co.edu.unbosque.accioneselbosque.shared.exceptions.FondosInsuficientesException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SaldoService {

    private final CuentaFondosRepository cuentaRepo;
    private final ComisionRepository comisionRepo;
    private final IIntegracionAlpaca alpaca;

    public SaldoService(CuentaFondosRepository cuentaRepo, ComisionRepository comisionRepo,
                        IIntegracionAlpaca alpaca) {
        this.cuentaRepo = cuentaRepo;
        this.comisionRepo = comisionRepo;
        this.alpaca = alpaca;
    }

    @Transactional
    public CuentaFondos obtenerOCrear(Long inversionistaId) {
        return cuentaRepo.findById(inversionistaId).orElseGet(() -> {
            CuentaFondos nueva = new CuentaFondos();
            nueva.setInversionistaId(inversionistaId);
            nueva.setSaldoDisponible(BigDecimal.ZERO);
            nueva.setFondosReservados(BigDecimal.ZERO);
            nueva.setActualizadoEn(LocalDateTime.now());
            return cuentaRepo.save(nueva);
        });
    }

    @Transactional
    public CuentaFondos sincronizarConAlpaca(Long inversionistaId, String alpacaAccountId) {
        if (alpacaAccountId == null) return obtenerOCrear(inversionistaId);
        Map<String, Object> cuentaAlpaca = alpaca.obtenerCuenta(alpacaAccountId);
        if (cuentaAlpaca.isEmpty()) return obtenerOCrear(inversionistaId);

        CuentaFondos cuenta = obtenerOCrear(inversionistaId);
        try {
            if (cuentaAlpaca.get("cash") != null) {
                cuenta.setSaldoDisponible(new BigDecimal(cuentaAlpaca.get("cash").toString()));
            }
        } catch (Exception ignored) {}
        cuenta.setActualizadoEn(LocalDateTime.now());
        return cuentaRepo.save(cuenta);
    }

    @Transactional
    public void reservarFondos(Long inversionistaId, BigDecimal monto) {
        CuentaFondos cuenta = obtenerOCrear(inversionistaId);
        if (cuenta.getSaldoDisponible().compareTo(monto) < 0) {
            throw new FondosInsuficientesException(
                    "Fondos insuficientes. Disponible: " + cuenta.getSaldoDisponible()
                            + ", requerido: " + monto);
        }
        cuenta.setSaldoDisponible(cuenta.getSaldoDisponible().subtract(monto));
        cuenta.setFondosReservados(cuenta.getFondosReservados().add(monto));
        cuenta.setActualizadoEn(LocalDateTime.now());
        cuentaRepo.save(cuenta);
    }

    @Transactional
    public void liberarFondosReservados(Long inversionistaId, BigDecimal monto) {
        CuentaFondos cuenta = obtenerOCrear(inversionistaId);
        BigDecimal aLiberar = monto.min(cuenta.getFondosReservados());
        cuenta.setFondosReservados(cuenta.getFondosReservados().subtract(aLiberar));
        cuenta.setSaldoDisponible(cuenta.getSaldoDisponible().add(aLiberar));
        cuenta.setActualizadoEn(LocalDateTime.now());
        cuentaRepo.save(cuenta);
    }

    @Transactional
    public void confirmarCompra(Long inversionistaId, BigDecimal montoTotal) {
        CuentaFondos cuenta = obtenerOCrear(inversionistaId);
        BigDecimal aDescontar = montoTotal.min(cuenta.getFondosReservados());
        cuenta.setFondosReservados(cuenta.getFondosReservados().subtract(aDescontar));
        cuenta.setActualizadoEn(LocalDateTime.now());
        cuentaRepo.save(cuenta);
    }

    @Transactional
    public void confirmarVenta(Long inversionistaId, BigDecimal montoNeto) {
        CuentaFondos cuenta = obtenerOCrear(inversionistaId);
        cuenta.setSaldoDisponible(cuenta.getSaldoDisponible().add(montoNeto));
        cuenta.setActualizadoEn(LocalDateTime.now());
        cuentaRepo.save(cuenta);
    }

    @Transactional
    public void depositar(Long inversionistaId, BigDecimal monto) {
        CuentaFondos cuenta = obtenerOCrear(inversionistaId);
        cuenta.setSaldoDisponible(cuenta.getSaldoDisponible().add(monto));
        cuenta.setActualizadoEn(LocalDateTime.now());
        cuentaRepo.save(cuenta);
    }

    @Transactional
    public SaldoDTO obtenerSaldoDTO(Long inversionistaId) {
        CuentaFondos cuenta = obtenerOCrear(inversionistaId);
        List<Comision> comisiones = comisionRepo.findByUsuarioIdOrderByCreadaEnDesc(inversionistaId);

        BigDecimal totalComisiones = comisiones.stream()
                .map(Comision::getMontoComision)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ComisionDetalleDTO> historial = new ArrayList<>();
        for (Comision c : comisiones) {
            ComisionDetalleDTO det = new ComisionDetalleDTO();
            det.setOrdenId(c.getOrdenId());
            det.setMontoBase(c.getMontoBase());
            det.setPorcentajeComision(c.getPorcentajeComision());
            det.setMontoComision(c.getMontoComision());
            det.setCreadaEn(c.getCreadaEn());
            historial.add(det);
        }

        SaldoDTO dto = new SaldoDTO();
        dto.setSaldoDisponible(cuenta.getSaldoDisponible());
        dto.setFondosReservados(cuenta.getFondosReservados());
        dto.setTotalComisionesPagadas(totalComisiones);
        dto.setHistorialComisiones(historial);
        return dto;
    }
}
