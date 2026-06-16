package br.com.munif.stella.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when the artificial intelligence usage limit is reached.
 *
 * <p>May represent different limitation scenarios:</p>
 * <ul>
 *   <li>Daily or monthly quota of AI API calls exhausted.</li>
 *   <li>Provider's token or credit limit reached.</li>
 *   <li>Rate limit response received from the external provider.</li>
 * </ul>
 *
 * <p>The HTTP status returned is configurable by the caller throwing the exception,
 * generally {@code 429 Too Many Requests} or {@code 402 Payment Required}.</p>
 */
public class AiUsageLimitException extends RuntimeException {

    /** HTTP status that must be returned to the client when handling this exception. */
    private final HttpStatus status;

    /**
     * Creates a new AI usage limit exception.
     *
     * @param status  HTTP status to be returned to the client (e.g.: {@code HttpStatus.TOO_MANY_REQUESTS})
     * @param message description of the limit reached
     */
    public AiUsageLimitException(HttpStatus status, String message) {
        super(message);
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
