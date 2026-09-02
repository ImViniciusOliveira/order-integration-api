package com.dcriar.orderintegration.domain.order.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Resultado financeiro neutro produzido por um client de marketplace.
 * <p>
 * Este modelo transporta os dados externos até os calculadores financeiros já existentes,
 * sem duplicar regras de comissão ou de prova real.
 *
 * @param status                 situação da consulta financeira
 * @param platform               código normalizado da plataforma
 * @param orderId                identificador do pedido na plataforma
 * @param accountId              identificador da loja ou vendedor na plataforma
 * @param grossAmount            valor bruto da venda, quando informado
 * @param netAmount              valor líquido efetivamente liberado
 * @param commissionAmount       comissão informada pela plataforma
 * @param transactionFee         taxa de transação ou processamento
 * @param shippingFee            frete debitado do vendedor
 * @param externalFees           demais taxas informadas pela plataforma
 * @param financialDetails       detalhamento financeiro externo normalizado
 * @param queriedAt              instante da consulta externa
 * @param pendingReason          motivo da indisponibilidade ou falha da consulta
 */
public record MarketplaceSettlement(
        SettlementStatus status,
        String platform,
        String orderId,
        String accountId,
        BigDecimal grossAmount,
        BigDecimal netAmount,
        BigDecimal commissionAmount,
        BigDecimal transactionFee,
        BigDecimal shippingFee,
        BigDecimal externalFees,
        Map<String, Object> financialDetails,
        OffsetDateTime queriedAt,
        String pendingReason
) {
}
