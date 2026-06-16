package br.com.stella.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Update DTO for an item category.
 *
 * <p>Contains the editable fields of an already registered category.
 * All fields are fully replaced — omitted fields ({@code null})
 * clear the existing value, except the name which is required.</p>
 *
 * @param nome      category name; required, up to 150 characters
 * @param descricao explanatory text about the category scope; up to 500 characters; optional
 * @param icone     visual icon key (e.g.: {@code "ferramentas"}); up to 50 characters; optional
 * @param ativa     indicates whether the category is active; optional
 */
public record CategoryUpdateDTO(
        @NotBlank(message = "Name is required.")
        @Size(max = 150, message = "Name must not exceed 150 characters.")
        String nome,

        @Size(max = 500, message = "Description must not exceed 500 characters.")
        String descricao,

        @Size(max = 50, message = "Icon must not exceed 50 characters.")
        String icone,

        Boolean ativa
) {}
