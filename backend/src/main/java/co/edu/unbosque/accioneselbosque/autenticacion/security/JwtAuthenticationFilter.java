package co.edu.unbosque.accioneselbosque.autenticacion.security;

import co.edu.unbosque.accioneselbosque.autenticacion.model.EstadoCuenta;
import co.edu.unbosque.accioneselbosque.autenticacion.repository.UsuarioRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UsuarioRepository usuarioRepo;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, @Lazy UsuarioRepository usuarioRepo) {
        this.jwtUtil = jwtUtil;
        this.usuarioRepo = usuarioRepo;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String encabezado = request.getHeader("Authorization");

        if (encabezado != null && encabezado.startsWith("Bearer ")) {
            String token = encabezado.substring(7);
            if (jwtUtil.esValido(token)) {
                Claims claims = jwtUtil.parsearToken(token);
                String correo = claims.getSubject();
                String rol = claims.get("rol", String.class);

                boolean cuentaValida = false;
                try {
                    var usuarioOpt = usuarioRepo.findByCorreo(correo);
                    cuentaValida = usuarioOpt.isPresent()
                            && usuarioOpt.get().getEstadoCuenta() != EstadoCuenta.INACTIVA;
                } catch (Exception e) {
                    // Si hay error al consultar la BD, se permite el acceso para no bloquear el sistema
                    cuentaValida = true;
                }

                if (!cuentaValida) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"error\":\"CUENTA_INACTIVA\"}");
                    response.getWriter().flush();
                    return;
                }

                UsernamePasswordAuthenticationToken autenticacion =
                        new UsernamePasswordAuthenticationToken(
                                correo,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + rol)));

                SecurityContextHolder.getContext().setAuthentication(autenticacion);
            }
        }

        filterChain.doFilter(request, response);
    }
}
