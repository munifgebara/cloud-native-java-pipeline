package br.com.munif.stella.api.dto;

import java.util.UUID;

/**
 * DTO resumido de uma pessoa, utilizado em seletores e referências cruzadas.
 *
 * <p>Contém apenas o identificador e o nome, suficientes para exibir
 * a pessoa em listas de seleção (ex.: ao registrar um empréstimo).</p>
 *
 * @param id   identificador único da pessoa
 * @param nome nome completo ou razão social da pessoa
 */
public record PessoaResumoDTO(
        UUID id,
        String nome
) {
}
