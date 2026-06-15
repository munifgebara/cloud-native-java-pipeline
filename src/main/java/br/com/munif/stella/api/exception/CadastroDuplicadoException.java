package br.com.munif.stella.api.exception;

/**
 * Exceção lançada quando se tenta cadastrar um registro com uma chave única já existente no sistema.
 *
 * <p>Exemplos de uso:</p>
 * <ul>
 *   <li>Tentativa de cadastrar uma pessoa com CPF/CNPJ já existente.</li>
 *   <li>Tentativa de criar um usuário com e-mail ou login duplicado.</li>
 * </ul>
 *
 * <p>Deve ser tratada pelo {@code GlobalExceptionHandler} e mapeada para
 * uma resposta HTTP 409 (Conflict).</p>
 */
public class CadastroDuplicadoException extends RuntimeException {

    /**
     * Cria uma nova exceção com a mensagem descrevendo qual campo está duplicado.
     *
     * @param message descrição do conflito (ex.: "CPF 123.456.789-00 já cadastrado.")
     */
    public CadastroDuplicadoException(String message) {
        super(message);
    }
}
