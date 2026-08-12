package com.augustoomb.api_loja_do_sol_ecommerce.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.augustoomb.api_loja_do_sol_ecommerce.web.RequestIdFilter;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
        log.warn("Recurso não encontrado: {} (path={})", ex.getMessage(), pathOf(request));
        return build(HttpStatus.NOT_FOUND, "Not Found", ex, request);
    }

    @ExceptionHandler(EmailAlreadyInUseException.class)
    public ResponseEntity<Map<String, Object>> handleEmailAlreadyInUse(EmailAlreadyInUseException ex, WebRequest request) {
        log.warn("Conflito: {} (path={})", ex.getMessage(), pathOf(request));
        return build(HttpStatus.CONFLICT, "Conflict", ex, request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(InvalidCredentialsException ex, WebRequest request) {
        log.warn("Credenciais inválidas (path={})", pathOf(request));
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized", ex, request);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException ex, WebRequest request) {
        log.warn("Regra de negócio: {} (path={})", ex.getMessage(), pathOf(request));
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "Unprocessable Entity", ex, request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(NoResourceFoundException ex, WebRequest request) {
        log.warn("Rota não encontrada: {} (path={})", ex.getResourcePath(), pathOf(request));
        Map<String, Object> body = baseBody(HttpStatus.NOT_FOUND, "Not Found", request);
        body.put("message", "Rota não encontrada.");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex, WebRequest request) {
        log.error("Erro inesperado em {} (requestId={})", pathOf(request), MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY), ex);
        Map<String, Object> body = baseBody(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", request);
        body.put("message", "Ocorreu um erro inesperado. Tente novamente mais tarde.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String error,
                                                      Exception ex, WebRequest request) {
        Map<String, Object> body = baseBody(status, error, request);
        body.put("message", ex.getMessage());
        return ResponseEntity.status(status).body(body);
    }

    private Map<String, Object> baseBody(HttpStatus status, String error, WebRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", error);
        body.put("path", pathOf(request));
        body.put("requestId", MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY));
        return body;
    }

    private String pathOf(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
