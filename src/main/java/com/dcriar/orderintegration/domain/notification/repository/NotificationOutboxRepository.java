package com.dcriar.orderintegration.domain.notification.repository;

import com.dcriar.orderintegration.domain.notification.entity.NotificationOutboxEvent;
import com.dcriar.orderintegration.domain.notification.model.NotificationOutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Repositório JPA dos eventos de notificação pendentes ou já entregues.
 */
public interface NotificationOutboxRepository extends JpaRepository<NotificationOutboxEvent, Long> {

    /**
     * Busca um lote limitado de eventos prontos para nova tentativa.
     *
     * @param status estado do evento
     * @param now instante limite para liberação
     * @return eventos ordenados pela criação
     */
    List<NotificationOutboxEvent> findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            NotificationOutboxStatus status,
            OffsetDateTime now
    );
}
