package br.com.munif.stella.api.dto;

import java.util.UUID;

public record ConsultaSemanticaLocalDTO(
        UUID id,
        String nome,
        long quantidade
) {}
