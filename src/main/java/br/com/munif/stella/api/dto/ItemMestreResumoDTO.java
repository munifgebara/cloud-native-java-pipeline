package br.com.munif.stella.api.dto;

import java.util.UUID;

public record ItemMestreResumoDTO(
        UUID id,
        String nome,
        String descricao,
        UUID categoriaId,
        String categoriaNome,
        String categoriaIcone,
        String imagemUrl,
        boolean ativa
) {}
