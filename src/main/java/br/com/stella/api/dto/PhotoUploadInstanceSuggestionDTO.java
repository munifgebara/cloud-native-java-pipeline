package br.com.stella.api.dto;

public record PhotoUploadInstanceSuggestionDTO(
        String identificador,
        String patrimonio,
        String numeroSerie,
        String estadoConservacao,
        String observacoes,
        Double confianca
) {}
