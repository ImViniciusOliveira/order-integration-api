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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

/**
 * Testes unitários para o serviço de fila de atraso de conciliação de Escrow usando Redis ZSet.
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
                new OrderIntegrationProperties.SecurityProperties("test-key")
        );

        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        delayQueueService = new EscrowDelayQueueServiceImpl(redisTemplate, properties);
    }

    @Test
    @DisplayName("Deve agendar pedido no ZSet do Redis com timestamp futuro")
    void deveAgendarPedidoNoRedis() {
        delayQueueService.scheduleReconciliation("SHOPEE", "240828XYZ", Duration.ofMinutes(120));

        verify(zSetOperations).add(eq("dcriar:orders:escrow_delay_queue"), eq("SHOPEE:240828XYZ"), anyDouble());
    }

    @Test
    @DisplayName("Deve buscar pedidos prontos do Redis")
    void deveBuscarPedidosProntos() {
        lenient().when(zSetOperations.rangeByScore(eq("dcriar:orders:escrow_delay_queue"), eq(0.0), anyDouble(), eq(0L), eq(50L)))
                .thenReturn(Set.of("SHOPEE:240828XYZ"));

        Set<String> readyOrders = delayQueueService.pollReadyOrders(50);

        assertThat(readyOrders).containsExactly("SHOPEE:240828XYZ");
    }

    @Test
    @DisplayName("Deve remover pedido do Redis com sucesso")
    void deveRemoverPedidoDoRedis() {
        delayQueueService.remove("SHOPEE", "240828XYZ");

        verify(zSetOperations).remove("dcriar:orders:escrow_delay_queue", "SHOPEE:240828XYZ");
    }

    @Test
    @DisplayName("Deve construir member padronizado PLATFORM:ORDER_SN")
    void deveConstruirMemberPadronizado() {
        String member = EscrowDelayQueueService.buildQueueMember("shopee", "240828abc123");
        assertThat(member).isEqualTo("SHOPEE:240828abc123");
    }
}
