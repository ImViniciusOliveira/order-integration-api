package com.dcriar.orderintegration.api.hateoas;

import com.dcriar.orderintegration.api.dto.response.OrderMasterResponse;
import com.dcriar.orderintegration.domain.order.model.FinancialAuditStatus;
import com.dcriar.orderintegration.api.mapper.OrderMasterMapper;
import com.dcriar.orderintegration.domain.order.entity.OrderMaster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para validar a conversão de Entidade para Model HATEOAS no OrderMasterModelAssembler.
 */
@ExtendWith(MockitoExtension.class)
class OrderMasterModelAssemblerTest {

    @Mock
    private OrderMasterMapper mapper;

    private OrderMasterModelAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new OrderMasterModelAssembler(mapper);
    }

    @Test
    @DisplayName("Deve converter Entidade JPA para EntityModel com links self, collection e channels")
    void deveConverterEntidadeParaModeloHateoas() {
        // Arrange
        OrderMaster entity = OrderMaster.builder()
                .id(100L)
                .platform("SHOPEE")
                .shopId("123456")
                .orderSn("240828ABC123")
                .status("COMPLETED")
                .trackingNo("BR123456789")
                .build();

        OrderMasterResponse response = new OrderMasterResponse(
                100L, "SHOPEE", "123456", "240828ABC123", "COMPLETED", "BR123456789",
                new BigDecimal("15.50"), new BigDecimal("85.00"), new BigDecimal("0.00"),
                true, FinancialAuditStatus.RECONCILED, Map.of("item_count", 2),
                OffsetDateTime.now(), OffsetDateTime.now()
        );

        when(mapper.toResponse(entity)).thenReturn(response);

        // Act
        EntityModel<OrderMasterResponse> model = assembler.toModel(entity);

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
    @DisplayName("Deve converter lista de entidades para CollectionModel com links HATEOAS")
    void deveConverterListaDeEntidadesParaCollectionModel() {
        // Arrange
        OrderMaster entity = OrderMaster.builder().id(100L).platform("SHOPEE").orderSn("240828ABC123").build();
        OrderMasterResponse response = new OrderMasterResponse(
                100L, "SHOPEE", "123456", "240828ABC123", "COMPLETED", "BR123456789",
                new BigDecimal("15.50"), new BigDecimal("85.00"), new BigDecimal("0.00"),
                true, FinancialAuditStatus.RECONCILED, Map.of("item_count", 2),
                OffsetDateTime.now(), OffsetDateTime.now()
        );

        when(mapper.toResponse(any(OrderMaster.class))).thenReturn(response);

        // Act
        CollectionModel<EntityModel<OrderMasterResponse>> collectionModel = assembler.toCollectionModel(List.of(entity));

        // Assert
        assertThat(collectionModel).isNotNull();
        assertThat(collectionModel.getContent()).hasSize(1);
        assertThat(collectionModel.hasLink("self")).isTrue();
        assertThat(collectionModel.getLink("self").orElseThrow().getHref()).isEqualTo("/api/v1/orders");
    }
}
