package br.com.munif.stella.api.dto;

import java.util.List;

public record CadastroFotoItemSugestaoDTO(
        String nome,
        String descricao,
        String categoriaSugerida,
        String marca,
        String modelo,
        Integer quantidade,
        String estadoConservacao,
        String observacoes,
        Double confianca,
        List<CadastroFotoInstanciaSugestaoDTO> instancias
) {}
