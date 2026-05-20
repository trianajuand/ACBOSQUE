package co.edu.unbosque.accioneselbosque.autenticacion.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Recursos estáticos (HTML de prueba)
                .requestMatchers("/", "/test-auth.html", "/dashboard.html",
                        "/favicon.ico", "/*.css", "/*.js", "/api/health").permitAll()
                // Endpoints públicos de autenticación
                .requestMatchers(
                        "/api/auth/register/email-disponible",
                        "/api/auth/register/investor",
                        "/api/auth/register/confirm",
                        "/api/auth/login",
                        "/api/auth/mfa/verify",
                        "/api/auth/forgot-password",
                        "/api/auth/reset-password",
                        "/api/suscripciones/confirmar-checkout",
                        "/api/mercado/simbolos"
                ).permitAll()
                // Swagger UI (solo en desarrollo)
                .requestMatchers(
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                ).permitAll()
                // Todo lo demás requiere autenticación
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
