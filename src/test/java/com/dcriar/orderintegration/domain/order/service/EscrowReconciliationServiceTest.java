package com.dcriar.orderintegration.domain.order.service;

import com.dcriar.orderintegration.config.OrderIntegrationProperties;
import com.dcriar.orderintegration.domain.notification.service.OrderReconciliationNotificationService;
import com.dcriar.orderintegration.domain.order.calculator.ShopeeCpfFeeCalculator;
import com.dcriar.orderintegration.domain.order.calculator.mapper.FeeCalculationMapper;
import com.dcriar.orderintegration.domain.order.entity.OrderMaster;
import com.dcriar.orderintegration.domain.order.repository.OrderMasterRepository;
import com.dcriar.orderintegration.domain.order.service.impl.EscrowReconciliationServiceImpl;
import com.dcriar.orderintegration.domain.queue.service.EscrowDelayQueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Suite de testes unitários para a implementação do serviço {@link EscrowReconciliationServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class EscrowReconciliationServiceTest {

    @Mock
    private OrderMasterRepository orderMasterRepository;

    @Mock
    private EscrowDelayQueueService delayQueueService;

    @Mock
    private OrderReconciliationNotificationService notificationService;

    private EscrowReconciliationService reconciliationService;

    @BeforeEach
    void setUp() {
        OrderIntegrationProperties properties = new OrderIntegrationProperties(
                new OrderIntegrationProperties.RedisProperties("dcriar:orders:escrow_delay_queue"),
                new OrderIntegrationProperties.EscrowProperties(120, 30, 60000L, 50, 5),
                new OrderIntegrationProperties.SecurityProperties("test-key"),
                new OrderIntegrationProperties.CorsProperties(List.of("http://localhost:8081")),
                new OrderIntegrationProperties.NotificationProperties("http://n8n-order:5678/webhook/v1/notifications/reconciled")
        );

        reconciliationService = new EscrowReconciliationServiceImpl(
                orderMasterRepository,
                delayQueueService,
                properties,
                List.of(new ShopeeCpfFeeCalculator()),
                new FeeCalculationMapper(),
                notificationService
        );
    }

    @Test
    @DisplayName("Deve conciliar pedidos pendentes com sucesso resgatados da fila e gravar auditoria financeira")
    void deveConciliarPedidosPendentesComSucesso() {
        // Arrange
        Map<String, Object> item = Map.of(
                "item_id", 123L,
                "item_name", "Produto Teste",
                "model_discounted_price", "100.00",
                "model_quantity_purchased", 1
        );

        OrderMaster order = OrderMaster.builder()
                .id(1L)
                .platform("SHOPEE")
                .shopId("123")
                .orderSn("240828ABC")
                .status("COMPLETED")
                .reconciled(false)
                .metadata(Map.of(
                        "escrow_amount", 76.00,
                        "shipping_fee_borne_by_seller", 0.00,
                        "item_list", List.of(item)
                ))
                .build();

        when(delayQueueService.pollReadyOrders(50)).thenReturn(Set.of("SHOPEE:240828ABC"));
        when(orderMasterRepository.findByPlatformAndOrderSn("SHOPEE", "240828ABC")).thenReturn(Optional.of(order));
        when(orderMasterRepository.save(any(OrderMaster.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        int reconciledCount = reconciliationService.reconcilePendingOrders();

        // Assert
        assertThat(reconciledCount).isEqualTo(1);
        assertThat(order.isReconciled()).isTrue();
        assertThat(order.getEscrowAmount()).isEqualByComparingTo(new BigDecimal("76.00"));
        assertThat(order.getShippingFeeBorneBySeller()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(order.getMetadata()).containsKey("auditoria_financeira");

        @SuppressWarnings("unchecked")
        Map<String, Object> auditoria = (Map<String, Object>) order.getMetadata().get("auditoria_financeira");
        assertThat(auditoria).isNotNull();
        assertThat(auditoria.get("versao_regra")).isEqualTo("SHOPEE_CPF_BR_2026");
        assertThat(auditoria.get("has_divergence")).isEqualTo(false);

        verify(orderMasterRepository).save(order);
        verify(delayQueueService).remove("SHOPEE", "240828ABC");
        verify(notificationService).notifyReconciliationCompleted(
                eq(order),
                argThat(v -> v != null && v.compareTo(new BigDecimal("100.00")) == 0),
                argThat(v -> v != null && v.compareTo(new BigDecimal("76.00")) == 0)
        );
    }

    @Test
    @DisplayName("Deve reagendar conciliação com retry quando pedido estiver pendente de extrato")
    void deveReagendarConciliacaoQuandoPendente() {
        // Arrange
        OrderMaster order = OrderMaster.builder()
                .id(2L)
                .platform("SHOPEE")
                .shopId("123")
                .orderSn("PENDING_SN")
                .status("COMPLETED")
                .reconciled(false)
                .metadata(Map.of())
                .build();

        when(delayQueueService.pollReadyOrders(50)).thenReturn(Set.of("SHOPEE:PENDING_SN"));
        when(orderMasterRepository.findByPlatformAndOrderSn("SHOPEE", "PENDING_SN")).thenReturn(Optional.of(order));

        // Act
        int reconciledCount = reconciliationService.reconcilePendingOrders();

        // Assert
        assertThat(reconciledCount).isZero();
        verify(delayQueueService).scheduleReconciliation(eq("SHOPEE"), eq("PENDING_SN"), any(Duration.class));
    }

    @Test
    @DisplayName("Deve retornar 0 quando não houver pedidos prontos na fila do Redis")
    void deveRetornarZeroQuandoFilaVazia() {
        when(delayQueueService.pollReadyOrders(50)).thenReturn(Set.of());

        int count = reconciliationService.reconcilePendingOrders();

        assertThat(count).isZero();
        verifyNoInteractions(orderMasterRepository);
    }
}
