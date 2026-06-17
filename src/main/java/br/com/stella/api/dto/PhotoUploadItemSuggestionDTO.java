package br.com.stella.api.dto;

import java.util.List;

public record PhotoUploadItemSuggestionDTO(
        String name,
        String description,
        String suggestedCategory,
        String marca,
        String modelo,
        String autor,
        String editora,
        String anoPublicacao,
        String isbn,
        String fontePesquisa,
        Boolean identificacaoVerificada,
        Integer quantity,
        String estadoConservacao,
        String notes,
        Double confianca,
        List<PhotoUploadInstanceSuggestionDTO> instances
) {}
