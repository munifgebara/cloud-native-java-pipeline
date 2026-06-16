package br.com.munif.stella.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Creation DTO for a storage location.
 *
 * <p>Allows creating locations at any level of the hierarchy: a root location (without {@code paiId})
 * or a sub-location (providing the {@code paiId} of the parent location). The image is sent
 * via a separate endpoint after creation.</p>
 *
 * @param nome      location name; required, up to 150 characters
 * @param descricao optional description of the location (capacity, item type, etc.); up to 500 characters
 * @param paiId     identifier of the parent location in the hierarchy; {@code null} indicates a root location
 * @param ativa     indicates whether the location should be created as active; when {@code null}, defaults to {@code true}
 */
public record LocalArmazenamentoCreateDTO(
        @NotBlank(message = "Name is required.")
        @Size(max = 150, message = "Name must not exceed 150 characters.")
        String nome,

        @Size(max = 500, message = "Description must not exceed 500 characters.")
        String descricao,

        UUID paiId,

        Boolean ativa
) {}
