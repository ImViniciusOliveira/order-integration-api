package com.dcriar.orderintegration.domain.marketplace.shopee.settlement.client;

import com.dcriar.orderintegration.config.OrderIntegrationProperties;
import com.dcriar.orderintegration.domain.marketplace.shopee.credential.document.ShopeeCredentialDocument;
import com.dcriar.orderintegration.domain.marketplace.shopee.credential.service.ShopeeCredentialService;
import com.dcriar.orderintegration.domain.order.model.MarketplaceSettlement;
import com.dcriar.orderintegration.domain.order.model.SettlementStatus;
import com.dcriar.orderintegration.domain.order.service.MarketplaceSettlementClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Client HTTP da Shopee para consulta do detalhe financeiro de escrow.
 * <p>
 * Converte a resposta específica da Shopee em {@link MarketplaceSettlement}, deixando
 * as regras matemáticas sob responsabilidade do calculador financeiro da plataforma.
 */
@Slf4j
@Component
public class ShopeeSettlementClient implements MarketplaceSettlementClient {

    public static final String PLATFORM_CODE = "SHOPEE";

    private final ShopeeCredentialService credentialService;
    private final OrderIntegrationProperties.ShopeeProperties properties;
    private final RestClient restClient;

    /**
     * Cria o client da Shopee com as credenciais e configurações tipadas da aplicação.
     *
     * @param credentialService serviço de credenciais Shopee
     * @param properties        configurações HTTP da Shopee
     */
    public ShopeeSettlementClient(
            ShopeeCredentialService credentialService,
            OrderIntegrationProperties properties
    ) {
        this.credentialService = credentialService;
        this.properties = properties.shopee();
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
        String signature = generateSignature(
                credential.getPartnerId(),
                properties.escrowPath(),
                timestamp,
                credential.getLiveAccessToken(),
                credential.getShopId(),
                credential.getLivePartnerKey()
        );

        Map<String, Object> body;
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

            body = restClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientResponseException exception) {
            SettlementStatus status = exception.getStatusCode().is5xxServerError()
                    || exception.getStatusCode().value() == 429
                    ? SettlementStatus.RETRYABLE_ERROR
                    : SettlementStatus.PERMANENT_ERROR;
            return unavailableSettlement(accountId, orderId, status,
                    "Shopee respondeu HTTP " + exception.getStatusCode().value());
        } catch (ResourceAccessException exception) {
            return unavailableSettlement(accountId, orderId, SettlementStatus.RETRYABLE_ERROR,
                    "Falha de comunicação com a Shopee");
        }

        return mapResponse(accountId, orderId, body);
    }

    static String generateSignature(
            String partnerId,
            String apiPath,
            long timestamp,
            String accessToken,
            String shopId,
            String partnerKey
    ) {
        try {
            String baseString = partnerId + apiPath + timestamp + accessToken + shopId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(partnerKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(baseString.getBytes(StandardCharsets.UTF_8));
            StringBuilder signature = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                signature.append(String.format("%02x", value));
            }
            return signature.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível gerar a assinatura da Shopee", exception);
        }
    }

    private MarketplaceSettlement mapResponse(String accountId, String orderId, Map<String, Object> body) {
        Map<String, Object> response = mapValue(body, "response");
        if (response == null || response.isEmpty()) {
            return unavailableSettlement(accountId, orderId, SettlementStatus.PENDING,
                    "Detalhes de escrow ainda não disponíveis");
        }

        BigDecimal netAmount = decimalValue(response, "escrow_amount");
        if (netAmount == null || netAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return unavailableSettlement(accountId, orderId, SettlementStatus.PENDING,
                    "Escrow ainda não liberado pela Shopee");
        }

        Map<String, Object> incomeDetails = mapValue(response, "income_details");
        BigDecimal shippingFee = decimalValue(incomeDetails, "shipping_fee_borne_by_seller");
        Map<String, Object> financialDetails = new LinkedHashMap<>(response);
        if (incomeDetails != null) {
            financialDetails.put("income_details", incomeDetails);
        }

        return new MarketplaceSettlement(
                SettlementStatus.AVAILABLE,
                PLATFORM_CODE,
                orderId,
                accountId,
                decimalValue(response, "buyer_total_amount"),
                netAmount,
                decimalValue(response, "commission_fee"),
                decimalValue(response, "transaction_fee"),
                shippingFee != null ? shippingFee : BigDecimal.ZERO,
                null,
                financialDetails,
                OffsetDateTime.now(),
                null
        );
    }

    private MarketplaceSettlement unavailableSettlement(
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
                OffsetDateTime.now(),
                reason
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

    private Map<String, Object> mapValue(Map<String, Object> source, String key) {
        if (source == null) {
            return Map.of();
        }
        Object value = source.get(key);
        return value instanceof Map<?, ?> map ? castMap(map) : Map.of();
    }

    private Map<String, Object> castMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
    }

    private BigDecimal decimalValue(Map<String, Object> source, String key) {
        if (source == null) {
            return null;
        }
        Object value = source.get(key);
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException exception) {
            log.warn("Valor financeiro inválido na resposta Shopee para o campo '{}'", key);
            return null;
        }
    }
}
