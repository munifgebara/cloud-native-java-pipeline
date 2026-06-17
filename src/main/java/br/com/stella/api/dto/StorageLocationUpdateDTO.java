package br.com.stella.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Update DTO for a storage location.
 *
 * <p>Allows changing the name, description, parent location and active status of an already registered location.
 * The image is updated via a separate endpoint. Providing {@code null} for {@code parentId}
 * removes the hierarchy, making the location a root node.</p>
 *
 * @param name      location name; required, up to 150 characters
 * @param description optional description; up to 500 characters; optional
 * @param parentId     identifier of the new parent location; {@code null} makes the location a root
 * @param ativa     indicates whether the location is active; optional
 */
public record StorageLocationUpdateDTO(
        @NotBlank(message = "Name is required.")
        @Size(max = 150, message = "Name must not exceed 150 characters.")
        String name,

        @Size(max = 500, message = "Description must not exceed 500 characters.")
        String description,

        UUID parentId,

        Boolean ativa
) {}
