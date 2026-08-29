package com.dcriar.orderintegration.domain.order.service;

import com.dcriar.orderintegration.config.OrderIntegrationProperties;
import com.dcriar.orderintegration.domain.channel.entity.MarketplaceChannel;
import com.dcriar.orderintegration.domain.channel.repository.MarketplaceChannelRepository;
import com.dcriar.orderintegration.domain.order.entity.MarketplaceRawEvent;
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
import static org.mockito.Mockito.*;

/**
 * Suite de testes unitários para a implementação do serviço {@link MarketplaceIngestionServiceImpl}.
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
                new OrderIntegrationProperties.SecurityProperties("test-key"),
                new OrderIntegrationProperties.CorsProperties(List.of("http://localhost:8081"))
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
    @DisplayName("Deve ingerir com sucesso evento da Shopee com status READY_TO_SHIP e persistir dados mestres")
    void deveIngerirEventoShopeeReadyToShipComSucesso() {
        // Arrange
        String platform = "SHOPEE";
        String shopId = "shop_123";
        Map<String, Object> payload = Map.of(
                "order_sn", "240828ABC123",
                "order_status", "READY_TO_SHIP",
                "tracking_no", "BR123456789",
                "estimated_shipping_fee", "12.5000"
        );

        MarketplaceChannel channel = MarketplaceChannel.builder()
                .code("SHOPEE")
                .name("Shopee Oficial")
                .active(true)
                .build();

        when(channelRepository.findByCodeAndActiveTrue("SHOPEE")).thenReturn(Optional.of(channel));
        when(orderMasterRepository.findByPlatformAndOrderSn("SHOPEE", "240828ABC123")).thenReturn(Optional.empty());
        when(orderMasterRepository.save(any(OrderMaster.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OrderMaster result = ingestionService.ingestEvent(platform, shopId, payload);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPlatform()).isEqualTo("SHOPEE");
        assertThat(result.getOrderSn()).isEqualTo("240828ABC123");
        assertThat(result.getStatus()).isEqualTo("READY_TO_SHIP");
        assertThat(result.getTrackingNo()).isEqualTo("BR123456789");
        assertThat(result.getEstimatedShippingFee()).isEqualByComparingTo(new BigDecimal("12.5000"));

        verify(rawEventRepository).save(any(MarketplaceRawEvent.class));
        verify(orderMasterRepository).save(any(OrderMaster.class));
        verifyNoInteractions(delayQueueService);
    }

    @Test
    @DisplayName("Deve agendar conciliação no Redis quando o status do pedido for COMPLETED")
    void deveAgendarConciliacaoQuandoStatusCompleted() {
        // Arrange
        String platform = "SHOPEE";
        String shopId = "shop_123";
        Map<String, Object> payload = Map.of(
                "order_sn", "240828ABC123",
                "order_status", "COMPLETED",
                "tracking_no", "BR123456789"
        );

        MarketplaceChannel channel = MarketplaceChannel.builder()
                .code("SHOPEE")
                .name("Shopee Oficial")
                .active(true)
                .build();

        when(channelRepository.findByCodeAndActiveTrue("SHOPEE")).thenReturn(Optional.of(channel));
        when(orderMasterRepository.findByPlatformAndOrderSn("SHOPEE", "240828ABC123")).thenReturn(Optional.empty());
        when(orderMasterRepository.save(any(OrderMaster.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OrderMaster result = ingestionService.ingestEvent(platform, shopId, payload);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("COMPLETED");

        verify(delayQueueService).scheduleReconciliation(eq("SHOPEE"), eq("240828ABC123"), eq(Duration.ofMinutes(120)));
    }

    @Test
    @DisplayName("Deve lançar IllegalStateException quando o canal de marketplace estiver inativo ou não cadastrado")
    void deveLancarExcecaoQuandoCanalInativo() {
        // Arrange
        when(channelRepository.findByCodeAndActiveTrue("SHOPEE")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> ingestionService.ingestEvent("SHOPEE", "shop_123", Map.of("order_sn", "123")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("não está ativo");

        verifyNoInteractions(rawEventRepository);
        verifyNoInteractions(orderMasterRepository);
    }
}
