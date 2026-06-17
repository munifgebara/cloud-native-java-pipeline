package br.com.stella.api.dto;

import br.com.stella.api.entity.ItemInstanceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Creation DTO for an item instance.
 *
 * <p>Contains the data required to register a new physical instance of an
 * {@code MainItem} in the system, including identification, initial location
 * and starting operational status.</p>
 *
 * @param mainItemId      identifier of the main item to which this instance belongs; required
 * @param currentLocationId      identifier of the initial storage location of the instance; optional
 * @param identifier     internal identification code of the instance (up to 100 characters); optional
 * @param assetTag        asset number assigned to the item (up to 100 characters); optional
 * @param serialNumber       manufacturer's serial number stamped on the equipment (up to 150 characters); optional
 * @param operationalStatus initial operational status; when {@code null}, defaults to {@code DISPONIVEL}
 * @param notes       internal notes about this instance (up to 1000 characters); optional
 * @param registrationOrigin    registration origin (e.g.: {@code "MANUAL"}, {@code "FOTO"}); up to 50 characters; optional
 * @param ativa             indicates whether the instance should be created as active; when {@code null}, defaults to {@code true}
 */
public record ItemInstanceCreateDTO(
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
