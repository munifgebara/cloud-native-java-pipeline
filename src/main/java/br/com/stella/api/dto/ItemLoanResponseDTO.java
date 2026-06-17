package br.com.stella.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ItemLoanResponseDTO(
        UUID id,
        UUID instanciaItemId,
        String instanciaIdentificacao,
        UUID pessoaId,
        String pessoaNome,
        Instant loanDate,
        LocalDate previsaoDevolucao,
        Instant dataDevolucao,
        String observacao
) {}
