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
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para validar o serviço de conciliação financeira de Escrow
 * com suporte a retry inteligente e persistência no PostgreSQL.
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
                new OrderIntegrationProperties.EscrowProperties(120, 30, 60000L, 50, 5)
        );

        reconciliationService = new EscrowReconciliationServiceImpl(
                orderMasterRepository,
                delayQueueService,
                properties
        );
    }

    @Test
    @DisplayName("Deve conciliar pedido com sucesso quando os dados de Escrow estiverem disponíveis nos metadados")
    void deveConciliarPedidoComSucesso() {
        // Arrange
        OrderMaster order = OrderMaster.builder()
                .id(1L)
                .platform("SHOPEE")
                .shopId("shop_123")
                .orderSn("240828ABC123")
                .status("COMPLETED")
                .estimatedShippingFee(new BigDecimal("12.5000"))
                .reconciled(false)
                .metadata(Map.of(
                        "escrow_amount", "85.4000",
                        "shipping_fee_borne_by_seller", "3.2000"
                ))
                .build();

        when(orderMasterRepository.findByPlatformAndOrderSn("SHOPEE", "240828ABC123"))
                .thenReturn(Optional.of(order));
        when(orderMasterRepository.save(any(OrderMaster.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        boolean concilado = reconciliationService.reconcileOrder("SHOPEE", "240828ABC123");

        // Assert
        assertThat(concilado).isTrue();
        assertThat(order.isReconciled()).isTrue();
        assertThat(order.getEscrowAmount()).isEqualByComparingTo(new BigDecimal("85.4000"));
        assertThat(order.getShippingFeeBorneBySeller()).isEqualByComparingTo(new BigDecimal("3.2000"));

        verify(orderMasterRepository).save(order);
        verify(delayQueueService).remove("SHOPEE", "240828ABC123");
    }

    @Test
    @DisplayName("Deve efetuar reagendamento inteligente (retry) no Redis quando o extrato contábil ainda não estiver liberado")
    void deveReagendarNoRedisQuandoExtratoNaoEstiverLiberado() {
        // Arrange (pedido sem escrow_amount nos metadados)
        OrderMaster order = OrderMaster.builder()
                .id(1L)
                .platform("SHOPEE")
                .shopId("shop_123")
                .orderSn("240828ABC123")
                .status("COMPLETED")
                .reconciled(false)
                .metadata(Map.of("tracking_no", "BR123456"))
                .build();

        when(orderMasterRepository.findByPlatformAndOrderSn("SHOPEE", "240828ABC123"))
                .thenReturn(Optional.of(order));

        // Act
        boolean concilado = reconciliationService.reconcileOrder("SHOPEE", "240828ABC123");

        // Assert
        assertThat(concilado).isFalse();
        assertThat(order.isReconciled()).isFalse();

        // Não deve salvar no banco como conciliado
        verify(orderMasterRepository, never()).save(any(OrderMaster.class));
        // Não deve remover da fila
        verify(delayQueueService, never()).remove(eq("SHOPEE"), eq("240828ABC123"));
        // Deve reagendar com retry de 30 minutos
        verify(delayQueueService).scheduleReconciliation("SHOPEE", "240828ABC123", Duration.ofMinutes(30));
    }

    @Test
    @DisplayName("Deve apenas remover da fila do Redis se o pedido já estiver marcado como conciliado")
    void deveApenasRemoverDaFilaQuandoPedidoJaEstiverConciliado() {
        // Arrange
        OrderMaster order = OrderMaster.builder()
                .id(1L)
                .platform("SHOPEE")
                .orderSn("240828ABC123")
                .reconciled(true)
                .build();

        when(orderMasterRepository.findByPlatformAndOrderSn("SHOPEE", "240828ABC123"))
                .thenReturn(Optional.of(order));

        // Act
        boolean resultado = reconciliationService.reconcileOrder("SHOPEE", "240828ABC123");

        // Assert
        assertThat(resultado).isTrue();
        verify(delayQueueService).remove("SHOPEE", "240828ABC123");
        verify(orderMasterRepository, never()).save(any(OrderMaster.class));
    }

    @Test
    @DisplayName("Deve processar lote de pedidos resgatados da fila do Redis")
    void deveProcessarLoteDePedidosDaFila() {
        // Arrange
        when(delayQueueService.pollReadyOrders(50)).thenReturn(Set.of("SHOPEE:240828ABC123", "SHOPEE:240828XYZ999"));

        OrderMaster order1 = OrderMaster.builder()
                .platform("SHOPEE")
                .orderSn("240828ABC123")
                .reconciled(false)
                .metadata(Map.of("escrow_amount", "50.0000"))
                .build();

        OrderMaster order2 = OrderMaster.builder()
                .platform("SHOPEE")
                .orderSn("240828XYZ999")
                .reconciled(false)
                .metadata(Map.of("escrow_amount", "120.0000"))
                .build();

        when(orderMasterRepository.findByPlatformAndOrderSn("SHOPEE", "240828ABC123")).thenReturn(Optional.of(order1));
        when(orderMasterRepository.findByPlatformAndOrderSn("SHOPEE", "240828XYZ999")).thenReturn(Optional.of(order2));

        // Act
        int totalReconciliados = reconciliationService.reconcilePendingOrders();

        // Assert
        assertThat(totalReconciliados).isEqualTo(2);
        verify(delayQueueService).remove("SHOPEE", "240828ABC123");
        verify(delayQueueService).remove("SHOPEE", "240828XYZ999");
    }
}
