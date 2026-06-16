package br.com.stella.api.dto;

public record ImageAiResponseDTO(
        String dataUrl,
        String contentType,
        String provider
) {}
