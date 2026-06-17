package br.com.stella.api.dto;

import br.com.stella.api.entity.ItemInstanceStatus;

import java.util.UUID;

/**
 * Summary DTO of an item instance, used in listings.
 *
 * <p>Contains only the essential fields for quick identification and location
 * of the instance, without including less-used fields such as notes.</p>
 *
 * @param id                unique identifier of the instance
 * @param mainItemId      identifier of the main item to which this instance belongs
 * @param mainItemName    main item name (denormalized to avoid joins on the client)
 * @param categoryName     category name of the main item; may be {@code null}
 * @param categoryIcon    category icon key; may be {@code null}
 * @param currentLocationId      identifier of the current storage location; may be {@code null}
 * @param currentLocationName    current location name (denormalized to avoid joins on the client); may be {@code null}
 * @param identifier     internal identification code of the instance; may be {@code null}
 * @param assetTag        asset number; may be {@code null}
 * @param serialNumber       manufacturer's serial number; may be {@code null}
 * @param operationalStatus current operational status of the instance
 * @param ativa             indicates whether the instance is active in the system
 */
public record ItemInstanceSummaryDTO(
        UUID id,
        UUID mainItemId,
        String mainItemName,
        String categoryName,
        String categoryIcon,
        UUID currentLocationId,
        String currentLocationName,
        String identifier,
        String assetTag,
        String serialNumber,
        ItemInstanceStatus operationalStatus,
        boolean ativa
) {}
