package com.tiendapesca.APItiendapesca.Security;

import io.jsonwebtoken.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Componente utilitario para la gestión de Tokens JWT (JSON Web Tokens)
 * Proporciona funcionalidades para generar, validar y extraer información de tokens JWT
 */
@Component
public class JWT_TokenUtil {

    private final SecretKey SECRET_KEY;
    private final long EXPIRATION_TIME = 1000 * 60 * 60 * 10; // 10 horas

    /**
     * Constructor que inicializa la clave secreta para firmar tokens
     * @param secretKey Clave secreta para la firma de tokens
     */
    public JWT_TokenUtil(SecretKey secretKey) {
        this.SECRET_KEY = secretKey;
    }
    
    /**
     * Genera un token JWT para el usuario especificado
     * @param username Nombre de usuario para incluir en el token
     * @return Token JWT firmado
     */
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>(); 
        
        return Jwts.builder()
                .setClaims(claims) 
                .setSubject(username) 
                .setIssuedAt(new Date()) 
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) 
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256) 
                .compact(); 
    }

    /**
     * Valida si un token es válido para el UserDetails proporcionado
     * @param token Token JWT a validar
     * @param userDetails Detalles del usuario a validar contra el token
     * @return true si el token es válido, false en caso contrario
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * Extrae el nombre de usuario del token JWT
     * @param token Token JWT
     * @return Nombre de usuario contenido en el token
     * @throws JwtException Si el token es inválido o no puede ser procesado
     */
    public String extractUsername(String token) throws JwtException {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrae la fecha de expiración del token JWT
     * @param token Token JWT
     * @return Fecha de expiración del token
     * @throws JwtException Si el token es inválido o no puede ser procesado
     */
    public Date extractExpiration(String token) throws JwtException {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extrae un claim específico del token usando una función resolutora
     * @param token Token JWT
     * @param claimsResolver Función para extraer el claim específico
     * @return Valor del claim solicitado
     * @throws JwtException Si el token es inválido o no puede ser procesado
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) throws JwtException {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extrae todos los claims del token JWT
     * @param token Token JWT
     * @return Objeto Claims con toda la información del token
     * @throws JwtException Si el token es inválido o no puede ser procesado
     */
    private Claims extractAllClaims(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Verifica si el token ha expirado
     * @param token Token JWT
     * @return true si el token ha expirado, false en caso contrario
     * @throws JwtException Si el token es inválido o no puede ser procesado
     */
    private Boolean isTokenExpired(String token) throws JwtException {
        return extractExpiration(token).before(new Date());
    }
}