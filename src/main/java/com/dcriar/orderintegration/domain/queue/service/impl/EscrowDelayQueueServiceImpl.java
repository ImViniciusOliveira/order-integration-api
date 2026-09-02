package com.dcriar.orderintegration.domain.queue.service.impl;

import com.dcriar.orderintegration.config.OrderIntegrationProperties;
import com.dcriar.orderintegration.domain.queue.service.EscrowDelayQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

/**
 * Implementação concreta do serviço de gerenciamento da fila com atraso (Delay Queue) no Redis (Sorted Set).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EscrowDelayQueueServiceImpl implements EscrowDelayQueueService {

    private final StringRedisTemplate redisTemplate;
    private final OrderIntegrationProperties properties;

    @Override
    public void scheduleReconciliation(String platform, String orderSn, Duration delay) {
        if (platform == null || orderSn == null || delay == null) {
            log.warn("Tentativa de agendamento de conciliação com parâmetros nulos: platform={}, orderSn={}", platform, orderSn);
            return;
        }

        long executionTimestamp = System.currentTimeMillis() + delay.toMillis();
        String member = EscrowDelayQueueService.buildQueueMember(platform, orderSn);
        String queueKey = getQueueKey();

        redisTemplate.opsForZSet().add(queueKey, member, executionTimestamp);
        log.info("Pedido {} ({}) agendado na fila do Redis '{}' para conciliação de Escrow em {} minutos (timestamp: {})",
                orderSn, platform, queueKey, delay.toMinutes(), executionTimestamp);
    }

    @Override
    public Set<String> pollReadyOrders(int limit) {
        long now = System.currentTimeMillis();
        String queueKey = getQueueKey();
        Set<String> readyMembers = redisTemplate.opsForZSet().rangeByScore(queueKey, 0, now, 0, limit);

        if (readyMembers == null || readyMembers.isEmpty()) {
            return Collections.emptySet();
        }

        return readyMembers;
    }

    @Override
    public void remove(String platform, String orderSn) {
        if (platform == null || orderSn == null) {
            return;
        }
        String member = EscrowDelayQueueService.buildQueueMember(platform, orderSn);
        redisTemplate.opsForZSet().remove(getQueueKey(), member);
    }

    @Override
    public String getQueueKey() {
        if (properties != null && properties.redis() != null && properties.redis().escrowQueueKey() != null) {
            return properties.redis().escrowQueueKey();
        }
        return "dcriar:orders:escrow_delay_queue";
    }
}
