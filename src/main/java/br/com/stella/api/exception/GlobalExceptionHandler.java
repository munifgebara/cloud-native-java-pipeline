package br.com.stella.api.exception;

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

/**
 * Global exception handler for all REST controllers in the application.
 *
 * <p>Centralizes error handling, ensuring that all error responses follow
 * a consistent JSON format:</p>
 * <pre>
 * {
 *   "timestamp": "2024-01-15T10:30:00Z",
 *   "status": 400,
 *   "code": "Bad Request",
 *   "error": "Human-readable message for the user",
 *   "path": "/api/v0/resource"
 * }
 * </pre>
 *
 * <p>The {@link RestControllerAdvice} annotation makes Spring automatically intercept
 * any exception thrown in the controllers and call the corresponding
 * {@link ExceptionHandler} method, without needing try/catch blocks in controllers.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles Bean Validation failures (annotations such as {@code @NotBlank}, {@code @Size}, etc.).
     *
     * <p>Returns {@code 400 Bad Request} with the list of invalid fields and their messages.</p>
     *
     * @param ex      exception with details of invalid fields
     * @param request HTTP request that originated the error
     * @return 400 response with a map of errors per field
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        log.warn("Validation error in {} {}: {}", request.getMethod(), request.getRequestURI(), errors);

        Map<String, Object> body = body(HttpStatus.BAD_REQUEST, "Invalid data.", request);
        body.put("fields", errors);

        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Handles records not found in the database.
     * Returns {@code 404 Not Found}.
     *
     * @param ex      exception with the "not found" message
     * @param request request that originated the error
     * @return 404 response
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(EntityNotFoundException ex, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, ex.getMessage(), ex, request, false);
    }

    /**
     * Handles duplicate registration attempts (e.g., CPF already exists).
     * Returns {@code 409 Conflict}.
     *
     * @param ex      exception indicating a data conflict
     * @param request request that originated the error
     * @return 409 response
     */
    @ExceptionHandler(DuplicateRegistrationException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateRegistrationException ex, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, ex.getMessage(), ex, request, false);
    }

    /**
     * Handles exceeded AI usage limits.
     * The HTTP status is defined by the exception itself (may be {@code 429 Too Many Requests}
     * or another code indicated by the AI service).
     *
     * @param ex      exception with the status and exceeded-limit message
     * @param request request that originated the error
     * @return response with the status defined by the exception
     */
    @ExceptionHandler(AiUsageLimitException.class)
    public ResponseEntity<Map<String, Object>> handleAiUsageLimit(AiUsageLimitException ex, HttpServletRequest request) {
        return response(ex.getStatus(), ex.getMessage(), ex, request, false);
    }

    /**
     * Handles business rule violations expressed as {@link IllegalArgumentException}.
     * Returns {@code 400 Bad Request}.
     *
     * <p>This is the preferred way to signal business validation errors
     * in services (e.g., "Invalid CPF", "Storage location must be active").</p>
     *
     * @param ex      exception with the violated rule message
     * @param request request that originated the error
     * @return 400 response
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessRule(IllegalArgumentException ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, ex.getMessage(), ex, request, false);
    }

    /**
     * Handles failures in external integrations (e.g., Keycloak, MinIO, OpenAI).
     * Returns {@code 502 Bad Gateway}, indicating the problem is in an external service,
     * not in the client's request.
     *
     * @param ex      exception with integration failure details
     * @param request request that originated the error
     * @return 502 response
     */
    @ExceptionHandler(ExternalIntegrationException.class)
    public ResponseEntity<Map<String, Object>> handleExternalIntegration(ExternalIntegrationException ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_GATEWAY, ex.getMessage(), ex, request, true);
    }

    /**
     * Handles unexpected illegal states in the application.
     * Returns {@code 500 Internal Server Error} and logs the full error.
     *
     * @param ex      illegal state exception
     * @param request request that originated the error
     * @return 500 response
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleUnexpectedState(IllegalStateException ex, HttpServletRequest request) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error while processing the request.", ex, request, true);
    }

    /**
     * Handles communication errors with the identity provider (Keycloak).
     * The HTTP status is defined by the exception itself.
     *
     * @param ex      exception with the status and identity error message
     * @param request request that originated the error
     * @return response with the status defined by the exception
     */
    @ExceptionHandler(IdentityException.class)
    public ResponseEntity<Map<String, Object>> handleIdentity(IdentityException ex, HttpServletRequest request) {
        return response(ex.getStatus(), ex.getMessage(), ex, request, ex.getStatus().is5xxServerError());
    }

    /**
     * Handles referential integrity violations in the database.
     * Returns {@code 409 Conflict} with a generic message to avoid exposing schema details.
     *
     * @param ex      relational integrity exception thrown by JPA/Hibernate
     * @param request request that originated the error
     * @return 409 response
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "Unable to complete the operation due to conflict with existing or linked data.", ex, request, true);
    }

    /**
     * Handles malformed requests: invalid JSON, missing parameter, or wrong parameter type.
     * Returns {@code 400 Bad Request}.
     *
     * @param ex      request reading or binding exception
     * @param request request that originated the error
     * @return 400 response
     */
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<Map<String, Object>> handleInvalidRequest(Exception ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "Invalid request. Please check the submitted data.", ex, request, false);
    }

    /**
     * Last-resort handler for any exception not covered by other handlers.
     * Returns {@code 500 Internal Server Error} and logs the full stack trace.
     *
     * @param ex      any unhandled exception
     * @param request request that originated the error
     * @return 500 response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpectedError(Exception ex, HttpServletRequest request) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error while processing the request.", ex, request, true);
    }

    /**
     * Builds the error response, logs it, and returns the {@link ResponseEntity}.
     *
     * @param status           HTTP status code
     * @param message          human-readable message for the user
     * @param ex               original exception (used in logging)
     * @param request          HTTP request
     * @param includeStackTrace if {@code true}, logs the full stack trace; otherwise only the message
     * @return HTTP response with standardized JSON body
     */
    private ResponseEntity<Map<String, Object>> response(
            HttpStatus status,
            String message,
            Exception ex,
            HttpServletRequest request,
            boolean includeStackTrace
    ) {
        if (status.is5xxServerError() || includeStackTrace) {
            log.error("Error in {} {}: {}", request.getMethod(), request.getRequestURI(), message, ex);
        } else {
            log.warn("Error in {} {}: {}", request.getMethod(), request.getRequestURI(), message);
        }

        return ResponseEntity.status(status).body(body(status, message, request));
    }

    /**
     * Builds the standardized JSON body for error responses.
     *
     * @param status  HTTP status code
     * @param message error message
     * @param request HTTP request (to extract the path)
     * @return map with fields {@code timestamp}, {@code status}, {@code code}, {@code error}, and {@code path}
     */
    private Map<String, Object> body(HttpStatus status, String message, HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", status.value());
        body.put("code", status.getReasonPhrase());
        body.put("error", message);
        body.put("path", request.getRequestURI());
        return body;
    }
}
