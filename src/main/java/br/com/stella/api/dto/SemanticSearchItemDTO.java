package br.com.stella.api.dto;

import java.util.List;
import java.util.UUID;

public record SemanticSearchItemDTO(
        UUID mainItemId,
        String name,
        String description,
        String categoryName,
        String categoryIcon,
        String imageUrl,
        double similarity,
        List<SemanticSearchInstanceDTO> instances,
        List<SemanticSearchLocationDTO> probableLocations
) {}
