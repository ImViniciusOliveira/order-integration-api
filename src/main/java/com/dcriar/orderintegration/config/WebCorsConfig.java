package com.dcriar.orderintegration.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Configuração global de Cross-Origin Resource Sharing (CORS) para a API REST.
 * <p>
 * Permite integração segura com frontends, painéis administrativos e automações externas,
 * lendo as origens permitidas diretamente das propriedades tipadas {@link OrderIntegrationProperties}.
 */
@Configuration
@RequiredArgsConstructor
public class WebCorsConfig implements WebMvcConfigurer {

    private final OrderIntegrationProperties properties;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> allowedOrigins = properties.cors().allowedOrigins();

        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD")
                .allowedHeaders("Origin", "Content-Type", "Accept", "Authorization", "X-Internal-API-Key", "X-Shop-Id")
                .exposedHeaders("Location", "Content-Disposition")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
