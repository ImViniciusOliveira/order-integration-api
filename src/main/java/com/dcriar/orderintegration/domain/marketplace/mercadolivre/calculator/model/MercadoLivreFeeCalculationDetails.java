package com.dcriar.orderintegration.domain.marketplace.mercadolivre.calculator.model;

import com.dcriar.orderintegration.domain.marketplace.common.calculator.model.FeeCalculationDetails;

import java.math.BigDecimal;

/**
 * Detalhes das tarifas exclusivas da regra do Mercado Livre Brasil.
 *
 * @param saleFee comissão total informada pela API
 * @param percentageFee percentual informado pela API, quando disponível
 * @param fixedFee tarifa fixa informada pela API, quando disponível
 * @param financingAddOnFee custo de financiamento informado pela API, quando disponível
 */
public record MercadoLivreFeeCalculationDetails(
        BigDecimal saleFee,
        BigDecimal percentageFee,
        BigDecimal fixedFee,
        BigDecimal financingAddOnFee
) implements FeeCalculationDetails {
}
