package br.com.munif.pagadoria.api.exception;

public class CadastroDuplicadoException extends RuntimeException {

    public CadastroDuplicadoException(String message) {
        super(message);
    }
}