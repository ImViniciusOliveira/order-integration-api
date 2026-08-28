package com.dcriar.orderintegration.domain.order.model;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Representação padronizada do resultado de extração e processamento de eventos brutos de qualquer marketplace.
 *
 * @param shopId               identificador da loja no canal de venda
 * @param orderSn              número único do pedido no marketplace
 * @param status               status normalizado do pedido
 * @param trackingNo           código de rastreio logístico
 * @param estimatedShippingFee taxa de frete estimada
 * @param metadata             atributos e metadados dinâmicos
 */
public record OrderProcessingResult(
        String shopId,
        String orderSn,
        String status,
        String trackingNo,
        BigDecimal estimatedShippingFee,
        Map<String, Object> metadata
) {
}
