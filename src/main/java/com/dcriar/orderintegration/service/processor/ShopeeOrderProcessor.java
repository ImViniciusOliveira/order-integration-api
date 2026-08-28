package com.dcriar.orderintegration.service.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Processador de pedidos especializado para o marketplace Shopee.
 * <p>
 * Extrai campos específicos da API da Shopee (order_sn, status, tracking_no, estimated_shipping_fee)
 * e normaliza para o modelo unificado de domínio.
 */
@Slf4j
@Component
public class ShopeeOrderProcessor implements MarketplaceOrderProcessor {

    public static final String PLATFORM_CODE = "SHOPEE";

    @Override
    public boolean supports(String platform) {
        return PLATFORM_CODE.equalsIgnoreCase(platform);
    }

    @Override
    public OrderProcessingResult process(String shopId, Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            throw new IllegalArgumentException("Payload do webhook da Shopee não pode ser nulo ou vazio");
        }

        String orderSn = extractString(payload, "ordersn", "order_sn", "ordersn_id");
        if (orderSn == null || orderSn.isBlank()) {
            throw new IllegalArgumentException("Campo 'order_sn' obrigatório não encontrado no payload da Shopee");
        }

        String resolvedShopId = shopId != null && !shopId.isBlank()
                ? shopId
                : extractString(payload, "shop_id", "shopid", "shopId");

        String rawStatus = extractString(payload, "order_status", "status", "current_status");
        String normalizedStatus = normalizeShopeeStatus(rawStatus);

        String trackingNo = extractString(payload, "tracking_no", "tracking_number", "shipping_document_number");
        BigDecimal estimatedShippingFee = extractBigDecimal(payload, "estimated_shipping_fee", "actual_shipping_fee");
        BigDecimal escrowAmount = extractBigDecimal(payload, "escrow_amount", "escrow_tax_amount");
        BigDecimal shippingFeeBorneBySeller = extractBigDecimal(payload, "shipping_fee_borne_by_seller");

        log.debug("Processado evento Shopee para pedido {}: status={}, rastreio={}", orderSn, normalizedStatus, trackingNo);

        return new OrderProcessingResult(
                PLATFORM_CODE,
                resolvedShopId,
                orderSn,
                normalizedStatus,
                trackingNo,
                estimatedShippingFee,
                escrowAmount,
                shippingFeeBorneBySeller,
                payload
        );
    }

    private String normalizeShopeeStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return "UNKNOWN";
        }
        return switch (rawStatus.trim().toUpperCase()) {
            case "UNPAID" -> "UNPAID";
            case "READY_TO_SHIP", "TO_SHIP" -> "READY_TO_SHIP";
            case "PROCESSED", "SHIPPED", "IN_TRANSIT" -> "SHIPPED";
            case "TO_CONFIRM_RECEIVE", "DELIVERED", "COMPLETED" -> "COMPLETED";
            case "IN_CANCEL", "CANCELLED", "CANCELED" -> "CANCELLED";
            case "TO_RETURN", "RETURNED" -> "RETURNED";
            default -> rawStatus.trim().toUpperCase();
        };
    }

    private String extractString(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val != null) {
                return String.valueOf(val).trim();
            }
        }
        return null;
    }

    private BigDecimal extractBigDecimal(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val != null) {
                try {
                    return new BigDecimal(String.valueOf(val).trim());
                } catch (NumberFormatException ignored) {
                    log.warn("Falha ao converter campo '{}' com valor '{}' para BigDecimal", key, val);
                }
            }
        }
        return null;
    }
}
