package com.dcriar.orderintegration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configuração global do builder HTTP utilizado pelos clients de marketplaces.
 */
@Configuration
public class RestClientConfig {

    /**
     * Disponibiliza um builder compartilhado para criação dos clients HTTP.
     *
     * @return builder padrão do {@link RestClient}
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
