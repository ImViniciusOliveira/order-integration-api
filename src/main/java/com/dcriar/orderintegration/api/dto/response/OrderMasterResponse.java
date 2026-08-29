package com.dcriar.orderintegration.api.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * DTO imutável de resposta consolidada contendo todos os dados mestres e financeiros de um pedido integrado.
 *
 * @param id                       identificador único interno do pedido
 * @param platform                 plataforma/marketplace de origem (ex: "SHOPEE")
 * @param shopId                   identificador da loja no marketplace
 * @param orderSn                  número único do pedido no marketplace
 * @param status                   status unificado do pedido (ex: "READY_TO_SHIP", "COMPLETED")
 * @param trackingNo               código de rastreio logístico
 * @param estimatedShippingFee     frete estimado inicialmente
 * @param escrowAmount             repasse líquido definitivo liberado após entrega (Escrow)
 * @param shippingFeeBorneBySeller custo real de frete cobrado do vendedor
 * @param reconciled               indica se a conciliação financeira definitiva já foi processada
 * @param metadata                 detalhes dinâmicos de itens, SKUs e metadados adicionais
 * @param createdAt                data e hora de ingestão do pedido
 * @param updatedAt                data e hora da última atualização
 */
public record OrderMasterResponse(
        Long id,
        String platform,
        String shopId,
        String orderSn,
        String status,
        String trackingNo,
        BigDecimal estimatedShippingFee,
        BigDecimal escrowAmount,
        BigDecimal shippingFeeBorneBySeller,
        boolean reconciled,
        Map<String, Object> metadata,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
