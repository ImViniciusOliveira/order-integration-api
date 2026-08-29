package com.dcriar.orderintegration.exception.custom;

/**
 * Exceção de negócio para violações de regras de domínio, estados inconsistentes ou dados conflitantes.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
