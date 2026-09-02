package com.dcriar.orderintegration.domain.marketplace.shopee.calculator;

import com.dcriar.orderintegration.domain.marketplace.common.calculator.model.FeeCalculationResult;
import com.dcriar.orderintegration.domain.marketplace.shopee.calculator.model.ShopeeFeeCalculationDetails;
import com.dcriar.orderintegration.domain.order.entity.OrderMaster;
import com.dcriar.orderintegration.exception.custom.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes unitários para o motor de cálculo e prova real da Shopee CPF ({@link ShopeeCpfFeeCalculator}).
 */
class ShopeeCpfFeeCalculatorTest {

    private ShopeeCpfFeeCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new ShopeeCpfFeeCalculator();
    }

    @Test
    @DisplayName("Deve suportar apenas a plataforma SHOPEE (case-insensitive)")
    void shouldSupportShopeePlatform() {
        assertThat(calculator.supports("SHOPEE")).isTrue();
        assertThat(calculator.supports("shopee")).isTrue();
        assertThat(calculator.supports("Shopee")).isTrue();
        assertThat(calculator.supports("MERCADO_LIVRE")).isFalse();
        assertThat(calculator.supports("AMAZON")).isFalse();
    }

    @Test
    @DisplayName("Deve lançar exceção de negócio se o pedido for nulo")
    void shouldThrowExceptionWhenOrderIsNull() {
        assertThatThrownBy(() -> calculator.calculate(null, BigDecimal.TEN, BigDecimal.ZERO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("O pedido para auditoria financeira não pode ser nulo");
    }

    @Test
    @DisplayName("Deve calcular corretamente pedido padrão com 1 item maior que R$ 8,00 e sem divergência")
    void shouldCalculateStandardOrderWithoutDivergence() {
        // Cenário do exemplo: Adesivo R$ 68,40 (1 unidade)
        Map<String, Object> item = Map.of(
                "item_id", 22992592237L,
                "item_name", "Adesivo Impresso Mod.74",
                "model_name", "Adesivo Transparente 60 pcs",
                "model_discounted_price", "68.40",
                "model_quantity_purchased", 1
        );

        OrderMaster order = OrderMaster.builder()
                .platform("SHOPEE")
                .shopId("326559200")
                .orderSn("2608125R18PH4R")
                .metadata(Map.of("item_list", List.of(item)))
                .build();

        BigDecimal actualEscrow = new BigDecimal("50.72");
        BigDecimal sellerShipping = BigDecimal.ZERO;

        FeeCalculationResult result = calculator.calculate(order, actualEscrow, sellerShipping);

        assertThat(result.ruleVersion()).isEqualTo("SHOPEE_CPF_BR_2026");
        assertThat(result.hasDivergence()).isFalse();
        assertThat(result.subtotalItems()).isEqualByComparingTo("68.40");
        assertThat(result.totalQuantityItems()).isEqualTo(1);
        ShopeeFeeCalculationDetails details = (ShopeeFeeCalculationDetails) result.platformDetails();
        assertThat(details.baseCommission14()).isEqualByComparingTo("9.58"); // 68.40 * 0.14 = 9.576 -> 9.58
        assertThat(details.transactionFee6()).isEqualByComparingTo("4.10");   // 68.40 * 0.06 = 4.104 -> 4.10
        assertThat(details.fixedItemFee4()).isEqualByComparingTo("4.00");    // 1 * 4.00
        assertThat(details.lowValueSurcharge5()).isEqualByComparingTo("0.00");
        assertThat(result.totalMarketplaceFees()).isEqualByComparingTo("17.68"); // 9.58 + 4.10 + 4.00 = 17.68
        assertThat(result.theoreticalPayout()).isEqualByComparingTo("50.72");   // 68.40 - 17.68 = 50.72
        assertThat(result.actualPayout()).isEqualByComparingTo("50.72");
        assertThat(result.calculatedDifference()).isEqualByComparingTo("0.00");
        assertThat(result.divergenceReason()).isNull();
        assertThat(result.auditedItems()).hasSize(1);
        assertThat(result.auditedItems().getFirst().lowValueItem()).isFalse();
    }

    @Test
    @DisplayName("Deve aplicar sobretaxa de R$ 5,00 por unidade para itens abaixo de R$ 8,00")
    void shouldApplyLowValueSurchargeForItemsUnderEightReais() {
        // Item de R$ 7,00 (2 unidades)
        Map<String, Object> item = Map.of(
                "item_id", 100L,
                "item_name", "Chaveiro Pequeno",
                "model_discounted_price", "7.00",
                "model_quantity_purchased", 2
        );

        OrderMaster order = OrderMaster.builder()
                .platform("SHOPEE")
                .shopId("326559200")
                .orderSn("260812LOWVAL1")
                .metadata(Map.of("item_list", List.of(item)))
                .build();

        // Subtotal = 14.00
        // Comissão 14% = 1.96
        // Transação 6% = 0.84
        // Taxa Fixa = 2 * 4.00 = 8.00
        // Sobretaxa Baixo Valor = 2 * 5.00 = 10.00
        // Total Taxas = 1.96 + 0.84 + 8.00 + 10.00 = 20.80
        // Repasse Teórico = 14.00 - 20.80 = -6.80
        BigDecimal actualEscrow = new BigDecimal("-6.80");

        FeeCalculationResult result = calculator.calculate(order, actualEscrow, BigDecimal.ZERO);

        assertThat(result.hasDivergence()).isFalse();
        assertThat(result.subtotalItems()).isEqualByComparingTo("14.00");
        assertThat(result.totalQuantityItems()).isEqualTo(2);
        ShopeeFeeCalculationDetails details = (ShopeeFeeCalculationDetails) result.platformDetails();
        assertThat(details.baseCommission14()).isEqualByComparingTo("1.96");
        assertThat(details.transactionFee6()).isEqualByComparingTo("0.84");
        assertThat(details.fixedItemFee4()).isEqualByComparingTo("8.00");
        assertThat(details.lowValueSurcharge5()).isEqualByComparingTo("10.00");
        assertThat(result.totalMarketplaceFees()).isEqualByComparingTo("20.80");
        assertThat(result.theoreticalPayout()).isEqualByComparingTo("-6.80");
        assertThat(result.auditedItems().getFirst().lowValueItem()).isTrue();
        assertThat(result.auditedItems().getFirst().surchargeApplied()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("Deve detectar divergência quando o repasse da Shopee for inferior ao cálculo oficial")
    void shouldDetectDivergenceWhenShopeePaysLessThanExpected() {
        Map<String, Object> item = Map.of(
                "item_id", 101L,
                "item_name", "Kit Canecas",
                "model_discounted_price", "50.00",
                "model_quantity_purchased", 1
        );

        OrderMaster order = OrderMaster.builder()
                .platform("SHOPEE")
                .orderSn("2608DIVERGENCE")
                .metadata(Map.of("item_list", List.of(item)))
                .build();

        // Subtotal = 50.00
        // 14% = 7.00
        // 6% = 3.00
        // Fixa = 4.00
        // Total Taxas = 14.00
        // Repasse Teórico = 36.00
        // Shopee repassou apenas 30.00 (divergência de R$ 6,00)
        BigDecimal actualEscrow = new BigDecimal("30.00");

        FeeCalculationResult result = calculator.calculate(order, actualEscrow, BigDecimal.ZERO);

        assertThat(result.hasDivergence()).isTrue();
        assertThat(result.theoreticalPayout()).isEqualByComparingTo("36.00");
        assertThat(result.actualPayout()).isEqualByComparingTo("30.00");
        assertThat(result.calculatedDifference()).isEqualByComparingTo("-6.00");
        assertThat(result.divergenceReason())
                .contains("Divergência de R$ 6.00 detectada")
                .contains("30.00")
                .contains("36.00");
    }

    @Test
    @DisplayName("Deve tolerar diferenças de centavos dentro do limite de R$ 0,05 sem acusar divergência")
    void shouldTolerateSmallCentDifferencesWithinLimit() {
        Map<String, Object> item = Map.of(
                "item_id", 102L,
                "item_name", "Produto Teste",
                "model_discounted_price", "100.00",
                "model_quantity_purchased", 1
        );

        OrderMaster order = OrderMaster.builder()
                .platform("SHOPEE")
                .orderSn("2608TOLERANCE")
                .metadata(Map.of("item_list", List.of(item)))
                .build();

        // Subtotal = 100.00 | Taxas: 14 + 6 + 4 = 24.00 | Teórico = 76.00
        // Diferença de 3 centavos (dentro de 0.05)
        BigDecimal actualEscrow = new BigDecimal("75.97");

        FeeCalculationResult result = calculator.calculate(order, actualEscrow, BigDecimal.ZERO);

        assertThat(result.hasDivergence()).isFalse();
        assertThat(result.divergenceReason()).isNull();
    }

    @Test
    @DisplayName("Deve extrair itens aninhados na estrutura response.order_list[0].item_list da Shopee")
    void shouldExtractNestedItemListFromShopeeResponse() {
        Map<String, Object> rawItem = Map.of(
                "item_id", 999L,
                "item_name", "Camisa Polo",
                "model_discounted_price", "40.00",
                "model_quantity_purchased", 2
        );

        Map<String, Object> nestedMetadata = Map.of(
                "response", Map.of(
                        "order_list", List.of(
                                Map.of("item_list", List.of(rawItem))
                        )
                )
        );

        OrderMaster order = OrderMaster.builder()
                .platform("SHOPEE")
                .orderSn("2608NESTED")
                .metadata(nestedMetadata)
                .build();

        FeeCalculationResult result = calculator.calculate(order, new BigDecimal("60.00"), BigDecimal.ZERO);

        // Subtotal = 80.00 (2 * 40.00)
        // 14% = 11.20 | 6% = 4.80 | Fixa = 2 * 4 = 8.00 | Total Taxas = 24.00
        // Repasse Teórico = 80.00 - 24.00 = 56.00
        // Actual = 60.00 -> Divergência (+4.00)
        assertThat(result.subtotalItems()).isEqualByComparingTo("80.00");
        assertThat(result.totalQuantityItems()).isEqualTo(2);
        assertThat(result.theoreticalPayout()).isEqualByComparingTo("56.00");
        assertThat(result.hasDivergence()).isTrue();
    }
}
