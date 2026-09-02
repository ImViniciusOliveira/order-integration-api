package com.dcriar.orderintegration.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários da configuração global de clients HTTP.
 */
class RestClientConfigTest {

    @Test
    @DisplayName("Deve disponibilizar um builder HTTP global")
    void deveDisponibilizarBuilderHttpGlobal() {
        RestClient.Builder builder = new RestClientConfig().restClientBuilder();

        assertThat(builder).isNotNull();
        assertThat(builder.build()).isNotNull();
    }
}
