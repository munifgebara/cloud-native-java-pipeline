package br.com.munif.pagadoria.api.dto;

import java.util.UUID;

public record PessoaResponseDTO(
        UUID id,
        String nome,
        String cpfCnpj
) {
}
