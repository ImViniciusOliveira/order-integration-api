package com.dcriar.orderintegration.domain.queue.service;

import java.time.Duration;
import java.util.Set;

/**
 * Contrato de serviço responsável por gerenciar a fila com atraso (Delay Queue) no Redis
 * utilizando estrutura de Sorted Set (ZSet) para agendamento de conciliações de Escrow pós-entrega.
 */
public interface EscrowDelayQueueService {

    /**
     * Agenda a conciliação de um pedido adicionando-o ao ZSet do Redis com pontuação (score)
     * correspondente ao timestamp Unix em milissegundos em que o delay expira.
     *
     * @param platform código do marketplace (ex: "SHOPEE")
     * @param orderSn  número único do pedido
     * @param delay    tempo de espera antes da conciliação
     */
    void scheduleReconciliation(String platform, String orderSn, Duration delay);

    /**
     * Resgata pedidos cujo timestamp de delay já foi atingido (score <= timestamp atual).
     *
     * @param limit quantidade máxima de registros a recuperar por lote
     * @return conjunto de identificadores formatados como "PLATFORM:ORDER_SN"
     */
    Set<String> pollReadyOrders(int limit);

    /**
     * Remove um pedido da fila de atraso do Redis após a conclusão do processamento.
     *
     * @param platform código da plataforma
     * @param orderSn  número do pedido
     */
    void remove(String platform, String orderSn);

    /**
     * Incrementa a quantidade de tentativas de conciliação de um pedido.
     *
     * @param platform código do marketplace
     * @param orderSn número do pedido
     * @return quantidade atualizada de tentativas
     */
    long incrementRetry(String platform, String orderSn);

    /**
     * Limpa o contador de tentativas após o encerramento do processamento.
     *
     * @param platform código do marketplace
     * @param orderSn número do pedido
     */
    void clearRetry(String platform, String orderSn);

    /**
     * Move o pedido para a fila de quarentena de falhas definitivas ou excedentes.
     *
     * @param platform código do marketplace
     * @param orderSn número do pedido
     */
    void moveToDeadLetterQueue(String platform, String orderSn);

    /**
     * Retorna a chave configurada da fila de atraso no Redis.
     *
     * @return nome da chave no Redis
     */
    String getQueueKey();

    /**
     * Monta o identificador padrão do item na fila do Redis.
     *
     * @param platform plataforma de origem
     * @param orderSn  código do pedido
     * @return string formatada no padrão "PLATFORM:ORDER_SN"
     */
    static String buildQueueMember(String platform, String orderSn) {
        if (platform == null || orderSn == null) {
            throw new IllegalArgumentException("Plataforma e orderSn não podem ser nulos para gerar member de fila");
        }
        return platform.toUpperCase() + ":" + orderSn;
    }
}
