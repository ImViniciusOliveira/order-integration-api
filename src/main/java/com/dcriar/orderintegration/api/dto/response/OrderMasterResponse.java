package com.dcriar.orderintegration.api.dto.response;

import com.dcriar.orderintegration.domain.order.model.FinancialAuditStatus;
import io.swagger.v3.oas.annotations.media.Schema;

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
 * @param financialAuditStatus     estado detalhado da auditoria financeira
 * @param metadata                 detalhes dinâmicos de itens, SKUs e metadados adicionais
 * @param createdAt                data e hora de ingestão do pedido
 * @param updatedAt                data e hora da última atualização
 */
@Schema(description = "Representação consolidada da visão mestre de um pedido integrado")
public record OrderMasterResponse(
        @Schema(description = "Identificador único interno do pedido", example = "1")
        Long id,

        @Schema(description = "Plataforma/marketplace de origem", example = "SHOPEE")
        String platform,

        @Schema(description = "Identificador da loja no marketplace", example = "12345678")
        String shopId,

        @Schema(description = "Número único do pedido no marketplace", example = "260828ABC123XYZ")
        String orderSn,

        @Schema(description = "Status unificado do pedido", example = "READY_TO_SHIP")
        String status,

        @Schema(description = "Código de rastreio logístico", example = "BR2608289999X")
        String trackingNo,

        @Schema(description = "Frete estimado inicialmente", example = "15.50")
        BigDecimal estimatedShippingFee,

        @Schema(description = "Repasse líquido definitivo liberado após entrega (Escrow)", example = "84.50")
        BigDecimal escrowAmount,

        @Schema(description = "Custo real de frete cobrado do vendedor", example = "0.00")
        BigDecimal shippingFeeBorneBySeller,

        @Schema(description = "Indica se a conciliação financeira definitiva já foi processada", example = "false")
        boolean reconciled,

        @Schema(description = "Estado detalhado da auditoria financeira", example = "PENDING_SETTLEMENT")
        FinancialAuditStatus financialAuditStatus,

        @Schema(description = "Detalhes dinâmicos de itens, SKUs e metadados adicionais", example = "{\"order_sn\":\"260828ABC123XYZ\",\"items\":[{\"item_name\":\"Camiseta\",\"quantity\":1}]}")
        Map<String, Object> metadata,

        @Schema(description = "Data e hora de ingestão do pedido", example = "2026-08-29T10:44:26.273835Z")
        OffsetDateTime createdAt,

        @Schema(description = "Data e hora da última atualização", example = "2026-08-29T10:44:26.273835Z")
        OffsetDateTime updatedAt
) {
}
