package br.com.munif.comum.dto;

import java.time.Instant;

public record RevisaoDTO<T>(
        Number revisao,
        Instant dataHora,
        T entidade
) {
}
