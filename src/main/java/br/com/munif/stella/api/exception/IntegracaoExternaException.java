package br.com.munif.stella.api.exception;

public class IntegracaoExternaException extends RuntimeException {

    public IntegracaoExternaException(String mensagem) {
        super(mensagem);
    }

    public IntegracaoExternaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
