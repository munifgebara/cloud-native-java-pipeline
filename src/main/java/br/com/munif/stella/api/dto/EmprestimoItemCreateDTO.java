package br.com.munif.stella.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record EmprestimoItemCreateDTO(
        @NotNull(message = "Instância é obrigatória.")
        UUID instanciaItemId,

        @NotNull(message = "Pessoa é obrigatória.")
        UUID pessoaId,

        LocalDate previsaoDevolucao,

        @Size(max = 1000, message = "Observação deve ter no máximo 1000 caracteres.")
        String observacao
) {}
