package com.dcriar.orderintegration.api.controller.order;

import com.dcriar.orderintegration.api.dto.response.OrderMasterResponse;
import com.dcriar.orderintegration.domain.order.model.FinancialAuditStatus;
import com.dcriar.orderintegration.api.hateoas.OrderMasterModelAssembler;
import com.dcriar.orderintegration.config.OrderIntegrationProperties;
import com.dcriar.orderintegration.domain.order.entity.OrderMaster;
import com.dcriar.orderintegration.domain.order.service.MarketplaceIngestionService;
import com.dcriar.orderintegration.exception.custom.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para validação da segurança de autenticação interna e recepção de webhooks no MarketplaceWebhookController.
 */
@ExtendWith(MockitoExtension.class)
class MarketplaceWebhookControllerTest {

    @Mock
    private MarketplaceIngestionService ingestionService;

    @Mock
    private OrderMasterModelAssembler orderMasterModelAssembler;

    @Mock
    private OrderIntegrationProperties properties;

    private MarketplaceWebhookController controller;

    @BeforeEach
    void setUp() {
        controller = new MarketplaceWebhookController(ingestionService, orderMasterModelAssembler, properties);
    }

    @Test
    @DisplayName("POST /api/v1/webhooks/{platform} - Deve aceitar webhook com X-Internal-API-Key válida e retornar 200 OK")
    void deveAceitarWebhookComApiKeyValida() {
        // Arrange
        String validKey = "secret-key-123";
        OrderIntegrationProperties.SecurityProperties security = new OrderIntegrationProperties.SecurityProperties(validKey);
        when(properties.security()).thenReturn(security);

        OrderMaster orderMaster = OrderMaster.builder()
                .id(1L)
                .platform("SHOPEE")
                .shopId("123456")
                .orderSn("240828ABC")
                .status("READY_TO_SHIP")
                .build();

        OrderMasterResponse response = new OrderMasterResponse(
                1L, "SHOPEE", "123456", "240828ABC", "READY_TO_SHIP", "BR123",
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                false, FinancialAuditStatus.PENDING_SETTLEMENT, Map.of(), OffsetDateTime.now(), OffsetDateTime.now()
        );

        EntityModel<OrderMasterResponse> entityModel = EntityModel.of(response, Link.of("/api/v1/orders/1").withSelfRel());

        Map<String, Object> payload = Map.of("order_sn", "240828ABC", "status", "READY_TO_SHIP");
        when(ingestionService.ingestEvent(eq("SHOPEE"), eq("123456"), eq(payload))).thenReturn(orderMaster);
        when(orderMasterModelAssembler.toModel(orderMaster)).thenReturn(entityModel);

        // Act
        ResponseEntity<EntityModel<OrderMasterResponse>> result = controller.ingestWebhook("SHOPEE", validKey, "123456", payload);

        // Assert
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getContent()).isEqualTo(response);
    }

    @Test
    @DisplayName("POST /api/v1/webhooks/{platform} - Deve lançar BusinessException quando a API Key for inválida")
    void deveRejeitarWebhookComApiKeyInvalida() {
        // Arrange
        String validKey = "secret-key-123";
        OrderIntegrationProperties.SecurityProperties security = new OrderIntegrationProperties.SecurityProperties(validKey);
        when(properties.security()).thenReturn(security);

        Map<String, Object> payload = Map.of("order_sn", "240828ABC");

        // Act & Assert
        assertThatThrownBy(() -> controller.ingestWebhook("SHOPEE", "invalid-key", "123456", payload))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Chave de autenticação interna inválida ou ausente.");
    }
}
