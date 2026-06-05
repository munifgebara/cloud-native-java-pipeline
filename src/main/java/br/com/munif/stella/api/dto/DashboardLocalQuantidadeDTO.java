package br.com.munif.stella.api.dto;

import java.util.UUID;

public record DashboardLocalQuantidadeDTO(
        UUID id,
        String nome,
        long quantidadeInstancias
) {
}
