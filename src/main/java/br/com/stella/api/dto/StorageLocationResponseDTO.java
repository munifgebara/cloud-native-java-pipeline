package br.com.stella.api.dto;

import java.util.UUID;

/**
 * Full response DTO of a storage location.
 *
 * <p>Returned in create, update, and individual query operations.
 * Includes complete image metadata and hierarchy information.</p>
 *
 * @param id                 unique identifier of the location
 * @param name               location name
 * @param description          description of the location; may be {@code null}
 * @param parentId              identifier of the parent location; {@code null} for root locations
 * @param parentName            parent location name (denormalized); {@code null} for root locations
 * @param path            full path in the hierarchy (e.g.: {@code "Building A > Room 101"})
 * @param level              depth in the hierarchy: {@code 0} for root locations
 * @param imageUrl          relative URL for accessing the image; {@code null} when no image
 * @param imageContentType  image MIME type (e.g.: {@code "image/jpeg"}); may be {@code null}
 * @param imageSizeBytes image size in bytes; may be {@code null}
 * @param active              indicates whether the location is active in the system
 */
public record StorageLocationResponseDTO(
        UUID id,
        String name,
        String description,
        UUID parentId,
        String parentName,
        String path,
        int level,
        String imageUrl,
        String imageContentType,
        Long imageSizeBytes,
        boolean active
) {}
