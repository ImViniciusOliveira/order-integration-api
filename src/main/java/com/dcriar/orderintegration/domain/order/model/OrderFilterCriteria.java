package com.dcriar.orderintegration.domain.order.model;

import java.time.OffsetDateTime;

/**
 * Critérios de busca e filtros dinâmicos para consulta de pedidos mestre.
 *
 * @param platform   código da plataforma para filtro (ex: "SHOPEE")
 * @param shopId     identificador da loja
 * @param status     status do pedido
 * @param reconciled indicador de conciliação concluída
 * @param financialAuditStatus estado detalhado da auditoria financeira
 * @param orderSn    termo de busca por número do pedido
 * @param trackingNo termo de busca por código de rastreio
 * @param startDate  data inicial de criação
 * @param endDate    data final de criação
 */
public record OrderFilterCriteria(
        String platform,
        String shopId,
        String status,
        Boolean reconciled,
        FinancialAuditStatus financialAuditStatus,
        String orderSn,
        String trackingNo,
        OffsetDateTime startDate,
        OffsetDateTime endDate
) {
}
