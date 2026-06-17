package br.com.stella.api.dto;

import java.util.UUID;

public record DashboardCategoryQuantityDTO(
        UUID id,
        String nome,
        long quantidadeItens
) {
}
