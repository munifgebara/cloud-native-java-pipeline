package br.com.stella.api.dto;

public record CadastroFotoInstanciaSugestaoDTO(
        String identificador,
        String patrimonio,
        String numeroSerie,
        String estadoConservacao,
        String observacoes,
        Double confianca
) {}
