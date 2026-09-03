package com.dcriar.orderintegration.domain.marketplace.shopee.calculator;

import com.dcriar.orderintegration.domain.marketplace.common.calculator.model.FeeAuditStatus;
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
 * Testes unitários para a auditoria financeira oficial da Shopee.
 */
class ShopeeCpfFeeCalculatorTest {

    private ShopeeCpfFeeCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new ShopeeCpfFeeCalculator();
    }

    @Test
    @DisplayName("Deve suportar apenas a plataforma Shopee")
    void deveSuportarSomenteShopee() {
        assertThat(calculator.supports("SHOPEE")).isTrue();
        assertThat(calculator.supports("mercadolivre")).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar pedido nulo")
    void deveRejeitarPedidoNulo() {
        assertThatThrownBy(() -> calculator.calculate(null, BigDecimal.TEN, BigDecimal.ZERO))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Deve usar os componentes oficiais do order_income")
    void deveUsarComponentesOficiais() {
        OrderMaster order = orderWithIncome(Map.of(
                "commission_fee", "9.58",
                "service_fee", "1.25",
                "seller_transaction_fee", "4.10",
                "income_details", Map.of("actual_shipping_fee", "2.30")
        ));

        FeeCalculationResult result = calculator.calculate(
                order,
                new BigDecimal("50.72"),
                new BigDecimal("2.30")
        );

        assertThat(result.auditStatus()).isEqualTo(FeeAuditStatus.COMPLETE);
        assertThat(result.hasDivergence()).isTrue();
        assertThat(result.totalMarketplaceFees()).isEqualByComparingTo("14.93");

        ShopeeFeeCalculationDetails details =
                (ShopeeFeeCalculationDetails) result.platformDetails();
        assertThat(details.commissionFee()).isEqualByComparingTo("9.58");
        assertThat(details.serviceFee()).isEqualByComparingTo("1.25");
        assertThat(details.sellerTransactionFee()).isEqualByComparingTo("4.10");
    }

    @Test
    @DisplayName("Deve marcar auditoria incompleta sem taxas oficiais")
    void deveMarcarAuditoriaIncompletaSemTaxas() {
        OrderMaster order = OrderMaster.builder()
                .platform("SHOPEE")
                .orderSn("ORDER-INCOMPLETE")
                .metadata(Map.of("item_list", List.of(item())))
                .build();

        FeeCalculationResult result = calculator.calculate(
                order,
                new BigDecimal("50.00"),
                BigDecimal.ZERO
        );

        assertThat(result.auditStatus()).isEqualTo(FeeAuditStatus.INCOMPLETE);
        assertThat(result.hasDivergence()).isFalse();
        assertThat(result.divergenceReason()).contains("commission_fee");
    }

    @Test
    @DisplayName("Nao deve tratar taxa de servico como taxa fixa")
    void naoDeveTratarTaxaDeServicoComoTaxaFixa() {
        OrderMaster order = orderWithIncome(Map.of(
                "commission_fee", "1.20",
                "service_fee", "0.30",
                "seller_transaction_fee", "0.10",
                "income_details", Map.of("final_shipping_fee", "0.40")
        ));

        FeeCalculationResult result = calculator.calculate(
                order,
                new BigDecimal("8.00"),
                new BigDecimal("0.40")
        );

        assertThat(result.totalMarketplaceFees()).isEqualByComparingTo("1.60");
        assertThat(result.theoreticalPayout()).isEqualByComparingTo("66.40");
    }

    private OrderMaster orderWithIncome(Map<String, Object> income) {
        return OrderMaster.builder()
                .platform("SHOPEE")
                .shopId("326559200")
                .orderSn("ORDER-OFFICIAL")
                .metadata(Map.of(
                        "item_list", List.of(item()),
                        "settlement_financial_details", Map.of("order_income", income)
                ))
                .build();
    }

    private Map<String, Object> item() {
        return Map.of(
                "item_id", 1L,
                "item_name", "Produto",
                "model_discounted_price", "68.40",
                "model_quantity_purchased", 1
        );
    }
}
