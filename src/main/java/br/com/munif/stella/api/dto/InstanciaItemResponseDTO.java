package br.com.munif.stella.api.dto;

import java.util.UUID;

public record InstanciaItemResponseDTO(
        UUID id,
        UUID itemMestreId,
        String itemMestreNome,
        UUID categoriaId,
        String categoriaNome,
        String categoriaIcone,
        String identificador,
        String patrimonio,
        String numeroSerie,
        String observacoes,
        boolean ativa
) {}
