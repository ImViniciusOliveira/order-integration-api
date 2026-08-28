package com.dcriar.orderintegration.domain.order.service;

import com.dcriar.orderintegration.config.OrderIntegrationProperties;
import com.dcriar.orderintegration.domain.channel.entity.MarketplaceChannel;
import com.dcriar.orderintegration.domain.channel.repository.MarketplaceChannelRepository;
import com.dcriar.orderintegration.domain.order.entity.OrderMaster;
import com.dcriar.orderintegration.domain.order.processor.ShopeeOrderProcessor;
import com.dcriar.orderintegration.domain.order.repository.MarketplaceRawEventRepository;
import com.dcriar.orderintegration.domain.order.repository.OrderMasterRepository;
import com.dcriar.orderintegration.domain.order.service.impl.MarketplaceIngestionServiceImpl;
import com.dcriar.orderintegration.domain.queue.service.EscrowDelayQueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketplaceIngestionServiceTest {

    @Mock
    private MarketplaceChannelRepository channelRepository;

    @Mock
    private MarketplaceRawEventRepository rawEventRepository;

    @Mock
    private OrderMasterRepository orderMasterRepository;

    @Mock
    private EscrowDelayQueueService delayQueueService;

    private MarketplaceIngestionService ingestionService;

    @BeforeEach
    void setUp() {
        ShopeeOrderProcessor shopeeProcessor = new ShopeeOrderProcessor();
        OrderIntegrationProperties properties = new OrderIntegrationProperties(
                new OrderIntegrationProperties.RedisProperties("dcriar:orders:escrow_delay_queue"),
                new OrderIntegrationProperties.EscrowProperties(120)
        );

        ingestionService = new MarketplaceIngestionServiceImpl(
                channelRepository,
                rawEventRepository,
                orderMasterRepository,
                delayQueueService,
                properties,
                List.of(shopeeProcessor)
        );
    }

    @Test
    @DisplayName("Deve provisionar pedido Shopee no status READY_TO_SHIP e salvar sem agendar no Redis")
    void shouldIngestShopeeReadyToShipOrderSuccessfully() {
        MarketplaceChannel channel = MarketplaceChannel.builder()
                .code("SHOPEE")
                .name("Shopee Brasil")
                .active(true)
                .build();

        when(channelRepository.findByCodeAndActiveTrue("SHOPEE")).thenReturn(Optional.of(channel));
        when(orderMasterRepository.findByPlatformAndOrderSn("SHOPEE", "240828ABC123")).thenReturn(Optional.empty());
        when(orderMasterRepository.save(any(OrderMaster.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> payload = Map.of(
                "ordersn", "240828ABC123",
                "shop_id", "shop_999",
                "order_status", "READY_TO_SHIP",
                "tracking_no", "BR123456789SP",
                "estimated_shipping_fee", "15.50"
        );

        OrderMaster saved = ingestionService.ingestEvent("SHOPEE", "shop_999", payload);

        assertThat(saved).isNotNull();
        assertThat(saved.getPlatform()).isEqualTo("SHOPEE");
        assertThat(saved.getOrderSn()).isEqualTo("240828ABC123");
        assertThat(saved.getStatus()).isEqualTo("READY_TO_SHIP");
        assertThat(saved.getTrackingNo()).isEqualTo("BR123456789SP");
        assertThat(saved.getEstimatedShippingFee()).isEqualByComparingTo(new BigDecimal("15.50"));
        assertThat(saved.isReconciled()).isFalse();

        verify(rawEventRepository, times(1)).save(any());
        verify(orderMasterRepository, times(1)).save(any(OrderMaster.class));
        verifyNoInteractions(delayQueueService);
    }

    @Test
    @DisplayName("Deve atualizar pedido para COMPLETED, salvar no PostgreSQL primeiro e agendar no Redis depois")
    void shouldIngestCompletedOrderAndRespectExecutionOrder() {
        MarketplaceChannel channel = MarketplaceChannel.builder()
                .code("SHOPEE")
                .name("Shopee Brasil")
                .active(true)
                .build();

        OrderMaster existingOrder = OrderMaster.builder()
                .platform("SHOPEE")
                .shopId("shop_999")
                .orderSn("240828ABC123")
                .status("READY_TO_SHIP")
                .reconciled(false)
                .build();

        when(channelRepository.findByCodeAndActiveTrue("SHOPEE")).thenReturn(Optional.of(channel));
        when(orderMasterRepository.findByPlatformAndOrderSn("SHOPEE", "240828ABC123")).thenReturn(Optional.of(existingOrder));
        when(orderMasterRepository.save(any(OrderMaster.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> payload = Map.of(
                "ordersn", "240828ABC123",
                "order_status", "COMPLETED"
        );

        OrderMaster saved = ingestionService.ingestEvent("SHOPEE", "shop_999", payload);

        assertThat(saved.getStatus()).isEqualTo("COMPLETED");

        // Verificação estrita da ordem de execução: Banco PRIMEIRO, Redis DEPOIS
        InOrder inOrder = inOrder(orderMasterRepository, delayQueueService);
        inOrder.verify(orderMasterRepository).save(any(OrderMaster.class));
        inOrder.verify(delayQueueService).scheduleReconciliation(eq("SHOPEE"), eq("240828ABC123"), eq(Duration.ofMinutes(120)));
    }

    @Test
    @DisplayName("Deve rejeitar ingestão quando canal não estiver ativo no banco")
    void shouldRejectIngestionWhenChannelIsInactive() {
        when(channelRepository.findByCodeAndActiveTrue("SHOPEE")).thenReturn(Optional.empty());

        Map<String, Object> payload = Map.of("ordersn", "240828ABC123");

        assertThatThrownBy(() -> ingestionService.ingestEvent("SHOPEE", "shop_1", payload))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("não está ativo");

        verifyNoInteractions(rawEventRepository);
        verifyNoInteractions(orderMasterRepository);
        verifyNoInteractions(delayQueueService);
    }
}
