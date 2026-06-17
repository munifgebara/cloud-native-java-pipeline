package br.com.stella.api.dto;

import java.util.UUID;

public record SemanticSearchLocationDTO(
        UUID id,
        String name,
        long quantity
) {}
