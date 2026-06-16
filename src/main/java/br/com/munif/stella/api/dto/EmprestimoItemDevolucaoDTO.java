package br.com.munif.stella.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record EmprestimoItemDevolucaoDTO(
        @NotNull(message = "Instance is required.")
        UUID instanciaItemId,

        @NotNull(message = "Return location is required.")
        UUID localRetornoId,

        @Size(max = 1000, message = "Observation must not exceed 1000 characters.")
        String observacao
) {}
