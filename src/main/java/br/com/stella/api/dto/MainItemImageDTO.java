package br.com.stella.api.dto;

public record MainItemImageDTO(
        String bucket,
        String objectKey,
        String contentType,
        Long sizeBytes
) {}
