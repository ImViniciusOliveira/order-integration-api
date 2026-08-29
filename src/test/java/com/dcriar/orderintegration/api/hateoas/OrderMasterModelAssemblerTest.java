package com.dcriar.orderintegration.api.hateoas;

import com.dcriar.orderintegration.api.dto.response.OrderMasterResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários para validar os links gerados pelo OrderMasterModelAssembler.
 */
class OrderMasterModelAssemblerTest {

    private final OrderMasterModelAssembler assembler = new OrderMasterModelAssembler();

    @Test
    @DisplayName("Deve gerar modelo HATEOAS com links self, collection e channels")
    void deveGerarModeloComLinksHateoas() {
        // Arrange
        OrderMasterResponse response = new OrderMasterResponse(
                100L, "SHOPEE", "123456", "240828ABC123", "COMPLETED", "BR123456789",
                new BigDecimal("15.50"), new BigDecimal("85.00"), new BigDecimal("0.00"),
                true, Map.of("item_count", 2),
                OffsetDateTime.now(), OffsetDateTime.now()
        );

        // Act
        EntityModel<OrderMasterResponse> model = assembler.toModel(response);

        // Assert
        assertThat(model).isNotNull();
        assertThat(model.getContent()).isEqualTo(response);
        assertThat(model.hasLink("self")).isTrue();
        assertThat(model.getLink("self").orElseThrow().getHref()).isEqualTo("/api/v1/orders/100");
        assertThat(model.hasLink("collection")).isTrue();
        assertThat(model.getLink("collection").orElseThrow().getHref()).isEqualTo("/api/v1/orders");
        assertThat(model.hasLink("channels")).isTrue();
        assertThat(model.getLink("channels").orElseThrow().getHref()).isEqualTo("/api/v1/channels");
    }

    @Test
    @DisplayName("Deve gerar CollectionModel com links HATEOAS para lista de pedidos")
    void deveGerarCollectionModelParaListaDePedidos() {
        // Arrange
        OrderMasterResponse response = new OrderMasterResponse(
                100L, "SHOPEE", "123456", "240828ABC123", "COMPLETED", "BR123456789",
                new BigDecimal("15.50"), new BigDecimal("85.00"), new BigDecimal("0.00"),
                true, Map.of("item_count", 2),
                OffsetDateTime.now(), OffsetDateTime.now()
        );

        // Act
        CollectionModel<EntityModel<OrderMasterResponse>> collectionModel = assembler.toCollectionModel(List.of(response));

        // Assert
        assertThat(collectionModel).isNotNull();
        assertThat(collectionModel.getContent()).hasSize(1);
        assertThat(collectionModel.hasLink("self")).isTrue();
        assertThat(collectionModel.getLink("self").orElseThrow().getHref()).isEqualTo("/api/v1/orders");
    }
}
