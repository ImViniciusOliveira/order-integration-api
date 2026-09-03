package com.dcriar.orderintegration.domain.notification.worker;

import com.dcriar.orderintegration.domain.notification.service.impl.N8nOrderReconciliationNotificationServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Worker que entrega eventos persistidos na Outbox após a transação principal.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationOutboxWorker {

    private final N8nOrderReconciliationNotificationServiceImpl notificationService;

    /**
     * Processa notificações pendentes em intervalo regular.
     */
    @Scheduled(fixedDelayString = "${order-integration.escrow.worker-interval-ms:60000}")
    public void dispatchPendingNotifications() {
        int sentCount = notificationService.dispatchPendingNotifications();
        if (sentCount > 0) {
            log.info("Worker da Outbox entregou {} notificação(ões) ao n8n.", sentCount);
        }
    }
}
