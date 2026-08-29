package com.dcriar.orderintegration.exception.handler;

import com.dcriar.orderintegration.exception.custom.BusinessException;
import com.dcriar.orderintegration.exception.custom.InvalidOrderPayloadException;
import com.dcriar.orderintegration.exception.custom.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Manipulador global de exceções da aplicação REST em conformidade com a especificação RFC 7807 (ProblemDetail).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String BASE_TYPE_URI = "https://api.dcriar.com/errors/";

    /**
     * Trata exceções de recursos não encontrados (HTTP 404).
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Recurso não encontrado: path={}, mensagem={}", request.getRequestURI(), ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Recurso não encontrado");
        problemDetail.setType(URI.create(BASE_TYPE_URI + "resource-not-found"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    /**
     * Trata exceções de regras de negócio e conflitos de domínio (HTTP 422).
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.warn("Violação de regra de negócio: path={}, mensagem={}", request.getRequestURI(), ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problemDetail.setTitle("Violação de regra de negócio");
        problemDetail.setType(URI.create(BASE_TYPE_URI + "business-rule-violation"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problemDetail);
    }

    /**
     * Trata estados inválidos e canais inativos (HTTP 422 / 400).
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleIllegalStateException(IllegalStateException ex, HttpServletRequest request) {
        log.warn("Estado inválido de operação: path={}, mensagem={}", request.getRequestURI(), ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problemDetail.setTitle("Operação não permitida");
        problemDetail.setType(URI.create(BASE_TYPE_URI + "illegal-state"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problemDetail);
    }

    /**
     * Trata payloads inválidos e argumentos incorretos (HTTP 400).
     */
    @ExceptionHandler({InvalidOrderPayloadException.class, IllegalArgumentException.class})
    public ResponseEntity<ProblemDetail> handleInvalidPayloadException(RuntimeException ex, HttpServletRequest request) {
        log.warn("Dados de requisição inválidos: path={}, mensagem={}", request.getRequestURI(), ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Requisição inválida");
        problemDetail.setType(URI.create(BASE_TYPE_URI + "invalid-request-payload"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    /**
     * Trata erros de validação de DTOs disparados pelo Bean Validation (@Valid / @NotNull / @NotBlank) (HTTP 400).
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        List<Map<String, String>> invalidParams = ex.getBindingResult().getFieldErrors().stream()
                .map(this::mapFieldError)
                .toList();

        String uri = (request instanceof ServletWebRequest servletWebRequest)
                ? servletWebRequest.getRequest().getRequestURI()
                : "/";

        log.warn("Falha de validação nos parâmetros da requisição: path={}, erros={}", uri, invalidParams);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Um ou mais campos da requisição possuem valores inválidos. Corrija-os e tente novamente."
        );
        problemDetail.setTitle("Dados de entrada inválidos");
        problemDetail.setType(URI.create(BASE_TYPE_URI + "validation-error"));
        problemDetail.setInstance(URI.create(uri));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("invalidParams", invalidParams);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    /**
     * Trata qualquer erro inesperado ou exceção não mapeada de infraestrutura (HTTP 500).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUncaughtException(Exception ex, HttpServletRequest request) {
        log.error("Erro interno inesperado no servidor: path={}, erro={}", request.getRequestURI(), ex.getMessage(), ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro interno inesperado no servidor. Por favor, tente novamente mais tarde."
        );
        problemDetail.setTitle("Erro interno no servidor");
        problemDetail.setType(URI.create(BASE_TYPE_URI + "internal-server-error"));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }

    private Map<String, String> mapFieldError(FieldError fieldError) {
        return Map.of(
                "name", fieldError.getField(),
                "reason", fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Valor inválido"
        );
    }
}
