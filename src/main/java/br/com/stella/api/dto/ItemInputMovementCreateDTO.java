package br.com.stella.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ItemInputMovementCreateDTO(
        @NotNull(message = "Main item is required.")
        UUID mainItemId,

        @NotNull(message = "Destination location is required.")
        UUID destinationLocationId,

        @Size(max = 100, message = "Identifier must not exceed 100 characters.")
        String identifier,

        @Size(max = 100, message = "Asset number must not exceed 100 characters.")
        String assetTag,

        @Size(max = 150, message = "Serial number must not exceed 150 characters.")
        String serialNumber,

        @Size(max = 1000, message = "Observation must not exceed 1000 characters.")
        String notes
) {}
