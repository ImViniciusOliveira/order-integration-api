package com.dcriar.orderintegration.domain.marketplace.mercadolivre.processor;

import com.dcriar.orderintegration.domain.marketplace.common.model.OrderProcessingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes unitários para a estratégia de ingestão do Mercado Livre ({@link MercadoLivreOrderProcessor}).
 */
class MercadoLivreOrderProcessorTest {

    private MercadoLivreOrderProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new MercadoLivreOrderProcessor();
    }

    @Test
    @DisplayName("Deve suportar apenas a plataforma MERCADOLIVRE (case-insensitive)")
    void shouldSupportMercadoLivrePlatform() {
        assertThat(processor.supports("MERCADOLIVRE")).isTrue();
        assertThat(processor.supports("mercadolivre")).isTrue();
        assertThat(processor.supports("MercadoLivre")).isTrue();
        assertThat(processor.supports("SHOPEE")).isFalse();
        assertThat(processor.supports("AMAZON")).isFalse();
    }

    @Test
    @DisplayName("Deve lançar exceção quando o payload for nulo ou vazio")
    void shouldThrowExceptionWhenPayloadIsNullOrEmpty() {
        assertThatThrownBy(() -> processor.process("shop1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Payload do Mercado Livre não pode ser nulo ou vazio");

        assertThatThrownBy(() -> processor.process("shop1", Collections.emptyMap()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Payload do Mercado Livre não pode ser nulo ou vazio");
    }

    @Test
    @DisplayName("Deve lançar exceção quando o número do pedido estiver ausente")
    void shouldThrowExceptionWhenOrderSnIsMissing() {
        Map<String, Object> payload = Map.of("shop_id", "3644237792", "status", "paid");
        assertThatThrownBy(() -> processor.process("shop1", payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("O número do pedido (ordersn/order_sn/id) é obrigatório");
    }

    @Test
    @DisplayName("Deve extrair com sucesso pedido do Mercado Livre com chaves padrão")
    void shouldProcessMercadoLivrePayloadSuccessfully() {
        Map<String, Object> payload = Map.of(
                "order_sn", "2000018236707690",
                "shop_id", "3644237792",
                "status", "READY_TO_SHIP",
                "tracking_no", "MLB123456789",
                "estimated_shipping_fee", "9.99"
        );

        OrderProcessingResult result = processor.process(null, payload);

        assertThat(result).isNotNull();
        assertThat(result.orderSn()).isEqualTo("2000018236707690");
        assertThat(result.shopId()).isEqualTo("3644237792");
        assertThat(result.status()).isEqualTo("READY_TO_SHIP");
        assertThat(result.trackingNo()).isEqualTo("MLB123456789");
        assertThat(result.estimatedShippingFee()).isEqualByComparingTo(new BigDecimal("9.99"));
        assertThat(result.metadata()).isEqualTo(payload);
    }

    @Test
    @DisplayName("Deve resolver id e seller_id nativos do Mercado Livre quando chaves order_sn não forem informadas")
    void shouldResolveNativeMercadoLivreKeys() {
        Map<String, Object> payload = Map.of(
                "id", 2000018236707690L,
                "seller_id", 3644237792L,
                "status", "paid",
                "shipping_cost", 15.50
        );

        OrderProcessingResult result = processor.process("externalShop", payload);

        assertThat(result.orderSn()).isEqualTo("2000018236707690");
        assertThat(result.shopId()).isEqualTo("externalShop");
        assertThat(result.status()).isEqualTo("PAID");
        assertThat(result.estimatedShippingFee()).isEqualByComparingTo(new BigDecimal("15.50"));
    }
}
