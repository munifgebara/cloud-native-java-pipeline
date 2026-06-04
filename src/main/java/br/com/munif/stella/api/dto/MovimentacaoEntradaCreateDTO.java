package br.com.munif.stella.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record MovimentacaoEntradaCreateDTO(
        @NotNull(message = "Item mestre é obrigatório.")
        UUID itemMestreId,

        @NotNull(message = "Local destino é obrigatório.")
        UUID localDestinoId,

        @Size(max = 100, message = "Identificador deve ter no máximo 100 caracteres.")
        String identificador,

        @Size(max = 100, message = "Patrimônio deve ter no máximo 100 caracteres.")
        String patrimonio,

        @Size(max = 150, message = "Número de série deve ter no máximo 150 caracteres.")
        String numeroSerie,

        @Size(max = 1000, message = "Observação deve ter no máximo 1000 caracteres.")
        String observacao
) {}
