package br.com.munif.stella.api.dto;

import java.time.Instant;
import java.util.List;

public record PessoaRevisaoDTO(
        Number revisao,
        Instant dataHora,
        String tipo,
        PessoaResponseDTO pessoa,
        List<String> camposAlterados
) {
}
