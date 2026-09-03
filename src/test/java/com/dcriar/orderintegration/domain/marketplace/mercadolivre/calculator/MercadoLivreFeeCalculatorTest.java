package com.dcriar.orderintegration.domain.marketplace.mercadolivre.calculator;

import com.dcriar.orderintegration.domain.marketplace.common.calculator.model.FeeCalculationResult;
import com.dcriar.orderintegration.domain.marketplace.common.calculator.model.FeeAuditStatus;
import com.dcriar.orderintegration.domain.marketplace.mercadolivre.calculator.model.MercadoLivreFeeCalculationDetails;
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
 * Testes unitários para o motor de cálculo e prova real do Mercado Livre ({@link MercadoLivreFeeCalculator}).
 */
class MercadoLivreFeeCalculatorTest {

    private MercadoLivreFeeCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new MercadoLivreFeeCalculator();
    }

    @Test
    @DisplayName("Deve suportar apenas a plataforma MERCADOLIVRE (case-insensitive)")
    void shouldSupportMercadoLivrePlatform() {
        assertThat(calculator.supports("MERCADOLIVRE")).isTrue();
        assertThat(calculator.supports("mercadolivre")).isTrue();
        assertThat(calculator.supports("MercadoLivre")).isTrue();
        assertThat(calculator.supports("SHOPEE")).isFalse();
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
    @DisplayName("Deve auditar com sucesso usando o sale_fee da API do Mercado Livre sem divergência")
    void shouldAuditWithDirectSaleFeeSuccessfully() {
        // Cenário do pedido real: Glitter 15g por R$ 20,00 com sale_fee de R$ 2,30
        Map<String, Object> item = Map.of(
                "item_id", "MLB5175107233",
                "item_name", "15g De Glitter Flocado Diversas Cores Cor Colorido Roxo",
                "model_name", "Única",
                "unit_price", 20.0,
                "quantity", 1,
                "sale_fee", 2.30
        );

        OrderMaster order = OrderMaster.builder()
                .platform("MERCADOLIVRE")
                .shopId("3644237792")
                .orderSn("2000018236707690")
                .status("COMPLETED")
                .metadata(Map.of(
                        "item_list", List.of(item),
                        "sale_fee", 2.30
                ))
                .build();

        // Repasse esperado: 20.00 - 2.30 = 17.70
        BigDecimal actualPayout = new BigDecimal("17.70");
        BigDecimal sellerShippingFee = BigDecimal.ZERO;

        FeeCalculationResult result = calculator.calculate(order, actualPayout, sellerShippingFee);

        assertThat(result).isNotNull();
        assertThat(result.hasDivergence()).isFalse();
        assertThat(result.ruleVersion()).isEqualTo("MERCADOLIVRE_BR_2026");
        assertThat(result.subtotalItems()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(result.totalQuantityItems()).isEqualTo(1);
        assertThat(result.totalMarketplaceFees()).isEqualByComparingTo(new BigDecimal("2.30"));
        assertThat(result.theoreticalPayout()).isEqualByComparingTo(new BigDecimal("17.70"));
        assertThat(result.actualPayout()).isEqualByComparingTo(new BigDecimal("17.70"));
        assertThat(result.calculatedDifference()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.divergenceReason()).isNull();
    }

    @Test
    @DisplayName("Deve detectar divergência contábil quando o repasse do Mercado Pago diferir do cálculo teórico")
    void shouldDetectFinancialDivergenceWhenPayoutDiffers() {
        Map<String, Object> item = Map.of(
                "item_id", "MLB5175107233",
                "item_name", "Glitter Flocado",
                "unit_price", 20.0,
                "quantity", 1,
                "sale_fee", 2.30
        );

        OrderMaster order = OrderMaster.builder()
                .platform("MERCADOLIVRE")
                .shopId("3644237792")
                .orderSn("2000018236707690")
                .status("COMPLETED")
                .metadata(Map.of("item_list", List.of(item)))
                .build();

        // Repasse teórico é 17.70, mas o extrato veio com R$ 15.00 (desconto indevido de R$ 2,70)
        BigDecimal actualPayoutWithFeeLeak = new BigDecimal("15.00");

        FeeCalculationResult result = calculator.calculate(order, actualPayoutWithFeeLeak, BigDecimal.ZERO);

        assertThat(result).isNotNull();
        assertThat(result.hasDivergence()).isTrue();
        assertThat(result.calculatedDifference()).isEqualByComparingTo(new BigDecimal("-2.70"));
        assertThat(result.divergenceReason()).contains("Divergência de R$ 2.70 detectada");
    }

    @Test
    @DisplayName("Deve tolerar diferenças de até R$ 0,05 sem sinalizar divergência")
    void shouldTolerateMicroCentDifferencesWithinTolerance() {
        Map<String, Object> item = Map.of(
                "unit_price", 20.0,
                "quantity", 1,
                "sale_fee", 2.30
        );

        OrderMaster order = OrderMaster.builder()
                .platform("MERCADOLIVRE")
                .orderSn("2000018236707690")
                .metadata(Map.of("item_list", List.of(item)))
                .build();

        // Repasse teórico: 17.70 -> Repasse real: 17.73 (diferença de R$ 0,03 dentro dos R$ 0,05 de tolerância)
        BigDecimal payoutWithTolerance = new BigDecimal("17.73");

        FeeCalculationResult result = calculator.calculate(order, payoutWithTolerance, BigDecimal.ZERO);

        assertThat(result.hasDivergence()).isFalse();
        assertThat(result.divergenceReason()).isNull();
    }

    @Test
    @DisplayName("Deve preservar os componentes oficiais de sale_fee_details")
    void shouldPreserveOfficialSaleFeeDetails() {
        Map<String, Object> orderResponse = Map.of(
                "sale_fee_amount", 1.50,
                "sale_fee_details", Map.of(
                        "financing_add_on_fee", 0.10,
                        "fixed_fee", 0.20,
                        "gross_amount", 1.50,
                        "percentage_fee", 15.0
                )
        );
        OrderMaster order = OrderMaster.builder()
                .platform("MERCADOLIVRE")
                .orderSn("2000018236707690")
                .metadata(Map.of(
                        "item_list", List.of(Map.of("unit_price", 10.00, "quantity", 1)),
                        "settlement_financial_details", Map.of("order", orderResponse)
                ))
                .build();

        FeeCalculationResult result = calculator.calculate(
                order,
                new BigDecimal("8.50"),
                BigDecimal.ZERO
        );

        assertThat(result.auditStatus()).isEqualTo(FeeAuditStatus.COMPLETE);
        MercadoLivreFeeCalculationDetails details =
                (MercadoLivreFeeCalculationDetails) result.platformDetails();
        assertThat(details.saleFee()).isEqualByComparingTo("1.50");
        assertThat(details.percentageFee()).isEqualByComparingTo("15.0");
        assertThat(details.fixedFee()).isEqualByComparingTo("0.20");
        assertThat(details.financingAddOnFee()).isEqualByComparingTo("0.10");
    }
}
