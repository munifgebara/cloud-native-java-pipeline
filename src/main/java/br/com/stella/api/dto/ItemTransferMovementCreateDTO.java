package br.com.stella.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ItemTransferMovementCreateDTO(
        @NotNull(message = "Instance is required.")
        UUID itemInstanceId,

        @NotNull(message = "Destination location is required.")
        UUID destinationLocationId,

        @Size(max = 1000, message = "Observation must not exceed 1000 characters.")
        String notes
) {}
