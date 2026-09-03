package com.dcriar.orderintegration.domain.marketplace.shopee.calculator.model;

import com.dcriar.orderintegration.domain.marketplace.common.calculator.model.FeeCalculationDetails;

import java.math.BigDecimal;

/**
 * Detalhes das tarifas exclusivas da regra Shopee CPF Brasil.
 *
 * @param commissionFee comissão oficial retornada pela Shopee
 * @param serviceFee taxa de serviço oficial retornada pela Shopee
 * @param sellerTransactionFee taxa de transação do vendedor
 * @param otherFees demais taxas oficiais identificadas
 */
public record ShopeeFeeCalculationDetails(
        BigDecimal commissionFee,
        BigDecimal serviceFee,
        BigDecimal sellerTransactionFee,
        BigDecimal otherFees
) implements FeeCalculationDetails {
}
