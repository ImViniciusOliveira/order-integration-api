package com.dcriar.orderintegration.domain.marketplace.mercadolivre.settlement.client;

import com.dcriar.orderintegration.config.OrderIntegrationProperties;
import com.dcriar.orderintegration.domain.marketplace.common.model.MarketplaceSettlement;
import com.dcriar.orderintegration.domain.marketplace.common.model.SettlementStatus;
import com.dcriar.orderintegration.domain.marketplace.common.service.MarketplaceSettlementClient;
import com.dcriar.orderintegration.domain.marketplace.mercadolivre.credential.document.MercadoLivreCredentialDocument;
import com.dcriar.orderintegration.domain.marketplace.mercadolivre.credential.service.MercadoLivreCredentialService;
import com.dcriar.orderintegration.domain.marketplace.mercadolivre.settlement.mapper.MercadoLivreSettlementResponseMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * Client HTTP do Mercado Livre para consolidar pedido, envio e pagamento.
 */
@Component
public class MercadoLivreSettlementClient implements MarketplaceSettlementClient {

    public static final String PLATFORM_CODE = "MERCADOLIVRE";

    private final MercadoLivreCredentialService credentialService;
    private final OrderIntegrationProperties.MercadoLivreProperties properties;
    private final MercadoLivreSettlementResponseMapper responseMapper;
    private final RestClient restClient;

    /**
     * Cria o client do Mercado Livre.
     *
     * @param credentialService serviço de credenciais
     * @param properties        propriedades globais da aplicação
     * @param responseMapper    mapper das respostas externas
     * @param restClientBuilder  builder HTTP global da aplicação
     */
    public MercadoLivreSettlementClient(
            MercadoLivreCredentialService credentialService,
            OrderIntegrationProperties properties,
            MercadoLivreSettlementResponseMapper responseMapper,
            RestClient.Builder restClientBuilder
    ) {
        this.credentialService = credentialService;
        this.properties = properties.mercadoLivre();
        this.responseMapper = responseMapper;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public boolean supports(String platform) {
        return PLATFORM_CODE.equalsIgnoreCase(platform);
    }

    @Override
    public MarketplaceSettlement fetchSettlement(String accountId, String orderId) {
        validateInput(accountId, orderId);
        MercadoLivreCredentialDocument credential = credentialService.getCredential(accountId);
        validateCredential(credential);

        try {
            Map<String, Object> order = get(
                    properties.ordersPath() + "/" + orderId,
                    credential.getLiveAccessToken()
            );
            Map<String, Object> shipment = fetchShipment(order, credential.getLiveAccessToken());
            Map<String, Object> payment = fetchPayment(order, credential.getLiveAccessToken());
            return responseMapper.map(accountId, orderId, order, shipment, payment);
        } catch (RestClientResponseException exception) {
            SettlementStatus status = exception.getStatusCode().is5xxServerError()
                    || exception.getStatusCode().value() == 429
                    ? SettlementStatus.RETRYABLE_ERROR
                    : SettlementStatus.PERMANENT_ERROR;
            return unavailable(accountId, orderId, status,
                    "Mercado Livre respondeu HTTP " + exception.getStatusCode().value());
        } catch (ResourceAccessException exception) {
            return unavailable(accountId, orderId, SettlementStatus.RETRYABLE_ERROR,
                    "Falha de comunicação com o Mercado Livre");
        }
    }

    private Map<String, Object> fetchShipment(
            Map<String, Object> order,
            String accessToken
    ) {
        String shipmentId = stringValue(order, "shipping", "id");
        return shipmentId == null
                ? Map.of()
                : get(properties.shipmentsPath() + "/" + shipmentId, accessToken);
    }

    private Map<String, Object> fetchPayment(
            Map<String, Object> order,
            String accessToken
    ) {
        Object payments = order.get("payments");
        if (!(payments instanceof java.util.List<?> list) || list.isEmpty()
                || !(list.getFirst() instanceof Map<?, ?> payment)) {
            return Map.of();
        }
        Object paymentId = payment.get("id");
        return paymentId == null
                ? Map.of()
                : get(properties.paymentsPath() + "/" + paymentId, accessToken);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> get(String path, String accessToken) {
        return restClient.get()
                .uri(UriComponentsBuilder.fromUriString(properties.baseUrl()).path(path).build().toUri())
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(Map.class);
    }

    private MarketplaceSettlement unavailable(
            String accountId,
            String orderId,
            SettlementStatus status,
            String reason
    ) {
        return new MarketplaceSettlement(
                status,
                PLATFORM_CODE,
                orderId,
                accountId,
                null,
                null,
                null,
                null,
                null,
                null,
                Map.of(),
                java.time.OffsetDateTime.now(),
                reason
        );
    }

    private void validateInput(String accountId, String orderId) {
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("O identificador da conta Mercado Livre é obrigatório");
        }
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("O order_id do Mercado Livre é obrigatório");
        }
    }

    private void validateCredential(MercadoLivreCredentialDocument credential) {
        if (credential == null
                || credential.getLiveAccessToken() == null
                || credential.getLiveAccessToken().isBlank()) {
            throw new IllegalStateException("Credencial Mercado Livre incompleta para consulta financeira");
        }
    }

    private String stringValue(Map<String, Object> source, String... path) {
        Object value = source;
        for (String key : path) {
            if (!(value instanceof Map<?, ?> map)) {
                return null;
            }
            value = map.get(key);
        }
        return value == null ? null : value.toString();
    }
}
