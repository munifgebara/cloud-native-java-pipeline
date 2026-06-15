package br.com.munif.stella.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Exceção lançada quando o limite de uso de inteligência artificial é atingido.
 *
 * <p>Pode representar diferentes cenários de limitação:</p>
 * <ul>
 *   <li>Cota diária ou mensal de chamadas à API de IA esgotada.</li>
 *   <li>Limite de tokens ou créditos do provedor atingido.</li>
 *   <li>Resposta de limite de taxa (rate limit) recebida do provedor externo.</li>
 * </ul>
 *
 * <p>O status HTTP retornado é configurável por quem lança a exceção,
 * geralmente {@code 429 Too Many Requests} ou {@code 402 Payment Required}.</p>
 */
public class AiUsageLimitException extends RuntimeException {

    /** Status HTTP que deve ser retornado ao cliente ao tratar esta exceção. */
    private final HttpStatus status;

    /**
     * Cria uma nova exceção de limite de uso de IA.
     *
     * @param status  status HTTP a ser retornado ao cliente (ex.: {@code HttpStatus.TOO_MANY_REQUESTS})
     * @param message descrição do limite atingido
     */
    public AiUsageLimitException(HttpStatus status, String message) {
        super(message);
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
