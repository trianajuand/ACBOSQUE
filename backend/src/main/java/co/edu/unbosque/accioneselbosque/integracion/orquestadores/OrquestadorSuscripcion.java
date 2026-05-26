package co.edu.unbosque.accioneselbosque.integracion.orquestadores;

import co.edu.unbosque.accioneselbosque.autenticacion.interfaces.IAsignacionComisionista;
import co.edu.unbosque.accioneselbosque.autenticacion.model.EstadoCuenta;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Suscripcion;
import co.edu.unbosque.accioneselbosque.autenticacion.model.Usuario;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.SuscripcionRepository;
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
    private final SuscripcionRepository suscripcionRepository;
    private final IAuditLog auditLog;
    private final OrquestadorRegistro orquestadorRegistro;
    private final IAsignacionComisionista asignacionComisionista;
    private final String successUrl;
    private final String cancelUrl;

    public OrquestadorSuscripcion(
            IIntegracionStripe stripe,
            UsuarioRepository usuarioRepository,
            SuscripcionRepository suscripcionRepository,
            IAuditLog auditLog,
            OrquestadorRegistro orquestadorRegistro,
            IAsignacionComisionista asignacionComisionista,
            @Value("${stripe.success-url}") String successUrl,
            @Value("${stripe.cancel-url}") String cancelUrl) {
        this.stripe = stripe;
        this.usuarioRepository = usuarioRepository;
        this.suscripcionRepository = suscripcionRepository;
        this.auditLog = auditLog;
        this.orquestadorRegistro = orquestadorRegistro;
        this.asignacionComisionista = asignacionComisionista;
        this.successUrl = successUrl;
        this.cancelUrl = cancelUrl;
    }

    public String iniciarSuscripcion(Usuario usuario) {
        Suscripcion suscripcion = suscripcionRepository.findById(usuario.getId())
                .orElseGet(() -> {
                    Suscripcion s = new Suscripcion();
                    s.setInversionistaId(usuario.getId());
                    s.setPlanSuscripcion("BASICO");
                    return s;
                });
        String url = stripe.crearSesionCheckout(
                usuario.getCorreo(),
                suscripcion.getPlanSuscripcion(),
                usuario.getId(),
                successUrl,
                cancelUrl
        );
        auditLog.registrar(
                TipoEvento.SUSCRIPCION_PREMIUM_INICIADA,
                usuario.getCorreo(),
                "Sesion Stripe creada para plan: " + suscripcion.getPlanSuscripcion()
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

        Suscripcion suscripcion = suscripcionRepository.findById(usuario.getId())
                .orElseGet(() -> {
                    Suscripcion s = new Suscripcion();
                    s.setInversionistaId(usuario.getId());
                    return s;
                });

        String plan = session.getMetadata().getOrDefault("plan", suscripcion.getPlanSuscripcion());
        usuario.setEstadoCuenta(EstadoCuenta.ACTIVA);
        usuario.setMfaHabilitado(true);
        suscripcion.setPlanSuscripcion(plan);
        suscripcion.setEsPremium(true);
        suscripcion.setStripeCustomerId(session.getCustomer());
        if (session.getSubscription() != null) {
            suscripcion.setStripeSuscripcionId(session.getSubscription());
        }
        suscripcion.setFechaExpiracionPremium("PREMIUM_ANUAL".equalsIgnoreCase(plan)
                ? LocalDate.now().plusYears(1)
                : LocalDate.now().plusMonths(1));

        usuarioRepository.save(usuario);
        suscripcionRepository.save(suscripcion);
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
