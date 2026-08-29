package com.dcriar.orderintegration.domain.order.service;

import com.dcriar.orderintegration.config.OrderIntegrationProperties;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    private EscrowReconciliationService reconciliationService;

    @BeforeEach
    void setUp() {
        OrderIntegrationProperties properties = new OrderIntegrationProperties(
                new OrderIntegrationProperties.RedisProperties("dcriar:orders:escrow_delay_queue"),
                new OrderIntegrationProperties.EscrowProperties(120, 30, 60000L, 50, 5),
                new OrderIntegrationProperties.SecurityProperties("test-key"),
                new OrderIntegrationProperties.CorsProperties(List.of("http://localhost:8081"))
        );

        reconciliationService = new EscrowReconciliationServiceImpl(
                orderMasterRepository,
                delayQueueService,
                properties
        );
    }

    @Test
    @DisplayName("Deve conciliar pedidos pendentes com sucesso resgatados da fila")
    void deveConciliarPedidosPendentesComSucesso() {
        // Arrange
        OrderMaster order = OrderMaster.builder()
                .id(1L)
                .platform("SHOPEE")
                .shopId("123")
                .orderSn("240828ABC")
                .status("COMPLETED")
                .reconciled(false)
                .metadata(Map.of("escrow_amount", 85.50, "shipping_fee_borne_by_seller", 5.00))
                .build();

        when(delayQueueService.pollReadyOrders(50)).thenReturn(Set.of("SHOPEE:240828ABC"));
        when(orderMasterRepository.findByPlatformAndOrderSn("SHOPEE", "240828ABC")).thenReturn(Optional.of(order));
        when(orderMasterRepository.save(any(OrderMaster.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        int reconciledCount = reconciliationService.reconcilePendingOrders();

        // Assert
        assertThat(reconciledCount).isEqualTo(1);
        assertThat(order.isReconciled()).isTrue();
        assertThat(order.getEscrowAmount()).isEqualByComparingTo(new BigDecimal("85.50"));
        assertThat(order.getShippingFeeBorneBySeller()).isEqualByComparingTo(new BigDecimal("5.00"));
        verify(orderMasterRepository).save(order);
        verify(delayQueueService).remove("SHOPEE", "240828ABC");
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
