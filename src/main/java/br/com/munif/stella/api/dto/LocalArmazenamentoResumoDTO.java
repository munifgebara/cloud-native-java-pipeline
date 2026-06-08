package br.com.munif.stella.api.dto;

import java.util.UUID;

public record LocalArmazenamentoResumoDTO(
        UUID id,
        String nome,
        String descricao,
        UUID paiId,
        String paiNome,
        String caminho,
        int nivel,
        String imagemUrl,
        boolean ativa
) {}
