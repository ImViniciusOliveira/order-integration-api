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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para validar a fila com atraso (Delay Queue) no Redis.
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
                new OrderIntegrationProperties.EscrowProperties(120, 30, 60000L, 50, 5)
        );

        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        delayQueueService = new EscrowDelayQueueServiceImpl(redisTemplate, properties);
    }

    @Test
    @DisplayName("Deve agendar pedido no ZSet do Redis com timestamp futuro")
    void deveAgendarPedidoNoRedisComTimestampFuturo() {
        // Act
        delayQueueService.scheduleReconciliation("SHOPEE", "240828ABC123", Duration.ofMinutes(120));

        // Assert
        verify(zSetOperations).add(eq("dcriar:orders:escrow_delay_queue"), eq("SHOPEE:240828ABC123"), anyDouble());
    }

    @Test
    @DisplayName("Deve resgatar pedidos prontos cujo score é menor ou igual ao tempo atual")
    void deveResgatarPedidosProntosDaFila() {
        // Arrange
        when(zSetOperations.rangeByScore(eq("dcriar:orders:escrow_delay_queue"), eq(0.0), anyDouble(), eq(0L), eq(50L)))
                .thenReturn(Set.of("SHOPEE:240828ABC123"));

        // Act
        Set<String> readyOrders = delayQueueService.pollReadyOrders(50);

        // Assert
        assertThat(readyOrders).containsExactly("SHOPEE:240828ABC123");
    }

    @Test
    @DisplayName("Deve remover pedido do ZSet do Redis")
    void deveRemoverPedidoDoRedis() {
        // Act
        delayQueueService.remove("SHOPEE", "240828ABC123");

        // Assert
        verify(zSetOperations).remove("dcriar:orders:escrow_delay_queue", "SHOPEE:240828ABC123");
    }

    @Test
    @DisplayName("Deve construir member padronizado PLATFORM:ORDER_SN")
    void deveConstruirMemberPadronizado() {
        String member = EscrowDelayQueueService.buildQueueMember("shopee", "240828abc123");
        assertThat(member).isEqualTo("SHOPEE:240828abc123");
    }
}
