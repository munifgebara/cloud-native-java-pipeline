package br.com.stella.api.dto;

import java.util.List;

public record PhotoUploadSuggestionResponseDTO(
        List<PhotoUploadItemSuggestionDTO> itens,
        String mensagem
) {}
