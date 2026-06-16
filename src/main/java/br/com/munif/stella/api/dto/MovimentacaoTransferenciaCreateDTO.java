package br.com.munif.stella.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record MovimentacaoTransferenciaCreateDTO(
        @NotNull(message = "Instance is required.")
        UUID instanciaItemId,

        @NotNull(message = "Destination location is required.")
        UUID localDestinoId,

        @Size(max = 1000, message = "Observation must not exceed 1000 characters.")
        String observacao
) {}
