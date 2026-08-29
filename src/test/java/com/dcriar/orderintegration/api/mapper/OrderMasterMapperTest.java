package com.dcriar.orderintegration.api.mapper;

import com.dcriar.orderintegration.api.dto.response.OrderMasterResponse;
import com.dcriar.orderintegration.domain.order.entity.OrderMaster;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários para validar as conversões do OrderMasterMapper.
 */
class OrderMasterMapperTest {

    private final OrderMasterMapper mapper = Mappers.getMapper(OrderMasterMapper.class);

    @Test
    @DisplayName("Deve converter entidade OrderMaster para OrderMasterResponse")
    void deveConverterOrderMasterParaResponse() {
        // Arrange
        OrderMaster entity = OrderMaster.builder()
                .id(100L)
                .platform("SHOPEE")
                .shopId("12345")
                .orderSn("240828ABC123")
                .status("COMPLETED")
                .trackingNo("BR123456789")
                .estimatedShippingFee(new BigDecimal("15.0000"))
                .escrowAmount(new BigDecimal("85.4000"))
                .shippingFeeBorneBySeller(new BigDecimal("3.2000"))
                .reconciled(true)
                .metadata(Map.of("item_count", 2))
                .build();

        // Act
        OrderMasterResponse response = mapper.toResponse(entity);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.platform()).isEqualTo("SHOPEE");
        assertThat(response.shopId()).isEqualTo("12345");
        assertThat(response.orderSn()).isEqualTo("240828ABC123");
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.trackingNo()).isEqualTo("BR123456789");
        assertThat(response.estimatedShippingFee()).isEqualByComparingTo("15.0000");
        assertThat(response.escrowAmount()).isEqualByComparingTo("85.4000");
        assertThat(response.shippingFeeBorneBySeller()).isEqualByComparingTo("3.2000");
        assertThat(response.reconciled()).isTrue();
        assertThat(response.metadata()).containsEntry("item_count", 2);
    }

    @Test
    @DisplayName("Deve converter lista de entidades OrderMaster para lista de responses")
    void deveConverterListaDeOrderMasterParaResponses() {
        // Arrange
        OrderMaster order1 = OrderMaster.builder().id(1L).platform("SHOPEE").orderSn("SN1").status("COMPLETED").build();
        OrderMaster order2 = OrderMaster.builder().id(2L).platform("SHOPEE").orderSn("SN2").status("READY_TO_SHIP").build();

        // Act
        List<OrderMasterResponse> responses = mapper.toResponseList(List.of(order1, order2));

        // Assert
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).orderSn()).isEqualTo("SN1");
        assertThat(responses.get(1).orderSn()).isEqualTo("SN2");
    }
}
