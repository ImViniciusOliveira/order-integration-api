package com.dcriar.orderintegration.domain.queue.repository;

import com.dcriar.orderintegration.domain.queue.entity.EscrowDeadLetterEntry;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório dos pedidos que chegaram ao estado de falha permanente na conciliação.
 */
public interface EscrowDeadLetterRepository extends JpaRepository<EscrowDeadLetterEntry, Long> {

    /**
     * Verifica se o pedido já possui registro persistido na DLQ.
     *
     * @param platform plataforma de origem
     * @param orderSn número do pedido
     * @return {@code true} quando já existe uma entrada para o pedido
     */
    boolean existsByPlatformAndOrderSn(String platform, String orderSn);
}
