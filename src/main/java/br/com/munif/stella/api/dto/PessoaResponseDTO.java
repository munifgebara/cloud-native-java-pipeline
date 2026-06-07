package br.com.munif.stella.api.dto;

import java.time.Instant;
import java.util.UUID;

public record PessoaResponseDTO(
        UUID id,
        String nome,
        String cpfCnpj,
        String telefonePrincipal,
        String telefoneSecundario,
        String email,
        String cep,
        String endereco,
        String complemento,
        String bairro,
        String cidade,
        String uf,
        Instant criadoEm,
        Instant alteradoEm
) {}
