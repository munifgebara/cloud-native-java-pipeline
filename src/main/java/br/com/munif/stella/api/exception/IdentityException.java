package br.com.munif.stella.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a communication error occurs with the identity provider.
 *
 * <p>Encapsulates failures related to authentication and user management in the external provider,
 * such as errors when creating accounts, changing passwords, validating tokens, or querying users.
 * Preserves the original cause ({@code cause}) to facilitate diagnosis.</p>
 *
 * <p>The returned HTTP status is configurable, allowing different scenarios to be mapped
 * (e.g., {@code 401 Unauthorized}, {@code 403 Forbidden}, {@code 502 Bad Gateway}).</p>
 */
public class IdentityException extends RuntimeException {

    /** HTTP status to be returned to the client when handling this exception. */
    private final HttpStatus status;

    /**
     * Creates a new identity exception with a root cause.
     *
     * @param status  HTTP status to be returned to the client
     * @param message description of the identity error
     * @param cause   original exception thrown by the identity provider
     */
    public IdentityException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    /**
     * Returns the HTTP status associated with this exception.
     *
     * @return HTTP status to be used in the response to the client
     */
    public HttpStatus getStatus() {
        return status;
    }
}
