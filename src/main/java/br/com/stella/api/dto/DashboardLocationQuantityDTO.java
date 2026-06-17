package br.com.stella.api.dto;

import java.util.UUID;

public record DashboardLocationQuantityDTO(
        UUID id,
        String name,
        long instanceCount
) {
}
