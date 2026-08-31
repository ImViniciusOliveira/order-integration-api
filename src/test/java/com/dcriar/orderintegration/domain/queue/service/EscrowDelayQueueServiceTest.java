package com.dcriar.orderintegration.domain.queue.service;

import com.dcriar.orderintegration.config.OrderIntegrationProperties;
import com.dcriar.orderintegration.domain.queue.service.impl.EscrowDelayQueueServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para o serviço de fila de delay do Redis ({@link EscrowDelayQueueServiceImpl}).
 */
@ExtendWith(MockitoExtension.class)
class EscrowDelayQueueServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private EscrowDelayQueueService delayQueueService;

    @BeforeEach
    void setUp() {
        OrderIntegrationProperties properties = new OrderIntegrationProperties(
                new OrderIntegrationProperties.RedisProperties("dcriar:orders:escrow_delay_queue"),
                new OrderIntegrationProperties.EscrowProperties(120, 30, 60000L, 50, 5),
                new OrderIntegrationProperties.SecurityProperties("test-key"),
                new OrderIntegrationProperties.CorsProperties(List.of("http://localhost:8081")),
                new OrderIntegrationProperties.NotificationProperties("http://n8n-order:5678/webhook/v1/notifications/reconciled")
        );

        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        delayQueueService = new EscrowDelayQueueServiceImpl(redisTemplate, properties);
    }

    @Test
    @DisplayName("Deve agendar pedido no ZSet do Redis com timestamp futuro")
    void deveAgendarPedidoNoRedis() {
        // Arrange
        String platform = "SHOPEE";
        String orderSn = "240828ABC123";
        Duration delay = Duration.ofMinutes(120);
        String expectedMember = "SHOPEE:240828ABC123";

        when(zSetOperations.add(eq("dcriar:orders:escrow_delay_queue"), eq(expectedMember), anyDouble()))
                .thenReturn(Boolean.TRUE);

        // Act
        delayQueueService.scheduleReconciliation(platform, orderSn, delay);

        // Assert
        verify(zSetOperations, times(1)).add(
                eq("dcriar:orders:escrow_delay_queue"),
                eq(expectedMember),
                doubleThat(score -> score >= Instant.now().plusSeconds(120 * 60L - 5).toEpochMilli())
        );
    }

    @Test
    @DisplayName("Deve buscar pedidos maduros prontos para conciliação até o timestamp atual")
    void deveBuscarPedidosProntos() {
        // Arrange
        int limit = 50;
        Set<String> expectedReadyOrders = Set.of("SHOPEE:240828ABC123", "SHOPEE:240828XYZ999");

        when(zSetOperations.rangeByScore(
                eq("dcriar:orders:escrow_delay_queue"),
                eq(0.0),
                anyDouble(),
                eq(0L),
                eq((long) limit)
        )).thenReturn(expectedReadyOrders);

        // Act
        Set<String> result = delayQueueService.pollReadyOrders(limit);

        // Assert
        assertThat(result).hasSize(2).containsExactlyInAnyOrderElementsOf(expectedReadyOrders);
        verify(zSetOperations, times(1)).rangeByScore(
                eq("dcriar:orders:escrow_delay_queue"),
                eq(0.0),
                doubleThat(score -> score <= Instant.now().plusSeconds(5).toEpochMilli()),
                eq(0L),
                eq((long) limit)
        );
    }

    @Test
    @DisplayName("Deve remover member da fila do Redis após processamento")
    void deveRemoverDaFila() {
        // Arrange
        String platform = "SHOPEE";
        String orderSn = "240828ABC123";
        String expectedMember = "SHOPEE:240828ABC123";
        when(zSetOperations.remove("dcriar:orders:escrow_delay_queue", expectedMember)).thenReturn(1L);

        // Act
        delayQueueService.remove(platform, orderSn);

        // Assert
        verify(zSetOperations, times(1)).remove("dcriar:orders:escrow_delay_queue", expectedMember);
    }
}
