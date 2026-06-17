package br.com.stella.api.dto;

import java.util.UUID;

/**
 * Full response DTO of a category.
 *
 * <p>Returned in create, update, and individual query operations.
 * Includes all fields of the category.</p>
 *
 * @param id        unique identifier of the category
 * @param name      category name
 * @param description descriptive text about the category scope; may be {@code null}
 * @param icon     associated visual icon key (e.g.: {@code "livros"}); may be {@code null}
 * @param active     indicates whether the category is active in the system
 */
public record CategoryResponseDTO(
        UUID id,
        String name,
        String description,
        String icon,
        boolean active
) {}
