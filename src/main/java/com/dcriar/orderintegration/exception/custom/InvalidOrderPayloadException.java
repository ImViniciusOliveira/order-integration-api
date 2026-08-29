package com.dcriar.orderintegration.exception.custom;

/**
 * Exceção lançada quando o payload recebido via webhook ou requisição de pedido for inválido,
 * corrompido ou desprovido dos atributos mínimos obrigatórios para processamento.
 */
public class InvalidOrderPayloadException extends RuntimeException {

    public InvalidOrderPayloadException(String message) {
        super(message);
    }

    public InvalidOrderPayloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
