package br.com.munif.stella.api.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> tratarValidacao(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> erros = new LinkedHashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            erros.put(error.getField(), error.getDefaultMessage());
        }

        log.warn("Erro de validação em {} {}: {}", request.getMethod(), request.getRequestURI(), erros);

        Map<String, Object> body = body(HttpStatus.BAD_REQUEST, "Dados inválidos.", request);
        body.put("campos", erros);

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> tratarNaoEncontrado(EntityNotFoundException ex, HttpServletRequest request) {
        return resposta(HttpStatus.NOT_FOUND, ex.getMessage(), ex, request, false);
    }

    @ExceptionHandler(CadastroDuplicadoException.class)
    public ResponseEntity<Map<String, Object>> tratarDuplicidade(CadastroDuplicadoException ex, HttpServletRequest request) {
        return resposta(HttpStatus.CONFLICT, ex.getMessage(), ex, request, false);
    }

    @ExceptionHandler(AiUsageLimitException.class)
    public ResponseEntity<Map<String, Object>> tratarUsoIa(AiUsageLimitException ex, HttpServletRequest request) {
        return resposta(ex.getStatus(), ex.getMessage(), ex, request, false);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> tratarRegraNegocio(IllegalArgumentException ex, HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, ex.getMessage(), ex, request, false);
    }

    @ExceptionHandler(IntegracaoExternaException.class)
    public ResponseEntity<Map<String, Object>> tratarFalhaIntegracao(IntegracaoExternaException ex, HttpServletRequest request) {
        return resposta(HttpStatus.BAD_GATEWAY, ex.getMessage(), ex, request, true);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> tratarEstadoIlegal(IllegalStateException ex, HttpServletRequest request) {
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro inesperado ao processar a solicitação.", ex, request, true);
    }

    @ExceptionHandler(IdentidadeException.class)
    public ResponseEntity<Map<String, Object>> tratarIdentidade(IdentidadeException ex, HttpServletRequest request) {
        return resposta(ex.getStatus(), ex.getMessage(), ex, request, ex.getStatus().is5xxServerError());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> tratarIntegridade(DataIntegrityViolationException ex, HttpServletRequest request) {
        return resposta(HttpStatus.CONFLICT, "Não foi possível concluir a operação por conflito com dados já existentes ou vinculados.", ex, request, true);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<Map<String, Object>> tratarRequisicaoInvalida(Exception ex, HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "Requisição inválida. Confira os dados enviados.", ex, request, false);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> tratarErroInesperado(Exception ex, HttpServletRequest request) {
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro inesperado ao processar a solicitação.", ex, request, true);
    }

    private ResponseEntity<Map<String, Object>> resposta(
            HttpStatus status,
            String mensagem,
            Exception ex,
            HttpServletRequest request,
            boolean incluirCausaNoLog
    ) {
        if (status.is5xxServerError() || incluirCausaNoLog) {
            log.error("Erro em {} {}: {}", request.getMethod(), request.getRequestURI(), mensagem, ex);
        } else {
            log.warn("Erro em {} {}: {}", request.getMethod(), request.getRequestURI(), mensagem);
        }

        return ResponseEntity.status(status).body(body(status, mensagem, request));
    }

    private Map<String, Object> body(HttpStatus status, String mensagem, HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", status.value());
        body.put("codigo", status.getReasonPhrase());
        body.put("erro", mensagem);
        body.put("path", request.getRequestURI());
        return body;
    }
}
