package br.com.munif.stella.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record EmprestimoItemCreateDTO(
        @NotNull(message = "Instance is required.")
        UUID instanciaItemId,

        @NotNull(message = "Person is required.")
        UUID pessoaId,

        LocalDate previsaoDevolucao,

        @Size(max = 1000, message = "Observation must not exceed 1000 characters.")
        String observacao
) {}
