package br.com.stella.api.mapper;

import br.com.stella.api.dto.ItemMovementResponseDTO;
import br.com.stella.api.entity.ItemInstance;
import br.com.stella.api.entity.StorageLocation;
import br.com.stella.api.entity.ItemMovement;

/**
 * Converts between the {@link ItemMovement} entity and its output DTOs.
 *
 * <p>Static utility class — must not be instantiated.
 * As movements do not have their own creation DTO (they are created internally by services),
 * this mapper exposes only the conversion to response.</p>
 */
public final class ItemMovementMapper {

    private ItemMovementMapper() {
    }

    /**
     * Converts the {@link ItemMovement} entity to the full response DTO.
     *
     * <p>Includes denormalized data from the instance, origin location, and destination location
     * to avoid additional requests on the client.</p>
     *
     * @param entity entity to convert; may be {@code null}
     * @return populated {@link ItemMovementResponseDTO}, or {@code null} if {@code entity} is {@code null}
     */
    public static ItemMovementResponseDTO toResponseDTO(ItemMovement entity) {
        if (entity == null) {
            return null;
        }

        ItemInstance instance = entity.getItemInstance();
        StorageLocation originLocation = entity.getOriginLocation();
        StorageLocation destinationLocation = entity.getDestinationLocation();
        return new ItemMovementResponseDTO(
                entity.getId(),
                entity.getType(),
                entity.getMovementDate(),
                instance == null ? null : instance.getId(),
                identification(instance),
                originLocation == null ? null : originLocation.getId(),
                originLocation == null ? null : originLocation.getName(),
                destinationLocation == null ? null : destinationLocation.getId(),
                destinationLocation == null ? null : destinationLocation.getName(),
                entity.getReason(),
                entity.getNotes()
        );
    }

    /**
     * Returns the readable identification of the instance, prioritizing:
     * internal identifier, asset number, and lastly serial number.
     *
     * @param instance instance whose identifier will be resolved; may be {@code null}
     * @return first non-null identification field, or {@code null} if the instance is {@code null}
     */
    private static String identification(ItemInstance instance) {
        if (instance == null) {
            return null;
        }
        if (instance.getIdentifier() != null) {
            return instance.getIdentifier();
        }
        if (instance.getAssetTag() != null) {
            return instance.getAssetTag();
        }
        return instance.getSerialNumber();
    }
}
