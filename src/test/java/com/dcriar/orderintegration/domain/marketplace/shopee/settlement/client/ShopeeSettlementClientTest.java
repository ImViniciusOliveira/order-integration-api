package com.dcriar.orderintegration.domain.marketplace.shopee.settlement.client;

import com.dcriar.orderintegration.config.OrderIntegrationProperties;
import com.dcriar.orderintegration.domain.marketplace.shopee.credential.service.ShopeeCredentialService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Testes unitários do client financeiro da Shopee.
 */
class ShopeeSettlementClientTest {

    @Test
    @DisplayName("Deve reconhecer somente a plataforma Shopee")
    void deveReconhecerSomenteShopee() {
        ShopeeSettlementClient client = newClient();

        assertThat(client.supports("SHOPEE")).isTrue();
        assertThat(client.supports("shopee")).isTrue();
        assertThat(client.supports("MERCADOLIVRE")).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar consulta sem shop_id")
    void deveRejeitarConsultaSemShopId() {
        ShopeeSettlementClient client = newClient();

        assertThatThrownBy(() -> client.fetchSettlement(" ", "ORDER-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("shop_id");
    }

    private ShopeeSettlementClient newClient() {
        OrderIntegrationProperties properties = new OrderIntegrationProperties(
                new OrderIntegrationProperties.RedisProperties("queue"),
                new OrderIntegrationProperties.EscrowProperties(120, 30, 60000L, 50, 5),
                new OrderIntegrationProperties.SecurityProperties("key"),
                new OrderIntegrationProperties.CorsProperties(List.of("http://localhost")),
                new OrderIntegrationProperties.NotificationProperties("http://localhost"),
                new OrderIntegrationProperties.ShopeeProperties(
                        "https://partner.shopeemobile.com",
                        "/api/v2/payment/get_escrow_detail"
                )
        );
        return new ShopeeSettlementClient(
                mock(ShopeeCredentialService.class),
                properties,
                new ShopeeRequestSigner(),
                new ShopeeSettlementResponseMapper()
        );
    }
}
