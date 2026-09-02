package com.dcriar.orderintegration.domain.marketplace.mercadolivre.settlement.client;

import com.dcriar.orderintegration.config.OrderIntegrationProperties;
import com.dcriar.orderintegration.domain.marketplace.mercadolivre.credential.service.MercadoLivreCredentialService;
import com.dcriar.orderintegration.domain.marketplace.mercadolivre.settlement.mapper.MercadoLivreSettlementResponseMapper;
import com.dcriar.orderintegration.domain.marketplace.mercadolivre.settlement.oauth.MercadoLivreTokenClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Testes unitários do client financeiro do Mercado Livre.
 */
class MercadoLivreSettlementClientTest {

    @Test
    @DisplayName("Deve reconhecer somente a plataforma Mercado Livre")
    void deveReconhecerSomenteMercadoLivre() {
        MercadoLivreSettlementClient client = newClient();

        assertThat(client.supports("MERCADOLIVRE")).isTrue();
        assertThat(client.supports("mercadolivre")).isTrue();
        assertThat(client.supports("SHOPEE")).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar consulta sem identificador da conta")
    void deveRejeitarConsultaSemConta() {
        MercadoLivreSettlementClient client = newClient();

        assertThatThrownBy(() -> client.fetchSettlement(" ", "2001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("conta");
    }

    private MercadoLivreSettlementClient newClient() {
        OrderIntegrationProperties properties = new OrderIntegrationProperties(
                new OrderIntegrationProperties.RedisProperties("queue"),
                new OrderIntegrationProperties.EscrowProperties(120, 30, 60000L, 50, 5),
                new OrderIntegrationProperties.SecurityProperties("key"),
                new OrderIntegrationProperties.CorsProperties(List.of("http://localhost")),
                new OrderIntegrationProperties.NotificationProperties("http://localhost"),
                new OrderIntegrationProperties.ShopeeProperties(
                        "https://partner.shopeemobile.com",
                        "/api/v2/payment/get_escrow_detail"
                ),
                new OrderIntegrationProperties.MercadoLivreProperties(
                        "https://api.mercadolibre.com", "/orders", "/shipments", "/v1/payments", "/oauth/token"
                )
        );
        return new MercadoLivreSettlementClient(
                mock(MercadoLivreCredentialService.class),
                properties,
                new MercadoLivreSettlementResponseMapper(),
                RestClient.builder(),
                mock(MercadoLivreTokenClient.class)
        );
    }
}
