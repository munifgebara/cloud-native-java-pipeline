package br.com.munif.stella.api.dto;

import java.util.UUID;

/**
 * Full response DTO of a category.
 *
 * <p>Returned in create, update, and individual query operations.
 * Includes all fields of the category.</p>
 *
 * @param id        unique identifier of the category
 * @param nome      category name
 * @param descricao descriptive text about the category scope; may be {@code null}
 * @param icone     associated visual icon key (e.g.: {@code "livros"}); may be {@code null}
 * @param ativa     indicates whether the category is active in the system
 */
public record CategoriaResponseDTO(
        UUID id,
        String nome,
        String descricao,
        String icone,
        boolean ativa
) {}
