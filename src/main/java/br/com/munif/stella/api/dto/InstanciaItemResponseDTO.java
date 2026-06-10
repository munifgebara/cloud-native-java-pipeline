package br.com.munif.stella.api.dto;

import br.com.munif.stella.api.entity.StatusOperacionalInstancia;

import java.util.UUID;

public record InstanciaItemResponseDTO(
        UUID id,
        UUID itemMestreId,
        String itemMestreNome,
        UUID categoriaId,
        String categoriaNome,
        String categoriaIcone,
        UUID localAtualId,
        String localAtualNome,
        String identificador,
        String patrimonio,
        String numeroSerie,
        StatusOperacionalInstancia statusOperacional,
        String observacoes,
        String origemCadastro,
        boolean ativa
) {}
