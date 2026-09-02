package com.dcriar.orderintegration.domain.marketplace.shopee.settlement.client;

import com.dcriar.orderintegration.config.OrderIntegrationProperties;
import com.dcriar.orderintegration.domain.marketplace.shopee.credential.document.ShopeeCredentialDocument;
import com.dcriar.orderintegration.domain.marketplace.shopee.credential.service.ShopeeCredentialService;
import com.dcriar.orderintegration.domain.marketplace.common.model.MarketplaceSettlement;
import com.dcriar.orderintegration.domain.marketplace.common.model.SettlementStatus;
import com.dcriar.orderintegration.domain.marketplace.common.service.MarketplaceSettlementClient;
import com.dcriar.orderintegration.domain.marketplace.shopee.settlement.mapper.ShopeeSettlementResponseMapper;
import com.dcriar.orderintegration.domain.marketplace.shopee.settlement.signer.ShopeeRequestSigner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.Map;

/**
 * Client HTTP da Shopee para consulta do detalhe financeiro de escrow.
 * <p>
 * Coordena credenciais, assinatura, chamada HTTP e conversão da resposta,
 * mantendo essas responsabilidades em componentes especializados.
 */
@Slf4j
@Component
public class ShopeeSettlementClient implements MarketplaceSettlementClient {

    public static final String PLATFORM_CODE = "SHOPEE";

    private final ShopeeCredentialService credentialService;
    private final OrderIntegrationProperties.ShopeeProperties properties;
    private final ShopeeRequestSigner requestSigner;
    private final ShopeeSettlementResponseMapper responseMapper;
    private final RestClient restClient;

    /**
     * Cria o client Shopee com suas dependências de integração.
     *
     * @param credentialService serviço de credenciais Shopee
     * @param properties        propriedades globais da aplicação
     * @param requestSigner     gerador de assinatura Shopee
     * @param responseMapper    conversor da resposta Shopee
     */
    public ShopeeSettlementClient(
            ShopeeCredentialService credentialService,
            OrderIntegrationProperties properties,
            ShopeeRequestSigner requestSigner,
            ShopeeSettlementResponseMapper responseMapper
    ) {
        this.credentialService = credentialService;
        this.properties = properties.shopee();
        this.requestSigner = requestSigner;
        this.responseMapper = responseMapper;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public boolean supports(String platform) {
        return PLATFORM_CODE.equalsIgnoreCase(platform);
    }

    @Override
    public MarketplaceSettlement fetchSettlement(String accountId, String orderId) {
        validateInput(accountId, orderId);

        ShopeeCredentialDocument credential = credentialService.getCredential(accountId);
        validateCredential(credential);

        long timestamp = Instant.now().getEpochSecond();
        String signature = requestSigner.sign(
                credential.getPartnerId(),
                properties.escrowPath(),
                timestamp,
                credential.getLiveAccessToken(),
                credential.getShopId(),
                credential.getLivePartnerKey()
        );

        try {
            String uri = UriComponentsBuilder.fromUriString(properties.baseUrl())
                    .path(properties.escrowPath())
                    .queryParam("order_sn", orderId)
                    .queryParam("partner_id", credential.getPartnerId())
                    .queryParam("timestamp", timestamp)
                    .queryParam("access_token", credential.getLiveAccessToken())
                    .queryParam("shop_id", credential.getShopId())
                    .queryParam("sign", signature)
                    .build()
                    .encode()
                    .toUriString();

            Map<String, Object> body = restClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Map.class);
            return responseMapper.map(accountId, orderId, body);
        } catch (RestClientResponseException exception) {
            SettlementStatus status = exception.getStatusCode().is5xxServerError()
                    || exception.getStatusCode().value() == 429
                    ? SettlementStatus.RETRYABLE_ERROR
                    : SettlementStatus.PERMANENT_ERROR;
            return unavailable(accountId, orderId, status,
                    "Shopee respondeu HTTP " + exception.getStatusCode().value());
        } catch (ResourceAccessException exception) {
            return unavailable(accountId, orderId, SettlementStatus.RETRYABLE_ERROR,
                    "Falha de comunicação com a Shopee");
        }
    }

    private MarketplaceSettlement unavailable(
            String accountId,
            String orderId,
            SettlementStatus status,
            String reason
    ) {
        MarketplaceSettlement settlement = responseMapper.unavailable(accountId, orderId, reason);
        return new MarketplaceSettlement(
                status,
                settlement.platform(),
                settlement.orderId(),
                settlement.accountId(),
                settlement.grossAmount(),
                settlement.netAmount(),
                settlement.commissionAmount(),
                settlement.transactionFee(),
                settlement.shippingFee(),
                settlement.externalFees(),
                settlement.financialDetails(),
                settlement.queriedAt(),
                settlement.pendingReason()
        );
    }

    private void validateInput(String accountId, String orderId) {
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("O shop_id da Shopee é obrigatório");
        }
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("O order_sn da Shopee é obrigatório");
        }
    }

    private void validateCredential(ShopeeCredentialDocument credential) {
        if (credential.getPartnerId() == null || credential.getPartnerId().isBlank()
                || credential.getLivePartnerKey() == null || credential.getLivePartnerKey().isBlank()
                || credential.getLiveAccessToken() == null || credential.getLiveAccessToken().isBlank()
                || credential.getShopId() == null || credential.getShopId().isBlank()) {
            throw new IllegalStateException("Credencial Shopee incompleta para consulta de escrow");
        }
    }
}
