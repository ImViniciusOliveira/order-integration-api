package com.dcriar.orderintegration.api.controller.order;

import com.dcriar.orderintegration.api.dto.filter.OrderFilterRequest;
import com.dcriar.orderintegration.api.dto.response.OrderMasterResponse;
import com.dcriar.orderintegration.domain.order.model.FinancialAuditStatus;
import com.dcriar.orderintegration.api.hateoas.OrderMasterModelAssembler;
import com.dcriar.orderintegration.api.mapper.OrderMasterMapper;
import com.dcriar.orderintegration.domain.order.entity.OrderMaster;
import com.dcriar.orderintegration.domain.order.model.OrderFilterCriteria;
import com.dcriar.orderintegration.domain.order.service.OrderMasterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para validação dos endpoints REST de consulta de pedidos no OrderMasterController.
 */
@ExtendWith(MockitoExtension.class)
class OrderMasterControllerTest {

    @Mock
    private OrderMasterService orderMasterService;

    @Mock
    private OrderMasterMapper orderMasterMapper;

    @Mock
    private OrderMasterModelAssembler orderMasterModelAssembler;

    @Mock
    private PagedResourcesAssembler<OrderMaster> pagedResourcesAssembler;

    private OrderMasterController controller;

    @BeforeEach
    void setUp() {
        controller = new OrderMasterController(
                orderMasterService,
                orderMasterMapper,
                orderMasterModelAssembler,
                pagedResourcesAssembler
        );
    }

    @Test
    @DisplayName("GET /api/v1/orders - Deve retornar listagem paginada de pedidos com status 200 OK")
    void deveRetornarListagemPaginadaDePedidos() {
        // Arrange
        OrderFilterRequest filter = new OrderFilterRequest("SHOPEE", null, null, null, null, null, null, null);
        OrderFilterCriteria criteria = new OrderFilterCriteria("SHOPEE", null, null, null, null, null, null, null);
        Pageable pageable = PageRequest.of(0, 20);

        OrderMaster entity = OrderMaster.builder()
                .id(10L)
                .platform("SHOPEE")
                .shopId("123456")
                .orderSn("240828ABC")
                .status("COMPLETED")
                .build();

        OrderMasterResponse response = new OrderMasterResponse(
                10L, "SHOPEE", "123456", "240828ABC", "COMPLETED", "BR123",
                new BigDecimal("10.00"), new BigDecimal("90.00"), BigDecimal.ZERO,
                true, FinancialAuditStatus.RECONCILED, Map.of(), OffsetDateTime.now(), OffsetDateTime.now()
        );

        Page<OrderMaster> page = new PageImpl<>(List.of(entity), pageable, 1);
        EntityModel<OrderMasterResponse> entityModel = EntityModel.of(response, Link.of("/api/v1/orders/10").withSelfRel());
        PagedModel<EntityModel<OrderMasterResponse>> pagedModel = PagedModel.of(
                List.of(entityModel),
                new PagedModel.PageMetadata(20, 0, 1, 1),
                Link.of("/api/v1/orders").withSelfRel()
        );

        when(orderMasterMapper.toCriteria(filter)).thenReturn(criteria);
        when(orderMasterService.searchOrders(criteria, pageable)).thenReturn(page);
        when(pagedResourcesAssembler.toModel(eq(page), eq(orderMasterModelAssembler))).thenReturn(pagedModel);

        // Act
        ResponseEntity<PagedModel<EntityModel<OrderMasterResponse>>> result = controller.searchOrders(filter, pageable);

        // Assert
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getContent()).hasSize(1);
    }

    @Test
    @DisplayName("GET /api/v1/orders/{id} - Deve retornar detalhes do pedido por ID")
    void deveBuscarPedidoPorId() {
        // Arrange
        OrderMaster entity = OrderMaster.builder()
                .id(10L)
                .platform("SHOPEE")
                .shopId("123456")
                .orderSn("240828ABC")
                .status("COMPLETED")
                .build();

        OrderMasterResponse response = new OrderMasterResponse(
                10L, "SHOPEE", "123456", "240828ABC", "COMPLETED", "BR123",
                new BigDecimal("10.00"), new BigDecimal("90.00"), BigDecimal.ZERO,
                true, FinancialAuditStatus.RECONCILED, Map.of(), OffsetDateTime.now(), OffsetDateTime.now()
        );

        EntityModel<OrderMasterResponse> entityModel = EntityModel.of(response, Link.of("/api/v1/orders/10").withSelfRel());

        when(orderMasterService.findById(10L)).thenReturn(entity);
        when(orderMasterModelAssembler.toModel(entity)).thenReturn(entityModel);

        // Act
        ResponseEntity<EntityModel<OrderMasterResponse>> result = controller.findById(10L);

        // Assert
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getContent()).isEqualTo(response);
    }
}
