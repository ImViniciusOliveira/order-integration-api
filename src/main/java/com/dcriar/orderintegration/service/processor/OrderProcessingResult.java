package com.dcriar.orderintegration.service.processor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Resultado padronizado da extração de dados de pedidos processados por um {@link MarketplaceOrderProcessor}.
 *
 * @param platform                  código da plataforma (ex: "SHOPEE", "TIKTOK")
 * @param shopId                    identificador da loja no marketplace
 * @param orderSn                   código único do pedido
 * @param status                    status normalizado do pedido (ex: "READY_TO_SHIP", "COMPLETED")
 * @param trackingNo                código de rastreio da transportadora (se presente)
 * @param estimatedShippingFee      taxa de frete estimada provisionada no momento do despacho
 * @param escrowAmount              valor líquido repassado no extrato financeiro (se disponível)
 * @param shippingFeeBorneBySeller  taxa real de frete cobrada do vendedor no extrato (se disponível)
 * @param metadata                  dados brutos ou adicionais específicos da plataforma para persistência em JSONB
 */
public record OrderProcessingResult(
        String platform,
        String shopId,
        String orderSn,
        String status,
        String trackingNo,
        BigDecimal estimatedShippingFee,
        BigDecimal escrowAmount,
        BigDecimal shippingFeeBorneBySeller,
        Map<String, Object> metadata
) {
}
