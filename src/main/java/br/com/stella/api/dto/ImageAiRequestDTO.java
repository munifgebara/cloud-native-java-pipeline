package br.com.stella.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ImageAiRequestDTO(
        @NotBlank
        @Size(max = 150)
        String name,

        @Size(max = 150)
        String category,

        @Size(max = 500)
        String description
) {}
