package br.com.munif.stella.api.entity;

/**
 * Enumeração que representa os possíveis estados operacionais de uma {@link InstanciaItem}.
 *
 * <p>O status controla o que pode ser feito com cada instância: apenas instâncias
 * {@link #DISPONIVEL} podem ser emprestadas ou movimentadas. As transições de status
 * são gerenciadas pelos serviços de movimentação e empréstimo.</p>
 */
public enum StatusOperacionalInstancia {

    /**
     * A instância está disponível para uso, empréstimo ou movimentação.
     * Este é o estado padrão após o cadastro.
     */
    DISPONIVEL,

    /**
     * A instância está em processo de movimentação entre locais de armazenamento.
     * Neste estado, o item não pode ser emprestado até a movimentação ser concluída.
     */
    EM_MOVIMENTACAO,

    /**
     * A instância foi emprestada a uma {@link Pessoa} e está sob posse dela.
     * Permanece neste estado até a devolução ser registrada.
     */
    EMPRESTADO,

    /**
     * A instância foi desativada e não está mais em uso operacional.
     * Pode indicar itens descartados, aguardando manutenção ou fora de serviço permanentemente.
     */
    INATIVO
}
