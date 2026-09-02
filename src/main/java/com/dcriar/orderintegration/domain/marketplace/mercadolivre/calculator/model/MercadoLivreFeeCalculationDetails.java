package com.dcriar.orderintegration.domain.marketplace.mercadolivre.calculator.model;

import com.dcriar.orderintegration.domain.marketplace.common.calculator.model.FeeCalculationDetails;

import java.math.BigDecimal;

/**
 * Detalhes das tarifas exclusivas da regra do Mercado Livre Brasil.
 *
 * @param saleFee comissão total informada pela API, quando disponível
 * @param defaultCommission comissão calculada pela regra contingencial
 * @param fixedUnitFee tarifa fixa unitária aplicada pela regra
 */
public record MercadoLivreFeeCalculationDetails(
        BigDecimal saleFee,
        BigDecimal defaultCommission,
        BigDecimal fixedUnitFee
) implements FeeCalculationDetails {
}
