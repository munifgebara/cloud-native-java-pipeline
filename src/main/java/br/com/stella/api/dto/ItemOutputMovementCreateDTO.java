package br.com.stella.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ItemOutputMovementCreateDTO(
        @NotNull(message = "Instance is required.")
        UUID instanciaItemId,

        @NotBlank(message = "Reason is required.")
        @Size(max = 200, message = "Reason must not exceed 200 characters.")
        String motivo,

        @Size(max = 1000, message = "Observation must not exceed 1000 characters.")
        String observacao
) {}
