package br.com.munif.stella.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record MovimentacaoEntradaCreateDTO(
        @NotNull(message = "Main item is required.")
        UUID itemMestreId,

        @NotNull(message = "Destination location is required.")
        UUID localDestinoId,

        @Size(max = 100, message = "Identifier must not exceed 100 characters.")
        String identificador,

        @Size(max = 100, message = "Asset number must not exceed 100 characters.")
        String patrimonio,

        @Size(max = 150, message = "Serial number must not exceed 150 characters.")
        String numeroSerie,

        @Size(max = 1000, message = "Observation must not exceed 1000 characters.")
        String observacao
) {}
