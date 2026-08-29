package com.dcriar.orderintegration.exception.handler;

import com.dcriar.orderintegration.exception.custom.BusinessException;
import com.dcriar.orderintegration.exception.custom.InvalidOrderPayloadException;
import com.dcriar.orderintegration.exception.custom.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para validar a formatação padronizada RFC 7807 (ProblemDetail) no GlobalExceptionHandler.
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        when(request.getRequestURI()).thenReturn("/api/v1/orders/123");
    }

    @Test
    @DisplayName("Deve retornar HTTP 404 ProblemDetail quando ResourceNotFoundException for lançada")
    void deveTratarResourceNotFoundException() {
        // Arrange
        ResourceNotFoundException ex = new ResourceNotFoundException("Pedido não encontrado");

        // Act
        ResponseEntity<ProblemDetail> response = exceptionHandler.handleResourceNotFoundException(ex, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Recurso não encontrado");
        assertThat(response.getBody().getDetail()).isEqualTo("Pedido não encontrado");
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("Deve retornar HTTP 422 ProblemDetail quando BusinessException for lançada")
    void deveTratarBusinessException() {
        // Arrange
        BusinessException ex = new BusinessException("Canal de venda já cadastrado");

        // Act
        ResponseEntity<ProblemDetail> response = exceptionHandler.handleBusinessException(ex, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Violação de regra de negócio");
        assertThat(response.getBody().getDetail()).isEqualTo("Canal de venda já cadastrado");
        assertThat(response.getBody().getStatus()).isEqualTo(422);
    }

    @Test
    @DisplayName("Deve retornar HTTP 400 ProblemDetail quando InvalidOrderPayloadException for lançada")
    void deveTratarInvalidOrderPayloadException() {
        // Arrange
        InvalidOrderPayloadException ex = new InvalidOrderPayloadException("Payload do pedido sem ordersn");

        // Act
        ResponseEntity<ProblemDetail> response = exceptionHandler.handleInvalidPayloadException(ex, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Requisição inválida");
        assertThat(response.getBody().getDetail()).isEqualTo("Payload do pedido sem ordersn");
        assertThat(response.getBody().getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("Deve retornar HTTP 500 ProblemDetail quando uma exceção genérica inesperada for lançada")
    void deveTratarExcecaoGenerica() {
        // Arrange
        Exception ex = new RuntimeException("Erro de conexão de banco");

        // Act
        ResponseEntity<ProblemDetail> response = exceptionHandler.handleUncaughtException(ex, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Erro interno no servidor");
        assertThat(response.getBody().getStatus()).isEqualTo(500);
    }
}
