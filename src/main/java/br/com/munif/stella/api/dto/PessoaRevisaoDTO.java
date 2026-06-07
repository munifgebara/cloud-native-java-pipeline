package br.com.munif.stella.api.dto;

import java.time.Instant;

public record PessoaRevisaoDTO(
        Number revisao,
        Instant dataHora,
        String tipo,
        PessoaResponseDTO pessoa
) {
}
