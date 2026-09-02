package com.dcriar.orderintegration.domain.marketplace.common.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários do contrato neutro de settlement financeiro.
 */
class MarketplaceSettlementTest {

    @Test
    @DisplayName("Deve transportar settlement disponível sem aplicar regras de cálculo")
    void deveTransportarSettlementDisponivel() {
        OffsetDateTime queriedAt = OffsetDateTime.now();
        MarketplaceSettlement settlement = new MarketplaceSettlement(
                SettlementStatus.AVAILABLE,
                "SHOPEE",
                "2608125R18PH4R",
                "326559200",
                new BigDecimal("68.40"),
                new BigDecimal("50.72"),
                new BigDecimal("9.58"),
                new BigDecimal("4.10"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                Map.of("income_details", Map.of("commission_fee", "9.58")),
                queriedAt,
                null
        );

        assertThat(settlement.status()).isEqualTo(SettlementStatus.AVAILABLE);
        assertThat(settlement.platform()).isEqualTo("SHOPEE");
        assertThat(settlement.netAmount()).isEqualByComparingTo("50.72");
        assertThat(settlement.shippingFee()).isEqualByComparingTo("0.00");
        assertThat(settlement.queriedAt()).isEqualTo(queriedAt);
    }

    @Test
    @DisplayName("Deve representar settlement pendente com motivo para reagendamento")
    void deveRepresentarSettlementPendente() {
        MarketplaceSettlement settlement = new MarketplaceSettlement(
                SettlementStatus.PENDING,
                "MERCADOLIVRE",
                "2000018236707690",
                "3644237792",
                null,
                null,
                null,
                null,
                null,
                null,
                Map.of(),
                OffsetDateTime.now(),
                "money_release_date ainda não atingida"
        );

        assertThat(settlement.status()).isEqualTo(SettlementStatus.PENDING);
        assertThat(settlement.netAmount()).isNull();
        assertThat(settlement.pendingReason()).contains("money_release_date");
    }
}
