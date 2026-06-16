package br.com.munif.stella.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Creation DTO for a main item.
 *
 * <p>Contains the data provided by the user to register a new item model
 * in the inventory. Image fields are not included here — the image is sent
 * via a dedicated endpoint after creation.</p>
 *
 * @param nome           main item name; required, up to 150 characters
 * @param descricao      detailed description of the item (characteristics, usage, etc.); up to 500 characters; optional
 * @param observacoes    internal notes (maintenance history, restrictions, etc.); up to 1000 characters; optional
 * @param origemCadastro registration origin (e.g.: {@code "MANUAL"}, {@code "IA"}); up to 50 characters; optional
 * @param categoriaId    identifier of the item category; optional
 * @param ativa          indicates whether the item should be created as active; when {@code null}, defaults to {@code true}
 */
public record ItemMestreCreateDTO(
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
