package com.tiendapesca.APItiendapesca.Security;

import io.github.cdimascio.dotenv.Dotenv;
import io.jsonwebtoken.security.Keys;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Configuración para la generación de la clave secreta JWT
 * Lee la clave secreta desde variables de entorno y la configura para su uso en la aplicación
 */
@Configuration
public class JwtConfig {

    private final Dotenv dotenv;

    /**
     * Constructor que inyecta Dotenv con carga diferida para evitar ciclos de dependencia
     * @param dotenv Instancia de Dotenv para acceso a variables de entorno
     */
    public JwtConfig(@Lazy Dotenv dotenv) {
        this.dotenv = dotenv;
    }
    
    /**
     * Crea y configura la clave secreta para firmar Tokens JWT
     * @return SecretKey configurada a partir de la variable JWT_SECRET
     * @throws IllegalArgumentException si la clave secreta no está configurada o está vacía
     */
    @Bean
    public SecretKey jwtSecretKey() {
        String secretKey = dotenv.get("JWT_SECRET");
        if (secretKey == null || secretKey.trim().isEmpty()) {
            throw new IllegalArgumentException("La clave secreta JWT no puede estar vacía");
        }
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }
}