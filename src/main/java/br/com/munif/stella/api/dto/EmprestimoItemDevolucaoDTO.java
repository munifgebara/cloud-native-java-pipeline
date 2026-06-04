package br.com.munif.stella.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record EmprestimoItemDevolucaoDTO(
        @NotNull(message = "Instância é obrigatória.")
        UUID instanciaItemId,

        @NotNull(message = "Local de retorno é obrigatório.")
        UUID localRetornoId,

        @Size(max = 1000, message = "Observação deve ter no máximo 1000 caracteres.")
        String observacao
) {}
