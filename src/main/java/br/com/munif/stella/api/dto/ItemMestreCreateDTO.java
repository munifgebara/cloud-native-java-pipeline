package br.com.munif.stella.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ItemMestreCreateDTO(
        @NotBlank(message = "Nome é obrigatório.")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres.")
        String nome,

        @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres.")
        String descricao,

        @Size(max = 1000, message = "Observações devem ter no máximo 1000 caracteres.")
        String observacoes,

        @Size(max = 50, message = "Origem do cadastro deve ter no máximo 50 caracteres.")
        String origemCadastro,

        UUID categoriaId,

        Boolean ativa
) {}
