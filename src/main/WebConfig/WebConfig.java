package com.tiendapesca.APItiendapesca.WebConfig;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Configuración para Render - todas las imágenes bajo /Imagenes/**
        registry.addResourceHandler("/Imagenes/**")
                .addResourceLocations("classpath:/static/Imagenes/");
        
        // Para archivos CSS, JS y fonts
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
    }
}