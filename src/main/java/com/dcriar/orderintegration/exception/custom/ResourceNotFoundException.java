package com.dcriar.orderintegration.exception.custom;

/**
 * Exceção de negócio lançada quando um recurso solicitado (pedido, canal de marketplace, etc.)
 * não for localizado no banco de dados ou repositório.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(String.format("Recurso '%s' com identificador '%s' não foi encontrado.", resourceName, identifier));
    }
}
