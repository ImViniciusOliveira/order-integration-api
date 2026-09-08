package com.dcriar.orderintegration.domain.queue.repository;

import com.dcriar.orderintegration.domain.queue.entity.EscrowDeadLetterEntry;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositório dos pedidos que chegaram ao estado de falha permanente na conciliação.
 */
public interface EscrowDeadLetterRepository extends JpaRepository<EscrowDeadLetterEntry, Long> {

    /**
     * Persiste uma entrada na DLQ sem falhar quando o pedido já estiver registrado.
     *
     * @param platform plataforma de origem
     * @param orderSn número do pedido
     * @param reason motivo do descarte
     * @param attempts quantidade de tentativas realizadas
     * @return quantidade de registros inseridos
     */
    @Modifying
    @Query(value = """
            INSERT INTO escrow_dead_letter (platform, order_sn, reason, attempts, failed_at)
            VALUES (:platform, :orderSn, :reason, :attempts, CURRENT_TIMESTAMP)
            ON CONFLICT (platform, order_sn) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("platform") String platform,
            @Param("orderSn") String orderSn,
            @Param("reason") String reason,
            @Param("attempts") long attempts
    );
}
