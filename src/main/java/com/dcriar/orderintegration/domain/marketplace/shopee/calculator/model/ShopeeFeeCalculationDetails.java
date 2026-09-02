package com.dcriar.orderintegration.domain.marketplace.shopee.calculator.model;

import com.dcriar.orderintegration.domain.marketplace.common.calculator.model.FeeCalculationDetails;

import java.math.BigDecimal;

/**
 * Detalhes das tarifas exclusivas da regra Shopee CPF Brasil.
 *
 * @param baseCommission14 comissão base de 14%
 * @param transactionFee6 taxa de transação de 6%
 * @param fixedItemFee4 tarifa fixa por unidade
 * @param lowValueSurcharge5 sobretaxa por item abaixo do limite de baixo valor
 */
public record ShopeeFeeCalculationDetails(
        BigDecimal baseCommission14,
        BigDecimal transactionFee6,
        BigDecimal fixedItemFee4,
        BigDecimal lowValueSurcharge5
) implements FeeCalculationDetails {
}
