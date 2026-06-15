package br.com.munif.stella.api.service;

import br.com.munif.stella.api.entity.InstanciaItem;
import br.com.munif.stella.api.entity.StatusOperacionalInstancia;

/**
 * Regras de negócio isoladas para validação do estado de uma {@link InstanciaItem}.
 *
 * <p>Esta classe agrupa verificações que dependem apenas dos dados internos da instância,
 * sem acesso a repositórios ou outros serviços. Mantê-las separadas do
 * {@link InstanciaItemService} facilita os testes unitários e torna as regras
 * mais fáceis de localizar e manter.</p>
 *
 * <p>Visibilidade de pacote — esta classe não faz parte da API pública do sistema.</p>
 */
final class InstanciaItemRegras {

    private InstanciaItemRegras() {
        // Utilitário sem estado — não instanciável
    }

    /**
     * Valida a coerência entre o status operacional da instância e a presença de um local atual.
     *
     * <p>Regras:</p>
     * <ul>
     *   <li>{@code DISPONIVEL} → exige local atual (o item está guardado em algum lugar).</li>
     *   <li>{@code EMPRESTADO} → não deve ter local atual (o item está com um responsável).</li>
     *   <li>{@code EM_MOVIMENTACAO} → não deve ter local atual (está em trânsito).</li>
     *   <li>{@code INATIVO} → não deve ter local atual (foi retirado do inventário).</li>
     * </ul>
     *
     * @param instancia instância a validar
     * @throws IllegalArgumentException se o status e a localização forem incompatíveis
     */
    static void validarCoerenciaStatusLocal(InstanciaItem instancia) {
        StatusOperacionalInstancia status = instancia.getStatusOperacional();

        if (status == StatusOperacionalInstancia.DISPONIVEL && instancia.getLocalAtual() == null) {
            throw new IllegalArgumentException("Instância disponível deve possuir local atual.");
        }
        if (status == StatusOperacionalInstancia.EMPRESTADO && instancia.getLocalAtual() != null) {
            throw new IllegalArgumentException("Instância emprestada não deve possuir local atual.");
        }
        if (status == StatusOperacionalInstancia.EM_MOVIMENTACAO && instancia.getLocalAtual() != null) {
            throw new IllegalArgumentException("Instância em movimentação não deve possuir local atual.");
        }
        if (status == StatusOperacionalInstancia.INATIVO && instancia.getLocalAtual() != null) {
            throw new IllegalArgumentException("Instância inativa não deve possuir local atual.");
        }
    }

    /**
     * Garante que a instância está ativa, disponível e possui local atual.
     *
     * <p>Utilizado antes de operações que exigem que o item esteja fisicamente
     * presente e pronto para uso, como empréstimos e transferências.</p>
     *
     * @param instancia                a instância a verificar
     * @param mensagemInstanciaInativa mensagem de erro caso a instância esteja inativa
     * @param mensagemStatusInvalido   mensagem de erro caso o status não seja {@code DISPONIVEL}
     * @param mensagemLocalAusente     mensagem de erro caso não haja local atual informado
     * @throws IllegalArgumentException se qualquer uma das condições não for satisfeita
     */
    static void exigirDisponivelComLocal(
            InstanciaItem instancia,
            String mensagemInstanciaInativa,
            String mensagemStatusInvalido,
            String mensagemLocalAusente
    ) {
        if (!instancia.isAtivo()) {
            throw new IllegalArgumentException(mensagemInstanciaInativa);
        }
        if (instancia.getStatusOperacional() != StatusOperacionalInstancia.DISPONIVEL) {
            throw new IllegalArgumentException(mensagemStatusInvalido);
        }
        if (instancia.getLocalAtual() == null) {
            throw new IllegalArgumentException(mensagemLocalAusente);
        }
    }

    /**
     * Garante que a instância está com status {@code EMPRESTADO}.
     *
     * <p>Utilizado antes de registrar a devolução de um item emprestado.</p>
     *
     * @param instancia              a instância a verificar
     * @param mensagemStatusInvalido mensagem de erro caso o status não seja {@code EMPRESTADO}
     * @throws IllegalArgumentException se a instância não estiver emprestada
     */
    static void exigirEmprestada(InstanciaItem instancia, String mensagemStatusInvalido) {
        if (instancia.getStatusOperacional() != StatusOperacionalInstancia.EMPRESTADO) {
            throw new IllegalArgumentException(mensagemStatusInvalido);
        }
    }
}
