package br.com.munif.stella.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Update DTO for a main item.
 *
 * <p>Contains the editable fields of an already registered main item.
 * Image fields are not updated by this DTO — use the image upload endpoint
 * to replace the item's image.</p>
 *
 * @param nome           main item name; required, up to 150 characters
 * @param descricao      detailed description of the item; up to 500 characters; optional
 * @param observacoes    internal notes; up to 1000 characters; optional
 * @param origemCadastro registration origin; up to 50 characters; optional
 * @param categoriaId    category identifier; {@code null} removes the association with the category
 * @param ativa          indicates whether the item is active; optional
 */
public record ItemMestreUpdateDTO(
        @NotBlank(message = "Name is required.")
        @Size(max = 150, message = "Name must not exceed 150 characters.")
        String nome,

        @Size(max = 500, message = "Description must not exceed 500 characters.")
        String descricao,

        @Size(max = 1000, message = "Notes must not exceed 1000 characters.")
        String observacoes,

        @Size(max = 50, message = "Registration origin must not exceed 50 characters.")
        String origemCadastro,

        UUID categoriaId,

        Boolean ativa
) {}
