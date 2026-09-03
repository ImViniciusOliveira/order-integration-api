package com.dcriar.orderintegration.domain.notification.entity;

import com.dcriar.orderintegration.domain.common.AuditableEntity;
import com.dcriar.orderintegration.domain.notification.model.NotificationOutboxStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Evento de notificação persistido transacionalmente antes da entrega ao n8n.
 * <p>
 * A entidade garante que falhas temporárias da integração externa não eliminem
 * a intenção de notificar sobre uma conciliação financeira concluída.
 */
@Entity
@Table(name = "notification_outbox")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationOutboxEvent extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(name = "aggregate_type", nullable = false, length = 80)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 180)
    private String aggregateId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private NotificationOutboxStatus status = NotificationOutboxStatus.PENDING;

    @Column(name = "attempts", nullable = false)
    @Builder.Default
    private int attempts = 0;

    @Column(name = "next_attempt_at", nullable = false)
    private OffsetDateTime nextAttemptAt;

    @Column(name = "last_error", length = 2000)
    private String lastError;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    /**
     * Cria uma notificação pendente para processamento assíncrono.
     *
     * @param eventType tipo do evento externo
     * @param aggregateType tipo do agregado de domínio
     * @param aggregateId identificador natural do agregado
     * @param payload conteúdo serializável da notificação
     * @return evento pronto para persistência na Outbox
     */
    public static NotificationOutboxEvent pending(
            String eventType,
            String aggregateType,
            String aggregateId,
            Map<String, Object> payload
    ) {
        return NotificationOutboxEvent.builder()
                .eventType(eventType)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .payload(payload)
                .nextAttemptAt(OffsetDateTime.now())
                .build();
    }

    /**
     * Marca o evento como entregue com sucesso.
     */
    public void markSent() {
        this.status = NotificationOutboxStatus.SENT;
        this.sentAt = OffsetDateTime.now();
        this.lastError = null;
    }

    /**
     * Registra uma falha transitória e agenda uma nova tentativa.
     *
     * @param errorMessage mensagem resumida da falha externa
     */
    public void registerFailure(String errorMessage) {
        this.attempts++;
        this.lastError = errorMessage;
        this.nextAttemptAt = OffsetDateTime.now().plusMinutes(Math.min(60, 5L * attempts));
    }
}
