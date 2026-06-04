package br.com.munif.stella.api.dto;

public record ImagemItemMestreDTO(
        String bucket,
        String objectKey,
        String contentType,
        Long tamanhoBytes
) {}
