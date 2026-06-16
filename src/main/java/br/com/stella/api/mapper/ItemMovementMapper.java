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
        StorageLocation localOrigem = entity.getOriginLocation();
        StorageLocation localDestino = entity.getDestinationLocation();
        return new ItemMovementResponseDTO(
                entity.getId(),
                entity.getType(),
                entity.getDataMovimentacao(),
                instance == null ? null : instance.getId(),
                identificacao(instance),
                localOrigem == null ? null : localOrigem.getId(),
                localOrigem == null ? null : localOrigem.getName(),
                localDestino == null ? null : localDestino.getId(),
                localDestino == null ? null : localDestino.getName(),
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
    private static String identificacao(ItemInstance instance) {
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
