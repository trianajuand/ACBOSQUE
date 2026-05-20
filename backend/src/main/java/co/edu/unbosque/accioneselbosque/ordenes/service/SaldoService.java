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

    /** Retorna o crea la cuenta de fondos para un usuario. */
    @Transactional
    public CuentaFondos obtenerOCrear(Long usuarioId) {
        return cuentaRepo.findByUsuarioId(usuarioId).orElseGet(() -> {
            CuentaFondos nueva = new CuentaFondos();
            nueva.setUsuarioId(usuarioId);
            nueva.setSaldoDisponible(BigDecimal.ZERO);
            nueva.setFondosReservados(BigDecimal.ZERO);
            nueva.setActualizadoEn(LocalDateTime.now());
            return cuentaRepo.save(nueva);
        });
    }

    /**
     * Sincroniza el saldo disponible con Alpaca y retorna la cuenta actualizada.
     * Se llama al inicio de sesión y periódicamente.
     */
    @Transactional
    public CuentaFondos sincronizarConAlpaca(Long usuarioId, String alpacaAccountId) {
        if (alpacaAccountId == null) return obtenerOCrear(usuarioId);
        Map<String, Object> cuentaAlpaca = alpaca.obtenerCuenta(alpacaAccountId);
        if (cuentaAlpaca.isEmpty()) return obtenerOCrear(usuarioId);

        CuentaFondos cuenta = obtenerOCrear(usuarioId);
        try {
            if (cuentaAlpaca.get("cash") != null) {
                cuenta.setSaldoDisponible(new BigDecimal(cuentaAlpaca.get("cash").toString()));
            }
        } catch (Exception ignored) {}
        cuenta.setActualizadoEn(LocalDateTime.now());
        return cuentaRepo.save(cuenta);
    }

    /** Reserva fondos al crear una orden de compra. */
    @Transactional
    public void reservarFondos(Long usuarioId, BigDecimal monto) {
        CuentaFondos cuenta = obtenerOCrear(usuarioId);
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

    /** Libera fondos reservados (al cancelar una orden). */
    @Transactional
    public void liberarFondosReservados(Long usuarioId, BigDecimal monto) {
        CuentaFondos cuenta = obtenerOCrear(usuarioId);
        BigDecimal aLiberar = monto.min(cuenta.getFondosReservados());
        cuenta.setFondosReservados(cuenta.getFondosReservados().subtract(aLiberar));
        cuenta.setSaldoDisponible(cuenta.getSaldoDisponible().add(aLiberar));
        cuenta.setActualizadoEn(LocalDateTime.now());
        cuentaRepo.save(cuenta);
    }

    /** Confirma una compra: descuenta de reservados, no devuelve al disponible. */
    @Transactional
    public void confirmarCompra(Long usuarioId, BigDecimal montoTotal) {
        CuentaFondos cuenta = obtenerOCrear(usuarioId);
        BigDecimal aDescontar = montoTotal.min(cuenta.getFondosReservados());
        cuenta.setFondosReservados(cuenta.getFondosReservados().subtract(aDescontar));
        cuenta.setActualizadoEn(LocalDateTime.now());
        cuentaRepo.save(cuenta);
    }

    /** Confirma una venta: acredita el neto al saldo disponible. */
    @Transactional
    public void confirmarVenta(Long usuarioId, BigDecimal montoNeto) {
        CuentaFondos cuenta = obtenerOCrear(usuarioId);
        cuenta.setSaldoDisponible(cuenta.getSaldoDisponible().add(montoNeto));
        cuenta.setActualizadoEn(LocalDateTime.now());
        cuentaRepo.save(cuenta);
    }

    /** Deposita fondos (endpoint de prueba / administrador). */
    @Transactional
    public void depositar(Long usuarioId, BigDecimal monto) {
        CuentaFondos cuenta = obtenerOCrear(usuarioId);
        cuenta.setSaldoDisponible(cuenta.getSaldoDisponible().add(monto));
        cuenta.setActualizadoEn(LocalDateTime.now());
        cuentaRepo.save(cuenta);
    }

    @Transactional
    public SaldoDTO obtenerSaldoDTO(Long usuarioId) {
        CuentaFondos cuenta = obtenerOCrear(usuarioId);
        List<Comision> comisiones = comisionRepo.findByUsuarioIdOrderByCreadaEnDesc(usuarioId);

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
