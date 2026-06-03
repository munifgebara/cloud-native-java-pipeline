package br.com.munif.stella.api.dto;

import java.util.UUID;

public record ItemMestreResponseDTO(
        UUID id,
        String nome,
        String descricao,
        String observacoes,
        UUID categoriaId,
        String categoriaNome,
        String categoriaIcone,
        boolean ativa
) {}
