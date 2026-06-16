package br.com.stella.api.dto;

import br.com.stella.api.entity.ItemInstanceStatus;

import java.util.UUID;

public record ConsultaSemanticaInstanciaDTO(
        UUID id,
        String identificador,
        String patrimonio,
        String numeroSerie,
        ItemInstanceStatus statusOperacional,
        UUID localAtualId,
        String localAtualNome
) {}
