package com.dcriar.orderintegration.domain.order.service;

/**
 * Contrato de serviço responsável pela orquestração do ciclo de conciliação financeira
 * pós-venda (Escrow / Settlement) de pedidos de marketplaces.
 */
public interface EscrowReconciliationService {

    /**
     * Resgata da fila de atraso do Redis os pedidos cujo tempo de delay já foi atingido
     * e executa o processo de conciliação financeira de cada um em lote.
     *
     * @return quantidade total de pedidos conciliados com sucesso nesta execução
     */
    int reconcilePendingOrders();

    /**
     * Executa a conciliação financeira individual de um pedido específico,
     * verificando a disponibilidade do extrato de liquidação, aplicando as taxas reais
     * no modelo de domínio e persistindo no PostgreSQL.
     *
     * @param platform código da plataforma de venda (ex: "SHOPEE")
     * @param orderSn  número único do pedido
     * @return {@code true} se o pedido foi conciliado com sucesso; {@code false} se foi reagendado para nova tentativa
     */
    boolean reconcileOrder(String platform, String orderSn);
}
