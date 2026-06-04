package br.com.munif.stella.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record MovimentacaoSaidaCreateDTO(
        @NotNull(message = "Instância é obrigatória.")
        UUID instanciaItemId,

        @NotBlank(message = "Motivo é obrigatório.")
        @Size(max = 200, message = "Motivo deve ter no máximo 200 caracteres.")
        String motivo,

        @Size(max = 1000, message = "Observação deve ter no máximo 1000 caracteres.")
        String observacao
) {}
