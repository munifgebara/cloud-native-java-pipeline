package br.com.munif.stella.api.dto;

import br.com.munif.stella.api.entity.StatusOperacionalInstancia;

import java.util.UUID;

public record ConsultaSemanticaInstanciaDTO(
        UUID id,
        String identificador,
        String patrimonio,
        String numeroSerie,
        StatusOperacionalInstancia statusOperacional,
        UUID localAtualId,
        String localAtualNome
) {}
