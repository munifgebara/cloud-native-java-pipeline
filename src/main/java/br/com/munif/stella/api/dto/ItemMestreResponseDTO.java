package br.com.munif.stella.api.dto;

import java.util.UUID;

public record ItemMestreResponseDTO(
        UUID id,
        String nome,
        String descricao,
        String observacoes,
        String origemCadastro,
        UUID categoriaId,
        String categoriaNome,
        String categoriaIcone,
        String imagemUrl,
        String imagemContentType,
        Long imagemTamanhoBytes,
        boolean ativa
) {}
