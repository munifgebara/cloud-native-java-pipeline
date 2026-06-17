package br.com.stella.api.dto;

import java.util.UUID;

/**
 * Summary DTO of a storage location, used in listings.
 *
 * <p>Includes hierarchy information (parent, path and level) to facilitate
 * display in tree structures and hierarchical lists in the interface.</p>
 *
 * @param id        unique identifier of the location
 * @param name      location name
 * @param description description of the location; may be {@code null}
 * @param parentId     identifier of the parent location; {@code null} for root locations
 * @param parentName   parent location name (denormalized); {@code null} for root locations
 * @param caminho   full path of the location in the hierarchy (e.g.: {@code "Building A > Room 101 > Cabinet 2"})
 * @param nivel     depth in the hierarchy: {@code 0} for root locations, {@code 1} for children, etc.
 * @param imageUrl relative URL for accessing the location image; {@code null} when no image
 * @param ativa     indicates whether the location is active in the system
 */
public record StorageLocationSummaryDTO(
        UUID id,
        String name,
        String description,
        UUID parentId,
        String parentName,
        String caminho,
        int nivel,
        String imageUrl,
        boolean ativa
) {}
