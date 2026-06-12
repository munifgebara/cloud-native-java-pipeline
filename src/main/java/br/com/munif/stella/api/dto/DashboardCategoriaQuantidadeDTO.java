package br.com.munif.stella.api.dto;

import java.util.UUID;

public record DashboardCategoriaQuantidadeDTO(
        UUID id,
        String nome,
        long quantidadeItens
) {
}
