package br.com.munif.stella.api.dto;

public record ImagemIaResponseDTO(
        String dataUrl,
        String contentType,
        String provider
) {}
