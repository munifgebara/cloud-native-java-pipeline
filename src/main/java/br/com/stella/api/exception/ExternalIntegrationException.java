package br.com.stella.api.exception;

/**
 * Exception thrown when a communication failure occurs with an external service.
 *
 * <p>Used to encapsulate integration errors with third-party APIs and services,
 * such as object storage providers (S3), AI services, postal code gateways, etc.
 * Allows the business layer to handle external failures uniformly,
 * without exposing integration-specific details.</p>
 *
 * <p>Should be handled by {@code GlobalExceptionHandler} and mapped to
 * HTTP 502 (Bad Gateway) or 503 (Service Unavailable).</p>
 */
public class ExternalIntegrationException extends RuntimeException {

    /**
     * Creates a new external integration exception with a descriptive message.
     *
     * @param message description of the failure (e.g., "Failed to upload image to S3.")
     */
    public ExternalIntegrationException(String message) {
        super(message);
    }

    /**
     * Creates a new external integration exception with a message and root cause.
     *
     * @param message description of the failure
     * @param cause   original exception thrown by the external service or client library
     */
    public ExternalIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
