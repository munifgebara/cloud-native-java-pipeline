package br.com.stella.api.dto;

import java.util.UUID;

public record DashboardCategoriaQuantidadeDTO(
        UUID id,
        String nome,
        long quantidadeItens
) {
}
