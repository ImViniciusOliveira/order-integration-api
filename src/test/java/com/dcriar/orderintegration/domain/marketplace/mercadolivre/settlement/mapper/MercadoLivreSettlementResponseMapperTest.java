package com.dcriar.orderintegration.domain.marketplace.mercadolivre.settlement.mapper;

import com.dcriar.orderintegration.domain.marketplace.common.model.MarketplaceSettlement;
import com.dcriar.orderintegration.domain.marketplace.common.model.SettlementStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários do mapper financeiro do Mercado Livre.
 */
class MercadoLivreSettlementResponseMapperTest {

    private final MercadoLivreSettlementResponseMapper mapper =
            new MercadoLivreSettlementResponseMapper();

    @Test
    @DisplayName("Deve mapear settlement disponível com pedido, envio e pagamento")
    void deveMapearSettlementDisponivel() {
        Map<String, Object> order = Map.of(
                "total_amount", 100.00,
                "order_items", List.of(Map.of("sale_fee", 12.00))
        );
        Map<String, Object> shipment = Map.of("base_cost", 8.00);
        Map<String, Object> payment = Map.of(
                "status", "approved",
                "money_release_date", OffsetDateTime.now().minusDays(1).toString(),
                "transaction_details", Map.of("net_received_amount", 80.00),
                "fee_details", Map.of("total", 1.50)
        );

        MarketplaceSettlement settlement = mapper.map("seller-1", "2001", order, shipment, payment);

        assertThat(settlement.status()).isEqualTo(SettlementStatus.AVAILABLE);
        assertThat(settlement.grossAmount()).isEqualByComparingTo("100.00");
        assertThat(settlement.netAmount()).isEqualByComparingTo("80.00");
        assertThat(settlement.commissionAmount()).isEqualByComparingTo("12.00");
        assertThat(settlement.shippingFee()).isEqualByComparingTo("8.00");
        assertThat(settlement.transactionFee()).isEqualByComparingTo("1.50");
    }

    @Test
    @DisplayName("Deve marcar pagamento com liberação futura como pendente")
    void deveMarcarPagamentoComLiberacaoFuturaComoPendente() {
        Map<String, Object> payment = Map.of(
                "status", "approved",
                "money_release_date", OffsetDateTime.now().plusDays(1).toString(),
                "transaction_details", Map.of("net_received_amount", 80.00)
        );

        MarketplaceSettlement settlement = mapper.map(
                "seller-1",
                "2001",
                Map.of("total_amount", 100.00),
                Map.of(),
                payment
        );

        assertThat(settlement.status()).isEqualTo(SettlementStatus.PENDING);
        assertThat(settlement.pendingReason()).contains("ainda não foi liberado");
    }

    @Test
    @DisplayName("Deve marcar pagamento ausente como pendente")
    void deveMarcarPagamentoAusenteComoPendente() {
        MarketplaceSettlement settlement = mapper.map(
                "seller-1",
                "2001",
                Map.of("total_amount", 100.00),
                Map.of(),
                Map.of()
        );

        assertThat(settlement.status()).isEqualTo(SettlementStatus.PENDING);
        assertThat(settlement.pendingReason()).contains("Detalhes do pagamento");
    }
}
