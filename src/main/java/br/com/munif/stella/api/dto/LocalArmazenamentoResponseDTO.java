package br.com.munif.stella.api.dto;

import java.util.UUID;

public record LocalArmazenamentoResponseDTO(
        UUID id,
        String nome,
        String descricao,
        UUID paiId,
        String paiNome,
        String caminho,
        int nivel,
        String imagemUrl,
        String imagemContentType,
        Long imagemTamanhoBytes,
        boolean ativa
) {}
