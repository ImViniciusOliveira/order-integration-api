package com.dcriar.orderintegration.domain.marketplace.shopee.settlement.client;

import com.dcriar.orderintegration.domain.order.model.MarketplaceSettlement;
import com.dcriar.orderintegration.domain.order.model.SettlementStatus;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários do mapeamento da resposta financeira Shopee.
 */
class ShopeeSettlementResponseMapperTest {

    private final ShopeeSettlementResponseMapper mapper = new ShopeeSettlementResponseMapper();

    @Test
    void deveMapearSettlementDisponivelEIncomeDetails() {
        MarketplaceSettlement settlement = mapper.map(
                "326559200",
                "2608125R18PH4R",
                Map.of(
                        "response", Map.of(
                                "buyer_total_amount", "68.40",
                                "escrow_amount", "50.72",
                                "commission_fee", "9.58",
                                "transaction_fee", "4.10",
                                "income_details", Map.of("shipping_fee_borne_by_seller", "0.00")
                        )
                )
        );

        assertThat(settlement.status()).isEqualTo(SettlementStatus.AVAILABLE);
        assertThat(settlement.netAmount()).isEqualByComparingTo("50.72");
        assertThat(settlement.shippingFee()).isEqualByComparingTo("0.00");
        assertThat(settlement.financialDetails()).containsKey("income_details");
    }

    @Test
    void deveClassificarEscrowZeradoComoPendente() {
        MarketplaceSettlement settlement = mapper.map(
                "326559200",
                "ORDER-PENDING",
                Map.of("response", Map.of("escrow_amount", "0"))
        );

        assertThat(settlement.status()).isEqualTo(SettlementStatus.PENDING);
        assertThat(settlement.netAmount()).isNull();
    }
}
