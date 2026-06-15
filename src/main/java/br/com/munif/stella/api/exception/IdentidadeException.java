package br.com.munif.stella.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Exceção lançada quando ocorre um erro na comunicação com o provedor de identidade.
 *
 * <p>Encapsula falhas relacionadas à autenticação e gestão de usuários no provedor externo,
 * como erros ao criar contas, alterar senhas, validar tokens ou consultar usuários.
 * Preserva a causa original ({@code cause}) para facilitar o diagnóstico.</p>
 *
 * <p>O status HTTP retornado é configurável, permitindo mapear diferentes cenários
 * (ex.: {@code 401 Unauthorized}, {@code 403 Forbidden}, {@code 502 Bad Gateway}).</p>
 */
public class IdentidadeException extends RuntimeException {

    /** Status HTTP que deve ser retornado ao cliente ao tratar esta exceção. */
    private final HttpStatus status;

    /**
     * Cria uma nova exceção de identidade com causa raiz.
     *
     * @param status  status HTTP a ser retornado ao cliente
     * @param message descrição do erro de identidade
     * @param cause   exceção original lançada pelo provedor de identidade
     */
    public IdentidadeException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    /**
     * Retorna o status HTTP associado a esta exceção.
     *
     * @return status HTTP que deve ser usado na resposta ao cliente
     */
    public HttpStatus getStatus() {
        return status;
    }
}
