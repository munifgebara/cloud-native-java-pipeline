package br.com.stella.api.dto;

import java.util.List;

public record CadastroFotoSugestaoResponseDTO(
        List<CadastroFotoItemSugestaoDTO> itens,
        String mensagem
) {}
