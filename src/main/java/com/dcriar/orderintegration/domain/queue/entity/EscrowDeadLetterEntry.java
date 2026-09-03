package com.dcriar.orderintegration.domain.queue.entity;

import com.dcriar.orderintegration.domain.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Registro permanente de pedidos que não puderam ser conciliados pelo worker de Escrow.
 */
@Entity
@Table(
        name = "escrow_dead_letter",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_escrow_dead_letter_platform_order_sn",
                columnNames = {"platform", "order_sn"}
        )
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EscrowDeadLetterEntry extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "platform", nullable = false, length = 30)
    private String platform;

    @Column(name = "order_sn", nullable = false, length = 100)
    private String orderSn;

    @Column(name = "reason", nullable = false, length = 100)
    private String reason;

    @Column(name = "attempts", nullable = false)
    private long attempts;

    @Column(name = "failed_at", nullable = false)
    private OffsetDateTime failedAt;

    /**
     * Cria o registro de falha permanente de um pedido.
     *
     * @param platform plataforma de origem
     * @param orderSn identificador do pedido
     * @param reason motivo do descarte
     * @param attempts quantidade de tentativas realizadas
     * @return entrada pronta para persistência
     */
    public static EscrowDeadLetterEntry create(
            String platform,
            String orderSn,
            String reason,
            long attempts
    ) {
        return EscrowDeadLetterEntry.builder()
                .platform(platform.toUpperCase())
                .orderSn(orderSn)
                .reason(reason)
                .attempts(attempts)
                .failedAt(OffsetDateTime.now())
                .build();
    }
}
