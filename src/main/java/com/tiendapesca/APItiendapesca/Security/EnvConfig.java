package com.tiendapesca.APItiendapesca.Security;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import java.util.Properties;

@Configuration
public class EnvConfig {

    private final ConfigurableEnvironment environment;

    /**
     * Constructor que inyecta el entorno configurable de Spring
     * @param environment el entorno configurable de Spring
     */
    public EnvConfig(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    /**
     * Configura y provee una instancia de Dotenv para cargar variables desde archivo .env
     * @return Instancia de Dotenv configurada
     */
    @Bean
    public Dotenv dotenv() {
        return Dotenv.configure()
                .ignoreIfMissing()
                .load();
    }

    /**
     * Integra las variables del archivo .env con el entorno de Spring después de la construcción del Bean
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        Dotenv dotenv = dotenv();
        Properties properties = new Properties();

        // Copia todas las variables de Dotenv a Properties
        dotenv.entries().forEach(entry -> {
            properties.put(entry.getKey(), entry.getValue());
        });

        // Agrega las propiedades al entorno de Spring con prioridad
        environment.getPropertySources().addFirst(
            new PropertiesPropertySource("dotenvProperties", properties)
        );
    }
}