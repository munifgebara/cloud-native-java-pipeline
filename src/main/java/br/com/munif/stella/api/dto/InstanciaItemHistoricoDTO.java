package br.com.munif.stella.api.dto;

import java.util.List;

public record InstanciaItemHistoricoDTO(
        InstanciaItemResponseDTO instancia,
        List<MovimentacaoItemResponseDTO> movimentacoes
) {}
