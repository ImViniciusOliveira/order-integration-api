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
import static org.mockito.Mockito.*;

/**
 * Testes unitários para validação do pipeline de ingestão de eventos e persistência no MarketplaceIngestionService.
 */
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
                new OrderIntegrationProperties.EscrowProperties(120, 30, 60000L, 50, 5),
                new OrderIntegrationProperties.SecurityProperties("test-key")
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
    @DisplayName("Deve rejeitar ingestão quando canal não estiver cadastrado ou ativo no banco")
    void deveRejeitarCanalNaoCadastradoOuInativo() {
        when(channelRepository.findByCodeAndActiveTrue("SHOPEE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ingestionService.ingestEvent("SHOPEE", "123", Map.of("order_sn", "123")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("não está ativo ou não foi cadastrado");
    }

    @Test
    @DisplayName("Deve rejeitar payload quando não possuir número do pedido")
    void deveRejeitarPayloadSemOrderSn() {
        MarketplaceChannel activeChannel = MarketplaceChannel.builder()
                .code("SHOPEE")
                .name("Shopee")
                .active(true)
                .build();

        when(channelRepository.findByCodeAndActiveTrue("SHOPEE")).thenReturn(Optional.of(activeChannel));

        assertThatThrownBy(() -> ingestionService.ingestEvent("SHOPEE", "123", Map.of("status", "READY_TO_SHIP")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("O número do pedido (ordersn) é obrigatório");
    }

    @Test
    @DisplayName("Deve processar pedido READY_TO_SHIP e salvar sem enfileirar no Redis")
    void deveProcessarPedidoReadyToShip() {
        MarketplaceChannel activeChannel = MarketplaceChannel.builder()
                .code("SHOPEE")
                .name("Shopee")
                .active(true)
                .build();

        when(channelRepository.findByCodeAndActiveTrue("SHOPEE")).thenReturn(Optional.of(activeChannel));
        when(orderMasterRepository.findByPlatformAndOrderSn("SHOPEE", "240828TEST")).thenReturn(Optional.empty());
        when(orderMasterRepository.save(any(OrderMaster.class))).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> payload = Map.of(
                "order_sn", "240828TEST",
                "order_status", "READY_TO_SHIP",
                "tracking_no", "BR987654321",
                "estimated_shipping_fee", 12.50
        );

        OrderMaster saved = ingestionService.ingestEvent("SHOPEE", "123456", payload);

        assertThat(saved).isNotNull();
        assertThat(saved.getOrderSn()).isEqualTo("240828TEST");
        assertThat(saved.getStatus()).isEqualTo("READY_TO_SHIP");
        assertThat(saved.getEstimatedShippingFee()).isEqualByComparingTo(new BigDecimal("12.5000"));
        verify(delayQueueService, never()).scheduleReconciliation(any(), any(), any(Duration.class));
    }

    @Test
    @DisplayName("Deve processar pedido COMPLETED, salvar no banco e enfileirar no Redis")
    void deveProcessarPedidoCompletedEAgendarRedis() {
        MarketplaceChannel activeChannel = MarketplaceChannel.builder()
                .code("SHOPEE")
                .name("Shopee")
                .active(true)
                .build();

        when(channelRepository.findByCodeAndActiveTrue("SHOPEE")).thenReturn(Optional.of(activeChannel));
        when(orderMasterRepository.findByPlatformAndOrderSn("SHOPEE", "240828COMP")).thenReturn(Optional.empty());
        when(orderMasterRepository.save(any(OrderMaster.class))).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> payload = Map.of(
                "order_sn", "240828COMP",
                "order_status", "COMPLETED"
        );

        OrderMaster saved = ingestionService.ingestEvent("SHOPEE", "123456", payload);

        assertThat(saved).isNotNull();
        assertThat(saved.getStatus()).isEqualTo("COMPLETED");
        verify(delayQueueService, times(1)).scheduleReconciliation("SHOPEE", "240828COMP", Duration.ofMinutes(120));
    }
}
