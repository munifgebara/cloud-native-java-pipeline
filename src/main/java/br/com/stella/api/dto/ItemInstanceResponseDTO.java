package br.com.stella.api.dto;

import br.com.stella.api.entity.ItemInstanceStatus;

import java.util.UUID;

/**
 * Full response DTO of an item instance.
 *
 * <p>Returned in create, update, and individual query operations.
 * Includes all fields of the instance, including those related to the category and registration origin.</p>
 *
 * @param id                unique identifier of the instance
 * @param mainItemId      identifier of the main item to which this instance belongs
 * @param mainItemName    main item name (denormalized)
 * @param categoryId       category identifier of the item; may be {@code null}
 * @param categoryName     category name; may be {@code null}
 * @param categoryIcon    category icon key; may be {@code null}
 * @param currentLocationId      identifier of the current storage location; may be {@code null}
 * @param currentLocationName    current location name (denormalized); may be {@code null}
 * @param identifier     internal identification code of the instance; may be {@code null}
 * @param assetTag        asset number; may be {@code null}
 * @param serialNumber       manufacturer's serial number; may be {@code null}
 * @param operationalStatus current operational status of the instance
 * @param notes       internal notes about this instance; may be {@code null}
 * @param registrationOrigin    registration origin (e.g.: {@code "MANUAL"}, {@code "FOTO"}); may be {@code null}
 * @param ativa             indicates whether the instance is active in the system
 */
public record ItemInstanceResponseDTO(
        UUID id,
        UUID mainItemId,
        String mainItemName,
        UUID categoryId,
        String categoryName,
        String categoryIcon,
        UUID currentLocationId,
        String currentLocationName,
        String identifier,
        String assetTag,
        String serialNumber,
        ItemInstanceStatus operationalStatus,
        String notes,
        String registrationOrigin,
        boolean ativa
) {}
