package br.com.munif.stella.api.entity;

/**
 * Enumeração que classifica o tipo de uma {@link MovimentacaoItem}.
 *
 * <p>O tipo determina a semântica da movimentação e influencia como os locais de origem
 * e destino são interpretados: em uma entrada não há origem; em uma saída não há destino;
 * em uma transferência ambos estão presentes.</p>
 */
public enum TipoMovimentacaoItem {

    /**
     * O item entra no sistema de controle de estoque.
     * Normalmente ocorre no cadastro inicial da instância ou no recebimento de um bem.
     * Não possui local de origem — apenas destino.
     */
    ENTRADA,

    /**
     * O item sai do sistema de controle de estoque.
     * Ocorre quando o item é descartado, devolvido ao fornecedor ou retirado permanentemente.
     * Não possui local de destino — apenas origem.
     */
    SAIDA,

    /**
     * O item é transferido de um local de armazenamento para outro dentro do sistema.
     * Possui tanto local de origem quanto local de destino.
     */
    TRANSFERENCIA
}
