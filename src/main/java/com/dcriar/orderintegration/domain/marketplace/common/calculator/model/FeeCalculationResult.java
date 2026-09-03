package com.dcriar.orderintegration.domain.marketplace.common.calculator.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Record imutável que consolida o resultado completo da prova real contábil de um pedido.
 * <p>
 * Atua exclusivamente como carreador de dados 100% puro e anêmico, em estrita conformidade
 * com as regras de Clean Code e separação de responsabilidades do projeto.
 *
 * @param ruleVersion          versão e identificador da regra aplicada (ex: "SHOPEE_CPF_BR_2026")
 * @param auditDate            data e hora exata em que a auditoria financeira foi calculada
 * @param hasDivergence        sinaliza se houve divergência contábil acima da tolerância permitida
 * @param tolerance            margem de tolerância em centavos para compensar arredondamentos bancários
 * @param subtotalItems        somatório do valor dos itens comprados
 * @param totalQuantityItems   quantidade total agregada de unidades vendidas
 * @param totalMarketplaceFees somatório de todas as tarifas e comissões do marketplace
 * @param sellerShippingFee    custo de frete real debitado da conta do vendedor
 * @param theoreticalPayout    repasse líquido esperado segundo a modelagem matemática oficial
 * @param actualPayout         repasse líquido real informado no extrato de liquidação do marketplace
 * @param calculatedDifference diferença matemática apurada entre o repasse real e o esperado
 * @param auditedItems         relação detalhada de todos os itens e variações auditados
 * @param platformDetails      detalhes das tarifas exclusivos do marketplace
 * @param divergenceReason     justificativa textual da inconsistência detectada ou nulo se conciliado
 */
public record FeeCalculationResult(
        String ruleVersion,
        OffsetDateTime auditDate,
        boolean hasDivergence,
        BigDecimal tolerance,
        BigDecimal subtotalItems,
        int totalQuantityItems,
        BigDecimal totalMarketplaceFees,
        BigDecimal sellerShippingFee,
        BigDecimal theoreticalPayout,
        BigDecimal actualPayout,
        BigDecimal calculatedDifference,
        List<FeeCalculationItem> auditedItems,
        FeeCalculationDetails platformDetails,
        String divergenceReason
) {
}
