package br.com.stella.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Creation DTO for an item category.
 *
 * <p>Contains the data required to register a new category in the system.
 * The icon must be a valid key as defined by {@code CategoryIcon}.</p>
 *
 * @param name      category name; required, up to 150 characters
 * @param description explanatory text about the category scope; up to 500 characters; optional
 * @param icon     visual icon key for the category (e.g.: {@code "eletronicos"}); up to 50 characters; optional
 * @param active     indicates whether the category should be created as active; when {@code null}, defaults to {@code true}
 * @param ownerPublic indicates whether other authenticated owners may read this category
 */
public record CategoryCreateDTO(
        @NotBlank(message = "Name is required.")
        @Size(max = 150, message = "Name must not exceed 150 characters.")
        String name,

        @Size(max = 500, message = "Description must not exceed 500 characters.")
        String description,

        @Size(max = 50, message = "Icon must not exceed 50 characters.")
        String icon,

        Boolean active,

        Boolean ownerPublic
) {}
