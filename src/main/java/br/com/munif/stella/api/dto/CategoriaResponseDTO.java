package br.com.munif.stella.api.dto;

import java.util.UUID;

/**
 * DTO completo de resposta de uma categoria.
 *
 * <p>Retornado nas operações de criação, atualização e consulta individual.
 * Inclui todos os campos da categoria.</p>
 *
 * @param id      identificador único da categoria
 * @param nome    nome da categoria
 * @param descricao texto descritivo sobre o escopo da categoria; pode ser {@code null}
 * @param icone   chave do ícone visual associado (ex.: {@code "livros"}); pode ser {@code null}
 * @param ativa   indica se a categoria está ativa no sistema
 */
public record CategoriaResponseDTO(
        UUID id,
        String nome,
        String descricao,
        String icone,
        boolean ativa
) {}
