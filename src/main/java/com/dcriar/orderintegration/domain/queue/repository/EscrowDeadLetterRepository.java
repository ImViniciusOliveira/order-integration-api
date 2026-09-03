package com.dcriar.orderintegration.domain.queue.repository;

import com.dcriar.orderintegration.domain.queue.entity.EscrowDeadLetterEntry;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório dos pedidos que chegaram ao estado de falha permanente na conciliação.
 */
public interface EscrowDeadLetterRepository extends JpaRepository<EscrowDeadLetterEntry, Long> {
}
