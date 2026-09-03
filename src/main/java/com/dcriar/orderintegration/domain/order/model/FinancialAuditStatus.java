package com.dcriar.orderintegration.domain.order.model;

/**
 * Estado persistente da auditoria financeira de um pedido.
 */
public enum FinancialAuditStatus {
    PENDING_SETTLEMENT,
    AUDIT_INCOMPLETE,
    RECONCILED,
    RECONCILED_WITH_DIVERGENCE,
    PERMANENT_ERROR
}
