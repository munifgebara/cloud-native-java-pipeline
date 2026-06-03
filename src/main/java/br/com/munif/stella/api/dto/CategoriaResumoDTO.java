package br.com.munif.stella.api.dto;

import java.util.UUID;

public record CategoriaResumoDTO(
        UUID id,
        String nome,
        String descricao,
        String icone,
        boolean ativa
) {}
