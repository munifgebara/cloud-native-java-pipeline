package br.com.munif.stella.api.exception;

import org.springframework.http.HttpStatus;

public class AiUsageLimitException extends RuntimeException {

    private final HttpStatus status;

    public AiUsageLimitException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
