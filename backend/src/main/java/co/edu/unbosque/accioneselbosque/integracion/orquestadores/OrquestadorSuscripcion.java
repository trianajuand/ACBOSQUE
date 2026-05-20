package co.edu.unbosque.accioneselbosque.integracion.orquestadores;

import co.edu.unbosque.accioneselbosque.autenticacion.interfaces.IAsignacionComisionista;
import co.edu.unbosque.accioneselbosque.autenticacion.model.EstadoCuenta;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Inversionista;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Usuario;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.InversionistaRepository;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.UsuarioRepository;
import co.edu.unbosque.accioneselbosque.integracion.adaptadores.stripe.IIntegracionStripe;
import co.edu.unbosque.accioneselbosque.trazabilidad.interfaces.IAuditLog;
import co.edu.unbosque.accioneselbosque.trazabilidad.model.TipoEvento;
import com.stripe.model.checkout.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class OrquestadorSuscripcion {

    private final IIntegracionStripe stripe;
    private final UsuarioRepository usuarioRepository;
    private final InversionistaRepository inversionistaRepository;
    private final IAuditLog auditLog;
    private final OrquestadorRegistro orquestadorRegistro;
    private final IAsignacionComisionista asignacionComisionista;
    private final String successUrl;
    private final String cancelUrl;

    public OrquestadorSuscripcion(
            IIntegracionStripe stripe,
            UsuarioRepository usuarioRepository,
            InversionistaRepository inversionistaRepository,
            IAuditLog auditLog,
            OrquestadorRegistro orquestadorRegistro,
            IAsignacionComisionista asignacionComisionista,
            @Value("${stripe.success-url}") String successUrl,
            @Value("${stripe.cancel-url}") String cancelUrl) {
        this.stripe = stripe;
        this.usuarioRepository = usuarioRepository;
        this.inversionistaRepository = inversionistaRepository;
        this.auditLog = auditLog;
        this.orquestadorRegistro = orquestadorRegistro;
        this.asignacionComisionista = asignacionComisionista;
        this.successUrl = successUrl;
        this.cancelUrl = cancelUrl;
    }

    public String iniciarSuscripcion(Usuario usuario) {
        Inversionista inversionista = inversionistaRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new IllegalStateException("Inversionista no encontrado para usuario " + usuario.getId()));
        String url = stripe.crearSesionCheckout(
                usuario.getCorreo(),
                inversionista.getPlanSuscripcion(),
                usuario.getId(),
                successUrl,
                cancelUrl
        );
        auditLog.registrar(
                TipoEvento.SUSCRIPCION_PREMIUM_INICIADA,
                usuario.getCorreo(),
                "Sesion Stripe creada para plan: " + inversionista.getPlanSuscripcion()
        );
        return url;
    }

    @Transactional
    public Usuario confirmarPagoCheckout(String sessionId) {
        Session session = stripe.obtenerSesionCheckout(sessionId);
        if (!"paid".equalsIgnoreCase(session.getPaymentStatus())) {
            throw new IllegalStateException("La sesion de Stripe no esta pagada");
        }

        String usuarioIdRaw = session.getMetadata() != null ? session.getMetadata().get("usuarioId") : null;
        if (usuarioIdRaw == null) {
            throw new IllegalStateException("La sesion de Stripe no contiene usuarioId");
        }

        Usuario usuario = usuarioRepository.findById(Long.valueOf(usuarioIdRaw))
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado para sesion Stripe"));
        Inversionista inversionista = inversionistaRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new IllegalStateException("Inversionista no encontrado para sesion Stripe"));

        String plan = session.getMetadata().getOrDefault("plan", inversionista.getPlanSuscripcion());
        usuario.setEstadoCuenta(EstadoCuenta.ACTIVA);
        usuario.setMfaHabilitado(true);
        inversionista.setPlanSuscripcion(plan);
        inversionista.setEsPremium(true);
        inversionista.setStripeCustomerId(session.getCustomer());
        if (session.getSubscription() != null) {
            inversionista.setStripeSuscripcionId(session.getSubscription());
        }
        inversionista.setFechaExpiracionPremium("PREMIUM_ANUAL".equalsIgnoreCase(plan)
                ? LocalDate.now().plusYears(1)
                : LocalDate.now().plusMonths(1));
        usuarioRepository.save(usuario);
        inversionistaRepository.save(inversionista);
        asignacionComisionista.asignarSiSolicitado(usuario);
        orquestadorRegistro.crearCuentaAlpaca(usuario);

        auditLog.registrar(
                TipoEvento.SUSCRIPCION_PREMIUM_INICIADA,
                usuario.getCorreo(),
                "Pago Stripe confirmado. Plan activo: " + plan
        );
        return usuario;
    }
}
