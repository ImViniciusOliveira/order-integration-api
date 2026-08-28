package com.dcriar.orderintegration.service.queue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

/**
 * Serviço responsável por gerenciar a fila com atraso (Delay Queue) no Redis
 * utilizando estrutura de Sorted Set (ZSet) para agendamento de conciliações de Escrow pós-entrega.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EscrowDelayQueueService {

    public static final String ESCROW_DELAY_QUEUE_KEY = "dcriar:orders:escrow_delay_queue";

    private final StringRedisTemplate redisTemplate;

    /**
     * Agenda a conciliação de um pedido adicionando-o ao ZSet do Redis com pontuação (score)
     * correspondente ao timestamp Unix em milissegundos em que o delay expira.
     *
     * @param platform código do marketplace (ex: "SHOPEE", "TIKTOK")
     * @param orderSn  número único do pedido
     * @param delay    tempo de espera antes da conciliação (ex: Duration.ofHours(2))
     */
    public void scheduleReconciliation(String platform, String orderSn, Duration delay) {
        if (platform == null || orderSn == null || delay == null) {
            log.warn("Tentativa de agendamento de conciliação com parâmetros nulos: platform={}, orderSn={}", platform, orderSn);
            return;
        }

        long executionTimestamp = System.currentTimeMillis() + delay.toMillis();
        String member = buildQueueMember(platform, orderSn);

        redisTemplate.opsForZSet().add(ESCROW_DELAY_QUEUE_KEY, member, executionTimestamp);
        log.info("Pedido {} ({}) agendado na fila do Redis para conciliação de Escrow em {} minutos (timestamp: {})",
                orderSn, platform, delay.toMinutes(), executionTimestamp);
    }

    /**
     * Coleta os pedidos cujo tempo de delay já foi atingido (score <= timestamp atual).
     *
     * @param limit quantidade máxima de registros a recuperar por lote
     * @return conjunto de identificadores formatados como "PLATFORM:ORDER_SN"
     */
    public Set<String> pollReadyOrders(int limit) {
        long now = System.currentTimeMillis();
        Set<String> readyMembers = redisTemplate.opsForZSet().rangeByScore(ESCROW_DELAY_QUEUE_KEY, 0, now, 0, limit);

        if (readyMembers == null || readyMembers.isEmpty()) {
            return Collections.emptySet();
        }

        return readyMembers;
    }

    /**
     * Remove um pedido da fila de atraso do Redis após a conclusão do processamento.
     *
     * @param platform código da plataforma
     * @param orderSn  número do pedido
     */
    public void remove(String platform, String orderSn) {
        String member = buildQueueMember(platform, orderSn);
        redisTemplate.opsForZSet().remove(ESCROW_DELAY_QUEUE_KEY, member);
    }

    /**
     * Monta o identificador padrão do item na fila do Redis.
     *
     * @param platform plataforma de origem
     * @param orderSn  código do pedido
     * @return string formatada no padrão "PLATFORM:ORDER_SN"
     */
    public static String buildQueueMember(String platform, String orderSn) {
        return platform.trim().toUpperCase() + ":" + orderSn.trim();
    }
}
