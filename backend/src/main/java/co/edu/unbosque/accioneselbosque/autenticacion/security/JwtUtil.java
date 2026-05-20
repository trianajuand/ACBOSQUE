package co.edu.unbosque.accioneselbosque.autenticacion.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey clave;
    private final long expiracionMs;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiracion-ms}") long expiracionMs) {
        this.clave = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiracionMs = expiracionMs;
    }

    public String generarToken(String correo, String rol) {
        return Jwts.builder()
                .subject(correo)
                .claim("rol", rol)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiracionMs))
                .signWith(clave)
                .compact();
    }

    public String generarTokenMfa(String correo) {
        return Jwts.builder()
                .subject(correo)
                .claim("tipo", "MFA")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 10 * 60 * 1000L))
                .signWith(clave)
                .compact();
    }

    public Claims parsearToken(String token) {
        return Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String obtenerCorreo(String token) {
        return parsearToken(token).getSubject();
    }

    public boolean esValido(String token) {
        try {
            parsearToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
