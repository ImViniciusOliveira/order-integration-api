package com.dcriar.orderintegration.api.dto.filter;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Filtros dinâmicos para consulta paginada de pedidos no Order Master")
public record OrderFilterRequest(
        @Schema(description = "Código do canal de marketplace", example = "SHOPEE")
        String platform,

        @Schema(description = "Identificador da loja na plataforma de origem", example = "12345678")
        String shopId,

        @Schema(description = "Status atual do pedido", example = "READY_TO_SHIP")
        String status,

        @Schema(description = "Filtro por pedidos já conciliados financeiramente", example = "false")
        Boolean reconciled,

        @Schema(description = "Código único do pedido no marketplace", example = "260828ABC123XYZ")
        String orderSn,

        @Schema(description = "Código de rastreio logístico", example = "BR2608289999X")
        String trackingNo,

        @Schema(description = "Data/hora inicial de criação (ISO 8601)", example = "2026-08-01T00:00:00-03:00")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        OffsetDateTime startDate,

        @Schema(description = "Data/hora final de criação (ISO 8601)", example = "2026-08-29T23:59:59-03:00")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        OffsetDateTime endDate
) {
}
