package com.dcriar.orderintegration.domain.marketplace.mercadolivre.processor;

import com.dcriar.orderintegration.domain.marketplace.common.model.OrderProcessingResult;
import com.dcriar.orderintegration.domain.marketplace.common.processor.MarketplaceOrderProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Implementação Strategy para extração e normalização de eventos do Mercado Livre Open API / Webhooks.
 */
@Slf4j
@Component
public class MercadoLivreOrderProcessor implements MarketplaceOrderProcessor {

    public static final String PLATFORM_CODE = "MERCADOLIVRE";

    @Override
    public boolean supports(String platform) {
        return PLATFORM_CODE.equalsIgnoreCase(platform);
    }

    @Override
    public OrderProcessingResult process(String shopId, Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            throw new IllegalArgumentException("Payload do Mercado Livre não pode ser nulo ou vazio");
        }

        // 1. Extrair número do pedido (order_sn / ordersn / id)
        String orderSn = extractString(payload, "ordersn", "order_sn", "orderSn", "id");
        if (orderSn == null || orderSn.isBlank()) {
            throw new IllegalArgumentException("O número do pedido (ordersn/order_sn/id) é obrigatório no payload do Mercado Livre");
        }

        // 2. Extrair shop_id / seller_id (se não vier no parâmetro externo)
        String resolvedShopId = (shopId != null && !shopId.isBlank())
                ? shopId
                : extractString(payload, "shop_id", "shopId", "seller_id", "sellerId");

        if (resolvedShopId == null) {
            resolvedShopId = "default";
        }

        // 3. Extrair status do pedido e normalizar
        String status = extractString(payload, "order_status", "status");
        if (status == null || status.isBlank()) {
            status = "UNKNOWN";
        }

        // 4. Extrair código de rastreamento se disponível
        String trackingNo = extractString(payload, "tracking_no", "tracking_number", "trackingNumber");

        // 5. Extrair taxa de frete estimada se disponível
        BigDecimal estimatedShippingFee = extractBigDecimal(payload, "estimated_shipping_fee", "estimated_shipping", "shipping_cost");

        log.debug("Processado evento Mercado Livre: orderSn={}, status={}, trackingNo={}, estimatedFee={}",
                orderSn, status, trackingNo, estimatedShippingFee);

        return new OrderProcessingResult(
                resolvedShopId,
                orderSn,
                status.toUpperCase(),
                trackingNo,
                estimatedShippingFee,
                payload
        );
    }

    private String extractString(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val != null && !val.toString().isBlank()) {
                return val.toString();
            }
        }
        return null;
    }

    private BigDecimal extractBigDecimal(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val != null) {
                try {
                    return new BigDecimal(val.toString());
                } catch (NumberFormatException e) {
                    log.warn("Falha ao converter valor '{}' para BigDecimal na chave '{}'", val, key);
                }
            }
        }
        return null;
    }
}
