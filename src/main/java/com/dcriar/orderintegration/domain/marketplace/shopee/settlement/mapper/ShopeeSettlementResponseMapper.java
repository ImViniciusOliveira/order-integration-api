package com.dcriar.orderintegration.domain.marketplace.shopee.settlement.mapper;

import com.dcriar.orderintegration.domain.marketplace.common.model.MarketplaceSettlement;
import com.dcriar.orderintegration.domain.marketplace.common.model.SettlementStatus;
import com.dcriar.orderintegration.domain.marketplace.shopee.settlement.client.ShopeeSettlementClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converte a resposta financeira heterogênea da Shopee para o modelo interno de settlement.
 */
@Component
public class ShopeeSettlementResponseMapper {

    /**
     * Mapeia a resposta da Shopee e identifica se o escrow está disponível.
     *
     * @param accountId identificador da loja Shopee
     * @param orderId   identificador do pedido
     * @param body      corpo JSON retornado pela Shopee
     * @return settlement normalizado
     */
    public MarketplaceSettlement map(String accountId, String orderId, Map<String, Object> body) {
        Map<String, Object> response = mapValue(body, "response");
        if (response.isEmpty()) {
            return unavailable(accountId, orderId, "Detalhes de escrow ainda não disponíveis");
        }

        BigDecimal netAmount = decimalValue(response, "escrow_amount");
        if (netAmount == null || netAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return unavailable(accountId, orderId, "Escrow ainda não liberado pela Shopee");
        }

        Map<String, Object> incomeDetails = mapValue(response, "income_details");
        Map<String, Object> financialDetails = new LinkedHashMap<>(response);
        if (!incomeDetails.isEmpty()) {
            financialDetails.put("income_details", incomeDetails);
        }

        BigDecimal shippingFee = decimalValue(incomeDetails, "shipping_fee_borne_by_seller");
        return new MarketplaceSettlement(
                SettlementStatus.AVAILABLE,
                ShopeeSettlementClient.PLATFORM_CODE,
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

    /**
     * Cria um resultado de settlement indisponível para reagendamento ou tratamento de erro.
     *
     * @param accountId identificador da loja Shopee
     * @param orderId   identificador do pedido
     * @param reason    motivo da indisponibilidade
     * @return settlement pendente
     */
    public MarketplaceSettlement unavailable(String accountId, String orderId, String reason) {
        return new MarketplaceSettlement(
                SettlementStatus.PENDING,
                ShopeeSettlementClient.PLATFORM_CODE,
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

    private Map<String, Object> mapValue(Map<String, Object> source, String key) {
        if (source == null) {
            return Map.of();
        }
        Object value = source.get(key);
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((mapKey, mapValue) -> result.put(String.valueOf(mapKey), mapValue));
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
            return null;
        }
    }
}
