package br.com.stella.api.dto;

public record PhotoUploadInstanceSuggestionDTO(
        String identifier,
        String assetTag,
        String serialNumber,
        String estadoConservacao,
        String notes,
        Double confianca
) {}
