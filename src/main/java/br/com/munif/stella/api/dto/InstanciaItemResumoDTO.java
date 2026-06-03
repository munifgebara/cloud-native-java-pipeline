package br.com.munif.stella.api.dto;

import br.com.munif.stella.api.entity.StatusOperacionalInstancia;

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
        StatusOperacionalInstancia statusOperacional,
        boolean ativa
) {}
