package com.dcriar.orderintegration.domain.marketplace.mercadolivre.settlement.oauth;

import com.dcriar.orderintegration.config.OrderIntegrationProperties;
import com.dcriar.orderintegration.domain.marketplace.mercadolivre.credential.document.MercadoLivreCredentialDocument;
import com.dcriar.orderintegration.domain.marketplace.mercadolivre.credential.service.MercadoLivreCredentialService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

/**
 * Testes unitários do refresh OAuth2 do Mercado Livre.
 */
class MercadoLivreTokenClientTest {

    @Test
    @DisplayName("Deve manter credencial quando o token ainda está válido")
    void deveManterCredencialValida() {
        MercadoLivreCredentialService credentialService = mock(MercadoLivreCredentialService.class);
        MercadoLivreTokenClient client = newClient(RestClient.builder(), credentialService);
        MercadoLivreCredentialDocument credential = credential("9999999999");

        assertThat(client.ensureValid(credential)).isSameAs(credential);
    }

    @Test
    @DisplayName("Deve reconhecer vencimento legado salvo em milissegundos")
    void deveReconhecerVencimentoLegadoEmMilissegundos() {
        MercadoLivreCredentialService credentialService = mock(MercadoLivreCredentialService.class);
        MercadoLivreTokenClient client = newClient(RestClient.builder(), credentialService);
        MercadoLivreCredentialDocument credential = credential(
                Long.toString((System.currentTimeMillis() / 1000) + 3600).concat("000")
        );

        assertThat(client.ensureValid(credential)).isSameAs(credential);
    }

    @Test
    @DisplayName("Deve renovar e persistir token expirado")
    void deveRenovarTokenExpirado() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MercadoLivreCredentialService credentialService = mock(MercadoLivreCredentialService.class);
        MercadoLivreCredentialDocument credential = credential("1000");
        MercadoLivreCredentialDocument updatedCredential = credential("2000");
        when(credentialService.updateTokens(
                eq(credential), eq("new-access-token"), eq("new-refresh-token"), anyLong()
        )).thenReturn(updatedCredential);

        server.expect(requestTo("https://api.mercadolibre.com/oauth/token"))
                .andExpect(method(POST))
                .andRespond(withSuccess(
                        "{\"access_token\":\"new-access-token\",\"refresh_token\":\"new-refresh-token\",\"expires_in\":21600}",
                        MediaType.APPLICATION_JSON
                ));

        MercadoLivreCredentialDocument result = newClient(builder, credentialService).ensureValid(credential);

        assertThat(result).isSameAs(updatedCredential);
        verify(credentialService).updateTokens(
                eq(credential), eq("new-access-token"), eq("new-refresh-token"), anyLong()
        );
        server.verify();
    }

    private MercadoLivreCredentialDocument credential(String expirationEpoch) {
        return MercadoLivreCredentialDocument.builder()
                .clientId("client-id")
                .clientSecret("client-secret")
                .liveRefreshToken("refresh-token")
                .vencimentoTokenTs(expirationEpoch)
                .build();
    }

    private MercadoLivreTokenClient newClient(
            RestClient.Builder builder,
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
                        "/api/v2/payment/get_escrow_detail",
                        "/api/v2/auth/access_token/get"
                ),
                new OrderIntegrationProperties.MercadoLivreProperties(
                        "https://api.mercadolibre.com",
                        "/orders",
                        "/shipments",
                        "/v1/payments",
                        "/oauth/token"
                )
        );
        return new MercadoLivreTokenClient(credentialService, properties, builder);
    }
}
