package br.com.munif.stella.api.exception;

/**
 * Exceção lançada quando ocorre uma falha na comunicação com um serviço externo.
 *
 * <p>Utilizada para encapsular erros de integração com APIs e serviços de terceiros,
 * como provedores de armazenamento de objetos (S3), serviços de IA, gateways de CEP, etc.
 * Permite que a camada de negócio trate falhas externas de forma uniforme,
 * sem expor detalhes específicos de cada integração.</p>
 *
 * <p>Deve ser tratada pelo {@code GlobalExceptionHandler} e mapeada para
 * uma resposta HTTP 502 (Bad Gateway) ou 503 (Service Unavailable).</p>
 */
public class IntegracaoExternaException extends RuntimeException {

    /**
     * Cria uma nova exceção de integração externa com mensagem descritiva.
     *
     * @param mensagem descrição da falha (ex.: "Falha ao enviar imagem para o S3.")
     */
    public IntegracaoExternaException(String mensagem) {
        super(mensagem);
    }

    /**
     * Cria uma nova exceção de integração externa com mensagem e causa raiz.
     *
     * @param mensagem descrição da falha
     * @param causa    exceção original lançada pelo serviço externo ou biblioteca cliente
     */
    public IntegracaoExternaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
