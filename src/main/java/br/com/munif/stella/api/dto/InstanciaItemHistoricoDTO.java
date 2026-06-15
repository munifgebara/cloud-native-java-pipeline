package br.com.munif.stella.api.dto;

import java.util.List;

/**
 * DTO que agrega os dados completos de uma instância de item e seu histórico de movimentações.
 *
 * <p>Utilizado no endpoint de histórico para retornar em uma única resposta tanto os dados
 * atuais da instância quanto a lista cronológica de todas as movimentações já realizadas.</p>
 *
 * @param instancia     dados completos da instância de item
 * @param movimentacoes lista de movimentações da instância em ordem cronológica crescente;
 *                      pode ser vazia quando nenhuma movimentação foi registrada
 */
public record InstanciaItemHistoricoDTO(
        InstanciaItemResponseDTO instancia,
        List<MovimentacaoItemResponseDTO> movimentacoes
) {}
