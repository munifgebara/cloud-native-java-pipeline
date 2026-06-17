package br.com.stella.api.dto;

import br.com.stella.api.entity.ItemInstanceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Update DTO for an item instance.
 *
 * <p>Contains the fields that can be changed in an already registered instance.
 * All fields (except {@code mainItemId}) are optional: {@code null} values
 * clear the corresponding field in the entity.</p>
 *
 * @param mainItemId      identifier of the main item to which this instance belongs; required
 * @param currentLocationId      identifier of the new storage location; {@code null} removes the location
 * @param identifier     internal identification code (up to 100 characters); optional
 * @param assetTag        asset number (up to 100 characters); optional
 * @param serialNumber       manufacturer's serial number (up to 150 characters); optional
 * @param operationalStatus new operational status of the instance; optional
 * @param notes       internal notes (up to 1000 characters); optional
 * @param registrationOrigin    registration origin (up to 50 characters); optional
 * @param ativa             indicates whether the instance is active; optional
 */
public record ItemInstanceUpdateDTO(
        @NotNull(message = "Main item is required.")
        UUID mainItemId,

        UUID currentLocationId,

        @Size(max = 100, message = "Identifier must not exceed 100 characters.")
        String identifier,

        @Size(max = 100, message = "Asset number must not exceed 100 characters.")
        String assetTag,

        @Size(max = 150, message = "Serial number must not exceed 150 characters.")
        String serialNumber,

        ItemInstanceStatus operationalStatus,

        @Size(max = 1000, message = "Notes must not exceed 1000 characters.")
        String notes,

        @Size(max = 50, message = "Registration origin must not exceed 50 characters.")
        String registrationOrigin,

        Boolean ativa
) {}
