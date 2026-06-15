package br.com.munif.stella.api.dto;

import java.util.UUID;

/**
 * DTO resumido de uma categoria, utilizado em listagens e seletores.
 *
 * <p>Contém os campos suficientes para exibir a categoria em grades e
 * listas de seleção, incluindo o ícone para identificação visual.</p>
 *
 * @param id      identificador único da categoria
 * @param nome    nome da categoria
 * @param descricao texto descritivo da categoria; pode ser {@code null}
 * @param icone   chave do ícone visual; pode ser {@code null}
 * @param ativa   indica se a categoria está ativa no sistema
 */
public record CategoriaResumoDTO(
        UUID id,
        String nome,
        String descricao,
        String icone,
        boolean ativa
) {}
