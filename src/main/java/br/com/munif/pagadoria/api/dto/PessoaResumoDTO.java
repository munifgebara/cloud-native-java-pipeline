package br.com.munif.pagadoria.api.dto;

import java.util.UUID;

public record PessoaResumoDTO(
        UUID id,
        String nome
) {
}
