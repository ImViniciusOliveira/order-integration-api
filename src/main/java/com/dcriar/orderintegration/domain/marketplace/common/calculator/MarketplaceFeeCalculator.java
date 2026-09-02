package com.dcriar.orderintegration.domain.marketplace.common.calculator;

import com.dcriar.orderintegration.domain.marketplace.common.calculator.model.FeeCalculationResult;
import com.dcriar.orderintegration.domain.order.entity.OrderMaster;

import java.math.BigDecimal;

/**
 * Interface Strategy para cálculo matemático de comissões, tarifas e prova real de repasse
 * financeiro específico para cada marketplace e modelo tributário integrado.
 */
public interface MarketplaceFeeCalculator {

    /**
     * Verifica se esta estratégia suporta a plataforma informada.
     *
     * @param platform código identificador da plataforma (ex: "SHOPEE")
     * @return {@code true} se a estratégia atende à plataforma, {@code false} caso contrário
     */
    boolean supports(String platform);

    /**
     * Executa a auditoria contábil e a prova real financeira confrontando o valor teórico
     * calculado com o extrato definitivo de liquidação do marketplace.
     *
     * @param order                   pedido mestre contendo os metadados brutos e itens comprados
     * @param actualEscrowAmount      valor líquido real que o marketplace informou no extrato
     * @param actualSellerShippingFee custo de frete real debitado da conta do vendedor (se houver)
     * @return resultado consolidado da auditoria contábil com detalhamento de taxas e eventuais divergências
     */
    FeeCalculationResult calculate(OrderMaster order, BigDecimal actualEscrowAmount, BigDecimal actualSellerShippingFee);
}
