package br.com.munif.stella.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EmprestimoItemResponseDTO(
        UUID id,
        UUID instanciaItemId,
        String instanciaIdentificacao,
        UUID pessoaId,
        String pessoaNome,
        Instant dataEmprestimo,
        LocalDate previsaoDevolucao,
        Instant dataDevolucao,
        String observacao
) {}
