package com.dcriar.orderintegration.domain.marketplace.mercadolivre.settlement.client;

import com.dcriar.orderintegration.config.OrderIntegrationProperties;
import com.dcriar.orderintegration.domain.marketplace.mercadolivre.credential.document.MercadoLivreCredentialDocument;
import com.dcriar.orderintegration.domain.marketplace.mercadolivre.credential.service.MercadoLivreCredentialService;
import com.dcriar.orderintegration.domain.marketplace.mercadolivre.settlement.mapper.MercadoLivreSettlementResponseMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;

/**
 * Testes unitários do client financeiro do Mercado Livre.
 */
class MercadoLivreSettlementClientTest {

    @Test
    @DisplayName("Deve reconhecer somente a plataforma Mercado Livre")
    void deveReconhecerSomenteMercadoLivre() {
        MercadoLivreSettlementClient client = newClient(RestClient.builder());

        assertThat(client.supports("MERCADOLIVRE")).isTrue();
        assertThat(client.supports("mercadolivre")).isTrue();
        assertThat(client.supports("SHOPEE")).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar consulta sem identificador da conta")
    void deveRejeitarConsultaSemConta() {
        MercadoLivreSettlementClient client = newClient(RestClient.builder());

        assertThatThrownBy(() -> client.fetchSettlement(" ", "2001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("conta");
    }

    @Test
    @DisplayName("Deve consultar pedido, envio e pagamento com Bearer token")
    void deveConsultarRecursosFinanceiros() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        MercadoLivreCredentialService credentialService = mock(MercadoLivreCredentialService.class);
        when(credentialService.getCredential("seller-1")).thenReturn(
                MercadoLivreCredentialDocument.builder()
                        .liveAccessToken("access-token")
                        .build()
        );
        MercadoLivreSettlementClient client = newClient(restClientBuilder, credentialService);

        server.expect(requestTo("https://api.mercadolibre.com/orders/2001"))
                .andExpect(method(GET))
                .andExpect(header("Authorization", "Bearer access-token"))
                .andRespond(withSuccess(
                        """
                        {"total_amount":100.00,"shipping":{"id":"3001"},"payments":[{"id": "4001"}],
                         "order_items":[{"sale_fee":12.00}]}
                        """,
                        MediaType.APPLICATION_JSON
                ));
        server.expect(requestTo("https://api.mercadolibre.com/shipments/3001"))
                .andExpect(method(GET))
                .andExpect(header("Authorization", "Bearer access-token"))
                .andRespond(withSuccess("{\"base_cost\":8.00}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.mercadolibre.com/v1/payments/4001"))
                .andExpect(method(GET))
                .andExpect(header("Authorization", "Bearer access-token"))
                .andRespond(withSuccess(
                        """
                        {"status":"approved","money_release_date":"2020-01-01T00:00:00Z",
                         "transaction_details":{"net_received_amount":80.00}}
                        """,
                        MediaType.APPLICATION_JSON
                ));

        assertThat(client.fetchSettlement("seller-1", "2001").netAmount())
                .isEqualByComparingTo("80.00");
        server.verify();
    }

    private MercadoLivreSettlementClient newClient(RestClient.Builder restClientBuilder) {
        return newClient(restClientBuilder, mock(MercadoLivreCredentialService.class));
    }

    private MercadoLivreSettlementClient newClient(
            RestClient.Builder restClientBuilder,
            MercadoLivreCredentialService credentialService
    ) {
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
                        "https://api.mercadolibre.com", "/orders", "/shipments", "/v1/payments"
                )
        );
        return new MercadoLivreSettlementClient(
                credentialService,
                properties.mercadoLivre(),
                new MercadoLivreSettlementResponseMapper(),
                restClientBuilder
        );
    }
}
