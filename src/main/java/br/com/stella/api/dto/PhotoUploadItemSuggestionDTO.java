package br.com.stella.api.dto;

import java.util.List;

public record PhotoUploadItemSuggestionDTO(
        String name,
        String description,
        String suggestedCategory,
        String brand,
        String model,
        String author,
        String publisher,
        String publicationYear,
        String isbn,
        String source,
        Boolean identificationVerified,
        Integer quantity,
        String condition,
        String notes,
        Double confidence,
        List<PhotoUploadInstanceSuggestionDTO> instances
) {}
