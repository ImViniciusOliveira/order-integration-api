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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para validar o serviço de ingestão de webhooks e garantia da ordem
 * de execução entre persistência no PostgreSQL e agendamento de delay no Redis.
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
                new OrderIntegrationProperties.EscrowProperties(120, 30, 60000L, 50, 5)
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
    @DisplayName("Deve salvar evento no Event Store, persistir no PostgreSQL e agendar delay no Redis quando status for COMPLETED")
    void deveProcessarPedidoCompletedComPersistenciaNoBancoAntesDoRedis() {
        // Arrange
        Map<String, Object> payload = Map.of(
                "order_sn", "240828ABC123",
                "status", "COMPLETED",
                "order_status", "COMPLETED",
                "estimated_shipping_fee", 14.50
        );

        MarketplaceChannel channel = MarketplaceChannel.builder()
                .id(1L)
                .code("SHOPEE")
                .name("Shopee Brasil")
                .active(true)
                .build();

        when(channelRepository.findByCodeAndActiveTrue("SHOPEE")).thenReturn(Optional.of(channel));
        when(orderMasterRepository.findByPlatformAndOrderSn("SHOPEE", "240828ABC123")).thenReturn(Optional.empty());
        when(orderMasterRepository.save(any(OrderMaster.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OrderMaster resultado = ingestionService.ingestEvent("SHOPEE", "shop_123", payload);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getOrderSn()).isEqualTo("240828ABC123");
        assertThat(resultado.getStatus()).isEqualTo("COMPLETED");
        assertThat(resultado.getEstimatedShippingFee()).isEqualByComparingTo(new BigDecimal("14.50"));

        // 1. Verifica gravação imutável no Event Store
        verify(rawEventRepository).save(any());

        // 2. Verifica gravação no banco PostgreSQL
        verify(orderMasterRepository).save(any(OrderMaster.class));

        // 3. Verifica agendamento no Redis com delay configurado de 120 minutos
        verify(delayQueueService).scheduleReconciliation("SHOPEE", "240828ABC123", Duration.ofMinutes(120));
    }

    @Test
    @DisplayName("Deve persistir evento READY_TO_SHIP com frete estimado no PostgreSQL e NÃO agendar no Redis")
    void deveProcessarPedidoReadyToShipSemEnfileirarNoRedis() {
        // Arrange
        Map<String, Object> payload = Map.of(
                "order_sn", "240828ABC123",
                "status", "READY_TO_SHIP",
                "order_status", "READY_TO_SHIP",
                "estimated_shipping_fee", 12.00,
                "tracking_no", "BR240828TRACK"
        );

        MarketplaceChannel channel = MarketplaceChannel.builder()
                .id(1L)
                .code("SHOPEE")
                .name("Shopee Brasil")
                .active(true)
                .build();

        when(channelRepository.findByCodeAndActiveTrue("SHOPEE")).thenReturn(Optional.of(channel));
        when(orderMasterRepository.findByPlatformAndOrderSn("SHOPEE", "240828ABC123")).thenReturn(Optional.empty());
        when(orderMasterRepository.save(any(OrderMaster.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OrderMaster resultado = ingestionService.ingestEvent("SHOPEE", "shop_123", payload);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getStatus()).isEqualTo("READY_TO_SHIP");
        assertThat(resultado.getTrackingNo()).isEqualTo("BR240828TRACK");

        verify(orderMasterRepository).save(any(OrderMaster.class));
        verify(delayQueueService, never()).scheduleReconciliation(eq("SHOPEE"), eq("240828ABC123"), any(Duration.class));
    }

    @Test
    @DisplayName("Deve lançar exceção se o canal de marketplace estiver inativo ou não cadastrado")
    void deveLancarExcecaoQuandoCanalEstiverInativo() {
        when(channelRepository.findByCodeAndActiveTrue("MERCADO_LIVRE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ingestionService.ingestEvent("MERCADO_LIVRE", "shop_123", Map.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Canal de marketplace 'MERCADO_LIVRE' não está ativo ou não foi cadastrado");
    }
}
