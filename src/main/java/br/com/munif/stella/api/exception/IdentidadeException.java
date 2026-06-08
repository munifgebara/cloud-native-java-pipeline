package br.com.munif.stella.api.exception;

import org.springframework.http.HttpStatus;

public class IdentidadeException extends RuntimeException {

    private final HttpStatus status;

    public IdentidadeException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
