package com.dcriar.orderintegration.domain.order.model;

/**
 * Situação do resultado retornado por uma consulta financeira ao marketplace.
 */
public enum SettlementStatus {

    /**
     * O extrato foi localizado e contém dados financeiros utilizáveis.
     */
    AVAILABLE,

    /**
     * O pedido existe, mas o extrato ainda não foi liberado pela plataforma.
     */
    PENDING,

    /**
     * A consulta falhou de forma temporária e pode ser repetida.
     */
    RETRYABLE_ERROR,

    /**
     * A consulta falhou de forma definitiva e não deve entrar em retry automático.
     */
    PERMANENT_ERROR
}
