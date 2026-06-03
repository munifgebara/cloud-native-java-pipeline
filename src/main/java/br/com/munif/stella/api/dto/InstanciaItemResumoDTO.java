package br.com.munif.stella.api.dto;

import java.util.UUID;

public record InstanciaItemResumoDTO(
        UUID id,
        UUID itemMestreId,
        String itemMestreNome,
        String categoriaNome,
        String categoriaIcone,
        String identificador,
        String patrimonio,
        String numeroSerie,
        boolean ativa
) {}
