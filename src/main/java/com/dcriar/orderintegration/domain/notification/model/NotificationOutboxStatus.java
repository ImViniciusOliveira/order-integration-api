package com.dcriar.orderintegration.domain.notification.model;

/**
 * Estados de processamento dos eventos persistidos na Outbox de notificações.
 */
public enum NotificationOutboxStatus {
    PENDING,
    SENT
}
