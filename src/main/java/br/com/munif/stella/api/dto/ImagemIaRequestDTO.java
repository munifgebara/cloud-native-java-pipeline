package br.com.munif.stella.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ImagemIaRequestDTO(
        @NotBlank
        @Size(max = 150)
        String nome,

        @Size(max = 150)
        String categoria,

        @Size(max = 500)
        String descricao
) {}
