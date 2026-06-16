package br.com.stella.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Creation DTO for an item category.
 *
 * <p>Contains the data required to register a new category in the system.
 * The icon must be a valid key as defined by {@code CategoryIcon}.</p>
 *
 * @param nome      category name; required, up to 150 characters
 * @param descricao explanatory text about the category scope; up to 500 characters; optional
 * @param icone     visual icon key for the category (e.g.: {@code "eletronicos"}); up to 50 characters; optional
 * @param ativa     indicates whether the category should be created as active; when {@code null}, defaults to {@code true}
 */
public record CategoryCreateDTO(
        @NotBlank(message = "Name is required.")
        @Size(max = 150, message = "Name must not exceed 150 characters.")
        String nome,

        @Size(max = 500, message = "Description must not exceed 500 characters.")
        String descricao,

        @Size(max = 50, message = "Icon must not exceed 50 characters.")
        String icone,

        Boolean ativa
) {}
