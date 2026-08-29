package com.dcriar.orderintegration.api.dto.filter;

import com.dcriar.orderintegration.domain.order.model.OrderFilterCriteria;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;

/**
 * DTO imutável de requisição para recebimento de filtros dinâmicos em consultas paginadas de pedidos.
 *
 * @param platform   filtro opcional por código do marketplace (ex: "SHOPEE")
 * @param shopId     filtro opcional por loja
 * @param status     filtro opcional por status do pedido
 * @param reconciled filtro opcional por estado de conciliação
 * @param orderSn    filtro opcional por número do pedido
 * @param trackingNo filtro opcional por código de rastreamento
 * @param startDate  data/hora inicial de criação (formato ISO 8601)
 * @param endDate    data/hora final de criação (formato ISO 8601)
 */
public record OrderFilterRequest(
        String platform,
        String shopId,
        String status,
        Boolean reconciled,
        String orderSn,
        String trackingNo,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        OffsetDateTime startDate,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        OffsetDateTime endDate
) {

    /**
     * Converte o DTO de filtro de requisição para o objeto de critérios de domínio.
     *
     * @return critérios de filtro de domínio para JPA Specifications
     */
    public OrderFilterCriteria toCriteria() {
        return new OrderFilterCriteria(
                platform != null && !platform.isBlank() ? platform.trim().toUpperCase() : null,
                shopId != null && !shopId.isBlank() ? shopId.trim() : null,
                status != null && !status.isBlank() ? status.trim().toUpperCase() : null,
                reconciled,
                orderSn != null && !orderSn.isBlank() ? orderSn.trim() : null,
                trackingNo != null && !trackingNo.isBlank() ? trackingNo.trim() : null,
                startDate,
                endDate
        );
    }
}
