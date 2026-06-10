package br.com.munif.stella.api.dto;

import java.util.List;

public record CadastroFotoItemSugestaoDTO(
        String nome,
        String descricao,
        String categoriaSugerida,
        String marca,
        String modelo,
        String autor,
        String editora,
        String anoPublicacao,
        String isbn,
        String fontePesquisa,
        Boolean identificacaoVerificada,
        Integer quantidade,
        String estadoConservacao,
        String observacoes,
        Double confianca,
        List<CadastroFotoInstanciaSugestaoDTO> instancias
) {}
