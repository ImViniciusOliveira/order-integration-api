package com.dcriar.orderintegration.service.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Processador de pedidos especializado para o marketplace TikTok Shop.
 * <p>
 * Extrai campos específicos da API do TikTok (order_id, order_status, tracking_number)
 * e normaliza para o modelo unificado de domínio.
 */
@Slf4j
@Component
public class TikTokOrderProcessor implements MarketplaceOrderProcessor {

    public static final String PLATFORM_CODE = "TIKTOK";

    @Override
    public boolean supports(String platform) {
        return PLATFORM_CODE.equalsIgnoreCase(platform);
    }

    @Override
    public OrderProcessingResult process(String shopId, Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            throw new IllegalArgumentException("Payload do webhook do TikTok não pode ser nulo ou vazio");
        }

        String orderSn = extractString(payload, "order_id", "order_sn", "orderId");
        if (orderSn == null || orderSn.isBlank()) {
            throw new IllegalArgumentException("Campo 'order_id' obrigatório não encontrado no payload do TikTok");
        }

        String resolvedShopId = shopId != null && !shopId.isBlank()
                ? shopId
                : extractString(payload, "shop_id", "seller_id", "shopId");

        String rawStatus = extractString(payload, "order_status", "status", "orderStatus");
        String normalizedStatus = normalizeTikTokStatus(rawStatus);

        String trackingNo = extractString(payload, "tracking_number", "tracking_no", "trackingNumber");
        BigDecimal estimatedShippingFee = extractBigDecimal(payload, "estimated_shipping_fee", "shipping_fee", "shippingFee");
        BigDecimal escrowAmount = extractBigDecimal(payload, "settlement_amount", "escrow_amount");
        BigDecimal shippingFeeBorneBySeller = extractBigDecimal(payload, "seller_shipping_fee", "shipping_fee_borne_by_seller");

        log.debug("Processado evento TikTok para pedido {}: status={}, rastreio={}", orderSn, normalizedStatus, trackingNo);

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

    private String normalizeTikTokStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return "UNKNOWN";
        }
        return switch (rawStatus.trim().toUpperCase()) {
            case "UNPAID" -> "UNPAID";
            case "AWAITING_SHIPMENT", "AWAITING_COLLECTION" -> "READY_TO_SHIP";
            case "IN_TRANSIT", "SHIPPED" -> "SHIPPED";
            case "DELIVERED", "COMPLETED" -> "COMPLETED";
            case "CANCELLED", "CANCELED" -> "CANCELLED";
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
