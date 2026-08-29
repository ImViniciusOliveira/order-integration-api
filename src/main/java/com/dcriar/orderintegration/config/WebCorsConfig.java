package com.dcriar.orderintegration.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração global de Cross-Origin Resource Sharing (CORS) para a API REST.
 * <p>
 * Permite integração segura com frontends, painéis administrativos e automações externas,
 * expondo cabeçalhos customizados e suportando pre-flight requests (OPTIONS).
 */
@Configuration
public class WebCorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD")
                .allowedHeaders("Origin", "Content-Type", "Accept", "Authorization", "X-Internal-API-Key", "X-Shop-Id")
                .exposedHeaders("Location", "Content-Disposition")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
